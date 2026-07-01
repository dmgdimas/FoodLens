package com.example.foodlens.camera

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.foodlens.utils.ArDataExtractor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.NotYetAvailableException
import io.github.sceneview.ar.ARScene
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ArCameraCapture(
    onImagesCaptured: (rgbFile: File, depthFile: File, rgbWidth: Int, depthWidth: Int) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentFrame by remember { mutableStateOf<Frame?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        ARScene(
            modifier = Modifier.fillMaxSize(),
            sessionConfiguration = { session, config ->
                config.depthMode = Config.DepthMode.AUTOMATIC
                config.focusMode = Config.FocusMode.AUTO
            },
            onSessionUpdated = { session, frame ->
                currentFrame = frame
            }
        )

        Button(
            onClick = {
                if (isCapturing) return@Button
                val frame = currentFrame ?: return@Button

                isCapturing = true
                Toast.makeText(context, "Захват глубины...", Toast.LENGTH_SHORT).show()

                coroutineScope.launch {
                    try {
                        val depthImage = frame.acquireDepthImage16Bits()
                        val rgbImage = frame.acquireCameraImage()

                        val depthWidth = depthImage.width
                        val rgbWidth = rgbImage.width

                        // 2. Уходим в фоновый поток, чтобы сохранить файлы на диск (наш вчерашний Extractor)
                        val (rgbFile, depthFile) = withContext(Dispatchers.IO) {
                            val rFile = ArDataExtractor.saveRgbImage(context, rgbImage)
                            val dFile = ArDataExtractor.saveDepthMapRaw(context, depthImage)

                            // Освобождаем память (это критически важно для ARCore!)
                            depthImage.close()
                            rgbImage.close()

                            Pair(rFile, dFile)
                        }

                        if (rgbFile != null && depthFile != null) {
                            // 3. Отдаем файлы наверх (во ViewModel)
                            onImagesCaptured(rgbFile, depthFile, rgbWidth, depthWidth)
                        } else {
                            onError("Ошибка сохранения файлов")
                        }
                    } catch (e: NotYetAvailableException) {
                        onError("Камера еще не готова или датчик глубины не сработал. Попробуйте поводить телефоном.")
                    } catch (e: Exception) {
                        onError("Ошибка: ${e.message}")
                    } finally {
                        isCapturing = false
                    }
                }
            },
            enabled = !isCapturing,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text(if (isCapturing) "Обработка..." else "Сфотографировать с 3D")
        }
    }
}