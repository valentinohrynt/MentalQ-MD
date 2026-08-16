package com.c242_ps246.mentalq.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object Utils {
    private const val MAX_IMAGE_BYTES = 500_000
    private const val MAX_IMAGE_DIMENSION = 2_048

    fun formatDate(dateString: String): String = try {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())

        if (dateString.contains("T")) {
            formatter.format(Instant.parse(dateString))
        } else {
            LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()))
        }
    } catch (_: Exception) {
        dateString
    }

    suspend fun prepareProfileImage(imageUri: Uri, context: Context): MultipartBody.Part? =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File.createTempFile("profile_", ".jpg", context.cacheDir)
                context.contentResolver.openInputStream(imageUri)?.use { input ->
                    file.outputStream().use(input::copyTo)
                } ?: return@runCatching null

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.path, bounds)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
                }
                val decoded = requireNotNull(BitmapFactory.decodeFile(file.path, options))
                val rotated = rotateForExif(decoded, file.path)
                val stream = ByteArrayOutputStream()
                var quality = 90
                var encoded: ByteArray
                do {
                    stream.reset()
                    rotated.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                    encoded = stream.toByteArray()
                    quality -= 10
                } while (encoded.size > MAX_IMAGE_BYTES && quality >= 30)
                file.outputStream().use { it.write(encoded) }
                if (rotated !== decoded) decoded.recycle()
                rotated.recycle()

                MultipartBody.Part.createFormData(
                    "profileImage",
                    file.name,
                    file.asRequestBody("image/jpeg".toMediaType())
                )
            }.getOrNull()
        }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > MAX_IMAGE_DIMENSION || height / sampleSize > MAX_IMAGE_DIMENSION) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun rotateForExif(source: Bitmap, imagePath: String): Bitmap {
        val orientation = runCatching {
            ExifInterface(imagePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val angle = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return source
        }
        return Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            Matrix().apply { postRotate(angle) },
            true
        )
    }

    fun formatTimestamp(timestamp: Long): String {
        val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0)
        val format = when {
            diff < 24 * 60 * 60 * 1_000L -> "HH:mm"
            diff < 7 * 24 * 60 * 60 * 1_000L -> "EEE"
            else -> "dd/MM/yy"
        }
        return SimpleDateFormat(format, Locale.getDefault()).format(Date(timestamp))
    }
}
