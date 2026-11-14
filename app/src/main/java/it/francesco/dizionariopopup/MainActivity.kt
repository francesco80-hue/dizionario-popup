package it.francesco.dizionariopopup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private var overlayGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overlayGranted = Settings.canDrawOverlays(this)
        setContent {
            MaterialTheme {
                var query by remember { mutableStateOf("") }
                val result = remember(query) { Dictionary.find(this, query) }
                Scaffold { padding ->
                    Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Dizionario Popup", style = MaterialTheme.typography.headlineLarge)
                        Text("Consulta le parole mentre leggi, senza uscire da JW Library. Dizionario offline: nessun account, pubblicità o connessione Internet.")
                        HorizontalDivider()
                        Text("PULSANTE MOBILE", style = MaterialTheme.typography.titleMedium)
                        Text(if (overlayGranted) "Permesso finestra mobile autorizzato." else "Per mostrare l'icona sopra JW Library serve il permesso Android 'Mostra sopra altre app'.")
                        Button(onClick = {
                            if (!Settings.canDrawOverlays(this@MainActivity)) {
                                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                            } else {
                                ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, FloatingButtonService::class.java))
                            }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (overlayGranted) "Attiva icona mobile" else "Concedi permesso")
                        }
                        OutlinedButton(onClick = {
                            stopService(Intent(this@MainActivity, FloatingButtonService::class.java))
                        }, modifier = Modifier.fillMaxWidth()) { Text("Disattiva icona mobile") }
                        Text("COME SI USA")
                        Text("1. In JW Library, tieni premuta la parola che vuoi cercare.")
                        Text("2. Tocca Copia nel menu.")
                        Text("3. Tocca il libro rosso: la scheda appare sopra la pagina.")
                        Text("4. Tocca Chiudi per continuare la lettura.")
                        Text("Puoi spostare il pulsante rosso trascinandolo sul bordo dello schermo.")
                        HorizontalDivider()
                        Text("RICERCA DI PROVA", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Cerca una parola") }, modifier = Modifier.fillMaxWidth())
                        if (result != null) {
                            Text(result.word, style = MaterialTheme.typography.headlineMedium)
                            Text(result.definition)
                            Text("Sinonimi: " + result.synonyms.joinToString(", "))
                            Text("Approfondimento: " + result.encyclopedia)
                        } else if (query.isNotBlank()) Text("Nessun risultato. Prova un'altra parola.")
                        Text("Le definizioni, i sinonimi e gli esempi dipendono dalle informazioni disponibili nella fonte del dizionario.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayGranted = Settings.canDrawOverlays(this)
    }
}
