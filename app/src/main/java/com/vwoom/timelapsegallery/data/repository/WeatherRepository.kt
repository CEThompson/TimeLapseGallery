package com.vwoom.timelapsegallery.data.repository

import com.vwoom.timelapsegallery.weather.ForecastResponse

class WeatherRepository(private val weatherRemoteDataSource: WeatherDataSource,
                        private val weatherLocalDataSource: WeatherDataSource) {

    fun getForecast(): ForecastResponse? = weatherLocalDataSource.getForecast()
}