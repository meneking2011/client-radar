package ai.clientradar.companion

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    private lateinit var permissionStatus: TextView
    private lateinit var codeInput: EditText
    private lateinit var connectButton: Button
    private var connected = false
    private var restoredPairingCode: String? = null

    private val navy = Color.rgb(9, 17, 31)
    private val panel = Color.rgb(18, 31, 49)
    private val cyan = Color.rgb(65, 210, 218)
    private val white = Color.rgb(239, 246, 252)
    private val muted = Color.rgb(160, 178, 196)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoredPairingCode = savedInstanceState?.getString("clientradar_pairing_code")
        window.statusBarColor = navy
        window.navigationBarColor = navy
        try {
            render()
        } catch (error: Throwable) {
            showStartupError(error)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::codeInput.isInitialized) {
            outState.putString("clientradar_pairing_code", codeInput.text.toString())
        }
        super.onSaveInstanceState(outState)
    }

    private fun showStartupError(error: Throwable) {
        android.util.Log.e("ClientRadar", "Main screen failed to start", error)
        val message = "ClientRadar could not finish opening.\\n\\n" +
            (error.javaClass.simpleName + ": " + (error.message ?: "Unknown startup error")) +
            "\\n\\nPlease share this message with support."
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(navy)
        }
        val heading = TextView(this).apply {
            text = "ClientRadar startup error"
            textSize = 22f
            setTextColor(white)
        }
        val details = TextView(this).apply {
            text = message
            textSize = 15f
            setTextColor(muted)
            setPadding(0, 20, 0, 0)
        }
        screen.addView(heading)
        screen.addView(details)
        setContentView(screen)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun cardBackground(color: Int = panel): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(18).toFloat()
        }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setLineSpacing(dp(3).toFloat(), 1f)
    }

    private fun add(parent: LinearLayout, view: View, bottom: Int = 12) {
        parent.addView(view, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(bottom) })
    }

    private fun makeButton(label: String, primary: Boolean, action: () -> Unit) =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 15f
            setTextColor(if (primary) Color.rgb(5, 24, 34) else white)
            background = cardBackground(if (primary) cyan else Color.rgb(35, 52, 72))
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setOnClickListener { action() }
        }

    private fun render() {
        val scroll = ScrollView(this).apply {
            setFillViewport(true)
            setBackgroundColor(navy)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
        }
        scroll.addView(root)
        setContentView(scroll)

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val mark = TextView(this).apply {
            text = "◉"
            textSize = 31f
            setTextColor(cyan)
            setPadding(0, 0, dp(12), 0)
        }
        val brandText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        add(brandText, text("CLIENTRADAR", 12f, cyan, true), 2)
        add(brandText, text("COMPANION", 22f, white, true), 0)
        brand.addView(mark)
        brand.addView(brandText)
        add(root, brand, 22)

        add(root, text(
            "Turn relevant Facebook notifications into opportunities in your ClientRadar workspace.",
            15f, muted
        ), 20)

        val connectionCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = cardBackground()
        }
        add(connectionCard, text("WORKSPACE CONNECTION", 12f, cyan, true), 10)
        add(connectionCard, text(
            "Generate a pairing code in your signed-in ClientRadar dashboard, then enter it here. Codes expire after five minutes.",
            14f, muted
        ), 14)
        codeInput = EditText(this).apply {
            hint = "Enter pairing code"
            setSingleLine(false)
            minLines = 2
            maxLines = 5
            gravity = Gravity.TOP or Gravity.START
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            setHorizontallyScrolling(false)
            isVerticalScrollBarEnabled = true
            textSize = 16f
            setTextColor(white)
            setHintTextColor(muted)
            setPadding(dp(14), dp(13), dp(14), dp(13))
            background = cardBackground(Color.rgb(29, 44, 63))
        }
        restoredPairingCode?.let { savedCode ->
            codeInput.setText(savedCode)
            codeInput.setSelection(codeInput.text.length)
        }
        add(connectionCard, codeInput, 12)
        connectButton = makeButton("Connect to ClientRadar", true) { connectDevice() }
        add(connectionCard, connectButton, 0)
        add(root, connectionCard, 14)

        val accessCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = cardBackground()
        }
        add(accessCard, text("NOTIFICATION ACCESS", 12f, cyan, true), 8)
        add(accessCard, text(
            "Allow ClientRadar Companion to read notifications so it can check Facebook and Facebook Lite alerts for your saved keywords.",
            14f, muted
        ), 10)
        permissionStatus = text("", 13f, white, true)
        add(accessCard, permissionStatus, 10)
        add(accessCard, makeButton("Open notification settings", false) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }, 0)
        add(root, accessCard, 14)

        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = cardBackground(Color.rgb(14, 43, 52))
        }
        add(statusCard, text("DEVICE STATUS", 12f, cyan, true), 8)
        status = text("Checking connection…", 14f, white)
        add(statusCard, status, 0)
        add(root, statusCard, 14)

        add(root, makeButton("Disconnect this phone", false) {
            try {
                SecurePrefs.clearToken(this)
                connected = false
                status.text = "Disconnected. Generate a new pairing code in ClientRadar to reconnect."
                Toast.makeText(this, "Phone disconnected.", Toast.LENGTH_SHORT).show()
            } catch (error: Exception) {
                status.text = "Could not clear the saved connection. Please try again."
            }
        }, 8)

        refreshStatus()
    }

    private fun hasNotificationAccess(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return enabled.split(":").any { it.contains(packageName) }
    }

    private fun refreshStatus() {
        connected = try {
            SecurePrefs.token(this).isNotBlank()
        } catch (_: Exception) {
            false
        }
        permissionStatus.text = if (hasNotificationAccess()) {
            "● Notification access is enabled"
        } else {
            "○ Notification access is not enabled"
        }
        status.text = when {
            connected && hasNotificationAccess() ->
                "Connected and ready. Matching Facebook notifications can be checked."
            connected ->
                "Phone paired. Enable notification access above to start capture."
            else ->
                "Not connected. Pair this phone with your ClientRadar workspace first."
        }
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) {
            try {
                refreshStatus()
            } catch (error: Throwable) {
                showStartupError(error)
            }
        }
    }

    private fun connectDevice() {
        // Ignore whitespace and line breaks added by wrapping or pasting.
        val code = codeInput.text.toString().replace(Regex("\\s+"), "")
        if (!code.matches(Regex("(?i)^[a-z0-9_-]{8,128}$"))) {
            Toast.makeText(this, "Enter the pairing code shown in ClientRadar.", Toast.LENGTH_LONG).show()
            return
        }

        (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(codeInput.windowToken, 0)
        connectButton.isEnabled = false
        status.text = "Connecting securely…"

        executor.execute {
            try {
                val token = ApiClient.exchangeCode(code)
                SecurePrefs.saveToken(this, token)
                runOnUiThread {
                    codeInput.text.clear()
                    connectButton.isEnabled = true
                    refreshStatus()
                    Toast.makeText(this, "ClientRadar connected.", Toast.LENGTH_LONG).show()
                }
            } catch (error: Exception) {
                runOnUiThread {
                    connectButton.isEnabled = true
                    val details = error.message.orEmpty()
                    status.text = if (details.contains("DOCTYPE", ignoreCase = true) ||
                        details.contains("<html", ignoreCase = true)) {
                        "Connection failed: ClientRadar returned a web page instead of the pairing API response. This points to a server/API response issue, not necessarily an invalid code. Check your internet connection and try again."
                    } else {
                        "Connection failed: ${details.ifBlank { "Please try again." }.take(300)}"
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
