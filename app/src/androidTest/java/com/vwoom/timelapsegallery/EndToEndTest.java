package com.vwoom.timelapsegallery;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.filters.LargeTest;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.vwoom.timelapsegallery.activities.AddPhotoActivity;
import com.vwoom.timelapsegallery.activities.MainActivity;
import com.vwoom.timelapsegallery.activities.NewProjectActivity;
import com.vwoom.timelapsegallery.utils.FileUtils;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

/*
* This test adds a new project with a test photo bypassing the implicit camera intent.
* A new photo is added to the project and the project is deleted.
* This represents the overall basic workflow of the application.
 */
@LargeTest
public class EndToEndTest {

    private Context mContext;
    private static String TAG = "EndToEnd";

    @Rule
    public IntentsTestRule<MainActivity> mMainActivityTestRule
            = new IntentsTestRule<>(MainActivity.class);

    @Test
    public void endToEndTest(){
        mContext = mMainActivityTestRule.getActivity();
        onView(withId(R.id.add_project_FAB)).perform(click());

        // Enter a project name
        onView(withId(R.id.project_name_edit_text)).perform(replaceText("verticalTestProject"));

        // Create a test photo file
        File temp = null;
        try {temp = FileUtils.createTemporaryImageFile(mContext);}
        catch (IOException e){
            Log.d(TAG, "temp file creation failed");
        }
        // Assert the file was created then copy test image to it
        Assert.assertNotNull(temp);
        writeDrawableToTempFile(temp, R.drawable.vtest);

        // Get current activity and set temp path
        Activity currentActivity = getActivityInstance();
        Assert.assertEquals(currentActivity.getClass(), NewProjectActivity.class);
        ((NewProjectActivity)currentActivity).setmTemporaryPhotoPath(temp.getAbsolutePath());

        // Submit the new project
        onView(withId(R.id.submit_new_project_fab)).perform(click());

        // Click the first project
        // Assumes project list is empty
        onView(withId(R.id.projects_recycler_view)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // Launch the add photo activity
        onView(withId(R.id.add_photo_fab)).perform(click());

        currentActivity = getActivityInstance();
        Assert.assertEquals(currentActivity.getClass(), AddPhotoActivity.class);

        // Create a test photo file
        File h = null;
        try {h = FileUtils.createTemporaryImageFile(currentActivity);}
        catch (IOException e){
            Log.d(TAG, "temp file creation failed");
        }
        // Assert the file was created then copy test image to it
        Assert.assertNotNull(h);

        // Set up intent stub for clicking on take photo fab
        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(Activity.RESULT_OK, null);
        intending(anyIntent()).respondWith(result);

        // Click the fab to take a photo
        onView(withId(R.id.take_photo_fab)).perform(click());

        // Overwrite the temporary photo path
        // Note that in test the UI will not display the photo but the path should work for test submission
        writeDrawableToTempFile(h, R.drawable.htest);
        ((AddPhotoActivity)currentActivity).setmTemporaryPhotoPath(h.getAbsolutePath());

        // Submit the photo
        onView(withId(R.id.submit_photo_fab)).perform(click());

        // Click the option to delete the project
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
        onView(withText(R.string.delete_project)).perform(click());
        onView(withText(android.R.string.yes))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(click());
        onView(withText(android.R.string.yes))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
                .perform(click());
    }

    /* Copies a drawable to a file */
    private void writeDrawableToTempFile(File destinationFile, int testImage){
        try {
            InputStream inputStream = getInstrumentation()
                    .getTargetContext().getResources().openRawResource(testImage);
            OutputStream out = new FileOutputStream(destinationFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0)
                out.write(buf, 0, len);
            out.close();
            inputStream.close();
        }
        catch (IOException e){
            if(e.getMessage()!=null)
                Log.d(TAG, e.getMessage());
        }
    }

    /* Gets the current activity instance for espresso */
    private Activity getActivityInstance(){
        final Activity[] currentActivity = {null};

        getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> resumedActivity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
            Iterator<Activity> it = resumedActivity.iterator();
            currentActivity[0] = it.next();
        });

        return currentActivity[0];
    }
}
