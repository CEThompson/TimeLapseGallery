package com.vwoom.timelapsegallery.fakes

import com.vwoom.timelapsegallery.data.source.IWeatherLocalDataSource
import com.vwoom.timelapsegallery.testing.TestForecastResponse
import com.vwoom.timelapsegallery.weather.data.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherResult

class FakeLocalDataSource(
        var forecastJsonString: String? = null,
        var isToday: Boolean = false) : IWeatherLocalDataSource {

    override suspend fun cacheForecast(forecastResponse: ForecastResponse) {
        forecastJsonString = TestForecastResponse.makeForecastResponse(forecastResponse)
    }

    override suspend fun getCachedWeather(): WeatherResult<ForecastResponse> {
        return try {
            val forecastFromStorage = TestForecastResponse.getForecastResponse(forecastJsonString)
            when {
                forecastFromStorage == null -> {
                    WeatherResult.NoData()
                }
                !isToday -> {
                    WeatherResult.CachedForecast(
                            forecastFromStorage,
                            System.currentTimeMillis(),
                            null
                    )
                }
                else -> {
                    WeatherResult.TodaysForecast(forecastFromStorage, System.currentTimeMillis())
                }
            }
        } catch (e: Exception) {
            WeatherResult.NoData()
        }
    }
}