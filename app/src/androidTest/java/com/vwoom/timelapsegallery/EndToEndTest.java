package com.vwoom.timelapsegallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.vwoom.timelapsegallery.activities.MainActivity;
import com.vwoom.timelapsegallery.activities.NewProjectActivity;

import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

// TODO put together end to end test
@LargeTest
public class EndToEndTest {

    @Rule
    public IntentsTestRule<MainActivity> mMainActivityTestRule
            = new IntentsTestRule<>(MainActivity.class);

    @Rule
    public IntentsTestRule<NewProjectActivity> mNewProjectActivityTestRule
            = new IntentsTestRule<>(NewProjectActivity.class);

    @Test
    public void clickNewProjectFab_shouldStartNewProjectActivity(){
        Context context = mMainActivityTestRule.getActivity();
        onView(withId(R.id.add_project_FAB)).perform(click());

        onView(withId(R.id.project_name_edit_text)).perform(replaceText("testProject"));

        //TODO Simulate camera intent here
        Bitmap icon = BitmapFactory.decodeResource(
                InstrumentationRegistry.getInstrumentation().getTargetContext().getResources(),
                R.mipmap.ic_launcher
        );

        onView(withId(R.id.submit_new_project_fab)).perform(click());

        
    }
    
}
