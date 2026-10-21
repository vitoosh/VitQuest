package io.github.vitoosh.vitquest.core.data.backup

import android.content.Context
import android.database.SQLException
import android.net.Uri
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vitoosh.vitquest.core.data.local.DatabaseMigrations
import io.github.vitoosh.vitquest.core.data.local.VitQuestDatabase
import io.github.vitoosh.vitquest.core.data.photos.FoodPhotoStore
import io.github.vitoosh.vitquest.core.data.preferences.UserPreferencesDataSource
import io.github.vitoosh.vitquest.core.data.repository.ReminderScheduler
import io.github.vitoosh.vitquest.core.data.seed.SeedDataLoader
import io.github.vitoosh.vitquest.core.time.TimeSource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Base64
import java.util.concurrent.Callable
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/** Why a backup could not be restored. In every case the data on the phone is left as it was. */
enum class RestoreError { UNREADABLE, NOT_A_BACKUP, NEWER_APP, DAMAGED }

sealed interface RestoreResult {
    data object Restored : RestoreResult
    data class Failed(val error: RestoreError) : RestoreResult
}

/** Local backup to a file the user picks. Nothing is uploaded anywhere by the app. */
interface BackupRepository {
    suspend fun exportTo(uri: Uri): Boolean
    suspend fun restoreFrom(uri: Uri): RestoreResult
}

