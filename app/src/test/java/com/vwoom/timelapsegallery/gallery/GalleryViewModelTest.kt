package com.vwoom.timelapsegallery.gallery

import android.content.Context
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.getOrAwaitValue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config


@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(AndroidJUnit4::class)
class GalleryViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Live data test template
    @Test
    fun galleryViewModelTest_projectsFiltered_onlyProjectsWithTagsRemain () {
        // Given a view model
        val context: Context = ApplicationProvider.getApplicationContext()
        // TODO create repository fakes
        /*val galleryViewModel = GalleryViewModel(projectRepository = testProjectRepo, tagRepository = testTagRepo, weatherRepository = testWeatherRepo)

        // When
        galleryViewModel.searchTags = arrayListOf(TagEntry("cat"), TagEntry("orange"))
        galleryViewModel.filterProjects()

        // Then projects should
        val filterResult = galleryViewModel.displayedProjectViews.getOrAwaitValue()
        // TODO make assertion on filter result*/
    }

}