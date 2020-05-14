package com.vwoom.timelapsegallery.data.datasource

import com.vwoom.timelapsegallery.weather.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRemoteDataSource {
    // Set up retrofit instance for data source
    private val retrofit = Retrofit.Builder()
            .baseUrl(weatherServiceBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    private val weatherService = retrofit.create(WeatherService::class.java)

    // Get the forecast from the national weather service api
    suspend fun getForecast(latitude: String, longitude: String): WeatherResult<ForecastResponse> {
        // 1. Get the url to query the forecast for this devices location
        val forecastLocationResponse: ForecastLocationResponse?
        try {
            forecastLocationResponse = weatherService.getForecastLocation(latitude, longitude)
        } catch (e: Exception) {
            return WeatherResult.Error(e)
        }
        // If the url did not return then no record the error
        if (forecastLocationResponse == null) return WeatherResult.Error(null)

        // 2. Call the url to get the forecast for the location and return the result
        val url = forecastLocationResponse.properties.forecast
        return try {
            val forecastResponse = weatherService.getForecast(url)
            // If we have a response return a weather result with the forecast packaged as data and the timestamp
            if (forecastResponse != null)
                WeatherResult.TodaysForecast(forecastResponse, System.currentTimeMillis())
            // Otherwise give back an error
            else WeatherResult.Error()
        } catch (e: Exception) {
            WeatherResult.Error(e)
        }
    }
}