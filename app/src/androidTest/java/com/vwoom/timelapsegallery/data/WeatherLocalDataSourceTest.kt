package com.vwoom.timelapsegallery.data

import com.vwoom.timelapsegallery.data.dao.WeatherDao
import com.vwoom.timelapsegallery.data.entry.WeatherEntry
import com.vwoom.timelapsegallery.data.source.WeatherLocalDataSource
import com.vwoom.timelapsegallery.testing.TestForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherResult
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

// TODO implement tests for local data source
class WeatherLocalDataSourceTest {

    private lateinit var fakeWeatherDao: FakeWeatherDao
    private lateinit var localDataSource: WeatherLocalDataSource

    @Before
    fun setup() {
        fakeWeatherDao = FakeWeatherDao()
        localDataSource = WeatherLocalDataSource(fakeWeatherDao)
    }

    @Test
    fun getCachedWeather_whenDaoEmpty_isEmpty() = runBlocking {
        // Given an initialized local data source with an empty entry
        fakeWeatherDao.weatherEntry = null
        // When we get the cache
        val cache = localDataSource.getCachedWeather()
        // Then the cache is empty
        assert(cache is WeatherResult.NoData)
    }
    @Test
    fun getCachedWeather_whenDaoHasCacheForToday_returnsTodaysWeather() = runBlocking {
        // Given an initialized local data source with an empty entry
        fakeWeatherDao.weatherEntry = WeatherEntry(forecastJsonString = TestForecastResponse.TEST_JSON, timestamp = System.currentTimeMillis())
        // When we get the cache
        val cache = localDataSource.getCachedWeather()
        // Then the cache is empty
        assert(cache is WeatherResult.TodaysForecast)
    }

    @Test
    fun getCachedWeather_whenDaoHasCacheForPrevious_returnsCachedWeather() = runBlocking {
        // Given an initialized local data source with an empty entry
        fakeWeatherDao.weatherEntry = WeatherEntry(forecastJsonString = TestForecastResponse.TEST_JSON, timestamp = 1)
        // When we get the cache
        val cache = localDataSource.getCachedWeather()
        // Then the cache is empty
        assert(cache is WeatherResult.CachedForecast)
    }


    @Test
    fun cacheForecast_whenEmptyCacheIsSet_cacheHasData() = runBlocking {
        // Given a local datasource
        fakeWeatherDao.weatherEntry = null

        // When we cache a forecast response
        localDataSource.cacheForecast(TestForecastResponse.getForecastResponse(TestForecastResponse.TEST_JSON)!!)

        // Then the empty cache is filled
        val cache = localDataSource.getCachedWeather()
        assert(cache !is WeatherResult.NoData)
    }

    @Test
    fun cacheForecast_whenFullCacheIsSet_cacheIsOverwritten() = runBlocking {
        // Given a local datasource
        fakeWeatherDao.weatherEntry = WeatherEntry(forecastJsonString = "test", timestamp = 1)

        // When we cache a forecast response
        localDataSource.cacheForecast(TestForecastResponse.getForecastResponse(TestForecastResponse.TEST_JSON)!!)

        // Then the empty cache is filled
        val newEntry = fakeWeatherDao.weatherEntry!!
        assert(newEntry.forecastJsonString != "test")
        assert(newEntry.timestamp != 1.toLong())
    }
}

class FakeWeatherDao: WeatherDao {
    var weatherEntry: WeatherEntry? = null

    override suspend fun getWeather(): WeatherEntry? {
        return weatherEntry
    }

    override suspend fun insertWeather(weatherEntry: WeatherEntry) {
        this.weatherEntry = weatherEntry
    }

    override suspend fun deleteWeather() {
        weatherEntry = null
    }

}