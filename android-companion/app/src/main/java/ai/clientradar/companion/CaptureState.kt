package ai.clientradar.companion

import android.content.Context

object CaptureState {
    private const val PREFS = "clientradar_capture_state"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setListenerConnected(context: Context, connected: Boolean) {
        prefs(context).edit().putBoolean("listener_connected", connected).apply()
    }

    fun listenerConnected(context: Context): Boolean =
        prefs(context).getBoolean("listener_connected", false)

    fun recordReceived(context: Context, title: String) {
        val p = prefs(context)
        p.edit()
            .putInt("received_count", p.getInt("received_count", 0) + 1)
            .putLong("last_notification_at", System.currentTimeMillis())
            .putString("last_notification_title", title.take(140))
            .apply()
    }

    fun recordSync(context: Context, matched: Boolean, keyword: String = "") {
        val p = prefs(context)
        val edit = p.edit()
            .putLong("last_sync_at", System.currentTimeMillis())
            .putString("last_sync_status", if (matched) "Matched and saved" else "Received; no keyword match")
        if (matched) {
            edit.putInt("matched_count", p.getInt("matched_count", 0) + 1)
                .putLong("last_matched_at", System.currentTimeMillis())
                .putString("last_matched_keyword", keyword)
        }
        edit.apply()
    }

    fun recordError(context: Context, message: String) {
        prefs(context).edit()
            .putLong("last_sync_at", System.currentTimeMillis())
            .putString("last_sync_status", "Sync failed: " + message.take(160))
            .apply()
    }

    fun receivedCount(context: Context) = prefs(context).getInt("received_count", 0)
    fun matchedCount(context: Context) = prefs(context).getInt("matched_count", 0)
    fun lastNotificationAt(context: Context) = prefs(context).getLong("last_notification_at", 0L)
    fun lastNotificationTitle(context: Context) = prefs(context).getString("last_notification_title", "").orEmpty()
    fun lastSyncAt(context: Context) = prefs(context).getLong("last_sync_at", 0L)
    fun lastSyncStatus(context: Context) = prefs(context).getString("last_sync_status", "No Facebook notification checked yet").orEmpty()
    fun lastMatchedAt(context: Context) = prefs(context).getLong("last_matched_at", 0L)
    fun lastMatchedKeyword(context: Context) = prefs(context).getString("last_matched_keyword", "").orEmpty()
}