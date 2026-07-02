package com.example.foodlens.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    val compressedFile = ImageCompressor.compressWithLetterboxing(context, uri)
                    if (compressedFile != null) {
                        viewModel.analyzeImage(compressedFile)
                    } else {
                        Toast.makeText(context, "Ошибка сжатия", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is ScannerUiState.Idle, is ScannerUiState.Error -> {
                if (uiState is ScannerUiState.Error) {
                    val message = (uiState as ScannerUiState.Error).message
                    LaunchedEffect(uiState) {
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                }

                CameraCapture(
                    onImageCaptured = { file ->
                        viewModel.analyzeImage(file)
                    },
                    onError = { errorMsg ->
                        Toast.makeText(context, "Ошибка камеры: $errorMsg", Toast.LENGTH_SHORT).show()
                    }
                )

                IconButton(
                    onClick = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 32.dp, bottom = 40.dp)
                        .size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Галерея")
                }
            }

            is ScannerUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Анализируем блюдо...")
                }
            }

            is ScannerUiState.Success -> {
                val response = (uiState as ScannerUiState.Success).response
                val detection = response.detections?.firstOrNull()

                if (detection != null) {
                    Card(modifier = Modifier.align(Alignment.Center).padding(24.dp)) {
                        Column(Modifier.padding(24.dp),
                            Alignment.CenterHorizontally as Arrangement.Vertical
                        ) {
                            Text(detection.nameRu.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                            Text("Калории: ${detection.nutrients.calories.toInt()} ккал", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                            Text("Вес: ~${detection.weight.toInt()} г", style = MaterialTheme.typography.bodyLarge)

                            Spacer(Modifier.height(24.dp))

                            Button(onClick = { viewModel.saveToHistory(detection); viewModel.resetState() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Добавить в дневник")
                            }
                            TextButton(onClick = { viewModel.resetState() }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Refresh, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Переснять")
                            }
                        }
                    }
                }
            }
        }
    }
}