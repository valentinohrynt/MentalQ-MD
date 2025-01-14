package com.c242_ps246.mentalq.ui.utils


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.Color
import androidx.exifinterface.media.ExifInterface
import com.c242_ps246.mentalq.ui.theme.OrangeDark
import com.c242_ps246.mentalq.ui.theme.OrangeLight
import com.c242_ps246.mentalq.ui.theme.RedDark
import com.c242_ps246.mentalq.ui.theme.RedLight
import com.c242_ps246.mentalq.ui.theme.YellowDark
import com.c242_ps246.mentalq.ui.theme.YellowLight
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object Utils {
    private const val MAXIMAL_SIZE = 500000
    private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
    private val timeStamp: String =
        SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())

    fun formatDate(dateString: String): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault())

            if (dateString.contains("T")) {
                val instant = Instant.parse(dateString)
                formatter.format(instant)
            } else {
                val localDate =
                    LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                localDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
            }
        } catch (e: Exception) {
            dateString
        }
    }

    fun createCustomTempFile(context: Context): File {
        val filesDir = context.externalCacheDir
        return File.createTempFile(timeStamp, ".jpeg", filesDir)
    }

    fun uriToFile(imageUri: Uri, context: Context): File {
        val myFile = createCustomTempFile(context)
        val inputStream = context.contentResolver.openInputStream(imageUri) as InputStream
        val outputStream = FileOutputStream(myFile)
        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) outputStream.write(
            buffer,
            0,
            length
        )
        outputStream.close()
        inputStream.close()
        return myFile
    }

    fun File.compressImageSize(): File {
        val file = this
        val bitmap = getBitmapWithCorrectRotation(file.path)
        var compressQuality = 60
        var streamLength: Int
        do {
            val bmpStream = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, compressQuality, bmpStream)
            val bmpPicByteArray = bmpStream.toByteArray()
            streamLength = bmpPicByteArray.size
            compressQuality -= 10
        } while (streamLength > MAXIMAL_SIZE)
        bitmap?.compress(Bitmap.CompressFormat.JPEG, compressQuality, FileOutputStream(file))
        return file
    }

    private fun getBitmapWithCorrectRotation(imagePath: String): Bitmap? {
        val options = BitmapFactory.Options()
        var bitmap = BitmapFactory.decodeFile(imagePath, options)

        try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            bitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return bitmap
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            matrix, true
        )
    }

    fun fetchServerTime(
        onTimeFetched: (LocalDateTime) -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://worldtimeapi.org/api/timezone/Asia/Jakarta")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    onError?.invoke(e.message ?: "Unknown error")
                    onTimeFetched(LocalDateTime.now())
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        Handler(Looper.getMainLooper()).post {
                            onError?.invoke("Unsuccessful response")
                            onTimeFetched(LocalDateTime.now())
                        }
                        return
                    }

                    response.body?.string()?.let { responseData ->
                        val json = JSONObject(responseData)
                        val dateTime = json.getString("datetime")
                        val serverTime = LocalDateTime.parse(dateTime.substring(0, 19))

                        Handler(Looper.getMainLooper()).post {
                            onTimeFetched(serverTime)
                        }
                    }
                } catch (e: Exception) {
                    Handler(Looper.getMainLooper()).post {
                        onError?.invoke(e.message ?: "Parsing error")
                        onTimeFetched(LocalDateTime.now())
                    }
                }
            }
        })
    }

    fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 24 * 60 * 60 * 1000 -> SimpleDateFormat("HH:mm", Locale.getDefault())
            diff < 7 * 24 * 60 * 60 * 1000 -> SimpleDateFormat("EEE", Locale.getDefault())
            else -> SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        }.format(Date(timestamp))
    }

    fun getColorBasedOnPercentage(isSystemDarkMode: Boolean, percentage: Int): Color {
        val clampedPercentage = percentage.coerceIn(0, 100)

        return when (clampedPercentage) {
            in 0..33 -> {
                if (isSystemDarkMode) YellowDark else YellowLight
            }

            in 34..66 -> {
                if (isSystemDarkMode) OrangeDark else OrangeLight
            }

            else -> {
                if (isSystemDarkMode) RedDark else RedLight
            }
        }
    }

    fun Long.toFormattedDate(
        format: String = "yyyy-MM-dd",
        locale: Locale = Locale.getDefault()
    ): String {
        val date = Date(this) // Create a Date object from the timestamp
        val formatter = SimpleDateFormat(
            format,
            locale
        ) // Create a formatter with the desired format and locale
        return formatter.format(date) // Format the date and return the result
    }
}