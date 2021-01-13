package com.vwoom.timelapsegallery.data.source

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.weather.WeatherResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4ClassRunner::class)
@MediumTest
class WeatherLocalDataSourceIntegrationTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var dataSource: WeatherLocalDataSource
    private lateinit var db: TimeLapseDatabase

    @Before
    fun setup(){
        db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                TimeLapseDatabase::class.java
        )
                .allowMainThreadQueries()
                .build()
        // TODO restore test by injecting dependencies
        dataSource = WeatherLocalDataSource(db.weatherDao(), MoshiHelper.instance) // TODO inject dispatcher
        dataSource.coroutineContext = Dispatchers.Main
    }

    @After
    fun cleanup(){
        db.close()
    }

    @Test
    fun getCachedWeather_noCache_cacheIsNull() = runBlocking {
        // GIVEN no cache stored (db is simply initialized)
        // When we retrieve the cache
        val cache = dataSource.getCachedWeather()
        // It is result no data
        assertTrue(cache is WeatherResult.NoData)
    }

    // TODO implement other integration tests

}