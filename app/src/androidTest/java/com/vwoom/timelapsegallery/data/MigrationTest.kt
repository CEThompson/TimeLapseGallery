package com.vwoom.timelapsegallery.data

import android.util.Log
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.vwoom.timelapsegallery.data.entry.WeatherEntry
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class MigrationTest {
    @Rule
    @JvmField
    val testHelper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            TimeLapseDatabase::class.java.canonicalName,
            FrameworkSQLiteOpenHelperFactory())

    @Test
    @Throws(IOException::class)
    fun migrationFrom1To2_containsCorrectData() {
        // Given an old version of the database
        // With two projects entered
        // With 2 photos for project 1 and 3 photos for project 2
        val db = testHelper.createDatabase(TEST_DB_NAME, 1)
        db.apply {
            // Insert project 1 into the database
            execSQL("INSERT INTO project (id, name, thumbnail_url, schedule, schedule_next_submission, timestamp) " +
                    "VALUES (1, 'test name one', 'thumbnail_url_1', 1, 1, 1)")
            // Insert two photos for project 1
            execSQL("INSERT INTO photo (id, project_id, url, timestamp) " +
                    "VALUES (1, 1, 'photo_1_url', 111)")
            execSQL("INSERT INTO photo (id, project_id, url, timestamp) " +
                    "VALUES (2, 1, 'photo_2_url', 222)")
            // Insert project 2 into the database
            execSQL("INSERT INTO project (id, name, thumbnail_url, schedule, schedule_next_submission, timestamp) " +
                    "VALUES (2, 'test name two', 'thumbnail_url_2', 2, 2, 2)")
            // Insert three photos for project 2
            execSQL("INSERT INTO photo (id, project_id, url, timestamp) " +
                    "VALUES (3, 2, 'photo_3_url', 333)")
            execSQL("INSERT INTO photo (id, project_id, url, timestamp) " +
                    "VALUES (4, 2, 'photo_4_url', 444)")
            execSQL("INSERT INTO photo (id, project_id, url, timestamp) " +
                    "VALUES (5, 2, 'photo_5_url', 555)")
            // Close db version 1
            close()
        }

        // When we run the migrations and validate
        testHelper.runMigrationsAndValidate(TEST_DB_NAME, 2, true, TimeLapseDatabase.MIGRATION_1_2)

        // Then the resulting database should have the same data
        val resultDb = getMigratedRoomDatabase()

        val projects = resultDb.projectDao().getProjects()
        val photos = resultDb.photoDao().getPhotos()
        Log.d(TAG, "$projects")
        Log.d(TAG, "$photos")

        // Make various assertions on the retrieved data
        assert(projects.size == 2)
        assert(photos.size == 5)
        assert(projects[0].id == 1.toLong())
        assert(projects[0].project_name == "test name one")
        assert(projects[1].id == 2.toLong())
        assert(projects[1].project_name == "test name two")

        val photosForProjectOne = resultDb.photoDao().getPhotosByProjectId(projects[0].id)
        assert(photosForProjectOne.size == 2)
        val photosForProjectTwo = resultDb.photoDao().getPhotosByProjectId(projects[1].id)
        assert(photosForProjectTwo.size == 3)

        // make sure to close the database
        resultDb.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrationFrom2To3_containsCorrectData() {
        // Given an old version of the database
        // With 2 photos entered for a project
        val db = testHelper.createDatabase(TEST_DB_NAME, 1)
        db.apply {
            // Insert a project into the database
            execSQL("INSERT INTO project (id, name, thumbnail_url, schedule, schedule_next_submission, timestamp) " +
                    "VALUES (1, 'test name one', 'thumbnail_url_1', 1, 1, 1)")
            // Insert two photos for project 1
            execSQL("INSERT INTO photo (id, project_id, url, timestamp) " +
                    "VALUES (1, 1, 'photo_1_url', 111)")
            execSQL("INSERT INTO photo (id, project_id, url, timestamp) " +
                    "VALUES (2, 1, 'photo_2_url', 222)")
            // Close db version 2
            close()
        }

        // When we run the migrations and validate
        testHelper.runMigrationsAndValidate(TEST_DB_NAME, 2, true, TimeLapseDatabase.MIGRATION_1_2)
        testHelper.runMigrationsAndValidate(TEST_DB_NAME, 3, true, TimeLapseDatabase.MIGRATION_2_3)
        val resultDb = getMigratedRoomDatabase()

        // Assert that resulting database persists the projects on migration
        val projects = resultDb.projectDao().getProjects()
        val photos = resultDb.photoDao().getPhotos()
        assert(projects.size == 1)
        assert(photos.size == 2)
        assert(projects[0].id == 1.toLong())
        assert(projects[0].project_name == "test name one")


        // Assert that weather table now useful
        var weather = runBlocking { resultDb.weatherDao().getWeather() }

        // First assert no entry
        assert(weather == null)

        // Insert an entry
        val testTime = System.currentTimeMillis()
        val forecastString = "test"
        val weatherEntry = WeatherEntry(forecastString, testTime)
        runBlocking { resultDb.weatherDao().insertWeather(weatherEntry) }

        // Check for entry match
        weather = runBlocking { resultDb.weatherDao().getWeather() }
        assert(weather?.forecast == forecastString)
        assert(weather?.timestamp == testTime)

        // Check deletion
        runBlocking { resultDb.weatherDao().deleteWeather() }
        weather = runBlocking { resultDb.weatherDao().getWeather() }
        assert(weather == null)

        // Clean up
        resultDb.close()
    }

    private fun getMigratedRoomDatabase(): TimeLapseDatabase {
        val database: TimeLapseDatabase = Room.databaseBuilder(ApplicationProvider.getApplicationContext(),
                TimeLapseDatabase::class.java, TEST_DB_NAME)
                .addMigrations(TimeLapseDatabase.MIGRATION_1_2)
                .addMigrations(TimeLapseDatabase.MIGRATION_2_3)
                .build()
        // close the database and release any stream resources when the test finishes
        testHelper.closeWhenFinished(database)
        return database
    }

    companion object {
        private const val TEST_DB_NAME = "test-db"
        private val TAG = MigrationTest::class.java.simpleName
    }

}