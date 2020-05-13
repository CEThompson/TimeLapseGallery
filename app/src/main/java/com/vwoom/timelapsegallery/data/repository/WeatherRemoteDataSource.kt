package com.vwoom.timelapsegallery.data.repository

import android.util.Log
import com.vwoom.timelapsegallery.weather.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherService
import com.vwoom.timelapsegallery.weather.weatherServiceBaseUrl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRemoteDataSource {
    private val retrofit = Retrofit.Builder()
            .baseUrl(weatherServiceBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    private val weatherService = retrofit.create(WeatherService::class.java)

    // Get the forecast from the national weather service api
    // TODO convert this to a response for error handling?
    @Throws(Exception::class)
    suspend fun getForecast(latitude: String, longitude: String): ForecastResponse? {
        Log.d("WeatherRemoteDataSource", "getting forecast")
        // 1. Get the url to query for the devices latitude / longitude
        val forecastLocationResponse = weatherService.getForecastLocation(latitude, longitude)
        Log.d("WeatherRemoteDataSource", "forecast location response: $forecastLocationResponse")
        if (forecastLocationResponse == null) return null

        val url = forecastLocationResponse.properties.forecast
        Log.d("WeatherRemoteDataSource", "forecast location url from response: $url")

        // 2. Call the url to get the forecast for the devices area and return the result
        val forecastResponse = weatherService.getForecast(url)
        Log.d("WeatherRemoteDataSource", "forecast response is: $forecastResponse")
        return forecastResponse
    }

}