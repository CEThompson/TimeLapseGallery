package com.vwoom.timelapsegallery.data.repository

import android.util.Log
import com.google.gson.Gson
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.weather.ForecastResponse
import java.io.File
import java.io.FileInputStream

class WeatherRepository(private val weatherLocalDataSource: WeatherLocalDataSource,
                        private val weatherRemoteDataSource: WeatherRemoteDataSource) {

    //suspend fun getForecast(): ForecastResponse? = weatherLocalDataSource.getForecast()

    suspend fun getForecast(latitude: String, longitude: String, externalFilesDir: File): ForecastResponse? {

        val localResponse = weatherLocalDataSource.getForecast(externalFilesDir)
        Log.d("WeatherRepository", "local response is $localResponse")

        // TODO Update from remote if there is no local response or the response is not up to date
        Log.d("WeatherRepository", "getting forecast for lat/lng: $latitude/$longitude")
        val response = weatherRemoteDataSource.getForecast(latitude, longitude, externalFilesDir)
        Log.d("WeatherRepository", "response is : $response")

        return response
    }

}