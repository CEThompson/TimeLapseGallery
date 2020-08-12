package com.vwoom.timelapsegallery.fakes

import android.location.Location
import com.vwoom.timelapsegallery.data.source.IWeatherRemoteDataSource
import com.vwoom.timelapsegallery.testing.TestForecastResponse
import com.vwoom.timelapsegallery.weather.data.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherResult

class FakeRemoteDataSource(var updateSuccess: Boolean = true): IWeatherRemoteDataSource {
    override suspend fun getForecast(location: Location): WeatherResult<ForecastResponse> {
        return try {
            val forecastFromStorage: ForecastResponse? = TestForecastResponse.getForecastResponse(TestForecastResponse.TEST_JSON)
            if (forecastFromStorage==null) WeatherResult.NoData()
            else {
                return if (updateSuccess)
                    WeatherResult.TodaysForecast(forecastFromStorage, timestamp = System.currentTimeMillis())
                else
                    WeatherResult.NoData()
            }
        } catch (e: Exception) {
            WeatherResult.NoData()
        }
    }
}