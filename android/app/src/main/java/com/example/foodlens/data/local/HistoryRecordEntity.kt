package com.example.foodlens.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_records")
data class HistoryRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val foodName: String,
    val weightGrams: Double,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
    val imagePath: String, // путь к фото в кэше
    val timestamp: Long = System.currentTimeMillis()
)