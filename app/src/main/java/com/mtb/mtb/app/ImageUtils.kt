package com.mtb.mtb.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

// The project's free Spark plan can't provision Cloud Storage, so product photos live
// directly in Firestore as Base64. This downsamples the picked image (pixel dimensions,
// not just JPEG quality) and compresses adaptively — starting at high quality and only
// stepping down if a specific photo is actually too large — to stay well under
// Firestore's 1MB-per-document limit without needlessly blurring every photo.
object ImageUtils {
    private const val MAX_DIMENSION_PX = 1280
    private const val INITIAL_JPEG_QUALITY = 80
    private const val MIN_JPEG_QUALITY = 35
    private const val QUALITY_STEP = 15
    // Base64 inflates raw bytes by ~33%, and the Firestore doc also carries id/reference/
    // weight/stock — this leaves comfortable headroom under the 1MB document cap.
    private const val MAX_JPEG_BYTES = 600 * 1024

    fun pickedImageToBase64(context: Context, uri: Uri): String? {
        val resolver = context.contentResolver

        // decodeStream() always returns null when inJustDecodeBounds is set (it only
        // measures, it doesn't decode) — so success here is judged by outWidth/outHeight,
        // not by the return value.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri) ?: return null
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= MAX_DIMENSION_PX &&
            bounds.outHeight / (sampleSize * 2) >= MAX_DIMENSION_PX
        ) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val sampledBitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null

        val longestSide = maxOf(sampledBitmap.width, sampledBitmap.height)
        val scale = MAX_DIMENSION_PX.toFloat() / longestSide
        val finalBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                sampledBitmap,
                (sampledBitmap.width * scale).toInt().coerceAtLeast(1),
                (sampledBitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            sampledBitmap
        }

        var quality = INITIAL_JPEG_QUALITY
        var jpegBytes: ByteArray
        do {
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            jpegBytes = outputStream.toByteArray()
            quality -= QUALITY_STEP
        } while (jpegBytes.size > MAX_JPEG_BYTES && quality >= MIN_JPEG_QUALITY)

        return Base64.encodeToString(jpegBytes, Base64.DEFAULT)
    }
}
