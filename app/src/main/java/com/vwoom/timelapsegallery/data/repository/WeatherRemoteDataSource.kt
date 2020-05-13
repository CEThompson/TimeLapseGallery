package com.vwoom.timelapsegallery.data.repository

import android.util.Log
import com.vwoom.timelapsegallery.weather.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherService
import com.vwoom.timelapsegallery.weather.weatherServiceBaseUrl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRemoteDataSource {

    // TODO figure out whether or not to inject retrofit / weather service
    private val retrofit = Retrofit.Builder()
            .baseUrl(weatherServiceBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    private val weatherService = retrofit.create(WeatherService::class.java)

    // Get the forecast from the national weather service api
    // TODO convert this to a response for error handling?
    suspend fun getForecast(latitude: String, longitude: String): ForecastResponse? {
        // 1. Get the url to query for the devices latitude / longitude
        val forecastLocationResponse = weatherService.getForecastLocation(latitude, longitude)
        val url = forecastLocationResponse?.properties?.forecast ?: return null

        Log.d("WeatherRemoteDataSource", "getting forecast")
        Log.d("WeatherRemoteDataSource", "forecast location response: $forecastLocationResponse")
        Log.d("WeatherRemoteDataSource", "forecast location url from response: $url")

        // 2. Call the url to get the forecast for the devices area and return the result
        return weatherService.getForecast(url)
    }

}