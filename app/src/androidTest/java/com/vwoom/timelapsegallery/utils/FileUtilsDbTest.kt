package com.vwoom.timelapsegallery.utils

import android.content.Context
import androidx.room.Room
import androidx.room.util.FileUtil
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.Project
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.IOException

@RunWith(AndroidJUnit4ClassRunner::class)
class FilesUtilsDbTest {

    @Rule
    @JvmField
    val testFolder = TemporaryFolder()

    private lateinit var externalFilesTestDir: File
    private lateinit var db: TimeLapseDatabase

    @Before
    fun createDb(){
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TimeLapseDatabase::class.java).build()
        externalFilesTestDir = testFolder.newFolder("pictures")
    }

    @After
    @Throws(IOException::class)
    fun closeDb(){
        db.close()
    }

    @Test
    fun addTagToProject_listOfTwoTags_resultInTwoTagsRepresented() {
        // begin with empty directory and database
        externalFilesTestDir.deleteRecursively()
        runBlocking {
            db.projectDao().deleteAllProjects()
            db.tagDao().deleteAllTags()
        }

        // given
        // a project in the file structure and database
        val projectEntry = ProjectEntry(1, "test name")
        runBlocking { db.projectDao().insertProject(projectEntry) }
        // and a list of tags
        val tags = listOf(TagEntry(1, "test one"), TagEntry(2, "test two"))

        // when we use the utility
        FileUtils.addTagToProject(externalFilesTestDir, projectEntry.id, tags)

        // then
        // each tag retrieved from the database should be in the list passed to the utility
        val projectTagEntries = runBlocking { db.projectTagDao().getProjectTagsByProjectId(projectEntry.id) }
        assert(projectTagEntries.size == tags.size) // two tags should be retrieved since we fed in two tags
        for (tag in projectTagEntries){
            val currentTag = runBlocking { db.tagDao().getTagById(tag.tag_id) }
            assert(tags.contains(currentTag)) // make the assertions
        }
        // and a tag file should exist in the projects meta directory
        val meta = FileUtils.getMetaDirectoryForProject(externalFilesTestDir, projectEntry.id)
        val tagsFile = File(meta, FileUtils.TAGS_DEFINITION_TEXT_FILE)
        assert(tagsFile.exists()) // make the assertion
        // and the text file should contain both of the tags
        val inputAsString = FileInputStream(tagsFile).bufferedReader().use { it.readText() }
        val tagsInFile: List<String> = inputAsString.split('\n')
        for (text in tagsInFile) {
            if (text.isEmpty()) continue
            val currentTag = runBlocking { db.tagDao().getTagByText(text) }
            assert(tags.contains(currentTag)) // make the assertion
        }
    }

    @Test
    fun addTagToProject_emptyTags_resultInZeroTagsRepresented() {
        // begin with empty directory and database
        externalFilesTestDir.deleteRecursively()
        runBlocking {
            db.projectDao().deleteAllProjects()
            db.tagDao().deleteAllTags()
        }

        // given
        // a project in the file structure and database
        val projectEntry = ProjectEntry(1, "test name")
        runBlocking { db.projectDao().insertProject(projectEntry) }
        // and a list of tags
        val tags = emptyList<TagEntry>()

        // when we use the utility
        FileUtils.addTagToProject(externalFilesTestDir, projectEntry.id, tags)

        // then
        // tags retrieved should be an empty list
        val projectTagEntries = runBlocking { db.projectTagDao().getProjectTagsByProjectId(projectEntry.id) }
        assert(projectTagEntries.isEmpty())

        // and an empty tag file should exist in the projects meta directory
        val meta = FileUtils.getMetaDirectoryForProject(externalFilesTestDir, projectEntry.id)
        val tagsFile = File(meta, FileUtils.TAGS_DEFINITION_TEXT_FILE)
        assert(tagsFile.exists()) // make the assertion
        val inputAsString = FileInputStream(tagsFile).bufferedReader().use { it.readText() }
        assert(inputAsString.isEmpty())
    }

    @Test
    fun scheduleProject(){
        // begin with empty directory and database
        externalFilesTestDir.deleteRecursively()
        runBlocking {
            db.projectDao().deleteAllProjects()
            db.tagDao().deleteAllTags()
        }

        // given
        // a project in the file structure and database
        val projectEntry = ProjectEntry(1, "test name")
        runBlocking { db.projectDao().insertProject(projectEntry) }
        // and a schedule entry
        val schedule = ProjectScheduleEntry(projectEntry.id, 7)

        // when we use the utility
        FileUtils.scheduleProject(externalFilesTestDir, projectEntry.id, schedule)

        // then
        // the retrieved schedule from the db exists and represents the interval
        val retrievedSchedule = runBlocking { db.projectScheduleDao().getProjectScheduleByProjectId(projectEntry.id) }
        assert(retrievedSchedule != null)
        assert(retrievedSchedule?.interval_days == 7)

        // and the text file exists and represents the interval
        val meta = FileUtils.getMetaDirectoryForProject(externalFilesTestDir, projectEntry.id)
        val scheduleFile = File(meta, FileUtils.SCHEDULE_TEXT_FILE)
        assert (scheduleFile.exists())
        val inputAsString = FileInputStream(scheduleFile).bufferedReader().use { it.readText() }
        assert(inputAsString.toInt() == 7)

    }

}