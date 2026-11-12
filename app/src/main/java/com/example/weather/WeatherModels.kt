package com.example.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    @Json(name = "current") val current: Current
)

@JsonClass(generateAdapter = true)
data class Current(
    @Json(name = "temperature") val temperature: Int,
    @Json(name = "weather_descriptions") val weatherDescriptions: List<String>,
    @Json(name = "weather_icons") val weatherIcons: List<String>
)
