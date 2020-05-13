package com.vwoom.timelapsegallery.data.repository

import android.text.format.DateUtils
import com.google.gson.Gson
import com.vwoom.timelapsegallery.data.dao.WeatherDao
import com.vwoom.timelapsegallery.data.entry.WeatherEntry
import com.vwoom.timelapsegallery.weather.ForecastResponse
import java.io.File

class WeatherLocalDataSource(private val weatherDao: WeatherDao) {

    /*fun getWeatherLiveData() = weatherDao.getWeatherLiveData()*/

    suspend fun getWeather(): WeatherEntry? {
        return weatherDao.getWeather()
    }

    // Retrieve the forecast from the database
    suspend fun getForecast(): ForecastResponse? {
        val weather = weatherDao.getWeather()
        weather ?: return null
        return if (DateUtils.isToday(weather.timestamp)) {
            Gson().fromJson(weather.forecast, ForecastResponse::class.java)
        } else null
    }

    // Saves the forecast as a json string to the database
    suspend fun cacheForecast(forecastResponse: ForecastResponse) {
        val jsonString = Gson().toJson(forecastResponse)
        val cacheTime = System.currentTimeMillis()
        val weather = WeatherEntry(jsonString, cacheTime)
        weatherDao.insertWeather(weather)
    }

}