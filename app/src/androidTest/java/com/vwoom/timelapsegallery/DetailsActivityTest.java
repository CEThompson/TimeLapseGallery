package com.vwoom.timelapsegallery;

import android.content.Context;
import android.util.Log;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.vwoom.timelapsegallery.activities.MainActivity;
import com.vwoom.timelapsegallery.database.TimeLapseDatabase;
import com.vwoom.timelapsegallery.database.entry.PhotoEntry;
import com.vwoom.timelapsegallery.database.entry.ProjectEntry;
import com.vwoom.timelapsegallery.utils.FileUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/*
* This test should check appropriate display of images, portrait vs landscape etc.
*/
@LargeTest
public class DetailsActivityTest {

    private Context mContext;
    private File horizontalPhotoFile = null;
    private File verticalPhotoFile = null;
    private TimeLapseDatabase mDb;

    private PhotoEntry verticalTestPhoto;
    private ProjectEntry verticalTestProject;

    private PhotoEntry horizontalTestPhoto;
    private ProjectEntry horizontalTestProject;

    private static String TAG = "EndToEnd";

    @Rule
    public ActivityTestRule<MainActivity> mMainActivityTestRule
            = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void setUp(){
        mContext = mMainActivityTestRule.getActivity();
        mDb = TimeLapseDatabase.getInstance(mContext);

        // create temporary files for test images
        try {
            horizontalPhotoFile = FileUtils.createTemporaryImageFile(mContext);
            verticalPhotoFile = FileUtils.createTemporaryImageFile(mContext);
        } catch (IOException e){
            Log.d(TAG, "Error creating temp file");
        }

        // copy vertical test drawable to temp file
        try {
            InputStream inputStream = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext().getResources().openRawResource(R.drawable.vtest);
            OutputStream out = new FileOutputStream(verticalPhotoFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0)
                out.write(buf, 0, len);
            out.close();
            inputStream.close();
        }
        catch (IOException e){
            if (e.getMessage()!=null)
            Log.d(TAG, e.getMessage());
        }

        // copy horizontal test drawable to temp file
        try {
            InputStream inputStream = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext().getResources().openRawResource(R.drawable.htest);
            OutputStream out = new FileOutputStream(horizontalPhotoFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0)
                out.write(buf, 0, len);
            out.close();
            inputStream.close();
        }
        catch (IOException e){
            if (e.getMessage()!=null)
            Log.d(TAG, e.getMessage());
        }

        // create the vertical test project entry
        long timestamp = System.currentTimeMillis();
        verticalTestProject =
                new ProjectEntry("verticalTestProject",
                        verticalPhotoFile.getAbsolutePath(),
                        0,
                        0,
                        timestamp);
        long verticalProjectId = mDb.projectDao().insertProject(verticalTestProject);
        verticalTestProject.setId(verticalProjectId);


        // create the horizontal test project entry
        horizontalTestProject =
                new ProjectEntry("horizontal test",
                        horizontalPhotoFile.getAbsolutePath(),
                        0,
                        0,
                        timestamp);
        long horizontalProjectId = mDb.projectDao().insertProject(horizontalTestProject);
        horizontalTestProject.setId(horizontalProjectId);

        // create final files for each
        File verticalFinalFile = null;
        File horizontalFinalFile = null;
        try {
            verticalFinalFile = FileUtils.createFinalFileFromTemp(mContext, verticalPhotoFile.getAbsolutePath(), verticalTestProject, timestamp);
            horizontalFinalFile = FileUtils.createFinalFileFromTemp(mContext, horizontalPhotoFile.getAbsolutePath(), horizontalTestProject, timestamp);
        } catch (IOException e){
            if (e.getMessage()!=null)
            Log.d(TAG, e.getMessage());
        }

        // If final files worked insert test photo and update project to update thumbnail
        if (verticalFinalFile != null) {
            verticalTestPhoto =
                    new PhotoEntry(verticalProjectId,
                            verticalFinalFile.getAbsolutePath(),
                            timestamp);
            verticalTestProject.setThumbnail_url(verticalFinalFile.getAbsolutePath());
            mDb.projectDao().updateProject(verticalTestProject);
            mDb.photoDao().insertPhoto(verticalTestPhoto);

        }

        // Do the same for the horizontal test
        if (horizontalFinalFile != null) {
            horizontalTestPhoto =
                    new PhotoEntry(horizontalProjectId,
                            horizontalFinalFile.getAbsolutePath(),
                            timestamp);
            horizontalTestProject.setThumbnail_url(horizontalFinalFile.getAbsolutePath());
            // insert the test entries
            mDb.projectDao().insertProject(horizontalTestProject);
            mDb.photoDao().insertPhoto(horizontalTestPhoto);

        }
    }

    @Test
    public void detailsTest(){
        onView(withId(R.id.projects_recycler_view)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        onView(withId(R.id.add_photo_fab)).perform(click());
    }

    @After
    public void cleanUp(){
        FileUtils.deleteTempFiles(mContext);
        FileUtils.deleteProject(mContext, verticalTestProject);
        FileUtils.deleteProject(mContext, horizontalTestProject);

        if (horizontalTestProject != null)
            mDb.projectDao().deleteProject(horizontalTestProject);

        if (verticalTestProject != null)
            mDb.projectDao().deleteProject(verticalTestProject);

        if (verticalTestPhoto != null)
            mDb.photoDao().deletePhoto(verticalTestPhoto);

        if (horizontalTestPhoto != null)
            mDb.photoDao().deletePhoto(horizontalTestPhoto);
    }

}
