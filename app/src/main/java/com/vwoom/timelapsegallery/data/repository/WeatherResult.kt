package com.vwoom.timelapsegallery.data.repository

sealed class WeatherResult<out T : Any> {
    // TODO propagate weather entry timestamp with today's forecast
    class TodaysForecast<out T : Any>(val data: T, val timestamp: Long) : WeatherResult<T>()

    // TODO propagate weather entry timestamp with cached forecast
    class CachedForecast<out T : Any>(val data: T, val timestamp: Long, val exception: Exception? = null) : WeatherResult<T>()

    object Loading : WeatherResult<Nothing>()

    class Error(val exception: Exception? = null) : WeatherResult<Nothing>()
}