package com.vwoom.timelapsegallery.data.repository

// TODO implement weather result?
sealed class WeatherResult<out T : Any> {
    class Success<out T : Any>(val data: T) : WeatherResult<T>()

    class Cached<out T : Any>(val data: T) : WeatherResult<T>()

    object Loading : WeatherResult<Nothing>()

    sealed class Failure(val exception: Exception?) : WeatherResult<Nothing>() {
        class NetworkRequired(exception: Exception? = null) : Failure(exception)

        class NoResponse(exception: Exception? = null) : Failure(exception)
    }
}