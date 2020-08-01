package com.vwoom.timelapsegallery.gallery

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.repository.fakes.FakeProjectRepository
import com.vwoom.timelapsegallery.data.repository.fakes.FakeTagRepository
import com.vwoom.timelapsegallery.data.repository.fakes.FakeWeatherRepository
import com.vwoom.timelapsegallery.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config


@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(AndroidJUnit4::class)
class GalleryViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun galleryViewModel_whenInitialized_fourProjectsExist() {
        val galleryViewModel = GalleryViewModel(
                projectRepository = FakeProjectRepository(),
                tagRepository = FakeTagRepository(),
                weatherRepository = FakeWeatherRepository())
        val projects = galleryViewModel.projects.getOrAwaitValue()
        assert(projects.size==4)
    }



    // Live data test template
    @Test
    fun galleryViewModelTest_variousFiltersApplied_allAssertionsPass () = runBlockingTest{
        // Given a view model
        /*val context: Context = ApplicationProvider.getApplicationContext()
        */
        val galleryViewModel = GalleryViewModel(
                projectRepository = FakeProjectRepository(),
                tagRepository = FakeTagRepository(),
                weatherRepository = FakeWeatherRepository())

        // When
        galleryViewModel.searchTags = arrayListOf(TagEntry("cat"), TagEntry("orange"))
        galleryViewModel.filterProjects()

        // Then
        var filterResult = galleryViewModel.displayedProjectViews.getOrAwaitValue()
        assert(filterResult.isEmpty())

        // When
        galleryViewModel.searchTags = arrayListOf(TagEntry("one"))
        galleryViewModel.filterProjects()

        // Then
        filterResult = galleryViewModel.displayedProjectViews.getOrAwaitValue()
        assert(filterResult.size == 1)


        // When
        galleryViewModel.searchTags = arrayListOf(TagEntry("zero"), TagEntry("one"))
        galleryViewModel.filterProjects()

        // Then
        filterResult = galleryViewModel.displayedProjectViews.getOrAwaitValue()
        assert(filterResult.size == 2)

        // When
        galleryViewModel.searchTags = arrayListOf(TagEntry("zero"), TagEntry("one"), TagEntry("two"))
        galleryViewModel.filterProjects()

        // Then
        filterResult = galleryViewModel.displayedProjectViews.getOrAwaitValue()
        assert(filterResult.size == 3)

        // When
        galleryViewModel.searchTags = arrayListOf(TagEntry("zero"), TagEntry("one"), TagEntry("two"), TagEntry("three"))
        galleryViewModel.filterProjects()

        // Then
        filterResult = galleryViewModel.displayedProjectViews.getOrAwaitValue()
        assert(filterResult.size == 4)
    }

    // Live data test template
    @Test
    fun galleryViewModelTest_fourProjectsFiltered_onlyTwoRemain () = runBlockingTest{
        // Given a view model
        val galleryViewModel = GalleryViewModel(
                projectRepository = FakeProjectRepository(),
                tagRepository = FakeTagRepository(),
                weatherRepository = FakeWeatherRepository())

        // When we filter for two projects
        galleryViewModel.searchTags = arrayListOf(TagEntry(1, "one"), TagEntry(2,"two"))
        galleryViewModel.filterProjects()

        // Then projects should contain two items
        val filterResult = galleryViewModel.displayedProjectViews.getOrAwaitValue()
        //for (project in filterResult) println(project)
        assert(filterResult.size == 2)
    }

}