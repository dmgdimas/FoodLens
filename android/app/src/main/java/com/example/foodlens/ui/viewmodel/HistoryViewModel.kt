package com.example.foodlens.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.foodlens.di.DatabaseProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

class HistoryViewModel : ViewModel() {
    val historyItems = DatabaseProvider.db.historyDao().getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}