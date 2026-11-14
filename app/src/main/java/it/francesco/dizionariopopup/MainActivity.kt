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
                var query by remember { mutableStateOf("perseveranza") }
                val result = remember(query) { Dictionary.find(this, query) }
                Scaffold { padding ->
                    Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Dizionario Popup", style = MaterialTheme.typography.headlineLarge)
                        Text("Dizionario locale, senza account, pubblicità, Internet o Accessibilità.")
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
                        Text("Dopo aver copiato una parola in JW Library, tocca la D mobile per aprire la scheda del dizionario.")
                        HorizontalDivider()
                        Text("RICERCA DI PROVA", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Cerca una parola") }, modifier = Modifier.fillMaxWidth())
                        if (result != null) {
                            Text(result.word, style = MaterialTheme.typography.headlineMedium)
                            Text(result.definition)
                            Text("Sinonimi: " + result.synonyms.joinToString(", "))
                            Text("Approfondimento: " + result.encyclopedia)
                        } else Text("Nessun risultato nel dizionario dimostrativo.")
                        Text("Parole incluse nella demo: perseveranza, mitezza, integrità, discernimento, costanza, saggezza.", style = MaterialTheme.typography.bodySmall)
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
