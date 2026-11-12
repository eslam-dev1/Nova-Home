package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupPayload(
    @Json(name = "schemaVersion") val schemaVersion: Int = 1,
    @Json(name = "exportedAt") val exportedAt: Long = System.currentTimeMillis(),
    @Json(name = "appVersion") val appVersion: String = "1.0",
    @Json(name = "showLabels") val showLabels: Boolean = true,
    @Json(name = "iconShape") val iconShape: String = "circle",
    @Json(name = "gridSize") val gridSize: String = "5x5",
    @Json(name = "deviceProfile") val deviceProfile: String = "Pixel 9 Style",
    @Json(name = "edgeBackGesture") val edgeBackGesture: Boolean = true,
    @Json(name = "iconPack") val iconPack: String? = null,
    @Json(name = "hiddenApps") val hiddenApps: List<String> = emptyList()
)
