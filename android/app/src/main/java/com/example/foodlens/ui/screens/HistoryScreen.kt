package com.example.foodlens.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodlens.ui.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val historyItems by viewModel.historyItems.collectAsState()

    // Считаем общую сумму калорий за день
    val totalCalories = remember(historyItems) {
        historyItems.sumOf { it.calories }.toInt()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Дневник питания") },
                // Добавим отображение суммы в заголовке
                actions = {
                    Text(
                        text = "$totalCalories ккал за сегодня",
                        modifier = Modifier.padding(end = 16.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    ) { padding ->
        if (historyItems.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("История пуста. Сделайте первое фото!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(historyItems) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.foodName, style = MaterialTheme.typography.headlineSmall)
                                Text("${item.weightGrams.toInt()} г", style = MaterialTheme.typography.bodyMedium)
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                thickness = DividerDefaults.Thickness,
                                color = DividerDefaults.color
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Энергия:", style = MaterialTheme.typography.bodyMedium)
                                Text("${item.calories} ккал", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }

                            Text(
                                text = "БЖУ: ${item.proteins} / ${item.fats} / ${item.carbs}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}