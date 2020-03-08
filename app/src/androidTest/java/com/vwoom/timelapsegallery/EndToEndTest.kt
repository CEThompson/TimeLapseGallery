package com.vwoom.timelapsegallery

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.firebase.annotations.PublicApi
import com.vwoom.timelapsegallery.gallery.GalleryAdapter
import com.vwoom.timelapsegallery.utils.FileUtils.createTemporaryImageFile
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.Thread.sleep

/*
* This test adds a new project.
* A new photo is added to the project and the project is deleted.
* This should represent the overall basic workflow of the application.
 */
@LargeTest
class EndToEndTest {

    private lateinit var mContext: Context

    @Rule
    @JvmField
    var mTimeLapseGalleryActivityTestRule = IntentsTestRule(TimeLapseGalleryActivity::class.java)

    // bypasses permission
    @get:Rule var permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    // TODO refactor end to end for navigation flow
    @Test
    fun endToEndTest() {
        mContext = mTimeLapseGalleryActivityTestRule.activity
        val externalFilesDir = mContext?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        onView(withId(R.id.add_project_FAB)).perform(click())

        sleep(1000)

        onView(withId(R.id.take_picture_fab)).perform(click())

        sleep(2000)

        onView(withId(R.id.gallery_recycler_view)).perform(
                RecyclerViewActions.actionOnItemAtPosition<GalleryAdapter.GalleryAdapterViewHolder>(0, click()))

        onView(withId(R.id.add_photo_fab)).perform(click())

        sleep(1000)

        onView(withId(R.id.take_picture_fab)).perform(click())

        sleep(2000)

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

        // Assert the item was deleted
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

        // Assert project was deleted
        itemCount = mTimeLapseGalleryActivityTestRule.activity
                .findViewById<RecyclerView>(R.id.gallery_recycler_view).adapter?.itemCount
        assert(itemCount == 0)


        /*
        // TODO click on new project fab: onView(withId(R.id.project_name_edit_text)).perform(replaceText("verticalTestProject"));
        // Create a test photo file
        var temp: File? = null
        try {
            temp = createTemporaryImageFile(externalFilesDir)
        } catch (e: IOException) {
            Log.d(TAG, "temp file creation failed")
        }
        // Assert the file was created then copy test image to it
        Assert.assertNotNull(temp)
        writeDrawableToTempFile(temp, R.drawable.vtest)

        // Submit the new project
        // TODO click on the take picture fab / submit a new project: onView(withId(R.id.submit_new_project_fab)).perform(click());
        // Click the first project
        // Assumes project list is empty
        // TODO verify new project shows up / click on the project: onView(withId(R.id.projects_recycler_view)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        // Launch the add photo activity
        // TODO click on the fab to add a photo to the project: onView(withId(R.id.add_photo_fab)).perform(click());

        // Create a test photo file
        var h: File? = null
        try {
            h = createTemporaryImageFile(externalFilesDir)
        } catch (e: IOException) {
            Log.d(TAG, "temp file creation failed")
        }

        // Assert the file was created then copy test image to it
        Assert.assertNotNull(h)
        writeDrawableToTempFile(h, R.drawable.htest)

        // Set up intent stub for clicking on take photo fab
        val result = ActivityResult(Activity.RESULT_OK, null)
        Intents.intending(IntentMatchers.anyIntent()).respondWith(result)

        // Click the fab to take a photo
        Espresso.onView(withId(R.id.add_photo_fab)).perform(ViewActions.click())

        // Submit the photo
        Espresso.onView(withId(R.id.take_picture_fab)).perform(ViewActions.click())

        // Click the option to delete the project
        Espresso.openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        Espresso.onView(ViewMatchers.withText(R.string.delete_project)).perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withText(android.R.string.yes))
                .inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withText(android.R.string.yes))
                .inRoot(RootMatchers.isDialog())
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(ViewActions.click())

         */
    }

    /* Copies a drawable to a file */
    private fun writeDrawableToTempFile(destinationFile: File?, testImage: Int) {
        try {
            val inputStream = InstrumentationRegistry.getInstrumentation()
                    .targetContext.resources.openRawResource(testImage)
            val out: OutputStream = FileOutputStream(destinationFile)
            val buf = ByteArray(1024)
            var len: Int
            while (inputStream.read(buf).also { len = it } > 0) out.write(buf, 0, len)
            out.close()
            inputStream.close()
        } catch (e: IOException) {
            if (e.message != null) Log.d(TAG, e.message)
        }
    }

    // TODO actually click permission dialog
    /*
    fun allowPermissionsIfNeeded(permissionNeeded: String) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasNeededPermission(permissionNeeded)) {
        sleep(PERMISSIONS_DIALOG_DELAY)
        val device: UiDevice = UiDevice.getInstance(getInstrumentation())
        val allowPermissions: UiObject = device.findObject(UiSelector()
          .clickable(true)
          .checkable(false)
          .index(GRANT_BUTTON_INDEX))
        if (allowPermissions.exists()) {
          allowPermissions.click()
        }
      }
    } catch (e: UiObjectNotFoundException) {
      System.out.println("There is no permissions dialog to interact with");
    }
  }
 */

    companion object {
        private const val TAG = "EndToEnd"
    }
}