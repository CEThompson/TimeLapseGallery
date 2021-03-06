package com.vwoom.timelapsegallery.data

import android.util.Log
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.vwoom.timelapsegallery.data.Migrations.MIGRATION_1_2
import com.vwoom.timelapsegallery.data.Migrations.MIGRATION_2_3
import com.vwoom.timelapsegallery.data.Migrations.MIGRATION_3_4
import com.vwoom.timelapsegallery.data.Migrations.MIGRATION_4_5
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.WeatherEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
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
        testHelper.runMigrationsAndValidate(TEST_DB_NAME, 2, true, Migrations.MIGRATION_1_2)

        // Then the resulting database should have the same data
        val resultDb = getMigratedRoomDatabase()

        val projects = resultDb.projectDao().getProjects()
        val photos = resultDb.photoDao().getPhotos()
        Log.d(TAG, "$projects")
        Log.d(TAG, "$photos")

        // Make various assertions on the retrieved data
        assertTrue(projects.size == 2)
        assertTrue(photos.size == 5)
        assertTrue(projects[0].id == 1.toLong())
        assertTrue(projects[0].project_name == "test name one")
        assertTrue(projects[1].id == 2.toLong())
        assertTrue(projects[1].project_name == "test name two")

        val photosForProjectOne = resultDb.photoDao().getPhotosByProjectId(projects[0].id)
        assertTrue(photosForProjectOne.size == 2)
        val photosForProjectTwo = resultDb.photoDao().getPhotosByProjectId(projects[1].id)
        assertTrue(photosForProjectTwo.size == 3)

        // make sure to close the database
        resultDb.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrationFrom2To3_containsCorrectData() {
        // Given an old version of the database
        // With 2 photos entered for a project
        val db = testHelper.createDatabase(TEST_DB_NAME, 2)
        db.apply {
            // Insert a project into the database
            execSQL("INSERT INTO project (id, project_name) " +
                    "VALUES (1, 'test name one')")
            // Insert two photos for project 1
            execSQL("INSERT INTO photo (id, project_id, timestamp) " +
                    "VALUES (1, 1, 111)")
            execSQL("INSERT INTO photo (id, project_id, timestamp) " +
                    "VALUES (2, 1, 222)")
            // Close old db
            close()
        }

        // When we run the migrations and validate
        //testHelper.runMigrationsAndValidate(TEST_DB_NAME, 2, true, MIGRATION_1_2)
        testHelper.runMigrationsAndValidate(TEST_DB_NAME, 3, true, MIGRATION_2_3)
        val resultDb = getMigratedRoomDatabase()

        // Assert that resulting database persists the projects on migration
        val projects = resultDb.projectDao().getProjects()
        val photos = resultDb.photoDao().getPhotos()
        assertTrue(projects.size == 1)
        assertTrue(photos.size == 2)
        assertTrue(projects[0].id == 1.toLong())
        assertTrue(projects[0].project_name == "test name one")

        // Assert that weather table now useful
        var weather = runBlocking { resultDb.weatherDao().getWeather() }

        // First assert no entry
        assertTrue(weather == null)

        // Insert an entry
        val testTime = System.currentTimeMillis()
        val forecastString = "test"
        val weatherEntry = WeatherEntry(forecastString, testTime)
        runBlocking { resultDb.weatherDao().insertWeather(weatherEntry) }

        // Check for entry match
        weather = runBlocking { resultDb.weatherDao().getWeather() }
        assertTrue(weather?.forecastJsonString == forecastString)
        assertTrue(weather?.timestamp == testTime)

        // Check deletion
        runBlocking { resultDb.weatherDao().deleteWeather() }
        weather = runBlocking { resultDb.weatherDao().getWeather() }
        assertTrue(weather == null)

        // Clean up
        resultDb.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrationFrom3To4_containsCorrectData() {
        // Given an old version of the database
        // With 2 photos entered for a project
        val db = testHelper.createDatabase(TEST_DB_NAME, 3)
        db.apply {
            // Insert a project into the database
            execSQL("INSERT INTO project (id, project_name) " +
                    "VALUES (1, 'test name one')")
            // Insert two photos for project 1
            execSQL("INSERT INTO photo (id, project_id, timestamp) " +
                    "VALUES (1, 1, 111)")
            close()
        }

        // When we run the migrations and validate
        testHelper.runMigrationsAndValidate(TEST_DB_NAME, 4, true, MIGRATION_3_4)
        val resultDb = getMigratedRoomDatabase()

        // Assert that resulting database persists the projects on migration
        val projects = resultDb.projectDao().getProjects()
        val photos = resultDb.photoDao().getPhotos()
        assertTrue(projects.size == 1)
        assertTrue(photos.size == 1)
        assertTrue(projects[0].id == 1.toLong())
        assertTrue(projects[0].project_name == "test name one")

        // Assert that adding the new column to project table worked
        assertTrue(projects[0].project_updated == 1)

        // Insert an entry
        val newProject = ProjectEntry(2, "project_two")
        runBlocking { resultDb.projectDao().insertProject(newProject) }

        assertTrue(newProject.project_updated==1)

        // Clean up
        resultDb.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrationFrom4To5_containsCorrectData() {
        // Given an old version of the database
        // With 2 photos entered for a project
        val db = testHelper.createDatabase(TEST_DB_NAME, 4)
        db.apply {
            // Insert two photos for project 1
            execSQL("INSERT INTO photo (id, project_id, timestamp) " +
                    "VALUES (1, 1, 111)")
            execSQL("INSERT INTO photo (id, project_id, timestamp) " +
                    "VALUES (2, 1, 222)")// Close db version 2
            close()
        }

        // When we run the migrations and validate
        testHelper.runMigrationsAndValidate(TEST_DB_NAME, 5, true, MIGRATION_4_5)
        val resultDb = getMigratedRoomDatabase()

        // Assert sensor fields have been added
        val photos = resultDb.photoDao().getPhotos()

        assertTrue(photos[0].light == null)
        assertTrue(photos[0].pressure == null)
        assertTrue(photos[0].temp == null)
        assertTrue(photos[0].humidity == null)

        // Clean up
        resultDb.close()
    }


    private fun getMigratedRoomDatabase(): TimeLapseDatabase {
        val database: TimeLapseDatabase = Room.databaseBuilder(ApplicationProvider.getApplicationContext(),
                TimeLapseDatabase::class.java, TEST_DB_NAME)
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_4_5)
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