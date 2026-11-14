package it.francesco.dizionariopopup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class FloatingButtonService : Service() {
    private var windowManager: WindowManager? = null
    private var bubble: TextView? = null
    private var popup: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var dragged = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dizionario Popup attivo")
            .setContentText("Tocca il pulsante mobile per cercare la parola copiata.")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        showBubble()
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun showBubble() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = TextView(this).apply {
            text = "📖"
            textSize = 21f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            alpha = 0.92f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(210, 45, 65))
                setStroke(dp(1), Color.argb(150, 255, 255, 255))
            }
            elevation = dp(5).toFloat()
        }
        bubble = view
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val params = WindowManager.LayoutParams(
            dp(44), dp(44), overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = prefs.getInt("bubble_x", dp(10))
            y = prefs.getInt("bubble_y", 0)
        }
        layoutParams = params

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    if (abs(dx) > ViewConfiguration.get(this).scaledTouchSlop ||
                        abs(dy) > ViewConfiguration.get(this).scaledTouchSlop) dragged = true
                    if (dragged) {
                        params.x = (startX - dx).coerceAtLeast(0)
                        params.y = startY + dy
                        try { windowManager?.updateViewLayout(view, params) } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (dragged) {
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putInt("bubble_x", params.x)
                            .putInt("bubble_y", params.y)
                            .apply()
                    } else {
                        showLookupPopup()
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
        try {
            windowManager?.addView(view, params)
        } catch (_: Exception) {
            stopSelf()
        }
    }

    private fun showLookupPopup() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val copied = if (clipboard.hasPrimaryClip()) {
            clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim().orEmpty()
        } else ""
        val word = copied.split(Regex("\\s+")).firstOrNull().orEmpty()
            .trim(' ', '.', ',', ';', ':', '!', '?', '“', '”', '"', '(', ')', '[', ']')
        val entry = if (word.isNotBlank()) Dictionary.find(this, word) else null

        bubble?.visibility = View.GONE
        popup?.let { try { windowManager?.removeView(it) } catch (_: Exception) {} }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(14))
            background = GradientDrawable().apply {
                setColor(Color.rgb(255, 248, 252))
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.rgb(225, 215, 225))
            }
            elevation = dp(12).toFloat()
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "Dizionario Popup"
            textSize = 18f
            setTextColor(Color.rgb(55, 45, 60))
        }
        header.addView(title, LinearLayout.LayoutParams(0, dp(44), 1f))
        val close = Button(this).apply {
            text = "Chiudi"
            setTextColor(Color.rgb(175, 35, 60))
            setOnClickListener { closeLookupPopup() }
        }
        header.addView(close)
        panel.addView(header)

        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(8))
        }
        fun addText(value: String, size: Float, color: Int = Color.rgb(45, 40, 48), bold: Boolean = false, top: Int = 8) {
            val text = TextView(this).apply {
                this.text = value
                textSize = size
                setTextColor(color)
                if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, dp(top), 0, dp(3))
            }
            body.addView(text)
        }

        if (entry != null) {
            addText(entry.word, 27f, Color.rgb(45, 40, 48), true, 4)
            if (entry.pos.isNotBlank()) addText(entry.pos, 13f, Color.rgb(130, 75, 145), false, 2)
            addText("VOCABOLARIO", 14f, Color.rgb(65, 55, 65), true, 14)
            addText(entry.definition, 17f)
            if (entry.synonyms.isNotEmpty()) {
                addText("Sinonimi", 15f, Color.rgb(65, 55, 65), true, 12)
                addText(entry.synonyms.joinToString(" · "), 16f)
            }
            if (entry.examples.isNotEmpty()) {
                addText("Esempio", 15f, Color.rgb(65, 55, 65), true, 12)
                entry.examples.forEach { addText("“$it”", 16f) }
            }
            addText("APPROFONDIMENTO ENCICLOPEDICO", 14f, Color.rgb(65, 55, 65), true, 16)
            addText(entry.encyclopedia.ifBlank { "Nessun approfondimento enciclopedico disponibile per questa voce." }, 16f)
        } else {
            addText(word.ifBlank { "Nessuna parola copiata" }, 24f, Color.rgb(45, 40, 48), true, 8)
            addText(if (word.isBlank()) "Copia una parola in JW Library e tocca il libro rosso." else "La parola non è presente nel dizionario.", 16f)
        }

        scroll.addView(body)
        panel.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(360)
        ))
        popup = panel
        val width = (resources.displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(dp(560))
        val params = WindowManager.LayoutParams(
            width, WindowManager.LayoutParams.WRAP_CONTENT, overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }
        try {
            windowManager?.addView(panel, params)
        } catch (_: Exception) {
            popup = null
            bubble?.visibility = View.VISIBLE
        }
    }

    private fun closeLookupPopup() {
        popup?.let { try { windowManager?.removeView(it) } catch (_: Exception) {} }
        popup = null
        bubble?.visibility = View.VISIBLE
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Dizionario Popup", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        popup?.let { try { windowManager?.removeView(it) } catch (_: Exception) {} }
        bubble?.let { try { windowManager?.removeView(it) } catch (_: Exception) {} }
        popup = null
        bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "dictionary_floating"
        private const val NOTIFICATION_ID = 7101
        private const val PREFS = "floating_button_settings"
    }
}
