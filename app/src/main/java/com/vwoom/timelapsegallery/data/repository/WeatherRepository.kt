package com.vwoom.timelapsegallery.data.repository

import android.text.format.DateUtils
import android.util.Log
import com.google.gson.Gson
import com.vwoom.timelapsegallery.data.entry.WeatherEntry
import com.vwoom.timelapsegallery.weather.ForecastResponse
import java.lang.Exception

// TODO test repository and local/remote datasources
class WeatherRepository(private val weatherLocalDataSource: WeatherLocalDataSource,
                        private val weatherRemoteDataSource: WeatherRemoteDataSource) {

    suspend fun updateForecast(latitude: String, longitude: String): WeatherResult<Any> {
        try {
            Log.d("WeatherRepository", "getting remote response to update forecast for lat/lng $latitude / $longitude")
            val remoteResponse = weatherRemoteDataSource.getForecast(latitude, longitude)
            Log.d("WeatherRepository", "response is $remoteResponse")
            if (remoteResponse == null) return WeatherResult.Failure(null)

            Log.d("WeatherRepository", "remote response recieved, caching and returning")
            weatherLocalDataSource.cacheForecast(remoteResponse)
            return WeatherResult.Success(remoteResponse)
        } catch (e: Exception) {
            val cachedData = weatherLocalDataSource.getWeather()
            if (cachedData!=null) {
                val cachedForecast = Gson().fromJson(cachedData?.forecast, ForecastResponse::class.java)
                return WeatherResult.Failure(null, e)
            }
            return WeatherResult.Failure(null, e)
        }
    }

    // Retrieves the forecast from a the database or the remote api
    suspend fun getForecast(latitude: String, longitude: String): WeatherResult<Any> {
        try {
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
                    return WeatherResult.Failure(null)
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
                        else WeatherResult.Failure(localResponse)
                    }
                }
            }
        } catch (e: Exception){
            return WeatherResult.Failure(null)
        }
    }
}