package com.example.foodlens.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insertRecord(record: HistoryRecordEntity)

    @Query("SELECT * FROM history_records ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryRecordEntity>>
}