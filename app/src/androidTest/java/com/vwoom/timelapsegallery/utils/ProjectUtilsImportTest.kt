package com.vwoom.timelapsegallery.utils

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.entry.*
import com.vwoom.timelapsegallery.data.view.Project
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

@RunWith(AndroidJUnit4ClassRunner::class)
class ProjectUtilsImportTest {

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

    // TODO implement test for importing projects.
    @Test
    fun importProjects() {
        // Given: projects in database that do not match file structure
        // Insert test projects into DB
        runBlocking {
            // Test project one
            db.projectDao().insertProject(ProjectEntry(1, null))
            db.photoDao().insertPhoto(PhotoEntry(1,1, 100))
            db.coverPhotoDao().insertPhoto(CoverPhotoEntry(1,1))
            db.projectScheduleDao().insertProjectSchedule(ProjectScheduleEntry(1,0))
            db.tagDao().insertTag(TagEntry(1,"a"))
            db.projectTagDao().insertProjectTag(ProjectTagEntry(1,1))

            // Test project two
            db.projectDao().insertProject(ProjectEntry(2, null))
            db.photoDao().insertPhoto(PhotoEntry(2,2, 200))
            db.coverPhotoDao().insertPhoto(CoverPhotoEntry(2,2))
            db.projectScheduleDao().insertProjectSchedule(ProjectScheduleEntry(2,1))
            db.tagDao().insertTag(TagEntry(2,"b"))
            db.projectTagDao().insertProjectTag(ProjectTagEntry(2,2))
        }

        // Create files for actual project one
        val projectOneDir = File(externalFilesTestDir, "1_test")
        projectOneDir.mkdirs()
        val projectOnePhoto = File(projectOneDir, "300")
        projectOnePhoto.createNewFile()
        val projectOneMetaDir = File(projectOneDir, FileUtils.META_FILE_SUBDIRECTORY)
        projectOneMetaDir.mkdirs()
        val projectOneDefinition = Project(1,"test",0,1,300)
        FileUtils.addTagToProject(
                externalFilesTestDir,
                projectOneDefinition,
                listOf(TagEntry(1, "c")))
        FileUtils.scheduleProject(externalFilesTestDir,
                projectOneDefinition,
                ProjectScheduleEntry(1,1))

        // Create files for actual project 2
        val projectTwoDir = File(externalFilesTestDir, "3_test two")
        projectTwoDir.mkdirs()
        val projectTwoPhoto = File(projectTwoDir, "400")
        projectTwoPhoto.createNewFile()
        val projectTwoMetaDir = File(projectTwoDir, FileUtils.META_FILE_SUBDIRECTORY)
        projectTwoMetaDir.mkdirs()
        val projectTwoDefinition = Project(3,"test two",0,2,400)
        FileUtils.addTagToProject(
                externalFilesTestDir,
                projectTwoDefinition,
                listOf(TagEntry(2, "d")))
        FileUtils.scheduleProject(externalFilesTestDir,
                projectTwoDefinition,
                ProjectScheduleEntry(3,3))

        // When: Projects are imported
        runBlocking {ProjectUtils.importProjects(db, externalFilesTestDir)}

        // Then: Database should match file structure
        runBlocking {
            // Assert state of project 1 files matches state of database
            var projectEntry = db.projectDao().getProjectById(1)
            assert(projectEntry.project_name == "test")
            var photoEntry = db.photoDao().getLastPhoto(1)
            assert(photoEntry.timestamp == 300.toLong())
            var tags = db.projectTagDao().getProjectTagsByProjectId(1)
            assert(tags.size==1)
            var tag = db.tagDao().getTagById(tags[0].tag_id)
            assert(tag.tag == "c")
            var projectSchedule = db.projectScheduleDao().getProjectScheduleByProjectId(1)
            assert(projectSchedule?.interval_days == 1)

            // Assert state of project 2 files matches state of database
            projectEntry = db.projectDao().getProjectById(3)
            assert(projectEntry.project_name =="test two")
            photoEntry = db.photoDao().getLastPhoto(3)
            assert (photoEntry.timestamp == 400.toLong())
            tags = db.projectTagDao().getProjectTagsByProjectId(3)
            assert(tags.size==1)
            tag = db.tagDao().getTagById(tags[0].tag_id)
            assert(tag.tag=="d")
            projectSchedule = db.projectScheduleDao().getProjectScheduleByProjectId(3)
            assert(projectSchedule?.interval_days == 3)

            // Check database deletions: These assertions check that the database was cleared!
            val projects = db.projectDao().getProjects()
            assert(projects.size == 2)

            val projectSchedules = db.projectScheduleDao().getProjectSchedules()
            assert(projectSchedules.size==2)

            val projectTags = db.projectTagDao().getProjectTags()
            assert(projectTags.size == 2)

            val allTags = db.tagDao().getTags()
            assert(allTags.size == 2)

            val coverPhotos = db.coverPhotoDao().getCoverPhotos()
            assert(coverPhotos.size == 2)

            val photos = db.photoDao().getPhotos()
            assert(photos.size==2)
        }

    }
}