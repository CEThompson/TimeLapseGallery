package com.vwoom.timelapsegallery.utils

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.entry.*
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.settings.ValidationResult
import com.vwoom.timelapsegallery.utils.ImportUtils.validateFileStructure
import com.vwoom.timelapsegallery.utils.ImportUtils.importProjects
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
class ImportUtilsTest {

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
        FileUtils.writeProjectTagsFile(
                externalFilesTestDir,
                projectOneDefinition.project_id,
                listOf(TagEntry(1, "c")))
        FileUtils.writeProjectScheduleFile(externalFilesTestDir,
                projectOneDefinition.project_id,
                ProjectScheduleEntry(1,1))

        // Create files for actual project 2
        val projectTwoDir = File(externalFilesTestDir, "3_test two")
        projectTwoDir.mkdirs()
        val projectTwoPhoto = File(projectTwoDir, "400")
        projectTwoPhoto.createNewFile()
        val projectTwoMetaDir = File(projectTwoDir, FileUtils.META_FILE_SUBDIRECTORY)
        projectTwoMetaDir.mkdirs()
        val projectTwoDefinition = Project(3,"test two",0,2,400)
        FileUtils.writeProjectTagsFile(
                externalFilesTestDir,
                projectTwoDefinition.project_id,
                listOf(TagEntry(2, "d")))
        FileUtils.writeProjectScheduleFile(externalFilesTestDir,
                projectTwoDefinition.project_id,
                ProjectScheduleEntry(3,3))

        // When: Projects are imported
        val result = validateFileStructure(externalFilesTestDir)
        assert(result is ValidationResult.Success<List<ImportUtils.ProjectDataBundle>>)
        val projectBundles = (result as ValidationResult.Success<List<ImportUtils.ProjectDataBundle>>).data
        runBlocking {importProjects(db, externalFilesTestDir, projectBundles, true)}

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
            assert(tag.text == "c")
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
            assert(tag.text=="d")
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


    @Test
    fun validateFileStructure_emptyFileStructure_returnsNoFilesInDirectory() {
        // Given
        externalFilesTestDir.deleteRecursively()

        // When
        val response = validateFileStructure(externalFilesTestDir)

        // Then
        assert(response is ValidationResult.Error.NoFilesError)
    }

    @Test
    fun validateFileStructure_invalidFolder_returnsInvalidFolderError() {
        // Given
        externalFilesTestDir.deleteRecursively()
        val projectFile = File(externalFilesTestDir, "project with no ID")
        projectFile.mkdirs()

        // When
        val response = validateFileStructure(externalFilesTestDir)

        // Then
        assert(response is ValidationResult.Error.InvalidFolder)
    }


    @Test
    fun validateFileStructure_fileNotTimestamp_returnsInvalidPhotoFileError() {
        // Given
        externalFilesTestDir.deleteRecursively()
        val projectFile = File(externalFilesTestDir, "1_test project")
        projectFile.mkdirs()
        val photoFile = File(projectFile, "1234nottimestamp.jpeg")
        photoFile.createNewFile()

        // When
        val response = validateFileStructure(externalFilesTestDir)

        // Then
        assert(response is ValidationResult.Error.InvalidPhotoFileError)
    }

    @Test
    fun validateFileStructure_projectsWithSameId_returnsDuplicateIdError() {
        // Given
        externalFilesTestDir.deleteRecursively()

        // Project 1
        val projectFile = File(externalFilesTestDir, "1_test project")
        projectFile.mkdirs()
        File(projectFile, "123456789.jpeg").createNewFile()

        // Project 2
        val projectFileTwo = File(externalFilesTestDir, "1_test project2")
        projectFileTwo.mkdirs()
        File(projectFileTwo, "123456789.jpeg").createNewFile()

        // When
        val response = validateFileStructure(externalFilesTestDir)
        println(response)
        // Then
        assert(response is ValidationResult.Error.DuplicateIdError)
    }

    @Test
    fun validateFileStructure_projectsWithNameContainingInvalidCharacters_returnsInvalidCharacterError() {
        // Given
        //val context = InstrumentationRegistry.getInstrumentation().context
        for (character in RESERVED_CHARACTERS) {
            println(character)
            if (character == '|'
                    || character == '\\'
                    || character == '?'
                    || character == '*'
                    || character == '*'
                    || character == '<'
                    || character == '"'
                    || character == ':'
                    || character == '>'
                    || character == '\''
                    || character == '/') continue
            externalFilesTestDir.deleteRecursively()
            val projectFile = File(externalFilesTestDir, "1_test project $character")
            projectFile.mkdirs()
            println(projectFile.absolutePath)
            File(projectFile, "123456789.jpeg").createNewFile()

            // When
            val response = validateFileStructure(externalFilesTestDir)

            // Then
            println(response)
            assert(response is ValidationResult.Error.InvalidCharacterError)
        }
    }

    @Test
    fun validateFileStructure_validStructure_returnsValidDirectoryStructure() {
        // Given
        //val context = InstrumentationRegistry.getInstrumentation().context
        externalFilesTestDir.deleteRecursively()

        val projectFolder = File(externalFilesTestDir, "1_test project")
        projectFolder.mkdirs()
        File(projectFolder, "1234567.jpeg").createNewFile()
        val projectFolderTwo = File(externalFilesTestDir, "2_test project")
        projectFolderTwo.mkdirs()
        File(projectFolderTwo, "2345671.jpeg").createNewFile()

        // When
        val response = validateFileStructure(externalFilesTestDir)

        // Then
        assert(response is ValidationResult.Success<List<ImportUtils.ProjectDataBundle>>)
    }

}