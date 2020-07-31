package com.vwoom.timelapsegallery.data.source

import android.location.Location
import com.vwoom.timelapsegallery.weather.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherApi
import com.vwoom.timelapsegallery.weather.WeatherResult

class FakeRemoteDataSource: IWeatherRemoteDataSource {
    override suspend fun getForecast(location: Location): WeatherResult<ForecastResponse> {
        return try {
            val forecastFromStorage: ForecastResponse? = WeatherApi.moshi.adapter(ForecastResponse::class.java)
                    .fromJson(FakeLocalDataSource.TEST_JSON)
            if (forecastFromStorage==null) WeatherResult.NoData()
            else return WeatherResult.TodaysForecast(forecastFromStorage, timestamp = System.currentTimeMillis())
        } catch (e: Exception) {
            WeatherResult.NoData()
        }
    }
}