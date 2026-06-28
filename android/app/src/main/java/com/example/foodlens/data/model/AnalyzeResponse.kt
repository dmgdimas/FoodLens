package com.example.foodlens.data.model

import com.google.gson.annotations.SerializedName

// Главный ответ сервера
data class AnalyzeResponse(
    @SerializedName("status") val status: String,
    @SerializedName("detections") val detections: List<Detection>?
)

// Найденный объект на фото
data class Detection(
    @SerializedName("class") val className: String,
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("estimated_volume_cm3") val volumeCm3: Double,
    @SerializedName("estimated_weight_g") val weightGrams: Double,
    @SerializedName("nutrients") val nutrients: Nutrients
)

// БЖУ и Калории
data class Nutrients(
    @SerializedName("calories") val calories: Double,
    @SerializedName("proteins") val proteins: Double,
    @SerializedName("fats") val fats: Double,
    @SerializedName("carbs") val carbs: Double
)