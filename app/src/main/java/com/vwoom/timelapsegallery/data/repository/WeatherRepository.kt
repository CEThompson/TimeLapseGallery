package com.vwoom.timelapsegallery.data.repository

import com.vwoom.timelapsegallery.data.datasource.WeatherLocalDataSource
import com.vwoom.timelapsegallery.data.datasource.WeatherRemoteDataSource
import com.vwoom.timelapsegallery.weather.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherResult

// TODO test repository and local/remote data sources
class WeatherRepository(private val weatherLocalDataSource: WeatherLocalDataSource,
                        private val weatherRemoteDataSource: WeatherRemoteDataSource) {

    // This function calls the national weather service API to attempt to update the forecast stored in the database
    // Returns either (1) Weather Result: Update Success or (2) Weather Result: Update Failure
    suspend fun updateForecast(latitude: String, longitude: String): WeatherResult<ForecastResponse> {
        val remoteResponse = weatherRemoteDataSource.getForecast(latitude, longitude)
        return if (remoteResponse is WeatherResult.TodaysForecast) {
            weatherLocalDataSource.cacheForecast(remoteResponse.data)
            WeatherResult.UpdateForecast.Success(remoteResponse.data, remoteResponse.timestamp)
        }
        else {
            WeatherResult.UpdateForecast.Failure((remoteResponse as WeatherResult.NoData).exception)
            //WeatherResult.NoData((remoteResponse as WeatherResult.NoData).exception, "Failed to update forecast")
        }
    }

    // Retrieves the forecast from the database
    // Also if the forecast is not today's then attempts to update the national weather service api
    // Returns (1) Weather Result: No Data (2) Weather Result: Cached Forecast or (3) Weather Result: Today's Forecast
    suspend fun getForecast(latitude: String, longitude: String): WeatherResult<ForecastResponse> {
        val databaseForecast = weatherLocalDataSource.getWeather()  // Can be (2) cached or (3) today's forecast

        if (databaseForecast !is WeatherResult.TodaysForecast){
            val remoteResponse = updateForecast(latitude, longitude)
            if (remoteResponse is WeatherResult.TodaysForecast) {
                weatherLocalDataSource.cacheForecast(remoteResponse.data)
                return remoteResponse   // Can be (1) no data or (3) today's forecast
            }
        }

        return databaseForecast
    }
}