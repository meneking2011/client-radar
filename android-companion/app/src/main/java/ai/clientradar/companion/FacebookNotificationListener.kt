package ai.clientradar.companion

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.util.concurrent.Executors

class FacebookNotificationListener : NotificationListenerService() {
    private val executor = Executors.newSingleThreadExecutor()
    private val facebookPackages = setOf("com.facebook.katana", "com.facebook.lite")

    override fun onCreate() {
        super.onCreate()
        createLeadNotificationChannel()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        CaptureState.setListenerConnected(this, true)
        Log.i("ClientRadar", "Facebook notification listener connected")
    }

    override fun onListenerDisconnected() {
        CaptureState.setListenerConnected(this, false)
        Log.w("ClientRadar", "Facebook notification listener disconnected")
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || sbn.packageName !in facebookPackages) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val parts = mutableListOf<String>()
        extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.let(parts::add)
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.let(parts::add)
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { parts.add(it.toString()) }
        val body = parts.map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString("\n")
        if (title.isBlank() && body.isBlank()) return

        CaptureState.recordReceived(this, title.ifBlank { body.take(140) })
        Log.i("ClientRadar", "Facebook notification received from ${sbn.packageName}")

        executor.execute {
            val token = try { SecurePrefs.token(this) } catch (error: Exception) { "" }
            if (token.isBlank()) {
                CaptureState.recordError(this, "Phone is not paired")
                return@execute
            }
            try {
                // Send every Facebook notification. The server applies the workspace's
                // current keywords and records non-matches for reliable diagnostics.
                val result = ApiClient.sendNotification(
                    token,
                    sbn.packageName,
                    title,
                    body.ifBlank { title }
                )
                val matched = result.optBoolean("matched", false)
                val keyword = result.optString("keyword", "")
                CaptureState.recordSync(this, matched, keyword)
                if (matched) showNewClientAlert(title, body, keyword)
            } catch (error: Exception) {
                CaptureState.recordError(this, error.message ?: "Network request failed")
                Log.w("ClientRadar", "Notification sync failed", error)
            }
        }
    }

    private fun createLeadNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                LEAD_CHANNEL,
                "New Client Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a Facebook notification matches your ClientRadar keywords"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNewClientAlert(title: String, body: String, keyword: String) {
        val manager = getSystemService(NotificationManager::class.java)
        val openApp = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            7101,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val detail = title.ifBlank { body }.take(150)
        val expanded = buildString {
            append(detail)
            if (keyword.isNotBlank()) append("\nMatched keyword: ").append(keyword)
        }
        val notification = Notification.Builder(this, LEAD_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New client found!")
            .setContentText(detail.ifBlank { "A Facebook post matched your ClientRadar keywords." })
            .setStyle(Notification.BigTextStyle().bigText(expanded))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .setPriority(Notification.PRIORITY_HIGH)
            .build()
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    override fun onDestroy() {
        CaptureState.setListenerConnected(this, false)
        executor.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val LEAD_CHANNEL = "clientradar_new_clients"
    }
}