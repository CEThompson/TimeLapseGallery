package com.vwoom.timelapsegallery.data.repository.fakes

import android.location.Location
import com.vwoom.timelapsegallery.data.repository.IWeatherRepository
import com.vwoom.timelapsegallery.data.source.fakes.FakeLocalDataSource
import com.vwoom.timelapsegallery.data.source.FakeRemoteDataSource
import com.vwoom.timelapsegallery.weather.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherResult

class FakeWeatherRepository : IWeatherRepository {

    val fakeLocalDataSource = FakeLocalDataSource()
    val fakeRemoteDataSource = FakeRemoteDataSource()

    override suspend fun getCachedForecast(): WeatherResult<ForecastResponse> {
        return fakeLocalDataSource.getCachedWeather()
    }

    override suspend fun updateForecast(location: Location): WeatherResult<ForecastResponse> {
        return fakeRemoteDataSource.getForecast(location)
    }
}