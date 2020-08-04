package com.vwoom.timelapsegallery.gallery

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.vwoom.timelapsegallery.MainCoroutineRule
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.repository.fakes.FakeProjectRepository
import com.vwoom.timelapsegallery.data.repository.fakes.FakeTagRepository
import com.vwoom.timelapsegallery.data.repository.fakes.FakeWeatherRepository
import com.vwoom.timelapsegallery.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// TODO: figure out if robolectric can be removed from this test and gradle

@ExperimentalCoroutinesApi
//@Config(sdk = [Build.VERSION_CODES.O_MR1])
//@RunWith(AndroidJUnit4::class)
class GalleryViewModelTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var galleryViewModel: GalleryViewModel

    @Before
    fun setup() {
        galleryViewModel = GalleryViewModel(
                projectRepository = FakeProjectRepository(),
                tagRepository = FakeTagRepository(),
                weatherRepository = FakeWeatherRepository())
    }

    @Test
    fun galleryViewModel_whenInitialized_fourProjectsExist() = mainCoroutineRule.runBlockingTest {
        // Given a created view model
        // When we get the test projects
        val projects = galleryViewModel.projects.getOrAwaitValue()
        // Then test projects size is four
        assert(projects.size == 4)
    }


    @Test
    fun galleryViewModelTest_variousFiltersApplied_allAssertionsPass() = mainCoroutineRule.runBlockingTest {
        // Given a view model

        // When we filter for tags that no project has
        galleryViewModel.searchTags = arrayListOf(TagEntry("cat"), TagEntry("orange"))
        galleryViewModel.filterProjects()

        // Then no projects are displayed
        var filterResult = galleryViewModel.displayedProjectViews.getOrAwaitValue()
        assert(filterResult.isEmpty())

        // When we filter for one tag
        galleryViewModel.searchTags = arrayListOf(TagEntry("one"))
        galleryViewModel.filterProjects()

        // Then one project is displayed
        filterResult = galleryViewModel.displayedProjectViews.getOrAwaitValue()
        assert(filterResult.size == 1)

        // When we filter for two tags
        galleryViewModel.searchTags = arrayListOf(TagEntry("zero"), TagEntry("one"))
        galleryViewModel.filterProjects()

        // Then two projects
        filterResult = galleryViewModel.displayedProjectViews.getOrAwaitValue()
        assert(filterResult.size == 2)

        // When three tags
        galleryViewModel.searchTags = arrayListOf(TagEntry("zero"), TagEntry("one"), TagEntry("two"))
        galleryViewModel.filterProjects()

        // Then three projects
        filterResult = galleryViewModel.displayedProjectViews.getOrAwaitValue()
        assert(filterResult.size == 3)

        // When four
        galleryViewModel.searchTags = arrayListOf(TagEntry("zero"), TagEntry("one"), TagEntry("two"), TagEntry("three"))
        galleryViewModel.filterProjects()

        // Then four
        filterResult = galleryViewModel.displayedProjectViews.getOrAwaitValue()
        assert(filterResult.size == 4)
    }

}