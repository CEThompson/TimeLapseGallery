package com.vwoom.timelapsegallery.data.source

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.vwoom.timelapsegallery.data.entry.WeatherEntry
import com.vwoom.timelapsegallery.fakes.FakeWeatherDao
import com.vwoom.timelapsegallery.testing.TestForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// TODO: fully implement tests for local data source
class WeatherLocalDataSourceTest {

    private lateinit var fakeWeatherDao: FakeWeatherDao
    private lateinit var localDataSource: WeatherLocalDataSource

    @Before
    fun setup() {
        fakeWeatherDao = FakeWeatherDao()
        // TODO restore test by injecting with
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        localDataSource = WeatherLocalDataSource(fakeWeatherDao, MoshiHelper.instance)
    }

    @Test
    fun getCachedWeather_whenDaoEmpty_isEmpty() = runBlocking {
        // Given an initialized local data source with an empty entry
        fakeWeatherDao.weatherEntry = null
        // When we get the cache
        val cache = localDataSource.getCachedWeather()
        // Then the cache is empty
        assertTrue(cache is WeatherResult.NoData)
    }

    @Test
    fun getCachedWeather_whenDaoHasCacheForToday_returnsTodaysWeather() = runBlocking {
        // Given an initialized local data source with an empty entry
        fakeWeatherDao.weatherEntry = WeatherEntry(forecastJsonString = TestForecastResponse.TEST_JSON, timestamp = System.currentTimeMillis())
        // When we get the cache
        val cache = localDataSource.getCachedWeather()
        // Then the cache is empty
        assertTrue(cache is WeatherResult.TodaysForecast)
    }

    @Test
    fun getCachedWeather_whenDaoHasCacheForPrevious_returnsCachedWeather() = runBlocking {
        // Given an initialized local data source with an empty entry
        fakeWeatherDao.weatherEntry = WeatherEntry(forecastJsonString = TestForecastResponse.TEST_JSON, timestamp = 1)
        // When we get the cache
        val cache = localDataSource.getCachedWeather()
        // Then the cache is empty
        assertTrue(cache is WeatherResult.CachedForecast)
    }


    @Test
    fun cacheForecast_whenEmptyCacheIsSet_cacheHasData() = runBlocking {
        // Given a local datasource
        fakeWeatherDao.weatherEntry = null

        // When we cache a forecast response
        localDataSource.cacheForecast(TestForecastResponse.getForecastResponse(TestForecastResponse.TEST_JSON)!!)

        // Then the empty cache is filled
        val cache = localDataSource.getCachedWeather()
        assertTrue(cache !is WeatherResult.NoData)
    }

    @Test
    fun cacheForecast_whenFullCacheIsSet_cacheIsOverwritten() = runBlocking {
        // Given a local datasource
        fakeWeatherDao.weatherEntry = WeatherEntry(forecastJsonString = "test", timestamp = 1)

        // When we cache a forecast response
        localDataSource.cacheForecast(TestForecastResponse.getForecastResponse(TestForecastResponse.TEST_JSON)!!)

        // Then the empty cache is filled
        val newEntry = fakeWeatherDao.weatherEntry!!
        assertTrue(newEntry.forecastJsonString != "test")
        assertTrue(newEntry.timestamp != 1.toLong())
    }
}
