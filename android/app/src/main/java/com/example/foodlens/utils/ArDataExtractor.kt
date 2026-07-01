package com.example.foodlens.utils

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.os.Environment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ArDataExtractor {

    // Сохраняем 16-битную карту глубины в RAW бинарник
    fun saveDepthMapRaw(context: Context, depthImage: Image): File? {
        try {
            val width = depthImage.width
            val height = depthImage.height

            val plane = depthImage.planes[0]
            val buffer = plane.buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "depth_${width}x${height}_${System.currentTimeMillis()}.raw")

            FileOutputStream(file).use { it.write(bytes) }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Сохраняем цветное фото из YUV в JPEG
    fun saveRgbImage(context: Context, rgbImage: Image): File? {
        try {
            val width = rgbImage.width
            val height = rgbImage.height

            // ARCore отдает фото в формате YUV_420_888. Переводим его в NV21
            val nv21 = yuv420ToNv21(rgbImage)
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)

            val out = ByteArrayOutputStream()
            // Жмем в JPEG (качество 90)
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 90, out)

            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "rgb_${width}x${height}_${System.currentTimeMillis()}.jpg")

            FileOutputStream(file).use { it.write(out.toByteArray()) }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // Вспомогательная функция конвертации форматов камер Android
    private fun yuv420ToNv21(image: Image): ByteArray {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Копируем пиксели яркости (Y)
        yBuffer.get(nv21, 0, ySize)
        // Копируем пиксели цвета (V и U)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }
}