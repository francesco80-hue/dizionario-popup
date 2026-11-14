package it.francesco.dizionariopopup

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object DictionaryUpdater {
    private const val RELEASE_API = "https://api.github.com/repos/francesco80-hue/dizionario-popup/releases/latest"
    private const val PREFS = "dictionary_update_settings"
    private const val KEY_VERSION = "installed_release_tag"
    private const val KEY_LAST_CHECK = "last_check_ms"

    fun autoCheck(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_CHECK, 0L)
        if (System.currentTimeMillis() - last < 24L * 60L * 60L * 1000L) return
        checkAndInstall(context.applicationContext, wifiOnly = true, showMessage = false)
    }

    fun checkManually(context: Context, wifiOnly: Boolean) {
        checkAndInstall(context.applicationContext, wifiOnly, showMessage = true)
    }

    private fun checkAndInstall(context: Context, wifiOnly: Boolean, showMessage: Boolean) {
        Thread {
            var message = "Non è stato possibile controllare gli aggiornamenti."
            try {
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val connection = URL(RELEASE_API).openConnection() as HttpURLConnection
                connection.connectTimeout = 12000
                connection.readTimeout = 20000
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "DizionarioPopup-Android")
                val release = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
                connection.disconnect()
                val tag = release.optString("tag_name")
                val assets = release.optJSONArray("assets") ?: throw IllegalStateException("Asset non trovato")
                var downloadUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.optString("name") == "dictionary.db") {
                        downloadUrl = asset.optString("browser_download_url")
                        break
                    }
                }
                if (tag.isBlank() || downloadUrl.isNullOrBlank()) throw IllegalStateException("Database non disponibile")
                prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
                if (tag == prefs.getString(KEY_VERSION, "")) {
                    message = "Il dizionario è già aggiornato."
                } else if (wifiOnly && !isOnWifi(context)) {
                    message = "Nuovo dizionario disponibile. Il download partirà quando sarai connesso al Wi-Fi."
                } else {
                    val file = File(context.cacheDir, "dictionary-update.db")
                    val download = URL(downloadUrl).openConnection() as HttpURLConnection
                    download.connectTimeout = 15000
                    download.readTimeout = 60000
                    download.setRequestProperty("User-Agent", "DizionarioPopup-Android")
                    download.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                    download.disconnect()
                    if (Dictionary.installDatabase(context, file)) {
                        prefs.edit().putString(KEY_VERSION, tag).apply()
                        message = "Dizionario aggiornato correttamente."
                    } else {
                        file.delete()
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
