package com.vwoom.timelapsegallery.data.repository

import android.util.Log
import com.vwoom.timelapsegallery.weather.ForecastLocationResponse
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
    suspend fun getForecast(latitude: String, longitude: String): WeatherResult<ForecastResponse> {
        Log.d("WeatherRemoteDataSource", "getting forecast")
        // 1. Get the url to query for the devices latitude / longitude

        val forecastLocationResponse: ForecastLocationResponse?
        try {
            forecastLocationResponse = weatherService.getForecastLocation(latitude, longitude)
        } catch (e: Exception) {
            return WeatherResult.Error(e)
        }

        Log.d("WeatherRemoteDataSource", "forecast location response: $forecastLocationResponse")
        if (forecastLocationResponse == null) return WeatherResult.Error(null)

        val url = forecastLocationResponse.properties.forecast
        Log.d("WeatherRemoteDataSource", "forecast location url from response: $url")

        // 2. Call the url to get the forecast for the devices area and return the result

        return try {
            val forecastResponse = weatherService.getForecast(url)
            Log.d("WeatherRemoteDataSource", "forecast response is: $forecastResponse")
            if (forecastResponse != null)
                WeatherResult.TodaysForecast(forecastResponse, System.currentTimeMillis())
            else WeatherResult.Error()
        } catch (e: Exception) {
            WeatherResult.Error(e)
        }
    }

}