package com.vwoom.timelapsegallery.data.source

import android.text.format.DateUtils
import com.vwoom.timelapsegallery.data.dao.WeatherDao
import com.vwoom.timelapsegallery.data.entry.WeatherEntry
import com.vwoom.timelapsegallery.weather.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherApi.moshi
import com.vwoom.timelapsegallery.weather.WeatherResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

interface IWeatherLocalDataSource {
    // Gets the forecast from the database
    // Returns either (1) No Data (2) Today's Forecast or (3) A Cached Forecast
    suspend fun getCachedWeather(): WeatherResult<ForecastResponse>

    // Saves the forecast as a json string to the database
    suspend fun cacheForecast(forecastResponse: ForecastResponse)
}

// TODO (1.3): figure out how to properly handle blocking calls in coroutines
@Suppress("BlockingMethodInNonBlockingContext")
class WeatherLocalDataSource
@Inject constructor(private val weatherDao: WeatherDao) : IWeatherLocalDataSource {

    var coroutineContext: CoroutineContext = Dispatchers.IO

    // Gets the forecast from the database
    // Returns either (1) No Data (2) Today's Forecast or (3) A Cached Forecast
    override suspend fun getCachedWeather(): WeatherResult<ForecastResponse> = withContext(coroutineContext) {
        val weatherEntry: WeatherEntry? = weatherDao.getWeather()
        return@withContext if (weatherEntry == null) WeatherResult.NoData()
        else {
            val localResponse: ForecastResponse?
            try {
                localResponse = moshi.adapter(ForecastResponse::class.java)
                        .fromJson(weatherEntry.forecastJsonString)
            } catch (e: Exception) {
                return@withContext WeatherResult.NoData()
            }
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

    // Saves the forecast as a json string to the database
    override suspend fun cacheForecast(forecastResponse: ForecastResponse) = withContext(coroutineContext) {
        val jsonString = moshi.adapter(ForecastResponse::class.java).toJson(forecastResponse)
        val cacheTime = System.currentTimeMillis()
        val weather = WeatherEntry(jsonString, cacheTime)
        weatherDao.insertWeather(weather)
    }

}