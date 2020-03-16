package com.vwoom.timelapsegallery.utils

import androidx.test.platform.app.InstrumentationRegistry
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

class ProjectUtilsTest {

    @Rule
    @JvmField
    val testFolder = TemporaryFolder()

    private lateinit var externalFilesTestDir: File

    @Before
    fun setUp(){
        externalFilesTestDir = testFolder.newFolder("pictures")
    }

    @Test
    fun validateFileStructure_emptyFileStructure_returnsNoFilesInDirectory() {
        // Given
        externalFilesTestDir.deleteRecursively()

        // When
        val response = ProjectUtils.validateFileStructure(externalFilesTestDir)

        // Then
        assert(response == NO_FILES_IN_DIRECTORY_ERROR)
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
        val response = ProjectUtils.validateFileStructure(externalFilesTestDir)

        // Then
        assert(response == INVALID_PHOTO_FILE_ERROR)
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
        val response = ProjectUtils.validateFileStructure(externalFilesTestDir)
        println(response)
        // Then
        assert(response == DUPLICATE_ID_ERROR)
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
            val response = ProjectUtils.validateFileStructure(externalFilesTestDir)

            // Then
            println(response)
            assert(response == INVALID_CHARACTER_ERROR)
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
        val response = ProjectUtils.validateFileStructure(externalFilesTestDir)

        // Then
        assert(response == VALID_DIRECTORY_STRUCTURE)
    }

    @Test
    fun importProjects() {
    }

    @Test
    fun isProjectDueToday() {
    }

    @Test
    fun isProjectDueTomorrow() {
    }
}