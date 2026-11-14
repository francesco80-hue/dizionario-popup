package it.francesco.dizionariopopup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var query by remember { mutableStateOf("perseveranza") }
                val result = remember(query) { Dictionary.find(this, query) }
                Scaffold { padding ->
                    Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Dizionario Popup", style = MaterialTheme.typography.headlineLarge)
                        Text("Prova di ricerca locale. Nessun account, pubblicità o permesso di Accessibilità.")
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
}
