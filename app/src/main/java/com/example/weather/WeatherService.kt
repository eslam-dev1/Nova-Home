package com.example.weather

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherService {
    @GET("current")
    suspend fun getCurrentWeather(
        @Query("access_key") apiKey: String,
        @Query("query") query: String
    ): WeatherResponse
}
