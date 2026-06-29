package com.example.foodlens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodlens.data.api.ApiClient
import com.example.foodlens.data.model.AnalyzeResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

sealed class ScannerUiState {
    object Idle : ScannerUiState() // ждет фото
    object Loading : ScannerUiState() // идет запрос
    data class Success(val response: AnalyzeResponse) : ScannerUiState() // получен ответ
    data class Error(val message: String) : ScannerUiState() // ошибка сети
}

class ScannerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun analyzeImage(file: File) {
        _uiState.value = ScannerUiState.Loading

        viewModelScope.launch {
            try {
                // подготовка файла
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val response = ApiClient.retrofitService.analyzeFoodImage(body)

                _uiState.value = ScannerUiState.Success(response)

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = ScannerUiState.Error("Ошибка соединения с сервером: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = ScannerUiState.Idle
    }
}