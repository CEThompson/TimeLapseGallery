package com.vwoom.timelapsegallery.data.repository

import android.util.Log
import com.vwoom.timelapsegallery.weather.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherService
import com.vwoom.timelapsegallery.weather.weatherServiceBaseUrl
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRemoteDataSource: WeatherDataSource {

    private val retrofit = Retrofit.Builder()
            .baseUrl(weatherServiceBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    private val weatherService = retrofit.create(WeatherService::class.java)

    override suspend fun getForecast(latitude: String?, longitude: String?): ForecastResponse? {
        if (latitude == null || longitude == null) return null
        Log.d("WeatherRemoteDataSource", "getting forecast")
        val forecastLocationResponse = weatherService.getForecastLocation(latitude, longitude)
        Log.d("WeatherRemoteDataSource", "forecast location response: $forecastLocationResponse")
        val url = forecastLocationResponse.properties.forecast
        Log.d("WeatherRemoteDataSource", "forecast location url: $url")
        return weatherService.getForecast(url)
    }

}