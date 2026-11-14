package it.francesco.dizionariopopup

import android.content.Context
import android.database.sqlite.SQLiteDatabase\nimport java.io.File
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
    private const val DATABASE_NAME = "dictionary.db"

    @Synchronized
    fun find(context: Context, query: String): Entry? {
        val q = fold(query)
        if (q.isBlank()) return null
        val database = openDatabase(context)
        return try {
            findByKey(database, "SELECT id, word, pos, definition, synonyms, examples, encyclopedia FROM entries WHERE word_key = ? LIMIT 1", q)
                ?: findByKey(database, "SELECT e.id, e.word, e.pos, e.definition, e.synonyms, e.examples, e.encyclopedia FROM aliases a JOIN entries e ON e.id = a.entry_id WHERE a.alias_key = ? LIMIT 1", q)
                ?: findByKey(database, "SELECT id, word, pos, definition, synonyms, examples, encyclopedia FROM entries WHERE word_key LIKE ? ESCAPE '\\' ORDER BY word_key LIMIT 1", q.replace("%", "\\%").replace("_", "\\_") + "%")
        } finally {
            database.close()
        }
    }

    @Synchronized\n    fun installDatabase(context: Context, downloaded: File): Boolean {\n        val check = SQLiteDatabase.openDatabase(downloaded.absolutePath, null, SQLiteDatabase.OPEN_READONLY)\n        try {\n            check.rawQuery("SELECT word_key FROM entries LIMIT 1", null).use { cursor ->\n                if (!cursor.moveToFirst()) return false\n            }\n        } finally {\n            check.close()\n        }\n        val target = context.getDatabasePath(DATABASE_NAME)\n        target.parentFile?.let { if (!it.exists()) it.mkdirs() }\n        val backup = File(target.parentFile, "$DATABASE_NAME.previous")\n        if (target.exists() && !target.renameTo(backup)) return false\n        if (!downloaded.renameTo(target)) {\n            if (backup.exists()) backup.renameTo(target)\n            return false\n        }\n        backup.delete()\n        return true\n    }\n\n    private fun openDatabase(context: Context): SQLiteDatabase {
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
