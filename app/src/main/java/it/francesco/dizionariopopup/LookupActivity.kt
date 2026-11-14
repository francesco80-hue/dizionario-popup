package it.francesco.dizionariopopup

import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class LookupActivity : ComponentActivity() {
    private var shownForCurrentIntent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!intent.getBooleanExtra(EXTRA_READ_CLIPBOARD, false)) showLookup()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && intent.getBooleanExtra(EXTRA_READ_CLIPBOARD, false) && !shownForCurrentIntent) {
            shownForCurrentIntent = true
            showLookup()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shownForCurrentIntent = false
        if (!intent.getBooleanExtra(EXTRA_READ_CLIPBOARD, false)) showLookup()
    }

    private fun showLookup() {
        val selected = if (intent.getBooleanExtra(EXTRA_READ_CLIPBOARD, false)) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip()) clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim().orEmpty() else ""
        } else {
            intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()?.trim().orEmpty()
        }
        val word = selected.split(Regex("\\s+")).firstOrNull().orEmpty()
            .trim(' ', '.', ',', ';', ':', '!', '?', '“', '”', '"', '(', ')', '[', ']')
        val entry = Dictionary.find(this, word)
        setContent {
            MaterialTheme {
                Surface {
                    Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(entry?.word ?: word.ifBlank { "Nessuna parola copiata" }, style = MaterialTheme.typography.headlineMedium)
                        if (entry != null) {
                            Text(entry.pos, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text("VOCABOLARIO", style = MaterialTheme.typography.labelLarge)
                            Text(entry.definition)
                            if (entry.synonyms.isNotEmpty()) {
                                Text("Sinonimi", style = MaterialTheme.typography.titleSmall)
                                Text(entry.synonyms.joinToString(" · "))
                            }
                            if (entry.examples.isNotEmpty()) {
                                Text("Esempio", style = MaterialTheme.typography.titleSmall)
                                entry.examples.forEach { Text("“$it”") }
                            }
                            HorizontalDivider()
                            Text("APPROFONDIMENTO ENCICLOPEDICO", style = MaterialTheme.typography.labelLarge)
                            Text(entry.encyclopedia.ifBlank { "Nessun approfondimento enciclopedico disponibile per questa voce." })
                        } else {
                            Text(if (word.isBlank()) "Copia prima una singola parola in JW Library, poi tocca l'icona D." else "La parola '$word' non è ancora presente nel dizionario di prova.")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { finish() }) { Text("Chiudi") }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_READ_CLIPBOARD = "read_dictionary_clipboard"
    }
}
