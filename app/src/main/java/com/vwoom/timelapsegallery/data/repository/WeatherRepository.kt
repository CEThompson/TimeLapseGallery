package com.vwoom.timelapsegallery.data.repository

import android.text.format.DateUtils
import com.google.gson.Gson
import com.vwoom.timelapsegallery.data.entry.WeatherEntry
import com.vwoom.timelapsegallery.weather.ForecastResponse

// TODO test repository and local/remote datasources
class WeatherRepository(private val weatherLocalDataSource: WeatherLocalDataSource,
                        private val weatherRemoteDataSource: WeatherRemoteDataSource) {

    // Retrieve the forecast from a the database
    // If the forecast does not belong to today, try to retrieve the forecast from remote
    suspend fun getForecast(latitude: String, longitude: String): ForecastResponse? {

        when (val localWeatherEntry: WeatherEntry? = weatherLocalDataSource.getWeather()) {
            // When local weather entry is null return the remote response
            null -> {
                val remoteResponse = weatherRemoteDataSource.getForecast(latitude, longitude)
                if (remoteResponse != null) weatherLocalDataSource.cacheForecast(remoteResponse)
                return remoteResponse
            }
            // Otherwise determine if the entry belongs to today
            else -> {
                // If the entry belongs to today return it
                var localResponse = Gson().fromJson(localWeatherEntry.forecast, ForecastResponse::class.java)
                return if (DateUtils.isToday(localWeatherEntry.timestamp)) {
                    localResponse
                }
                // Otherwise try to get a remote response
                else {
                    val remoteResponse = weatherRemoteDataSource.getForecast(latitude, longitude)
                    // If we get a remote response return it
                    if (remoteResponse != null) {
                        weatherLocalDataSource.cacheForecast(remoteResponse)
                        remoteResponse
                    }
                    // Otherwise return the cached local response
                    else localResponse
                }
            }
        }
    }
}