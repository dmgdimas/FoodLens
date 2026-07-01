package com.example.foodlens.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private var currentUrl: String = ""
    private var cachedService: BackendApi? = null

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("ngrok-skip-browser-warning", "true")
                .build()
            chain.proceed(request)
        }
        .build()

    fun getService(baseUrl: String): BackendApi {
        if (baseUrl != currentUrl || cachedService == null) {
            currentUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            cachedService = Retrofit.Builder()
                .baseUrl(currentUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BackendApi::class.java)
        }
        return cachedService!!
    }
}