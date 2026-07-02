package com.example.foodlens.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodlens.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    LocalContext.current
    val savedUrl by viewModel.savedUrl.collectAsState(initial = "")
    val status by viewModel.connectionStatus.collectAsState()

    var tempUrl by remember { mutableStateOf("") }

    LaunchedEffect(savedUrl) {
        tempUrl = savedUrl
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Конфигурация сервера", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = tempUrl,
            onValueChange = { tempUrl = it },
            label = { Text("Base URL (Ngrok / Cloudflare)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.updateUrl(tempUrl) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Сохранить адрес")
        }

        HorizontalDivider(
            Modifier.padding(vertical = 24.dp),
            DividerDefaults.Thickness,
            DividerDefaults.color
        )

        Text("Статус: $status", style = MaterialTheme.typography.bodyLarge)

        Button(
            onClick = { viewModel.checkHealth() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Проверить Health Check")
        }
    }
}