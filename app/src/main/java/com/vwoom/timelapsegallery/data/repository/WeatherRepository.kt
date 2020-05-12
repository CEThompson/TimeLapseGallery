package com.vwoom.timelapsegallery.data.repository

import android.util.Log
import com.vwoom.timelapsegallery.weather.ForecastResponse

class WeatherRepository(private val weatherLocalDataSource: WeatherLocalDataSource,
                        private val weatherRemoteDataSource: WeatherRemoteDataSource) {

    //suspend fun getForecast(): ForecastResponse? = weatherLocalDataSource.getForecast()

    suspend fun getForecast(latitude: String, longitude: String): ForecastResponse? {
        // TODO try to get forecast from text file
        // TODO check time of forecast
        // TODO Update from remote when necessary
        Log.d("WeatherRepository", "getting forecast for lat/lng: $latitude/$longitude")
        val response = weatherRemoteDataSource.getForecast(latitude, longitude)
        Log.d("WeatherRepository", "response is : $response")
        return response
    }

}