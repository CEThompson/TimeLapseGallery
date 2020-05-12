package com.vwoom.timelapsegallery.data.repository

import com.vwoom.timelapsegallery.weather.ForecastLocationResponse
import com.vwoom.timelapsegallery.weather.ForecastResponse

interface WeatherDataSource {
    //fun getForecastLocation(): ForecastLocationResponse
    fun getForecast(): ForecastResponse?
}