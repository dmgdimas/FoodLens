package com.example.foodlens.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Camera : Screen("camera", "Сканер", Icons.Default.CameraAlt)
    object History : Screen("history", "Дневник", Icons.AutoMirrored.Filled.List)
    object Settings : Screen("settings", "Настройки", Icons.Default.Settings)
}