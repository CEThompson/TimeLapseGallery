package com.vwoom.timelapsegallery.data.repository

import android.util.Log
import com.google.gson.Gson
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.weather.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherService
import com.vwoom.timelapsegallery.weather.weatherServiceBaseUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class WeatherRemoteDataSource {

    // TODO figure out whether or not to inject retrofit / weather service
    private val retrofit = Retrofit.Builder()
            .baseUrl(weatherServiceBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    private val weatherService = retrofit.create(WeatherService::class.java)

    // Get the forecast from the national weather service api
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