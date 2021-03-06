package com.vwoom.timelapsegallery.data.source

import android.location.Location
import com.vwoom.timelapsegallery.weather.data.ForecastLocationResponse
import com.vwoom.timelapsegallery.weather.data.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherResult
import com.vwoom.timelapsegallery.weather.WeatherService
import javax.inject.Inject

const val ERROR_NO_LOCATION_RESPONSE = "no_location_response"
const val ERROR_NO_RESPONSE = "no_response"

class WeatherRemoteDataSource @Inject constructor(private val weatherService: WeatherService): IWeatherRemoteDataSource {
    // Get the forecast from the national weather service api
    // Returns either (1) Weather Result: No Data or (2) Weather Result: Today's Forecast
    override suspend fun getForecast(location: Location): WeatherResult<ForecastResponse> {
        // 1. Get the url to query the forecast for this devices location
        val forecastLocationResponse: ForecastLocationResponse?
        try {
            val forecastLocationDeferred = weatherService
                    .getForecastLocationAsync(location.latitude.toString(), location.longitude.toString())
            forecastLocationResponse = forecastLocationDeferred.await()
        } catch (e: Exception) {
            return WeatherResult.NoData(e, e.localizedMessage)
        }
        // If the url did not return then no data can be returned
        if (forecastLocationResponse == null) return WeatherResult.NoData(null, ERROR_NO_LOCATION_RESPONSE)

        // 2. Call the url to get the forecast for the location and return the result
        val url = forecastLocationResponse.properties.forecast
        return try {
            val getForecastDeferred = weatherService.getForecastAsync(url)
            val forecastResponse = getForecastDeferred.await()

            // If we have a response return a weather result with the forecast packaged as data and the timestamp
            if (forecastResponse != null)
                WeatherResult.TodaysForecast(forecastResponse, System.currentTimeMillis())
            // Otherwise give back an error
            else WeatherResult.NoData(null, ERROR_NO_RESPONSE)
        } catch (e: Exception) {
            WeatherResult.NoData(e)
        }
    }
}