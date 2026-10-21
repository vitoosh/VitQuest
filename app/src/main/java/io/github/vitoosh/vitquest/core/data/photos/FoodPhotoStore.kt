package io.github.vitoosh.vitquest.core.data.photos

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Small food photo copies in app-private storage, one file per photo id. */
@Singleton
class FoodPhotoStore @Inject constructor(@ApplicationContext context: Context) {

    val directory: File = File(context.filesDir, DIRECTORY)

    fun file(id: Long): File = File(directory, "$id.jpg")

    /** Written to a temporary file first, so a half-written photo is never shown. */
    @Throws(IOException::class)
    fun write(id: Long, jpeg: ByteArray) {
        directory.mkdirs()
        val temp = File(directory, "$id.jpg.tmp")
        temp.writeBytes(jpeg)
        if (!temp.renameTo(file(id))) {
            temp.delete()
            throw IOException("Could not save photo $id")
        }
    }

    fun delete(id: Long) {
        file(id).delete()
    }

    companion object {
        const val DIRECTORY = "food_photos"

        /** Path of a photo inside a backup file, relative to app storage. */
        fun backupPath(id: Long) = "$DIRECTORY/$id.jpg"

        /** The photo id for a backup path, or null if the path is not a food photo. */
        fun idFromBackupPath(path: String): Long? =
            Regex("""$DIRECTORY/(\d{1,18})\.jpg""").matchEntire(path)?.groupValues?.get(1)?.toLongOrNull()
    }
}
