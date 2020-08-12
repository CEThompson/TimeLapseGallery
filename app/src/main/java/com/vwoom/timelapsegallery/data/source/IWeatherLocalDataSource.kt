package com.vwoom.timelapsegallery.data.source

import com.vwoom.timelapsegallery.weather.data.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherResult

interface IWeatherLocalDataSource {
    // Gets the forecast from the database
    // Returns either (1) No Data (2) Today's Forecast or (3) A Cached Forecast
    suspend fun getCachedWeather(): WeatherResult<ForecastResponse>

    // Saves the forecast as a json string to the database
    suspend fun cacheForecast(forecastResponse: ForecastResponse)
}