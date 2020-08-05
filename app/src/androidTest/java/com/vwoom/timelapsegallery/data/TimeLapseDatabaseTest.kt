package com.vwoom.timelapsegallery.data

import androidx.room.Room
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import com.vwoom.timelapsegallery.data.dao.*
import com.vwoom.timelapsegallery.data.entry.WeatherEntry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4ClassRunner::class)
class TimeLapseDatabaseTest {

    // DAOs
    private lateinit var weatherDao: WeatherDao
    private lateinit var projectDao: ProjectDao
    private lateinit var projectTagDao: ProjectTagDao
    private lateinit var projectScheduleDao: ProjectScheduleDao
    private lateinit var photoDao: PhotoDao
    private lateinit var coverPhotoDao: CoverPhotoDao
    private lateinit var tagDao: TagDao

    // Database
    private lateinit var db: TimeLapseDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, TimeLapseDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        weatherDao = db.weatherDao()
        projectDao = db.projectDao()
        projectTagDao = db.projectTagDao()
        projectScheduleDao = db.projectScheduleDao()
        tagDao = db.tagDao()
        coverPhotoDao = db.coverPhotoDao()
        photoDao = db.photoDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // TODO refactor this to a DAO test
    @Test
    @Throws(Exception::class)
    fun insertWeather() {
        // Should be null without an entry
        var res = runBlocking { weatherDao.getWeather() }
        assertTrue(res == null)

        // Should not be null if an entry is added
        val entry = WeatherEntry(forecastJsonString = "fake_json_string", timestamp = System.currentTimeMillis())
        runBlocking { weatherDao.insertWeather(entry) }
        res = runBlocking { weatherDao.getWeather() }
        assertTrue(res != null)
    }

}