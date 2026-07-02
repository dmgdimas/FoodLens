package com.example.foodlens.data.model

import com.google.gson.annotations.SerializedName

data class AnalyzeResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("detections") val detections: List<Detection>?
)

data class Detection(
    @SerializedName("class") val mlClass: String?,
    @SerializedName("name_ru") val nameRu: String?,
    @SerializedName("name_en") val nameEn: String?,
    @SerializedName("confidence") val confidence: Double?,
    @SerializedName("estimated_volume_cm3") val volume: Double?,
    @SerializedName("estimated_weight_g") val weight: Double?,
    @SerializedName("nutrients") val nutrients: Nutrients?
)

data class Nutrients(
    @SerializedName("calories") val calories: Double?,
    @SerializedName("proteins") val proteins: Double?,
    @SerializedName("fats") val fats: Double?,
    @SerializedName("carbs") val carbs: Double?
)