package it.francesco.dizionariopopup

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

object DictionaryUpdater {
    private const val MANIFEST_URL = "https://raw.githubusercontent.com/francesco80-hue/dizionario-popup/main/updates/dictionary-manifest.json"
    private const val PREFS = "dictionary_update_settings"
    private const val KEY_VERSION = "installed_release_tag"
    private const val KEY_LAST_CHECK = "last_check_ms"

    fun autoCheck(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_CHECK, 0L)
        if (System.currentTimeMillis() - last < 24L * 60L * 60L * 1000L) return
        checkAndInstall(context.applicationContext, prefs.getBoolean("wifi_only", true), false)
    }

    fun checkManually(context: Context, wifiOnly: Boolean) {
        checkAndInstall(context.applicationContext, wifiOnly, true)
    }

    private fun checkAndInstall(context: Context, wifiOnly: Boolean, showMessage: Boolean) {
        Thread {
            var message = "Non è stato possibile controllare gli aggiornamenti."
            try {
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val manifestConnection = URL(MANIFEST_URL).openConnection() as HttpURLConnection
                manifestConnection.connectTimeout = 12000
                manifestConnection.readTimeout = 20000
                manifestConnection.setRequestProperty("Cache-Control", "no-cache")
                val manifest = manifestConnection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
                manifestConnection.disconnect()
                val version = manifest.optString("version")
                val downloadUrl = manifest.optString("url")
                if (version.isBlank() || downloadUrl.isBlank()) throw IllegalStateException("Manifest non valido")
                prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
                if (version == prefs.getString(KEY_VERSION, "")) {
                    message = "Il dizionario è già aggiornato."
                } else if (wifiOnly && !isOnWifi(context)) {
                    message = "Nuovo dizionario disponibile. Il download partirà quando sarai connesso al Wi-Fi."
                } else {
                    val compressed = File(context.cacheDir, "dictionary-update.db.gz")
                    val unpacked = File(context.cacheDir, "dictionary-update.db")
                    val download = URL(downloadUrl).openConnection() as HttpURLConnection
                    download.connectTimeout = 15000
                    download.readTimeout = 120000
                    download.setRequestProperty("Cache-Control", "no-cache")
                    download.inputStream.use { input -> compressed.outputStream().use { output -> input.copyTo(output) } }
                    download.disconnect()
                    GZIPInputStream(compressed.inputStream()).use { input ->
                        unpacked.outputStream().use { output -> input.copyTo(output) }
                    }
                    compressed.delete()
                    if (Dictionary.installDatabase(context, unpacked)) {
                        prefs.edit().putString(KEY_VERSION, version).apply()
                        message = "Dizionario aggiornato correttamente."
                    } else {
                        unpacked.delete()
                        message = "Aggiornamento non valido: ho conservato il dizionario precedente."
                    }
                }
            } catch (_: Exception) {
                message = "Controllo non riuscito. Il dizionario attuale resta disponibile."
            }
            if (showMessage) android.os.Handler(context.mainLooper).post {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun isOnWifi(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
