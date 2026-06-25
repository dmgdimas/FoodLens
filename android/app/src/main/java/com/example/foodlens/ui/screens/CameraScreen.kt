package com.example.foodlens.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.foodlens.camera.CameraCapture

@Composable
fun CameraScreen() {
    val context = LocalContext.current

    CameraCapture(
        onImageCaptured = { uri ->
            Log.d("CameraScreen", "Фото сохранено: $uri")
            Toast.makeText(context, "Фото сохранено!", Toast.LENGTH_SHORT).show()
            // Потом этот uri будет передаваться во viewmodel для отправки на сервер
        },
        onError = { exc ->
            Log.e("CameraScreen", "Ошибка фото: ${exc.message}", exc)
            Toast.makeText(context, "Ошибка съемки", Toast.LENGTH_SHORT).show()
        }
    )
}