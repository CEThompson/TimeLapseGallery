package com.vwoom.timelapsegallery.data.repository

sealed class WeatherResult<out T : Any> {
    // TODO propagate weather entry timestamp with today's forecast
    class TodaysForecast<out T : Any>(val data: T) : WeatherResult<T>()

    // TODO propagate weather entry timestamp with cached forecast
    class CachedForecast<out T : Any>(val data: T, val exception: Exception? = null) : WeatherResult<T>()

    object Loading : WeatherResult<Nothing>()

    class Error<out T : Any>(val exception: Exception? = null) : WeatherResult<T>()
}