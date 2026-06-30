package com.example.foodlens.di

import android.content.Context
import com.example.foodlens.data.local.AppDatabase

object DatabaseProvider {
    lateinit var db: AppDatabase private set

    fun init(context: Context) {
        db = AppDatabase.getDatabase(context)
    }
}