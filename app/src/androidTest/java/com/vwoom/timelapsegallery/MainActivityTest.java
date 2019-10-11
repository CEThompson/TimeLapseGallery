package com.vwoom.timelapsegallery;

import android.content.Context;

import androidx.test.rule.ActivityTestRule;

import com.vwoom.timelapsegallery.activities.MainActivity;

import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class MainActivityTest {

    @Rule
    public ActivityTestRule<MainActivity> mMainActivityTestRule
            = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void clickNewProjectFab_shouldStartNewProjectActivity(){
        Context context = mMainActivityTestRule.getActivity();
        onView(withId(R.id.add_project_FAB)).perform(click());
    }
    
    @Test
    public void clickOnRecyclerViewElement_shouldStartDetailsActivity(){

    }

}
