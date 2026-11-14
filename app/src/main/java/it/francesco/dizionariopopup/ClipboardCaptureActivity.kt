package it.francesco.dizionariopopup

import android.app.Activity
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle

class ClipboardCaptureActivity : Activity() {
    private var clipboardHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La lettura degli appunti viene rimandata a quando questa Activity
        // trasparente è realmente in primo piano: Android limita l'accesso
        // alla clipboard alle app visibili o che hanno il focus.
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !clipboardHandled) {
            clipboardHandled = true
            // Esegui dopo il completamento del cambio di focus della finestra.
            window.decorView.post { captureClipboardAndClose() }
        }
    }

    private fun captureClipboardAndClose() {
        val copied = try {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip()) {
                clipboard.primaryClip
                    ?.getItemAt(0)
                    ?.coerceToText(this)
                    ?.toString()
                    ?.trim()
                    .orEmpty()
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }

        val show = Intent(this, FloatingButtonService::class.java).apply {
            action = FloatingButtonService.ACTION_SHOW_LOOKUP
            putExtra(FloatingButtonService.EXTRA_COPIED_TEXT, copied)
        }
        startService(show)
        finish()
        overridePendingTransition(0, 0)
    }
}
