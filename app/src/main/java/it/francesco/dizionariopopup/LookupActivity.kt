package it.francesco.dizionariopopup

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val selected = intent.getCharSequenceExtra(android.content.Intent.EXTRA_PROCESS_TEXT)?.toString()?.trim().orEmpty()
        val entry = Dictionary.find(this, selected)
        setContent {
            MaterialTheme {
                Surface {
                    Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(entry?.word ?: selected.ifBlank { "Parola non riconosciuta" }, style = MaterialTheme.typography.headlineMedium)
                        if (entry != null) {
                            Text(entry.pos, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text("VOCABOLARIO", style = MaterialTheme.typography.labelLarge)
                            Text(entry.definition)
                            if (entry.synonyms.isNotEmpty()) { Text("Sinonimi", style = MaterialTheme.typography.titleSmall); Text(entry.synonyms.joinToString(" · ")) }
                            if (entry.examples.isNotEmpty()) { Text("Esempio", style = MaterialTheme.typography.titleSmall); entry.examples.forEach { Text("“$it”") } }
                            HorizontalDivider()
                            Text("APPROFONDIMENTO ENCICLOPEDICO", style = MaterialTheme.typography.labelLarge)
                            Text(entry.encyclopedia)
                        } else {
                            Text("Questa parola non è ancora presente nel dizionario di prova.")
                            Text("La versione completa utilizzerà un archivio italiano più ampio.")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = { finish() }) { Text("Chiudi") } }
                    }
                }
            }
        }
    }
}
