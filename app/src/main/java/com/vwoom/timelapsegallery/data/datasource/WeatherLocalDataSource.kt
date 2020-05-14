package com.vwoom.timelapsegallery.data.datasource

import com.google.gson.Gson
import com.vwoom.timelapsegallery.data.dao.WeatherDao
import com.vwoom.timelapsegallery.data.entry.WeatherEntry
import com.vwoom.timelapsegallery.weather.ForecastResponse

class WeatherLocalDataSource(private val weatherDao: WeatherDao) {

    suspend fun getWeather(): WeatherEntry? {
        return weatherDao.getWeather()
    }

    // Saves the forecast as a json string to the database
    suspend fun cacheForecast(forecastResponse: ForecastResponse) {
        val jsonString = Gson().toJson(forecastResponse)
        val cacheTime = System.currentTimeMillis()
        val weather = WeatherEntry(jsonString, cacheTime)
        weatherDao.insertWeather(weather)
    }

}