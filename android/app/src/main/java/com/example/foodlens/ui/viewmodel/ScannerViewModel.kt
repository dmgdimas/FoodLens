package com.example.foodlens.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodlens.data.api.ApiClient
import com.example.foodlens.data.local.HistoryRecordEntity
import com.example.foodlens.di.DatabaseProvider
import com.example.foodlens.utils.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

sealed class ScannerUiState {
    object Idle : ScannerUiState()
    object Loading : ScannerUiState()
    data class Success(val response: com.example.foodlens.data.model.AnalyzeResponse) : ScannerUiState()
    data class Error(val message: String) : ScannerUiState()
}
class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefManager = PreferenceManager(application)

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun analyzeImage(file: File) {
        _uiState.value = ScannerUiState.Loading
        viewModelScope.launch {
            try {
                val api = ApiClient.getService(prefManager.serverUrl.first())
                val body = MultipartBody.Part.createFormData("image", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))

                val response = api.analyzeFoodImage(body)
                _uiState.value = ScannerUiState.Success(response)
            } catch (e: Exception) {
                _uiState.value = ScannerUiState.Error(e.message ?: "Ошибка")
            }
        }
    }

    fun saveToHistory(detection: com.example.foodlens.data.model.Detection) {
        viewModelScope.launch {
            val entity = HistoryRecordEntity(
                foodName = detection.nameRu,
                weightGrams = detection.weight,
                calories = detection.nutrients.calories,
                proteins = detection.nutrients.proteins,
                fats = detection.nutrients.fats,
                carbs = detection.nutrients.carbs,
                imagePath = ""
            )
            DatabaseProvider.db.historyDao().insertRecord(entity)
            resetState()
        }
    }

    fun resetState() {
        _uiState.value = ScannerUiState.Idle
    }
}