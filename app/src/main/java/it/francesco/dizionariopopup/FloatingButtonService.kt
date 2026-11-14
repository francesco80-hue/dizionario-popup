package it.francesco.dizionariopopup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
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
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class FloatingButtonService : Service() {
    private var windowManager: WindowManager? = null
    private var bubble: TextView? = null
    private var popup: View? = null
    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var dragged = false

    private val ink = Color.rgb(48, 43, 51)
    private val muted = Color.rgb(119, 111, 121)
    private val accent = Color.rgb(190, 43, 65)
    private val paper = Color.rgb(255, 253, 250)
    private val soft = Color.rgb(249, 241, 241)

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
            alpha = 0.96f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accent)
                setStroke(dp(1), Color.argb(170, 255, 255, 255))
            }
            elevation = dp(6).toFloat()
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
                        val capture = Intent(this@FloatingButtonService, ClipboardCaptureActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        }
                        startActivity(capture)
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SHOW_LOOKUP) {
            showLookupPopup(intent.getStringExtra(EXTRA_COPIED_TEXT).orEmpty())
        }
        return START_STICKY
    }

    private fun showLookupPopup(copied: String) {
        val word = copied.split(Regex("\\s+")).firstOrNull().orEmpty()
            .trim(' ', '.', ',', ';', ':', '!', '?', '“', '”', '"', '(', ')', '[', ']')
        val entry = if (word.isNotBlank()) Dictionary.find(this, word) else null

        bubble?.visibility = View.GONE
        popup?.let { try { windowManager?.removeView(it) } catch (_: Exception) {} }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(14))
            background = rounded(paper, 24, Color.rgb(235, 225, 225))
            elevation = dp(16).toFloat()
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        brand.addView(label("DIZIONARIO POPUP", 10f, accent, true, 0))
        brand.addView(label("La parola, mentre leggi", 13f, muted, false, 2))
        header.addView(brand, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val close = TextView(this).apply {
            text = "✕"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(muted)
            background = rounded(soft, 30)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            contentDescription = "Chiudi"
            setOnClickListener { closeLookupPopup() }
        }
        header.addView(close)
        panel.addView(header)

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            clipToPadding = false
            setPadding(0, dp(12), 0, dp(2))
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        if (word.isBlank()) {
            val emptyCard = sectionCard()
            emptyCard.addView(label("Nessuna parola copiata", 21f, ink, true, 0))
            emptyCard.addView(label("Copia una parola in un'app e tocca il pulsante rosso del dizionario.", 14f, muted, false, 8))
            body.addView(emptyCard)
        } else {
            val wordCard = sectionCard()
            wordCard.addView(label(word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }, 27f, ink, true, 0))
            if (entry?.pos?.isNotBlank() == true) {
                wordCard.addView(label(entry.pos, 13f, accent, true, 4))
            }
            body.addView(wordCard)

            if (entry != null) {
                val definitionCard = sectionCard()
                definitionCard.addView(label("SIGNIFICATO", 10f, accent, true, 0))
                definitionCard.addView(label(entry.definition, 16f, ink, false, 7))
                body.addView(definitionCard)

                val synonymsCard = sectionCard()
                synonymsCard.addView(label("SINONIMI E TERMINI AFFINI", 10f, accent, true, 0))
                val synonymText = if (entry.synonyms.isNotEmpty()) {
                    entry.synonyms.distinct().take(12).joinToString("  ·  ")
                } else {
                    "Per questa voce non sono ancora disponibili sinonimi verificati."
                }
                synonymsCard.addView(label(synonymText, 14f, ink, false, 6))
                body.addView(synonymsCard)
                if (entry.examples.isNotEmpty()) {
                    val examplesCard = sectionCard()
                    examplesCard.addView(label(if (entry.examples.size == 1) "ESEMPIO" else "ESEMPI", 10f, accent, true, 0))
                    entry.examples.take(2).forEachIndexed { index, example ->
                        examplesCard.addView(label("“$example”", 14f, ink, false, if (index == 0) 6 else 10))
                    }
                    body.addView(examplesCard)
                }
            } else {
                val missingCard = sectionCard()
                missingCard.addView(label("Voce non presente nel dizionario offline", 14f, ink, true, 0))
                missingCard.addView(label("Puoi comunque cercarla nelle fonti online qui sotto.", 13f, muted, false, 5))
                body.addView(missingCard)
            }

            // Approfondimenti volutamente compatti e chiusi all'apertura.
            val sourcesCard = sectionCard()
            val sourceHeader = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(2), 0, dp(2))
            }
            val sourceTitle = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            sourceTitle.addView(label("APPROFONDISCI", 10f, accent, true, 0))
            sourceTitle.addView(label("Altre informazioni, solo se ti servono", 12f, muted, false, 3))
            sourceHeader.addView(sourceTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            val expandIcon = TextView(this).apply {
                text = "＋"
                textSize = 21f
                setTextColor(accent)
                gravity = Gravity.CENTER
            }
            sourceHeader.addView(expandIcon, LinearLayout.LayoutParams(dp(34), dp(34)))
            sourcesCard.addView(sourceHeader)

            val sourceLinks = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                setPadding(0, dp(8), 0, 0)
            }
            sourceLinks.addView(sourceRow("Google", "Cerca significato e utilizzi", "G") {
                openWeb("https://www.google.com/search?q=" + Uri.encode("$word significato"))
            })
            sourceLinks.addView(sourceRow("Wikipedia", "Cerca una voce enciclopedica", "W") {
                openWeb("https://it.wikipedia.org/w/index.php?search=" + Uri.encode(word))
            })
            sourceLinks.addView(sourceRow("Altre fonti", "Dizionari e risorse linguistiche", "↗") {
                openWeb("https://www.google.com/search?q=" + Uri.encode("site:treccani.it/vocabolario $word"))
            })
            sourcesCard.addView(sourceLinks)
            sourceHeader.setOnClickListener {
                val expanded = sourceLinks.visibility != View.VISIBLE
                sourceLinks.visibility = if (expanded) View.VISIBLE else View.GONE
                expandIcon.text = if (expanded) "−" else "＋"
            }
            body.addView(sourcesCard)
        }

        scroll.addView(body)
        panel.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(430)
        ))
        popup = panel
        val width = (resources.displayMetrics.widthPixels * 0.92f).toInt().coerceAtMost(dp(560))
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

    private fun sectionCard(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = rounded(Color.rgb(255, 255, 255), 16, Color.rgb(241, 234, 234))
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(9)
        layoutParams = lp
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean, top: Int): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = true
            setPadding(0, dp(top), 0, 0)
            setLineSpacing(dp(2).toFloat(), 1f)
        }

    private fun sourceRow(title: String, subtitle: String, mark: String, action: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(9), dp(2), dp(9))
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }
        val icon = TextView(this).apply {
            text = mark
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(accent)
            background = rounded(soft, 10)
        }
        row.addView(icon, LinearLayout.LayoutParams(dp(36), dp(36)))
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), 0, 0, 0)
            addView(label(title, 14f, ink, true, 0))
            addView(label(subtitle, 11f, muted, false, 3))
        }
        row.addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(TextView(this).apply {
            text = "↗"
            textSize = 17f
            setTextColor(muted)
        })
        return row
    }

    private fun openWeb(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Nessun browser disponibile per aprire il collegamento.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rounded(color: Int, radiusDp: Int, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
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
        const val ACTION_SHOW_LOOKUP = "it.francesco.dizionariopopup.SHOW_LOOKUP"
        const val EXTRA_COPIED_TEXT = "copied_text"
        private const val CHANNEL_ID = "dictionary_floating"
        private const val NOTIFICATION_ID = 7101
        private const val PREFS = "floating_button_settings"
    }
}
