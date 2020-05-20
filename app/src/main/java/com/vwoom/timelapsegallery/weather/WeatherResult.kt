package com.vwoom.timelapsegallery.weather

sealed class WeatherResult<out T : Any> {
    class TodaysForecast<out T : Any>(val data: T, val timestamp: Long) : WeatherResult<T>()

    class CachedForecast<out T : Any>(val data: T,
                                      val timestamp: Long,
                                      var exception: Exception? = null,
                                      var message: String? = null) : WeatherResult<T>()

    object Loading : WeatherResult<Nothing>()

    class NoData(val exception: Exception? = null, var message: String? = null) : WeatherResult<Nothing>()
}