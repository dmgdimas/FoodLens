package com.example.foodlens.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlin.math.roundToInt

object ImageCompressor {
    private const val TARGET_SIZE = 640
    private const val QUALITY = 85 // Качество JPEG (0-100)

    suspend fun compressWithLetterboxing(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close() ?: return@withContext null

            val originalWidth = originalBitmap.width
            val originalHeight = originalBitmap.height

            val scale =
                (TARGET_SIZE.toFloat() / originalWidth).coerceAtMost(TARGET_SIZE.toFloat() / originalHeight)

            val scaledWidth = (originalWidth * scale).roundToInt()
            val scaledHeight = (originalHeight * scale).roundToInt()

            val scaledBitmap = originalBitmap.scale(scaledWidth, scaledHeight)

            val letterboxedBitmap = createBitmap(TARGET_SIZE, TARGET_SIZE)
            val canvas = Canvas(letterboxedBitmap)

            canvas.drawColor(Color.rgb(114, 114, 114))

            val left = (TARGET_SIZE - scaledWidth) / 2f
            val top = (TARGET_SIZE - scaledHeight) / 2f
            canvas.drawBitmap(scaledBitmap, left, top, null)

            val byteArrayOutputStream = ByteArrayOutputStream()
            letterboxedBitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, byteArrayOutputStream)

            val compressedFile = File(context.cacheDir, "yolo_food_${System.currentTimeMillis()}.jpg")
            val fileOutputStream = FileOutputStream(compressedFile)
            fileOutputStream.write(byteArrayOutputStream.toByteArray())
            fileOutputStream.flush()
            fileOutputStream.close()

            originalBitmap.recycle()
            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }
            letterboxedBitmap.recycle()

            return@withContext compressedFile

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}