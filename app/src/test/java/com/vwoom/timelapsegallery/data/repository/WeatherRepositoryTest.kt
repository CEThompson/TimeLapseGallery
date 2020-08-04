package com.vwoom.timelapsegallery.data.repository

import android.location.Location
import com.vwoom.timelapsegallery.MainCoroutineRule
import com.vwoom.timelapsegallery.data.source.fakes.FakeLocalDataSource
import com.vwoom.timelapsegallery.data.source.fakes.FakeRemoteDataSource
import com.vwoom.timelapsegallery.testing.TestForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// TODO use and inject test dispatcher for coroutines

@ExperimentalCoroutinesApi
class WeatherRepositoryTest {

    private lateinit var weatherLocalDataSource: FakeLocalDataSource
    private lateinit var weatherRemoteDataSource: FakeRemoteDataSource
    private lateinit var weatherRepository: WeatherRepository

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        // TODO inject test dispatcher somewhere here ... this will take the form of passing in Dispatchers.Main to the repository
        weatherLocalDataSource = FakeLocalDataSource()
        weatherRemoteDataSource = FakeRemoteDataSource()
        weatherRepository = WeatherRepository(weatherLocalDataSource, weatherRemoteDataSource)
    }

    @Test
    fun getCachedForecast_whenInitialized_isEmpty() = mainCoroutineRule.runBlockingTest {
        // Given an initialized weather repository without local cache data
        weatherLocalDataSource.forecastJsonString = null

        // When we get the cached forecast
        val weatherResult = weatherRepository.getCachedForecast()

        // Then the result is WeatherResult.NoData
        assert(weatherResult is WeatherResult.NoData)
    }

    @Test
    fun getCachedForecast_whenNotForToday_isCachedForecast() = mainCoroutineRule.runBlockingTest {
        // Given a repository where the local data source is set with a cache that does not belong to today
        weatherLocalDataSource.forecastJsonString = TestForecastResponse.TEST_JSON
        weatherLocalDataSource.isToday = false

        // When we get the cache
        val weatherResult = weatherRepository.getCachedForecast()

        // Then the result is today's forecast or a cached forecast
        assert(weatherResult is WeatherResult.CachedForecast)
    }

    @Test
    fun getCachedForecast_whenCacheIsSetForToday_isTodaysForecast() = mainCoroutineRule.runBlockingTest {
        // Given a repository where the local data source is set with a cache that does not belong to today
        weatherLocalDataSource.forecastJsonString = TestForecastResponse.TEST_JSON
        weatherLocalDataSource.isToday = true

        // When we get the cache
        val weatherResult = weatherRepository.getCachedForecast()
        
        // Then the result is today's forecast or a cached forecast
        assert(weatherResult is WeatherResult.TodaysForecast)
    }


    @Test
    fun updateForecast_whenRemoteUpdateSuccessful_resultIsTodaysForecast() = mainCoroutineRule.runBlockingTest {
        // Given a repository where the local data source is empty
        weatherLocalDataSource.forecastJsonString = null
        // and there is a simulated connection
        weatherRemoteDataSource.updateSuccess = true

        // When we call the update code
        val location = Location("test_provider")
        val weatherResult = weatherRepository.updateForecast(location)

        // Then the result is todays forecast
        assert(weatherResult is WeatherResult.TodaysForecast)
    }

    @Test
    fun updateForecast_whenRemoteUpdateFails_resultIsTheLocalCache() = mainCoroutineRule.runBlockingTest {
        // 1.
        // Given a repository where the local data source is empty
        weatherLocalDataSource.forecastJsonString = null
        // and there is a simulated failed connection
        weatherRemoteDataSource.updateSuccess = false

        // When we call the update code
        var location = Location("test_provider")
        var weatherResult = weatherRepository.updateForecast(location)

        // Then the result is the local cache
        assert(weatherResult is WeatherResult.NoData)


        // 2.
        // Given a repository where the local data source is empty
        weatherLocalDataSource.forecastJsonString = TestForecastResponse.TEST_JSON
        weatherLocalDataSource.isToday = false

        // and there is a simulated failed connection
        weatherRemoteDataSource.updateSuccess = false

        // When we call the update code
        location = Location("test_provider")
        weatherResult = weatherRepository.updateForecast(location)

        // Then the result is the local cache
        assert(weatherResult is WeatherResult.CachedForecast)


        // 3.
        // Given a repository where the local data source is empty
        weatherLocalDataSource.forecastJsonString = TestForecastResponse.TEST_JSON
        weatherLocalDataSource.isToday = true

        // and there is a simulated failed connection
        weatherRemoteDataSource.updateSuccess = false

        // When we call the update code
        location = Location("test_provider")
        weatherResult = weatherRepository.updateForecast(location)

        // Then the result is the local cache
        assert(weatherResult is WeatherResult.TodaysForecast)
    }

}