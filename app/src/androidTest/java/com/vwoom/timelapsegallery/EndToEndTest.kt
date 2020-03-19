package com.vwoom.timelapsegallery

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import com.vwoom.timelapsegallery.gallery.GalleryAdapter
import com.vwoom.timelapsegallery.testing.EspressoIdlingResource
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/*
* This test adds a new project.
* A new photo is added to the project and the project is deleted.
* This should represent the overall basic workflow of the application.
 */
@LargeTest
class EndToEndTest {

    @Rule
    @JvmField
    var mTimeLapseGalleryActivityTestRule = IntentsTestRule(TimeLapseGalleryActivity::class.java)

    // bypasses permission
    @get:Rule
    var permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private lateinit var idlingResource: IdlingResource

    @Before
    fun setUp() {
        idlingResource = EspressoIdlingResource.getIdlingResource()
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @After
    fun cleanUp() {
        IdlingRegistry.getInstance().unregister(idlingResource)
    }

    @Test
    fun endToEndTest() {
        // Click on add project fab, should wait for camera to initialize
        onView(withId(R.id.add_project_FAB)).perform(click())

        // Click on the take picture fab and return to gallery fragment
        onView(withId(R.id.take_picture_fab)).perform(click())

        // Click on the first recycler view item, the project just created
        onView(withId(R.id.gallery_recycler_view)).perform(
                RecyclerViewActions.actionOnItemAtPosition<GalleryAdapter.GalleryAdapterViewHolder>(0, click()))

        // Click on the fab to go to the camera to add a photo to the project
        onView(withId(R.id.add_photo_fab)).perform(click())

        // Click on the fab to take the picture
        onView(withId(R.id.take_picture_fab)).perform(click())

        // Assert that two photos are present in the detail fragment
        var itemCount = mTimeLapseGalleryActivityTestRule.activity
                .findViewById<RecyclerView>(R.id.details_recyclerview).adapter?.itemCount
        assert(itemCount == 2)

        // Use overflow to delete a photo from the project
        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)
        onView(ViewMatchers.withText(R.string.delete_photo)).perform(click())
        onView(ViewMatchers.withText(android.R.string.yes))
                .inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(click())

        // Assert that one item is left
        itemCount = mTimeLapseGalleryActivityTestRule.activity
                .findViewById<RecyclerView>(R.id.details_recyclerview).adapter?.itemCount
        assert(itemCount == 1)

        // Use overflow to delete the project
        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)
        onView(ViewMatchers.withText(R.string.delete_project)).perform(click())
        onView(ViewMatchers.withText(android.R.string.yes))
                .inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(click())
        onView(ViewMatchers.withText(android.R.string.yes))
                .inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(click())

        // Assert that no projects are left
        itemCount = mTimeLapseGalleryActivityTestRule.activity
                .findViewById<RecyclerView>(R.id.gallery_recycler_view).adapter?.itemCount
        assert(itemCount == 0)
    }
}