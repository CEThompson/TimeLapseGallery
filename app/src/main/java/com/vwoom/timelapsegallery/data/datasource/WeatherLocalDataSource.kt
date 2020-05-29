package com.vwoom.timelapsegallery.data.datasource

import android.text.format.DateUtils
import com.google.gson.Gson
import com.vwoom.timelapsegallery.data.dao.WeatherDao
import com.vwoom.timelapsegallery.data.entry.WeatherEntry
import com.vwoom.timelapsegallery.weather.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherResult

class WeatherLocalDataSource(private val weatherDao: WeatherDao) {

    // Gets the forecast from the database
    // Returns either (1) No Data (2) Todays Forecast or (3) A Cached Forecast
    suspend fun getWeather(): WeatherResult<ForecastResponse> {
        val weatherEntry: WeatherEntry? = weatherDao.getWeather()

        return if (weatherEntry == null) WeatherResult.NoData()
        else {
            val localResponse = Gson().fromJson(weatherEntry.forecastJsonString, ForecastResponse::class.java)
            if (DateUtils.isToday(weatherEntry.timestamp)) {
                WeatherResult.TodaysForecast(localResponse, weatherEntry.timestamp)
            } else {
                WeatherResult.CachedForecast(localResponse, weatherEntry.timestamp)
            }
        }
    }

    suspend fun getCache(): WeatherResult<ForecastResponse> {
        val weatherEntry: WeatherEntry? = weatherDao.getWeather()

        return if (weatherEntry == null) WeatherResult.NoData()
        else {
            val localResponse = Gson().fromJson(weatherEntry.forecastJsonString, ForecastResponse::class.java)
            WeatherResult.CachedForecast(localResponse, weatherEntry.timestamp)
        }
    }

    // Saves the forecast as a json string to the database
    suspend fun cacheForecast(forecastResponse: ForecastResponse) {
        val jsonString = Gson().toJson(forecastResponse)
        val cacheTime = System.currentTimeMillis()
        val weather = WeatherEntry(jsonString, cacheTime)
        weatherDao.insertWeather(weather)
    }

}