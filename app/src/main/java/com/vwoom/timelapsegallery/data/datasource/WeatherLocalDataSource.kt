package com.vwoom.timelapsegallery.data.datasource

import android.text.format.DateUtils
import com.vwoom.timelapsegallery.data.dao.WeatherDao
import com.vwoom.timelapsegallery.data.entry.WeatherEntry
import com.vwoom.timelapsegallery.weather.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherApi.moshi
import com.vwoom.timelapsegallery.weather.WeatherResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WeatherLocalDataSource
@Inject constructor (private val weatherDao: WeatherDao) {

    // Gets the forecast from the database
    // Returns either (1) No Data (2) Todays Forecast or (3) A Cached Forecast
    suspend fun getWeather(): WeatherResult<ForecastResponse> = withContext(Dispatchers.IO){
        val weatherEntry: WeatherEntry? = weatherDao.getWeather()

        return@withContext if (weatherEntry == null) WeatherResult.NoData()
        else {
            val localResponse: ForecastResponse? = moshi.adapter(ForecastResponse::class.java)
                    .fromJson(weatherEntry.forecastJsonString)
            return@withContext when {
                localResponse == null -> WeatherResult.NoData()
                DateUtils.isToday(weatherEntry.timestamp) -> {
                    WeatherResult.TodaysForecast(localResponse, weatherEntry.timestamp)
                }
                else -> {
                    WeatherResult.CachedForecast(localResponse, weatherEntry.timestamp)
                }
            }
        }
    }

    suspend fun getCache(): WeatherResult<ForecastResponse> = withContext(Dispatchers.IO){
        val weatherEntry: WeatherEntry? = weatherDao.getWeather()

        return@withContext if (weatherEntry == null) WeatherResult.NoData()
        else {
            val localResponse: ForecastResponse? = moshi.adapter(ForecastResponse::class.java)
                    .fromJson(weatherEntry.forecastJsonString)
            if (localResponse != null)
                WeatherResult.CachedForecast(localResponse, weatherEntry.timestamp)
            else
                WeatherResult.NoData()
        }
    }

    // Saves the forecast as a json string to the database
    suspend fun cacheForecast(forecastResponse: ForecastResponse) = withContext(Dispatchers.IO){
        val jsonString = moshi.adapter(ForecastResponse::class.java).toJson(forecastResponse)
        val cacheTime = System.currentTimeMillis()
        val weather = WeatherEntry(jsonString, cacheTime)
        weatherDao.insertWeather(weather)
    }

}