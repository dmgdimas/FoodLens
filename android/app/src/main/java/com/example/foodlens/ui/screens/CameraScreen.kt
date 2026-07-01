package com.example.foodlens.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodlens.camera.ArCameraCapture
import com.example.foodlens.ui.viewmodel.ScannerUiState
import com.example.foodlens.ui.viewmodel.ScannerViewModel

@Composable
fun CameraScreen(viewModel: ScannerViewModel = viewModel()) {
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()

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

                ArCameraCapture(
                    onImagesCaptured = { rgbFile, depthFile, rgbWidth, depthWidth ->
                        viewModel.analyzeImage(rgbFile, depthFile, rgbWidth, depthWidth)
                    },
                    onError = { errorMsg ->
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    }
                )
            }

            is ScannerUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Анализируем 3D-модель блюда...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            is ScannerUiState.Success -> {
                val response = (uiState as ScannerUiState.Success).response
                val detection = response.detections?.firstOrNull()

                if (detection != null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Результат 3D-анализа",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = detection.nameRu.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "${detection.nutrients.calories.toInt()} ккал",
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Расчетный вес: ${detection.weight.toInt()} г",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                thickness = DividerDefaults.Thickness,
                                color = DividerDefaults.color
                            )

                            // БЖУ
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                NutrientItem("Белки", detection.nutrients.proteins)
                                NutrientItem("Жиры", detection.nutrients.fats)
                                NutrientItem("Углев", detection.nutrients.carbs)
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Кнопки управления
                            Button(
                                onClick = {
                                    viewModel.saveToHistory(detection)
                                    Toast.makeText(context, "Сохранено в дневник!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Добавить в дневник")
                            }

                            TextButton(
                                onClick = { viewModel.resetState() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Переснять")
                            }
                        }
                    }
                } else {
                    // Если сервер ответил, но ничего не нашел
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Еда на фото не обнаружена")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.resetState() }) {
                                Text("Попробовать снова")
                            }
                        }
                    }
                }
            }
        }
    }
}

// Вспомогательный компонент для отображения БЖУ
@Composable
fun NutrientItem(label: String, value: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value.toInt().toString(), fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}