package com.example.foodlens.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.foodlens.camera.CameraCapture
import com.example.foodlens.utils.ImageCompressor
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    CameraCapture(
        onImageCaptured = { uri ->
            coroutineScope.launch {
                val originalFile = File(uri.path!!)
                val originalSizeKb = originalFile.length() / 1024
                Log.d("COMPRESSOR", "Исходный размер: $originalSizeKb КБ")

                val compressedFile = ImageCompressor.compressWithLetterboxing(context, uri)

                if (compressedFile != null) {
                    val compressedSizeKb = compressedFile.length() / 1024
                    Log.d("COMPRESSOR", "Сжатый размер: $compressedSizeKb КБ")

                    Toast.makeText(context, "Ужато с $originalSizeKb до $compressedSizeKb КБ!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Ошибка сжатия!", Toast.LENGTH_SHORT).show()
                }
            }
        },
        onError = { exc ->
            Log.e("CameraScreen", "Ошибка фото: ${exc.message}", exc)
            Toast.makeText(context, "Ошибка съемки", Toast.LENGTH_SHORT).show()
        }
    )
}