package it.francesco.dizionariopopup

import android.app.Activity
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle

class ClipboardCaptureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val copied = if (clipboard.hasPrimaryClip()) {
            clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim().orEmpty()
        } else ""

        val show = Intent(this, FloatingButtonService::class.java).apply {
            action = FloatingButtonService.ACTION_SHOW_LOOKUP
            putExtra(FloatingButtonService.EXTRA_COPIED_TEXT, copied)
        }
        startService(show)
        finish()
        overridePendingTransition(0, 0)
    }
}
