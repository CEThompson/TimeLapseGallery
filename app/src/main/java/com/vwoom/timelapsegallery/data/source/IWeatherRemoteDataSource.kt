package com.vwoom.timelapsegallery.data.source

import android.location.Location
import com.vwoom.timelapsegallery.weather.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherResult

interface IWeatherRemoteDataSource {
    // Get the forecast from the national weather service api
    // Returns either (1) Weather Result: No Data or (2) Weather Result: Today's Forecast
    suspend fun getForecast(location: Location): WeatherResult<ForecastResponse>
}