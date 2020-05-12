package com.vwoom.timelapsegallery.data.repository

import android.util.Log
import com.google.gson.Gson
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.weather.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherService
import com.vwoom.timelapsegallery.weather.weatherServiceBaseUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class WeatherRemoteDataSource {

    private val retrofit = Retrofit.Builder()
            .baseUrl(weatherServiceBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    private val weatherService = retrofit.create(WeatherService::class.java)

    suspend fun getForecast(latitude: String, longitude: String, externalFilesDir: File): ForecastResponse? {
        Log.d("WeatherRemoteDataSource", "getting forecast")
        val forecastLocationResponse = weatherService.getForecastLocation(latitude, longitude)
        Log.d("WeatherRemoteDataSource", "forecast location response: $forecastLocationResponse")
        val url = forecastLocationResponse?.properties?.forecast ?: return null

        Log.d("WeatherRemoteDataSource", "forecast location url: $url")
        val result: ForecastResponse? = weatherService.getForecast(url)

        withContext(Dispatchers.IO){
            val jsonString = Gson().toJson(result)
            Log.d("WeatherRemoteDataSource", "preparing to write response to text file: $jsonString")
            FileUtils.writeWeatherForecastResponse(externalFilesDir, jsonString)
        }

        return result
    }

}