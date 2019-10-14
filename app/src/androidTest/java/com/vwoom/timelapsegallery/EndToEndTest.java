package com.vwoom.timelapsegallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.room.util.FileUtil;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.vwoom.timelapsegallery.activities.MainActivity;
import com.vwoom.timelapsegallery.activities.NewProjectActivity;
import com.vwoom.timelapsegallery.database.TimeLapseDatabase;
import com.vwoom.timelapsegallery.database.entry.PhotoEntry;
import com.vwoom.timelapsegallery.database.entry.ProjectEntry;
import com.vwoom.timelapsegallery.utils.FileUtils;
import com.vwoom.timelapsegallery.utils.PhotoUtils;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

// TODO put together end to end test
@LargeTest
public class EndToEndTest {

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
            //horizontalPhotoFile = FileUtils.createTemporaryImageFile(mContext);
            verticalPhotoFile = FileUtils.createTemporaryImageFile(mContext);
        } catch (IOException e){
            Log.d(TAG, "Error creating temp file");
        }

        /*
        Bitmap horizontalTestImage = BitmapFactory.decodeResource(
                InstrumentationRegistry.getInstrumentation().getTargetContext().getResources(),
                R.drawable.htest
        );
        */

        try {
            InputStream inputStream = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext().getResources().openRawResource(R.drawable.vtest);
            OutputStream out = new FileOutputStream(verticalPhotoFile);
            byte buf[] = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0)
                out.write(buf, 0, len);
            out.close();
            inputStream.close();
        }
        catch (IOException e){
            Log.d(TAG, e.getMessage());
        }

        // copy test images into temp files
        /*
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(verticalPhotoFile);
            verticalTestImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();

            //fos = new FileOutputStream(verticalPhotoFile);
            //horizontalTestImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            //fos.close();
        } catch (IOException e){
            if (fos != null) {
                try { fos.close(); }
                catch (IOException n){
                    Log.d(TAG, "Error copying drawable into temp file");
                }
            }
        }
        */

        // create the test entries
        long timestamp = System.currentTimeMillis();
        verticalTestProject =
                new ProjectEntry("testProject",
                        verticalPhotoFile.getAbsolutePath(),
                        0,
                        0,
                        timestamp);
        long projectId = mDb.projectDao().insertProject(verticalTestProject);

        verticalTestProject.setId(projectId);

        /*
        horizontalTestProject =
                new ProjectEntry("horizontal test",
                        horizontalPhotoFile.getAbsolutePath(),
                        0,
                        0,
                        timestamp);
        horizontalTestPhoto =
                new PhotoEntry(horizontalTestProject.getId(),
                        horizontalPhotoFile.getAbsolutePath(),
                        timestamp);
        */
        File finalFile = null;
        try {
            finalFile = FileUtils.createFinalFileFromTemp(mContext, verticalPhotoFile.getAbsolutePath(), verticalTestProject, timestamp);
        } catch (IOException e){
            Log.d(TAG, e.getMessage());
        }

        verticalTestPhoto =
                new PhotoEntry(projectId,
                        finalFile.getAbsolutePath(),
                        timestamp);

        verticalTestProject.setThumbnail_url(finalFile.getAbsolutePath());

        // insert the test entries
        mDb.projectDao().updateProject(verticalTestProject);
        mDb.photoDao().insertPhoto(verticalTestPhoto);
        //mDb.projectDao().insertProject(horizontalTestProject);
        //mDb.photoDao().insertPhoto(horizontalTestPhoto);
    }

    @Test
    public void endToEndTest(){
        // Click on FAB to add a new project
        //onView(withId(R.id.add_project_FAB)).perform(click());

        // Enter a project name
        //onView(withId(R.id.project_name_edit_text)).perform(replaceText("testProject"));

        //TODO Simulate camera intent here

        // Submit the new project
        //onView(withId(R.id.submit_new_project_fab)).perform(click());

        //List<ProjectEntry> projects = mMainActivityTestRule.getActivity().getmProjects();
        //int lastPosition = projects.size()-1;
        // Click project recycler view
        try {Thread.sleep(2000);} catch (InterruptedException e){Log.d(TAG, e.getMessage());}
        onView(withId(R.id.projects_recycler_view)).perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        //try {
        //    Thread.sleep(2000);
        //} catch (InterruptedException i){
        //    Log.d(TAG, "interrupted exception");
        //}
        // Test details activity stuff here

        // Click add photo fab
        //onView(withId(R.id.add_photo_fab)).perform(click());

    }

    @After
    public void cleanUp(){
        FileUtils.deleteTempFiles(mContext);
        FileUtils.deleteProject(mContext, verticalTestProject);

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
