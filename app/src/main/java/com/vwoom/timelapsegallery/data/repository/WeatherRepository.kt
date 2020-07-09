package com.vwoom.timelapsegallery.data.repository

import android.location.Location
import com.vwoom.timelapsegallery.data.datasource.WeatherLocalDataSource
import com.vwoom.timelapsegallery.data.datasource.WeatherRemoteDataSource
import com.vwoom.timelapsegallery.weather.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherResult
import javax.inject.Inject

// TODO test repository and local/remote data sources
class WeatherRepository
@Inject constructor (private val weatherLocalDataSource: WeatherLocalDataSource,
                     private val weatherRemoteDataSource: WeatherRemoteDataSource) {

    // This function calls the national weather service API to attempt to update the forecast stored in the database
    // Returns either (1) Weather Result: Update Success or (2) Weather Result: Update Failure
    suspend fun updateForecast(location: Location): WeatherResult<ForecastResponse> {
        val remoteResponse = weatherRemoteDataSource.getForecast(location)
        return if (remoteResponse is WeatherResult.TodaysForecast) {
            weatherLocalDataSource.cacheForecast(remoteResponse.data)
            WeatherResult.TodaysForecast(remoteResponse.data, remoteResponse.timestamp)
        }
        else {
            // Otherwise there was no response
            val noDataResponse = remoteResponse as WeatherResult.NoData

            // Return the cached response if exists
            val cache = weatherLocalDataSource.getCache()
            if (cache is WeatherResult.CachedForecast)
                WeatherResult.CachedForecast(cache.data, cache.timestamp, remoteResponse.exception, remoteResponse.message)
            else
                // Otherwise return the failed update
                noDataResponse
        }
    }

    // Retrieves the forecast from the database
    // Returns (1) Weather Result: No Data (2) Weather Result: Cached Forecast or (3) Weather Result: Today's Forecast
    suspend fun getForecast(): WeatherResult<ForecastResponse> {
        return weatherLocalDataSource.getWeather()  // Can be (2) cached or (3) today's forecast
    }
}