package it.francesco.dizionariopopup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import java.io.File
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
    private const val DATABASE_NAME = "dictionary.db"

    @Synchronized
    fun find(context: Context, query: String): Entry? {
        val q = fold(query)
        if (q.isBlank()) return null
        val database = openDatabase(context)
        return try {
            findByKey(database, "SELECT id, word, pos, definition, synonyms, examples, encyclopedia FROM entries WHERE word_key = ? LIMIT 1", q)
                ?: findByKey(database, "SELECT e.id, e.word, e.pos, e.definition, e.synonyms, e.examples, e.encyclopedia FROM aliases a JOIN entries e ON e.id = a.entry_id WHERE a.alias_key = ? LIMIT 1", q)
                ?: findByKey(database, "SELECT id, word, pos, definition, synonyms, examples, encyclopedia FROM entries WHERE word_key LIKE ? ORDER BY word_key LIMIT 1", q.replace("%", "\\%").replace("_", "\\_") + "%")
        } finally {
            database.close()
        }
    }

    private fun openDatabase(context: Context): SQLiteDatabase {
        val target = context.getDatabasePath(DATABASE_NAME)
        if (!target.exists()) {
            target.parentFile?.let { if (!it.exists()) it.mkdirs() }
            context.assets.open(DATABASE_NAME).use { input ->
                target.outputStream().use { output -> input.copyTo(output, bufferSize = 32 * 1024) }
            }
        }
        return SQLiteDatabase.openDatabase(target.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }

    private fun findByKey(database: SQLiteDatabase, sql: String, key: String): Entry? {
        database.rawQuery(sql, arrayOf(key)).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return Entry(
                word = cursor.getString(1),
                pos = cursor.getString(2),
                definition = cursor.getString(3),
                synonyms = parseList(cursor.getString(4)),
                examples = parseList(cursor.getString(5)),
                encyclopedia = cursor.getString(6),
                aliases = emptyList()
            )
        }
    }

    private fun parseList(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList() else {
            val array = JSONArray(value)
            (0 until array.length()).map { array.getString(it) }
        }

    private fun fold(value: String): String =
        Normalizer.normalize(value.trim().lowercase(Locale.ITALIAN), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace('’', '\'')
            .trim()
}
