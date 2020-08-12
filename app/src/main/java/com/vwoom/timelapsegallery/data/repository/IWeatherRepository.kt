package com.vwoom.timelapsegallery.data.repository

import android.location.Location
import com.vwoom.timelapsegallery.weather.data.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherResult

interface IWeatherRepository {
    suspend fun updateForecast(location: Location): WeatherResult<ForecastResponse>
    suspend fun getCachedForecast(): WeatherResult<ForecastResponse>
}