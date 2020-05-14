package com.vwoom.timelapsegallery.data.repository

import android.text.format.DateUtils
import com.google.gson.Gson
import com.vwoom.timelapsegallery.data.datasource.WeatherLocalDataSource
import com.vwoom.timelapsegallery.data.datasource.WeatherRemoteDataSource
import com.vwoom.timelapsegallery.data.entry.WeatherEntry
import com.vwoom.timelapsegallery.weather.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherResult

// TODO test repository and local/remote datasources
class WeatherRepository(private val weatherLocalDataSource: WeatherLocalDataSource,
                        private val weatherRemoteDataSource: WeatherRemoteDataSource) {

    suspend fun forceUpdateForecast(latitude: String, longitude: String): WeatherResult<ForecastResponse> {
        return when (val remoteResponse = weatherRemoteDataSource.getForecast(latitude, longitude)) {
            // If the remote result is today's forecast then return it
            is WeatherResult.TodaysForecast -> {
                val forecast: ForecastResponse = remoteResponse.data
                weatherLocalDataSource.cacheForecast(forecast)
                remoteResponse
            }
            // Otherwise return the cached forecast is available
            is WeatherResult.Error -> {
                val localWeatherEntry = weatherLocalDataSource.getWeather()
                // If no cache return the original failed response
                if (localWeatherEntry == null) {
                    remoteResponse
                }
                // Otherwise returned the cached response
                else {
                    val localResponse = Gson().fromJson(localWeatherEntry.forecast, ForecastResponse::class.java)
                    WeatherResult.CachedForecast(localResponse, localWeatherEntry.timestamp, remoteResponse.exception)
                }
            }
            // Note: This case should not fire since the remote data source only returns the above two cases
            else -> {
                WeatherResult.Error()
            }
        }
    }

    // Retrieves the forecast from the database
    // If the forecast is not today's then attempts to call the national weather service api
    suspend fun getForecast(latitude: String, longitude: String): WeatherResult<ForecastResponse> {
        when (val localWeatherEntry: WeatherEntry? = weatherLocalDataSource.getWeather()) {
            // If there is no local entry defer to the remote result
            null -> {
                return weatherRemoteDataSource.getForecast(latitude, longitude)
            }

            else -> {
                // Parse the forecast response for later usage
                val localResponse = Gson().fromJson(localWeatherEntry.forecast, ForecastResponse::class.java)

                // If the entry belongs to today return it
                return if (DateUtils.isToday(localWeatherEntry.timestamp)) {
                    WeatherResult.TodaysForecast(localResponse, localWeatherEntry.timestamp)
                }
                // Otherwise try to get a remote response
                else {
                    return when (val remoteResponse = weatherRemoteDataSource.getForecast(latitude, longitude)) {
                        // If the remote response got today's forecast then cache and return
                        is WeatherResult.TodaysForecast -> {
                            val weatherData: ForecastResponse = remoteResponse.data
                            weatherLocalDataSource.cacheForecast(weatherData)
                            remoteResponse
                        }
                        // Otherwise return the cached forecast and propagate the exception message
                        is WeatherResult.Error -> {
                            WeatherResult.CachedForecast(localResponse, localWeatherEntry.timestamp, remoteResponse.exception)
                        }
                        // Note: This case should not fire since remote only returns today's forecast or an error
                        else -> {
                            WeatherResult.Error()
                        }
                    }
                }
            }
        }
    }
}