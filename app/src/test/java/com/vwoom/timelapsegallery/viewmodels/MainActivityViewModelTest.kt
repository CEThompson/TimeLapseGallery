package com.vwoom.timelapsegallery.viewmodels

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun getProjects_returnsListOfProjects() {
        // Given - some projects in database
        val mainActivityViewModel = MainActivityViewModel(ApplicationProvider.getApplicationContext())

        // TODO test, livedata WITHOUT robolectric
        // When calling getProjects()
        //val value = mainActivityViewModel.projects.getOrAwaitValue()

        // Then a list of the projects is returned
        //assert(value != null)
    }
}