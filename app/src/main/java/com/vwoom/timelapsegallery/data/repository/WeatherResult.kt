package com.vwoom.timelapsegallery.data.repository

// TODO implement weather result?
sealed class WeatherResult<out T : Any> {
    class Success<out T : Any>(val data: T) : WeatherResult<T>()

    //class Cached<out T : Any>(val data: T) : WeatherResult<T>()

    object Loading : WeatherResult<Nothing>()

    class Failure<out T: Any>(val data: T?, val exception: Exception? = null) : WeatherResult<T>()
}