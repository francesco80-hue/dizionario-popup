package it.francesco.dizionariopopup

import android.content.Context
import org.json.JSONArray
import java.text.Normalizer
import java.util.Locale

data class Entry(val word: String, val pos: String, val definition: String, val synonyms: List<String>, val examples: List<String>, val encyclopedia: String)

object Dictionary {
    private var entries: List<Entry>? = null
    fun find(context: Context, query: String): Entry? {
        val all = entries ?: load(context).also { entries = it }
        val q = fold(query.trim())
        return all.firstOrNull { fold(it.word) == q } ?: all.firstOrNull { fold(it.word).startsWith(q) }
    }
    private fun load(context: Context): List<Entry> {
        val raw = context.assets.open("dictionary.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Entry(o.getString("word"), o.optString("pos"), o.getString("definition"), o.optJSONArray("synonyms").toStringList(), o.optJSONArray("examples").toStringList(), o.optString("encyclopedia"))
        }
    }
    private fun JSONArray?.toStringList(): List<String> = if (this == null) emptyList() else (0 until length()).map { getString(it) }
    private fun fold(s: String): String = Normalizer.normalize(s.lowercase(Locale.ITALIAN), Normalizer.Form.NFD).replace("\\p{Mn}+".toRegex(), "")
}