@Singleton
class LocalBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: VitQuestDatabase,
    private val preferences: UserPreferencesDataSource,
    private val seedDataLoader: SeedDataLoader,
    private val reminderScheduler: ReminderScheduler,
    private val photoStore: FoodPhotoStore,
    private val timeSource: TimeSource,
) : BackupRepository {

    private val mutex = Mutex()

    override suspend fun exportTo(uri: Uri): Boolean = try {
        withContext(Dispatchers.IO) {
            // The picker creates a new, empty document, so plain "w" is enough (and supported by more providers).
            val output = context.contentResolver.openOutputStream(uri, "w") ?: throw IOException("No output stream")
            output.use { export(it) }
        }
        true
    } catch (_: IOException) {
        false
    } catch (_: SecurityException) {
        false
    } catch (_: SQLException) {
        false
    }

    override suspend fun restoreFrom(uri: Uri): RestoreResult = try {
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { input -> restore(input) }
        } ?: RestoreResult.Failed(RestoreError.UNREADABLE)
    } catch (_: IOException) {
        RestoreResult.Failed(RestoreError.UNREADABLE)
    } catch (_: SecurityException) {
        RestoreResult.Failed(RestoreError.UNREADABLE)
    }

    suspend fun export(output: OutputStream) = mutex.withLock {
        // Read in one transaction so the copy is consistent even while steps are being recorded.
        val tables = withContext(Dispatchers.IO) {
            database.runInTransaction(Callable { BackupTables.read(database.openHelper.writableDatabase) })
        }
        val snapshot = BackupSnapshot(VitQuestDatabase.VERSION, tables, preferences.backup(), withContext(Dispatchers.IO) { photoFiles(tables) })
        withContext(Dispatchers.IO) {
            output.writer(Charsets.UTF_8).let { writer ->
                writer.write(BackupFormat.encode(snapshot, timeSource.now()))
                writer.flush()
            }
        }
    }

    suspend fun restore(input: InputStream): RestoreResult = mutex.withLock {
        val backup = try {
            withContext(Dispatchers.IO) {
                val backup = BackupFormat.decode(readLimited(input))
                val current = when {
                    backup.databaseVersion > VitQuestDatabase.VERSION -> throw BackupException(RestoreError.NEWER_APP)
                    backup.databaseVersion < VitQuestDatabase.VERSION -> upgrade(backup)
                    else -> backup
                }
                // Photos are written to a side folder first and only swapped in once the records are in.
                val staged = stagePhotos(backup.files, photoIds(current.tables))
                try {
                    // One transaction: if any row does not fit, nothing on the phone changes.
                    database.runInTransaction { BackupTables.replace(database.openHelper.writableDatabase, current.tables) }
                } catch (error: Throwable) {
                    staged.deleteRecursively()
                    throw error
                }
                swapInPhotos(staged)
                backup
            }
        } catch (error: BackupException) {
            return@withLock RestoreResult.Failed(error.error)
        } catch (_: IOException) {
            return@withLock RestoreResult.Failed(RestoreError.UNREADABLE)
        } catch (_: SQLException) {
            // Rolled back with the transaction.
            return@withLock RestoreResult.Failed(RestoreError.DAMAGED)
        }
        // The records are in; settings, the seed top-up and reminders follow them.
        preferences.restore(backup.preferences)
        // A backup from before a food data update gets the newer built-in foods and recipes.
        seedDataLoader.ensureLoaded()
        reminderScheduler.rescheduleAll()
        RestoreResult.Restored
    }

    private fun photoIds(tables: Map<String, BackupRows>): Set<Long> =
        tables[PHOTO_TABLE].orEmpty().mapNotNull { it["id"] as? Long }.toSet()

    private fun photoFiles(tables: Map<String, BackupRows>): Map<String, String> =
        photoIds(tables).mapNotNull { id ->
            val file = photoStore.file(id)
            if (file.isFile) FoodPhotoStore.backupPath(id) to Base64.getEncoder().encodeToString(file.readBytes()) else null
        }.toMap()

    /** Only food photos that a restored row points at are accepted; anything else makes the file damaged. */
    private fun stagePhotos(files: Map<String, String>, knownIds: Set<Long>): File {
        val staging = File(photoStore.directory.parentFile, "${FoodPhotoStore.DIRECTORY}.restore").apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            files.forEach { (path, content) ->
                val id = FoodPhotoStore.idFromBackupPath(path)?.takeIf { it in knownIds } ?: throw BackupException(RestoreError.DAMAGED)
                File(staging, "$id.jpg").writeBytes(Base64.getDecoder().decode(content))
            }
        } catch (error: IllegalArgumentException) {
            staging.deleteRecursively()
            throw BackupException(RestoreError.DAMAGED, error)
        } catch (error: BackupException) {
            staging.deleteRecursively()
            throw error
        }
        return staging
    }

    private fun swapInPhotos(staged: File) {
        photoStore.directory.deleteRecursively()
        if (!staged.renameTo(photoStore.directory)) {
            // Different storage paths should not happen inside app storage, but copy if they do.
            staged.copyRecursively(photoStore.directory, overwrite = true)
            staged.deleteRecursively()
        }
    }

    private fun readLimited(input: InputStream): String {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(chunk)
            if (read < 0) break
            buffer.write(chunk, 0, read)
            if (buffer.size() > MAX_BYTES) throw BackupException(RestoreError.DAMAGED)
        }
        return buffer.toString(Charsets.UTF_8.name())
    }

    /**
     * Brings a backup from an older app version up to date with the same migrations
     * the app uses: the rows are written into a scratch database built from that
     * version's schema, Room migrates it, and the result is read back.
     */
    private fun upgrade(backup: BackupSnapshot): BackupSnapshot = try {
        migrate(backup)
    } catch (error: IOException) {
        // No schema for that version: the file claims a version that never existed.
        throw BackupException(RestoreError.DAMAGED, error)
    } catch (error: JSONException) {
        throw BackupException(RestoreError.DAMAGED, error)
    } catch (error: SQLException) {
        // The old rows did not fit the old schema, or a migration step rejected them.
        throw BackupException(RestoreError.DAMAGED, error)
    } catch (error: IllegalStateException) {
        // Room found no migration path, or the migrated tables did not validate.
        throw BackupException(RestoreError.DAMAGED, error)
    }

    private fun migrate(backup: BackupSnapshot): BackupSnapshot {
        val schema = context.assets.open("$SCHEMA_DIR/${backup.databaseVersion}.json").bufferedReader().use {
            JSONObject(it.readText()).getJSONObject("database")
        }
        context.deleteDatabase(SCRATCH_NAME)
        try {
            val helper = FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(SCRATCH_NAME)
                    .callback(SchemaCallback(backup.databaseVersion, schema))
                    .build(),
            )
            helper.writableDatabase.let { db ->
                db.beginTransaction()
                try {
                    BackupTables.replace(db, backup.tables)
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
            helper.close()
            val scratch = Room.databaseBuilder(context, VitQuestDatabase::class.java, SCRATCH_NAME)
                .addMigrations(*DatabaseMigrations.ALL)
                .build()
            return try {
                backup.copy(databaseVersion = VitQuestDatabase.VERSION, tables = BackupTables.read(scratch.openHelper.writableDatabase))
            } finally {
                scratch.close()
            }
        } finally {
            context.deleteDatabase(SCRATCH_NAME)
        }
    }

    /** Creates tables exactly as [version] shipped them, from the schema Room exported at build time. */
    private class SchemaCallback(version: Int, private val schema: JSONObject) : SupportSQLiteOpenHelper.Callback(version) {
        override fun onCreate(db: SupportSQLiteDatabase) {
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace(TABLE_NAME, table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace(TABLE_NAME, table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }

    private companion object {
        // Leaves room for about a thousand small food photo copies.
        const val MAX_BYTES = 64 * 1024 * 1024
        const val PHOTO_TABLE = "food_photos"
        const val SCRATCH_NAME = "vitquest-restore-scratch.db"
        const val SCHEMA_DIR = "io.github.vitoosh.vitquest.core.data.local.VitQuestDatabase"
        const val TABLE_NAME = "\${TABLE_NAME}"
    }
}
