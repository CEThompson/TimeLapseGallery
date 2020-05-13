package com.vwoom.timelapsegallery.data.repository

import android.text.format.DateUtils
import com.google.gson.Gson
import com.vwoom.timelapsegallery.data.entry.WeatherEntry
import com.vwoom.timelapsegallery.weather.ForecastResponse

// TODO test repository and local/remote datasources
class WeatherRepository(private val weatherLocalDataSource: WeatherLocalDataSource,
                        private val weatherRemoteDataSource: WeatherRemoteDataSource) {

    // Retrieves the forecast from a the database or the remote api
    suspend fun getForecast(latitude: String, longitude: String): WeatherResult<Any> {
        when (val localWeatherEntry: WeatherEntry? = weatherLocalDataSource.getWeather()) {
            // If there is no local saved entry defer to remote
            null -> {
                // Get and return the remote response
                val remoteResponse = weatherRemoteDataSource.getForecast(latitude, longitude)
                if (remoteResponse != null) {
                    weatherLocalDataSource.cacheForecast(remoteResponse)
                    return WeatherResult.Success(remoteResponse)
                }

                // Otherwise return a failure
                return WeatherResult.Failure.NoResponse()
            }
            // Determine if the local entry belongs to today
            else -> {
                val localResponse = Gson().fromJson(localWeatherEntry.forecast, ForecastResponse::class.java)

                // If the entry belongs to today return it
                return if (DateUtils.isToday(localWeatherEntry.timestamp)) {
                    WeatherResult.Success(localResponse)
                }
                // Otherwise try to get a remote response
                else {
                    val remoteResponse = weatherRemoteDataSource.getForecast(latitude, longitude)

                    // If we get a remote response return it
                    if (remoteResponse != null) {
                        weatherLocalDataSource.cacheForecast(remoteResponse)
                        WeatherResult.Success(remoteResponse)
                    }

                    // Otherwise return the cached local response
                    else WeatherResult.Cached(localResponse)
                }
            }
        }
    }
}