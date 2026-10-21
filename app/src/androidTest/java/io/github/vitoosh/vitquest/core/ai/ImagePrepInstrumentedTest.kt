package io.github.vitoosh.vitquest.core.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** The ImageDecoder path, which Robolectric cannot run. */
@RunWith(AndroidJUnit4::class)
class ImagePrepInstrumentedTest {

    @Test
    fun aLargePhotoIsScaledForUploadAndForTheKeptCopy() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "prep-test.jpg")
        val source = Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(200, 150, 90)) }
        file.outputStream().use { source.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        try {
            val prepared = ImagePrep.prepare(context, Uri.fromFile(file))
            val upload = BitmapFactory.decodeByteArray(prepared.upload, 0, prepared.upload.size)
            val thumbnail = BitmapFactory.decodeByteArray(prepared.thumbnail, 0, prepared.thumbnail.size)
            assertEquals(1024 to 768, upload.width to upload.height)
            assertEquals(320 to 240, thumbnail.width to thumbnail.height)
        } finally {
            file.delete()
        }
    }
}
