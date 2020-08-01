package com.vwoom.timelapsegallery.data.repository

import android.location.Location
import com.vwoom.timelapsegallery.data.source.fakes.FakeLocalDataSource
import com.vwoom.timelapsegallery.data.source.FakeRemoteDataSource
import com.vwoom.timelapsegallery.weather.WeatherResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

import org.junit.Before

@ExperimentalCoroutinesApi
class WeatherRepositoryTest {

    private lateinit var weatherLocalDataSource: FakeLocalDataSource
    private lateinit var weatherRemoteDataSource: FakeRemoteDataSource
    private lateinit var weatherRepository: WeatherRepository

    @Before
    fun setup(){
        weatherLocalDataSource = FakeLocalDataSource()
        weatherRemoteDataSource = FakeRemoteDataSource()
        weatherRepository = WeatherRepository(weatherLocalDataSource, weatherRemoteDataSource)
    }

    @Test
    fun getCachedForecast() = runBlockingTest {
        val weatherResult = weatherRepository.getCachedForecast()
        assert(weatherResult is WeatherResult.NoData)
    }

    @Test
    fun updateForecast() = runBlockingTest {
        weatherLocalDataSource.forecastJsonString = FakeLocalDataSource.TEST_JSON
        val location = Location("fake_provider")
        val weatherResult = weatherRepository.updateForecast(location)
        assert(weatherResult !is WeatherResult.NoData)
    }


}