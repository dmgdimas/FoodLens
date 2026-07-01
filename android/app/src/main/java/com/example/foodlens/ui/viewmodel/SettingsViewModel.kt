package com.example.foodlens.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodlens.data.api.ApiClient
import com.example.foodlens.utils.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefManager = PreferenceManager(application)

    val savedUrl = prefManager.serverUrl

    private val _connectionStatus = MutableStateFlow("Ожидание проверки...")
    val connectionStatus: StateFlow<String> = _connectionStatus

    fun updateUrl(newUrl: String) {
        viewModelScope.launch {
            prefManager.saveUrl(newUrl)
        }
    }

    fun checkHealth() {
        viewModelScope.launch {
            _connectionStatus.value = "Соединение..."
            try {
                val baseUrl = prefManager.serverUrl.first()
                val api = ApiClient.getService(baseUrl)
                val response = api.checkHealth()
                _connectionStatus.value = "Сервер онлайн!"
            } catch (e: Exception) {
                _connectionStatus.value = "Ошибка связи: ${e.message}"
            }
        }
    }
}