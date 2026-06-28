package com.example.foodlens.data.api

import com.example.foodlens.data.model.AnalyzeResponse
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface BackendApi {
    @Multipart
    @POST("/api/v1/analyze")
    suspend fun analyzeFoodImage(
        @Part file: MultipartBody.Part
    ): AnalyzeResponse
}