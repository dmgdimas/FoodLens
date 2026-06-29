package com.example.foodlens.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodlens.camera.CameraCapture
import com.example.foodlens.ui.viewmodel.ScannerUiState
import com.example.foodlens.ui.viewmodel.ScannerViewModel
import com.example.foodlens.utils.ImageCompressor
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(viewModel: ScannerViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is ScannerUiState.Idle, is ScannerUiState.Error -> {
                if (uiState is ScannerUiState.Error) {
                    val errorMsg = (uiState as ScannerUiState.Error).message
                    LaunchedEffect(uiState) {
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                }

                CameraCapture(
                    onImageCaptured = { uri ->
                        coroutineScope.launch {
                            val compressedFile = ImageCompressor.compressWithLetterboxing(context, uri)
                            if (compressedFile != null) {
                                viewModel.analyzeImage(compressedFile)
                            } else {
                                Toast.makeText(context, "Ошибка сжатия", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onError = { Toast.makeText(context, "Ошибка камеры", Toast.LENGTH_SHORT).show() }
                )
            }

            is ScannerUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Анализируем блюдо...")
                }
            }

            is ScannerUiState.Success -> {
                val result = (uiState as ScannerUiState.Success).response
                val detection = result.detections?.firstOrNull()

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Блюдо: ${detection?.className ?: "Не распознано"}", style = MaterialTheme.typography.headlineMedium)
                    Text("Вес: ~${detection?.weightGrams} г", style = MaterialTheme.typography.bodyLarge)
                    Text("Калории: ${detection?.nutrients?.calories} ккал", style = MaterialTheme.typography.bodyLarge)

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(onClick = { viewModel.resetState() }) {
                        Text("Сделать новое фото")
                    }
                }
            }
        }
    }
}