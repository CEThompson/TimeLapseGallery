package com.vwoom.timelapsegallery;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.core.internal.deps.guava.collect.Iterables;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.vwoom.timelapsegallery.activities.AddPhotoActivity;
import com.vwoom.timelapsegallery.activities.MainActivity;
import com.vwoom.timelapsegallery.activities.NewProjectActivity;
import com.vwoom.timelapsegallery.database.TimeLapseDatabase;
import com.vwoom.timelapsegallery.database.entry.PhotoEntry;
import com.vwoom.timelapsegallery.database.entry.ProjectEntry;
import com.vwoom.timelapsegallery.utils.FileUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

// TODO put together end to end test
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
        onView(withId(R.id.project_name_edit_text)).perform(replaceText("testProject"));

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

        // Click the new project
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

        // TODO intent stub here...
        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(Activity.RESULT_OK, null);

        intending(anyIntent()).respondWith(result);

        onView(withId(R.id.take_photo_fab)).perform(click());
        writeDrawableToTempFile(h, R.drawable.htest);
        ((AddPhotoActivity)currentActivity).setmTemporaryPhotoPath(h.getAbsolutePath());


        //intended(anyIntent());
        //try {Thread.sleep(3000);} catch(InterruptedException e){
            // do nothing}
        //}

        // Submit the temporary photo
        onView(withId(R.id.submit_photo_fab)).perform(click());

        onView(withId(R.id.details_recyclerview)).perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));

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

        try {onView(withText(R.string.welcome)).perform(click());}
        catch (NoMatchingViewException e){
        }

    }

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
