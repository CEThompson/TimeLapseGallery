package com.vwoom.timelapsegallery.data.repository

import android.text.format.DateUtils
import com.google.gson.Gson
import com.vwoom.timelapsegallery.data.entry.WeatherEntry
import com.vwoom.timelapsegallery.weather.ForecastResponse

// TODO test repository and local/remote datasources
class WeatherRepository(private val weatherLocalDataSource: WeatherLocalDataSource,
                        private val weatherRemoteDataSource: WeatherRemoteDataSource) {

    suspend fun forceUpdateForecast(latitude: String, longitude: String): WeatherResult<Any> {
        return when (val remoteResponse = weatherRemoteDataSource.getForecast(latitude, longitude)) {
            is WeatherResult.TodaysForecast -> {
                val forecast: ForecastResponse = remoteResponse.data as ForecastResponse
                weatherLocalDataSource.cacheForecast(forecast)
                remoteResponse
            }
            is WeatherResult.Error -> {
                val localWeatherEntry = weatherLocalDataSource.getWeather()
                if (localWeatherEntry == null) {
                    remoteResponse
                } else {
                    val localResponse = Gson().fromJson(localWeatherEntry.forecast, ForecastResponse::class.java)
                    WeatherResult.CachedForecast(localResponse, remoteResponse.exception)
                }
            }
            else -> {
                WeatherResult.Error()
            }
        }
    }

    // Retrieves the forecast from the database
    // If the forecast is not today's then attempts to call the national weather service api
    suspend fun getForecast(latitude: String, longitude: String): WeatherResult<Any> {

        when (val localWeatherEntry: WeatherEntry? = weatherLocalDataSource.getWeather()) {
            // If there is no local saved entry defer to remote
            null -> {
                return weatherRemoteDataSource.getForecast(latitude, longitude)
            }

            else -> {
                val localResponse = Gson().fromJson(localWeatherEntry.forecast, ForecastResponse::class.java)

                // If the entry belongs to today return it
                return if (DateUtils.isToday(localWeatherEntry.timestamp)) {
                    WeatherResult.TodaysForecast(localResponse)
                }

                // Otherwise try to get a remote response
                else {
                    return when (val remoteResponse =
                            weatherRemoteDataSource.getForecast(latitude, longitude)) {
                        // If the remote response got today's forecast then cache and return
                        is WeatherResult.TodaysForecast -> {
                            val weatherData: ForecastResponse = remoteResponse.data as ForecastResponse
                            weatherLocalDataSource.cacheForecast(weatherData)
                            remoteResponse
                        }
                        // Otherwise return the cached forecast and propagate the exception message
                        is WeatherResult.Error -> {
                            WeatherResult.CachedForecast(localResponse, remoteResponse.exception)
                        }

                        // This case should not fire since remote
                        else -> {
                            // TODO pass a meaningful error message here
                            WeatherResult.Error()
                        }
                    }
                }
            }
        }
    }
}