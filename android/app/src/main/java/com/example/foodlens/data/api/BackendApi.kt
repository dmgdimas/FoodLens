package com.example.foodlens.data.api

import com.example.foodlens.data.model.AnalyzeResponse
import okhttp3.*
import retrofit2.http.*

interface BackendApi {
    @Multipart
    @POST("api/v1/analyze")
    suspend fun analyzeFoodImage(
        @Part image: MultipartBody.Part,
        @Part depth: MultipartBody.Part,
        @Part("metadata") metadata: RequestBody
    ): AnalyzeResponse

    @GET("/health")
    suspend fun checkHealth(): ResponseBody
}