package it.francesco.dizionariopopup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class FloatingButtonService : Service() {
    private var windowManager: WindowManager? = null
    private var bubble: TextView? = null
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

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val params = WindowManager.LayoutParams(
            dp(44), dp(44), type,
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
                        openLookup()
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

    private fun openLookup() {
        val intent = Intent(this, LookupActivity::class.java).apply {
            putExtra(LookupActivity.EXTRA_READ_CLIPBOARD, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Dizionario Popup", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        bubble?.let { try { windowManager?.removeView(it) } catch (_: Exception) {} }
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
