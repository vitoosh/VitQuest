package io.github.vitoosh.vitquest.core.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

/** Prepares a chosen photo: one copy for the AI (enough detail to recognise food), one small copy to keep. */
object ImagePrep {
    /** Longest side sent for analysis. */
    const val UPLOAD_MAX_SIDE = 1024
    const val UPLOAD_QUALITY = 85

    /** Longest side of the copy kept in the diary and in backups. */
    const val THUMBNAIL_MAX_SIDE = 320
    const val THUMBNAIL_QUALITY = 70

    data class Prepared(val upload: ByteArray, val thumbnail: ByteArray)

    /** Width and height that fit within [maxSide], keeping the aspect ratio and never enlarging. */
    fun targetSize(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
        val longest = max(width, height)
        if (longest <= maxSide) return width to height
        val scale = maxSide.toDouble() / longest
        return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
    }

    @Throws(IOException::class)
    fun prepare(context: Context, uri: Uri): Prepared {
        val bitmap = decode(context, uri, UPLOAD_MAX_SIDE)
        val upload = bitmap.toJpeg(UPLOAD_QUALITY)
        val (w, h) = targetSize(bitmap.width, bitmap.height, THUMBNAIL_MAX_SIDE)
        val small = bitmap.scale(w, h)
        return Prepared(upload, small.toJpeg(THUMBNAIL_QUALITY))
    }

    /** Decodes at most about [maxSide] pixels on the longest side; ImageDecoder also applies EXIF rotation. */
    @Throws(IOException::class)
    private fun decode(context: Context, uri: Uri, maxSide: Int): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val (w, h) = targetSize(info.size.width, info.size.height, maxSide)
                decoder.setTargetSize(w, h)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IOException("Cannot read image")
        val (w, h) = targetSize(decoded.width, decoded.height, maxSide)
        return if (w == decoded.width) decoded else decoded.scale(w, h)
    }

    private fun Bitmap.toJpeg(quality: Int): ByteArray =
        ByteArrayOutputStream().also { compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
}
