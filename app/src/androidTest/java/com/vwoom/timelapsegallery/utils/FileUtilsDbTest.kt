package com.vwoom.timelapsegallery.utils

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.utils.ProjectUtils.getMetaDirectoryForProject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.IOException

// TODO: find out how to run these tests with coroutines
// Note: Currently there is an error with coroutines 1.3.7 running these tests, reverting to 1.3.6 fixes this issue
@RunWith(AndroidJUnit4ClassRunner::class)
class FilesUtilsDbTest {
    @Rule
    @JvmField
    val testFolder = TemporaryFolder()

    private lateinit var externalFilesTestDir: File
    private lateinit var db: TimeLapseDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TimeLapseDatabase::class.java).build()
        externalFilesTestDir = testFolder.newFolder("pictures")
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // Ensures that when a project is tagged, tags are represented in database and in a text file
    @Test
    fun addTagToProject_listOfTwoTags_resultInTwoTagsRepresented() {
        // Given
        // A project in the file structure and database
        val projectEntry = ProjectEntry("test name")
        val insertedId = runBlocking { db.projectDao().insertProject(projectEntry) }
        projectEntry.id = insertedId

        // And a list of tags and projectTags
        val tags = listOf(TagEntry(1, "test one"), TagEntry(2, "test two"))
        val projectTags: List<ProjectTagEntry> = listOf(
                ProjectTagEntry(project_id = projectEntry.id, tag_id = 1),
                ProjectTagEntry(project_id = projectEntry.id, tag_id = 2)
        )


        // When we add the project tags to the db
        runBlocking {
            db.tagDao().bulkInsert(tags)
            db.projectTagDao().bulkInsert(projectTags)
        }
        // and when we use the utility to write the tags to file
        FileUtils.writeProjectTagsFile(externalFilesTestDir, projectEntry.id, tags)

        // Then
        // Each tag retrieved from the database should be in the list passed to the utility
        val projectTagEntries = runBlocking { db.projectTagDao().getProjectTagsByProjectId(projectEntry.id) }
        Timber.d("projectTagEntries size = ${projectTagEntries.size}")
        Timber.d("tags size = ${tags.size}")
        assertTrue(projectTagEntries.size == tags.size) // two tags should be retrieved since we fed in two tags
        for (tag in projectTagEntries) {
            val currentTag = runBlocking { db.tagDao().getTagById(tag.tag_id) }
            assertTrue(tags.contains(currentTag)) // make the assertions
        }
        // And a tag file should exist in the projects meta directory
        val meta = getMetaDirectoryForProject(externalFilesTestDir, projectEntry.id)
        val tagsFile = File(meta, FileUtils.TAGS_DEFINITION_TEXT_FILE)
        assertTrue(tagsFile.exists()) // make the assertion
        // And the text file should contain both of the tags
        val inputAsString = FileInputStream(tagsFile).bufferedReader().use { it.readText() }
        val tagsInFile: List<String> = inputAsString.split('\n')
        for (text in tagsInFile) {
            if (text.isEmpty()) continue
            val currentTag = runBlocking { db.tagDao().getTagByText(text) }
            assertTrue(tags.contains(currentTag)) // make the assertion
        }
    }

    // Ensures that when a project is tagged, tags are represented in the database and as a text file
    @Test
    fun addTagToProject_emptyTags_resultInZeroTagsRepresented() {
        // Given
        // A project in the file structure and database
        val projectEntry = ProjectEntry("test name")
        val insertedId = runBlocking { db.projectDao().insertProject(projectEntry) }
        projectEntry.id = insertedId

        // And an empty list of tags
        val tags = emptyList<TagEntry>()
        val projectTags = emptyList<ProjectTagEntry>()

        // When we use the utility and insert into db
        FileUtils.writeProjectTagsFile(externalFilesTestDir, projectEntry.id, tags)
        runBlocking {
            db.tagDao().bulkInsert(tags)
            db.projectTagDao().bulkInsert(projectTags)
        }

        // Then
        // Tags retrieved should be an empty list
        val projectTagEntries = runBlocking { db.projectTagDao().getProjectTagsByProjectId(projectEntry.id) }
        assertTrue(projectTagEntries.isEmpty())

        // Agnd an empty tag file should exist in the projects meta directory
        val meta = getMetaDirectoryForProject(externalFilesTestDir, projectEntry.id)
        val tagsFile = File(meta, FileUtils.TAGS_DEFINITION_TEXT_FILE)
        assertTrue(tagsFile.exists()) // make the assertion
        val inputAsString = FileInputStream(tagsFile).bufferedReader().use { it.readText() }
        assertTrue(inputAsString.isEmpty())
    }

    // Ensures that when a project is scheduled it is represented both in the database and as a text file
    @Test
    fun scheduleProject() {
        // Given
        // A project in the file structure and database
        val projectEntry = ProjectEntry("test name")
        val insertedId = runBlocking { db.projectDao().insertProject(projectEntry) }
        projectEntry.id = insertedId

        // And a schedule entry
        val schedule = ProjectScheduleEntry(projectEntry.id, 7)

        // When we use the utility
        FileUtils.writeProjectScheduleFile(externalFilesTestDir, projectEntry.id, schedule)
        // And insert into the db
        runBlocking { db.projectScheduleDao().insertProjectSchedule(schedule) }

        // Then
        // The retrieved schedule from the db exists and represents the interval
        val retrievedSchedule = runBlocking { db.projectScheduleDao().getProjectScheduleByProjectId(projectEntry.id) }
        assertTrue(retrievedSchedule != null)
        assertTrue(retrievedSchedule?.interval_days == 7)

        // And the text file exists and represents the interval
        val meta = getMetaDirectoryForProject(externalFilesTestDir, projectEntry.id)
        val scheduleFile = File(meta, FileUtils.SCHEDULE_TEXT_FILE)
        assertTrue(scheduleFile.exists())
        val inputAsString = FileInputStream(scheduleFile).bufferedReader().use { it.readText() }
        assertTrue(inputAsString.toInt() == 7)
    }
}