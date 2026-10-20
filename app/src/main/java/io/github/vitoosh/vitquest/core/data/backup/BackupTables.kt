package io.github.vitoosh.vitquest.core.data.backup

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/** Reads and replaces whole tables with plain SQL, so new tables are included without extra code. */
object BackupTables {

    /**
     * Not backed up: SQLite and Room bookkeeping, and the step sensor checkpoint,
     * which only makes sense on the phone (and boot) that recorded it.
     */
    private val EXCLUDED = setOf("room_master_table", "android_metadata", "step_sensor_state")

    fun names(db: SupportSQLiteDatabase): List<String> =
        db.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }.filterNot { it in EXCLUDED }

    fun read(db: SupportSQLiteDatabase): Map<String, BackupRows> = names(db).associateWith { table ->
        db.query("SELECT * FROM `$table`").use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add((0 until cursor.columnCount).associate { i -> cursor.getColumnName(i) to cursor.value(i) })
                }
            }
        }
    }

    /**
     * Deletes every backed-up table and inserts [tables] instead. Must run inside a
     * transaction: any problem throws [BackupException] and the caller rolls back.
     */
    fun replace(db: SupportSQLiteDatabase, tables: Map<String, BackupRows>) {
        val known = names(db).associateWith { columns(db, it) }
        tables.forEach { (table, rows) ->
            val columns = known[table] ?: throw BackupException(RestoreError.DAMAGED)
            if (rows.any { !columns.containsAll(it.keys) }) throw BackupException(RestoreError.DAMAGED)
        }
        // Rows go in table by table, so references are only checked once everything is in.
        db.execSQL("PRAGMA defer_foreign_keys = ON")
        known.keys.forEach { db.execSQL("DELETE FROM `$it`") }
        try {
            tables.forEach { (table, rows) -> rows.forEach { db.insert(table, SQLiteDatabase.CONFLICT_ABORT, it.toContentValues()) } }
        } catch (error: android.database.SQLException) {
            throw BackupException(RestoreError.DAMAGED, error)
        }
        val broken = db.query("PRAGMA foreign_key_check").use { it.count }
        if (broken > 0) throw BackupException(RestoreError.DAMAGED)
    }

    private fun columns(db: SupportSQLiteDatabase, table: String): Set<String> =
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val name = cursor.getColumnIndexOrThrow("name")
            buildSet { while (cursor.moveToNext()) add(cursor.getString(name)) }
        }

    private fun Cursor.value(i: Int): Any? = when (getType(i)) {
        Cursor.FIELD_TYPE_NULL -> null
        Cursor.FIELD_TYPE_INTEGER -> getLong(i)
        Cursor.FIELD_TYPE_FLOAT -> getDouble(i)
        Cursor.FIELD_TYPE_STRING -> getString(i)
        else -> error("Blob columns are not backed up")
    }

    private fun Map<String, Any?>.toContentValues() = ContentValues(size).also { values ->
        forEach { (column, value) ->
            when (value) {
                null -> values.putNull(column)
                is Long -> values.put(column, value)
                is Double -> values.put(column, value)
                is String -> values.put(column, value)
                else -> throw BackupException(RestoreError.DAMAGED)
            }
        }
    }
}
