package com.vwoom.timelapsegallery;

import android.content.Context;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import com.vwoom.timelapsegallery.activities.MainActivity;

import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@LargeTest
public class EndToEndTest {

    // TODO click new project fab
    @Rule
    public ActivityTestRule<MainActivity> mMainActivityTestRule
            = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void clickNewProjectFab_shouldStartNewProjectActivity(){
        Context context = mMainActivityTestRule.getActivity();
        onView(withId(R.id.add_project_FAB)).perform(click());
    }
    // TODO enter project name

    // TODO click add photo fab

    // TODO take photo

    // TODO submit new project

    // TODO click submitted project

    // TODO click add photo fab

    // TODO click add photo fab

    // TODO take photo

    // TODO submit new photo

    // TODO verify new photo submitted
    
}
