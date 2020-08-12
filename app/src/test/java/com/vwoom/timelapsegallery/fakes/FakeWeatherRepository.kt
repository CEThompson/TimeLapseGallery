package com.vwoom.timelapsegallery.fakes

import android.location.Location
import com.vwoom.timelapsegallery.data.repository.IWeatherRepository
import com.vwoom.timelapsegallery.data.source.IWeatherLocalDataSource
import com.vwoom.timelapsegallery.data.source.IWeatherRemoteDataSource
import com.vwoom.timelapsegallery.weather.data.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherResult

class FakeWeatherRepository(private val localDataSource: IWeatherLocalDataSource = FakeLocalDataSource(),
                            private val remoteDataSource: IWeatherRemoteDataSource = FakeRemoteDataSource()) : IWeatherRepository {

    override suspend fun getCachedForecast(): WeatherResult<ForecastResponse> {
        return localDataSource.getCachedWeather()
    }

    override suspend fun updateForecast(location: Location): WeatherResult<ForecastResponse> {
        return remoteDataSource.getForecast(location)
    }
}