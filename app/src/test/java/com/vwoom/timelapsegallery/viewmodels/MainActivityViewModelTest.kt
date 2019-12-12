package com.vwoom.timelapsegallery.viewmodels

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun getProjects() {
        // Given - some projects in database
        val mainActivityViewModel = MainActivityViewModel(ApplicationProvider.getApplicationContext())

        // When calling getProjects()


        // Then a list of the projects is returned


    }
}