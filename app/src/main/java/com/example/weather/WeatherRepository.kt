package com.example.weather

import com.example.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class WeatherRepository {
    private val service = Retrofit.Builder()
        .baseUrl("http://api.weatherstack.com/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(WeatherService::class.java)

    suspend fun getWeather(lat: Double, lon: Double): WeatherResponse? {
        val apiKey = BuildConfig.WEATHER_API_KEY
        if (apiKey.isEmpty()) return null
        return try {
            service.getCurrentWeather(apiKey, "$lat,$lon")
        } catch (e: Exception) {
            null
        }
    }
}
