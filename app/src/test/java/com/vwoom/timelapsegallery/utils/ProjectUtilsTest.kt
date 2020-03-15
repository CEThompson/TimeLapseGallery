package com.vwoom.timelapsegallery.utils

import androidx.test.platform.app.InstrumentationRegistry
import com.vwoom.timelapsegallery.R
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
        //val context = InstrumentationRegistry.getInstrumentation().context
        externalFilesTestDir.deleteRecursively()

        // When
        val response = ProjectUtils.validateFileStructure(externalFilesTestDir)

        // Then
        assert(response == NO_FILES_IN_DIRECTORY_ERROR)
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