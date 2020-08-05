package com.vwoom.timelapsegallery.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


// TODO: test other DAO methods
// TODO: Convert all test assert methods to assertTrue or assertFalse etc.
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4ClassRunner::class)
@SmallTest
class ProjectDaoTest {

    // For executing synchronously using Architecture Components
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: TimeLapseDatabase

    @Before
    fun initDb() {
        db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                TimeLapseDatabase::class.java)
                .build()
    }

    @After
    fun closeDb() {
        db.close()
    }


    // Ensures that an inserted project may be retrieved by its ID
    @Test
    fun insertProject_projectIsInserted_projectMayBeRetrievedByID() = runBlockingTest {
        // Given a project entry
        val project = ProjectEntry(project_name = "test project")
        // When the project is inserted
        val resultId = db.projectDao().insertProject(project)
        // It may be retrieved by its ID
        val loaded = db.projectDao().getProjectById(resultId)!!
        assertTrue(loaded == project)

        // User timber for android test debug print statements
        //Timber.d("loaded == project is ${loaded == project}")
    }


}