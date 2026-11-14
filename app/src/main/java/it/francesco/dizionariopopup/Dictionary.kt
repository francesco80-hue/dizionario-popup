package it.francesco.dizionariopopup

import android.content.Context
import org.json.JSONArray
import java.text.Normalizer
import java.util.Locale

data class Entry(
    val word: String,
    val pos: String,
    val definition: String,
    val synonyms: List<String>,
    val examples: List<String>,
    val encyclopedia: String,
    val aliases: List<String> = emptyList()
)

object Dictionary {
    @Volatile private var entries: List<Entry>? = null

    fun find(context: Context, query: String): Entry? {
        val all = entries ?: synchronized(this) {
            entries ?: load(context).also { entries = it }
        }
        val q = fold(query.trim())
        if (q.isBlank()) return null
        return all.firstOrNull { fold(it.word) == q }
            ?: all.firstOrNull { entry -> entry.aliases.any { fold(it) == q } }
            ?: all.firstOrNull { fold(it.word).startsWith(q) }
    }

    private fun load(context: Context): List<Entry> {
        val raw = context.assets.open("dictionary.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Entry(
                word = o.getString("word"),
                pos = o.optString("pos"),
                definition = o.optString("definition"),
                synonyms = o.optJSONArray("synonyms").toStringList(),
                examples = o.optJSONArray("examples").toStringList(),
                encyclopedia = o.optString("encyclopedia"),
                aliases = o.optJSONArray("aliases").toStringList()
            )
        }
    }

    private fun JSONArray?.toStringList(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }

    private fun fold(s: String): String =
        Normalizer.normalize(s.lowercase(Locale.ITALIAN), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace('’', '\'')
            .trim()
}
