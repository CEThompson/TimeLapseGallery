package com.vwoom.timelapsegallery.weather

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

const val weatherServiceBaseUrl = "https://api.weather.gov/"

// Set up retrofit instance for data source
private val retrofit = Retrofit.Builder()
        .baseUrl(weatherServiceBaseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

interface WeatherService {
    @GET("points/{latitude},{longitude}")
    suspend fun getForecastLocation(@Path("latitude") latitude: String,
                                    @Path("longitude") longitude: String): ForecastLocationResponse?

    @GET
    suspend fun getForecast(@Url stringUrl: String): ForecastResponse?
}

object WeatherApi {
    val weatherService: WeatherService by lazy {
        retrofit.create(WeatherService::class.java)
    }
}