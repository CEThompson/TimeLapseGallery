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
    // Returns either (1) Weather Result: No Data or (2) Weather Result: Today's Forecast
    suspend fun getForecast(latitude: String, longitude: String): WeatherResult<ForecastResponse> {
        // 1. Get the url to query the forecast for this devices location
        val forecastLocationResponse: ForecastLocationResponse?
        try {
            forecastLocationResponse = weatherService.getForecastLocation(latitude, longitude)
        } catch (e: Exception) {
            return WeatherResult.NoData(e, "Exception Retrieving forecast location")
        }
        // If the url did not return then no data can be returned
        if (forecastLocationResponse == null) return WeatherResult.NoData(null, "Error retrieving forecast location")

        // 2. Call the url to get the forecast for the location and return the result
        val url = forecastLocationResponse.properties.forecast
        return try {
            val forecastResponse = weatherService.getForecast(url)
            // If we have a response return a weather result with the forecast packaged as data and the timestamp
            if (forecastResponse != null)
                WeatherResult.TodaysForecast(forecastResponse, System.currentTimeMillis())
            // Otherwise give back an error
            else WeatherResult.NoData(null, "Exception retrieving forecast")
        } catch (e: Exception) {
            WeatherResult.NoData(e)
        }
    }
}