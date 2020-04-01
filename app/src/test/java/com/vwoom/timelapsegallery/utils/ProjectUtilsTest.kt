package com.vwoom.timelapsegallery.utils

import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.settings.ValidationResult
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

class ProjectUtilsTest {

    @Test
    fun isProjectDueToday() {
        // Given
        val timestampTwoDaysAgo = System.currentTimeMillis() - (DAY_IN_MILLISECONDS*2)
        val project = Project(1, null, 2, 1, timestampTwoDaysAgo)
        // When
        val isProjectDueToday = ProjectUtils.isProjectDueToday(project)
        // Then
        assertTrue(isProjectDueToday)
    }

    @Test
    fun isProjectDueTomorrow() {
        // Given
        val project = Project(1, null, 1, 1, System.currentTimeMillis())
        // When
        val isProjectDueTomorrow = ProjectUtils.isProjectDueTomorrow(project)
        // Then
        assertTrue(isProjectDueTomorrow)
    }
}