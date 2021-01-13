package com.vwoom.timelapsegallery.gallery

import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith

// TODO convert all test with coroutines to use run blocking test
@ExperimentalCoroutinesApi
//@Config(application = TimeLapseGalleryApplication::class)
@RunWith(AndroidJUnit4ClassRunner::class)
@MediumTest
//@LooperMode(LooperMode.Mode.PAUSED)
class GalleryFragmentTest {

    // TODO figure out how to write fragment scenario tests with Dagger 2 injection
    @Test
    fun galleryFragmentInit_whenLaunchedNormally_UIDisplaysCorrectly() {
        /*val bundle = GalleryFragmentArgs(false).toBundle()
        launchFragmentInContainer<GalleryFragment>(bundle, R.style.AppTheme)
        Thread.sleep(1000)*/
        // TODO do assertions with espresso on gallery fragment UI
    }

    // TODO write mock to test that gallery navigates to detail when project is clicked
    @Test
    fun clickProject_navigateToDetailFragment() = runBlockingTest {
        /*// set repository test items
        val scenario = launchFragmentInContainer<GalleryFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        // WHEN - Click on item
        onView(withId(R.id.gallery_recycler_view)).perform(RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText("TITLE1")), click()
        ))
        // THEN verify that
        verify(navController).navigate(
                GalleryFragmentDirections.actionGalleryFragmentToDetailsFragment(clickedProjectView = projectView)
        )*/
    }

    @Test
    fun clickScrollUp_scrollsGalleryToTop() {
        // TODO test scrolling
    }

    @Test
    fun clickScrollDown_scrollsGalleryToBottom() {
        // TODO test scrolling
    }

    @Test
    fun whenLessThanXProjects_scrollFabsGone(){
        // TODO test scroll state
    }

    @Test
    fun whenMoreThanXProjects_scrollFabsVisible(){
        // TODO test scroll state
    }

    // TODO figure out how to launch fragment scenario with dagger
    /*private val viewModelFactory = mock() // do something else here
    private fun launchFragment(): FragmentScenario<GalleryFragment> {
        return launchFragmentInContainer(factory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return GalleryFragment().apply {
                    viewModelFactory = this@GalleryFragmentTest.viewModelFactory
                    //recyclerViewAdapter = this@GalleryFragmentTest.recyclerViewAdapter
                    //viewModel = this@GalleryFragmentTest.viewModel
                    // assign other deps here as per your needs
                }
            }
        }, themeResId = R.style.AppTheme)
    }*/

}


