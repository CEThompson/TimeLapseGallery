package com.vwoom.timelapsegallery.weather

sealed class WeatherResult<out T : Any> {
    // TODO propagate weather entry timestamp with today's forecast
    class TodaysForecast<out T : Any>(val data: T, val timestamp: Long) : WeatherResult<T>()

    // TODO propagate weather entry timestamp with cached forecast
    class CachedForecast<out T : Any>(val data: T,
                                      val timestamp: Long,
                                      var exception: Exception? = null,
                                      var message: String? = null) : WeatherResult<T>()

    sealed class UpdateForecast<out T: Any>(val data: T? = null,
                                            val timestamp: Long? = null,
                                            val exception: Exception? = null,
                                            var message: String? = null): WeatherResult<T>(){
        class Success<out T: Any>(data: T, timestamp: Long): UpdateForecast<T>(data, timestamp)
        class Failure<out T: Any>(exception: Exception? = null, message: String?)
            : UpdateForecast<T>(exception = exception, message = message)
    }

    object Loading : WeatherResult<Nothing>()

    class NoData(val exception: Exception? = null, var message: String? = null) : WeatherResult<Nothing>()
}