package com.vwoom.timelapsegallery.activities;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.vwoom.timelapsegallery.R;
import com.vwoom.timelapsegallery.database.entry.PhotoEntry;
import com.vwoom.timelapsegallery.database.entry.ProjectEntry;
import com.vwoom.timelapsegallery.database.TimeLapseDatabase;
import com.vwoom.timelapsegallery.utils.FileUtils;
import com.vwoom.timelapsegallery.utils.Keys;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AddPhotoActivity extends AppCompatActivity {

    public static final String TAG = AddPhotoActivity.class.getSimpleName();

    /* Photo Views */
    @BindView(R.id.previous_photo) ImageView mPreviousPhoto;
    @BindView(R.id.add_photo_result) ImageView mResultPhoto;

    /* Floating Action Buttons */
    @BindView(R.id.take_photo_fab) FloatingActionButton mRetakeFab;
    @BindView(R.id.compare_to_previous_fab) FloatingActionButton mCompareFab;
    @BindView(R.id.submit_photo_fab) FloatingActionButton mSubmitFab;
    @BindView(R.id.return_to_details_fab) FloatingActionButton mCancelFab;

    /* Photo data */
    private String mTemporaryPhotoPath;
    private String mBackupPhoto;
    private String mPreviousPhotoPath;

    private TimeLapseDatabase mTimeLapseDatabase;

    private boolean mCompared = false;
    private boolean mReturned = false;

    private ProjectEntry mCurrentProject;

    private Handler mAnimationHandler;

    /* Admob */
    private InterstitialAd mInterstitialAd;

    /* Analytics */
    private FirebaseAnalytics mFirebaseAnalytics;

    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final String KEY_BACKUP_PHOTO = "backup_photo"; // If user backs out of camera save a reference to their previous picture if they took one

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_photo);
        ButterKnife.bind(this);

        // Passing activity as context apparently causes memory leak, make sure to pass application context to interstitial ad
        mInterstitialAd = new InterstitialAd(getApplicationContext());
        // This ad unit id points to test ads in debug and actual ads in release
        mInterstitialAd.setAdUnitId(getString(R.string.add_photo_activity_ad_unit_id));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        // Set up db
        mTimeLapseDatabase = TimeLapseDatabase.getInstance(this);

        // Get information from intent
        mPreviousPhotoPath = getIntent().getStringExtra(Keys.PHOTO_PATH);
        mCurrentProject = getIntent().getParcelableExtra(Keys.PROJECT_ENTRY);

        setFabClickListeners();

        loadPreviousImage();

        // Recover the photo path
        if (savedInstanceState != null){
            mTemporaryPhotoPath = savedInstanceState.getString(Keys.TEMP_PATH);
            mBackupPhoto = savedInstanceState.getString(KEY_BACKUP_PHOTO);

            // load and show image if a temporary photo path has been created / the user has taken a picture
            if (mTemporaryPhotoPath != null) {
                mRetakeFab.setImageResource(R.drawable.ic_repeat_white_24dp);
                mResultPhoto.setVisibility(View.VISIBLE);
                mCompareFab.show();
                mSubmitFab.show();
                loadResultImage();
            }
        }

        // Prepare analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }

    /* Reload ad if consumed */
    @Override
    protected void onResume() {
        super.onResume();
        if (!mInterstitialAd.isLoaded()){
            mInterstitialAd.loadAd(new AdRequest.Builder().build());
        }

        if (mReturned && !mCompared) {
            animateCompareFab();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mAnimationHandler != null)
            mAnimationHandler.removeCallbacksAndMessages(null);
    }

    /* Make sure to remove ad listener in lifecycle*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mInterstitialAd.setAdListener(null);
    }

    /* Creates a temporary file and launches a camera intent to write a photo to that file */
    /* Note: this is exactly the same as in new project activity, these two methods can be abstracted together later */
    private void takeTemporaryPicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File tempFile = null;
            try {
                tempFile = FileUtils.createTemporaryImageFile(this);
                if (mTemporaryPhotoPath != null) mBackupPhoto = mTemporaryPhotoPath;
                mTemporaryPhotoPath = tempFile.getAbsolutePath();
            } catch (IOException e) {
                // Notify user of error
                Toast toast = Toast.makeText(this, getString(R.string.error_creating_temporary_image_file), Toast.LENGTH_SHORT);
                toast.show();

                // Track error
                Bundle params = new Bundle();
                params.putString(getString(R.string.analytics_error_text), e.getMessage());
                mFirebaseAnalytics.logEvent(getString(R.string.analytics_take_temporary_picture_error), params);
            }
            // Continue only if the File was successfully created
            if (tempFile != null) {
                Uri currentPhotoUri = FileProvider.getUriForFile(this,
                        Keys.FILEPROVIDER_AUTHORITY,
                        tempFile);

                // If the device is pre-lollipop grant permissions for any intent activities
                List<ResolveInfo> resInfoList = this.getPackageManager()
                        .queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    this.grantUriPermission(
                            packageName,
                            currentPhotoUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }


                // Then put extras and start the intent
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {

            mReturned = true;

            // Load ad if it isn't loaded
            if (!mInterstitialAd.isLoaded()){
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }

            loadResultUi();
        } else {
            // TODO restore previous taken photo
            mTemporaryPhotoPath = mBackupPhoto;
        }

        // Note: after activity result fires onResume fires and animates the compare fab depending upon comparison and succesful return state;
    }

    /* Sets up the ui after result from taking photo */
    private void loadResultUi(){
        // Show the result image
        mResultPhoto.setVisibility(View.VISIBLE);

        // Load the image
        loadResultImage();

        // Set up the ui for result
        mSubmitFab.show();
        mCompareFab.show();
        mRetakeFab.setImageResource(R.drawable.ic_repeat_white_24dp);
    }

    /* Set click listeners for all four fabs */
    private void setFabClickListeners(){
        /* Submits the photo to the project */
        mSubmitFab.setOnClickListener(view -> {
            // Only proceed with submission if the user has taken a photo
            if (!mTemporaryPhotoPath.isEmpty())
                submitPhoto();
        });
        
        /* Shows the previous photo for comparison */
        mCompareFab.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN){
                mResultPhoto.setVisibility(View.INVISIBLE);
                stopAnimation();
                mCompared = true;
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP){
                mResultPhoto.setVisibility(View.VISIBLE);
                return true;
            }
            return false;
        });

        /* Cancels adding a photo to the project */
        mCancelFab.setOnClickListener(view -> onSupportNavigateUp());

        /* Resends the implicit camera intent to take another picture */
        mRetakeFab.setOnClickListener(view -> takeTemporaryPicture());
    }

    /* Loads the result of the implicit camera intent into the overlaying image view*/
    private void loadResultImage() {
        File result = new File(mTemporaryPhotoPath);
        Glide.with(this)
                .load(result)
                .into(mResultPhoto);
    }

    /* Loads the latest photo in the project into a view for comparison to the newly taken photo */
    private void loadPreviousImage(){
        File previous = new File(mPreviousPhotoPath);
        Glide.with(this)
                .load(previous)
                .into(mPreviousPhoto);
    }

    /* Submits the photo to the database and saves photo to permanent file structure */
    private void submitPhoto(){
        TaskParameters taskParameters = new TaskParameters(this,
                mTemporaryPhotoPath,
                mCurrentProject,
                mTimeLapseDatabase,
                mInterstitialAd,
                mFirebaseAnalytics);

        new SubmitPhotoAsyncTask().execute(taskParameters);
    }

    /* Animated the compare fab to draw attention from the user for photo comparison*/
    private void animateCompareFab(){
        if (mAnimationHandler == null) mAnimationHandler = new Handler();
        fabIncrease();
    }

    private void stopAnimation(){
        if (mAnimationHandler!=null) mAnimationHandler.removeCallbacksAndMessages(null);
    }

    private void fabIncrease(){
        mCompareFab.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(50)
                .start();
        Runnable runnable = this::fabDecrease;
        mAnimationHandler.postDelayed(runnable, 200);
    }

    private void fabDecrease(){
        mCompareFab.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(50)
                .start();
        Runnable runnable = this::fabIncrease;
        mAnimationHandler.postDelayed(runnable, 200);
    }

    /* Save state on configuration change */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        /* Store member variables */
        outState.putString(Keys.TEMP_PATH, mTemporaryPhotoPath);
        outState.putString(KEY_BACKUP_PHOTO, mBackupPhoto);
    }

    /* Async Task used to submit a photo to the database and finish the activity */
    private static class SubmitPhotoAsyncTask extends AsyncTask<TaskParameters, Void, ResultParameters>{
        /* Writes the photo to disk and submits to database */
        @Override
        protected ResultParameters doInBackground(TaskParameters... params) {
            // Get the parameters
            TaskParameters taskParameters = params[0];
            Context context = taskParameters.getContext();
            String temporaryPhotoPath = taskParameters.getTempPhotoPath();
            ProjectEntry currentProject = taskParameters.getCurrentProject();
            TimeLapseDatabase timeLapseDatabase = taskParameters.getTimeLapseDatabase();
            InterstitialAd interstitialAd = taskParameters.getInterstitialAd();
            FirebaseAnalytics firebaseAnalytics = taskParameters.getFirebaseAnalytics();

            // Initialize the path for the final photo file
            String currentPhotoPath;
            long timestamp = System.currentTimeMillis();

            try {
                // Create the final file & get path
                File finalFile = FileUtils.createFinalFileFromTemp(context,
                        temporaryPhotoPath,
                        currentProject,
                        timestamp);
                currentPhotoPath = finalFile.getAbsolutePath();

                // Create and insert the photo entry
                PhotoEntry photoToSubmit = new PhotoEntry(currentProject.getId(),
                        currentPhotoPath,
                        timestamp);
                timeLapseDatabase.photoDao().insertPhoto(photoToSubmit);

                // Track added photo
                Bundle bundle = new Bundle();
                bundle.putString(context.getString(R.string.analytics_project_name), currentProject.getName());
                bundle.putString(context.getString(R.string.analytics_photo_number), String.valueOf(photoToSubmit.getId()));
                firebaseAnalytics.logEvent(context.getString(R.string.analytics_add_photo), bundle);

                // Return the parameters to run in post execute
                return new ResultParameters(context, photoToSubmit, interstitialAd, firebaseAnalytics);
            }
            catch (IOException e) {
                // Notify user of error
                Toast toast = Toast.makeText(context, context.getString(R.string.error_submitting_photo), Toast.LENGTH_LONG);
                toast.show();

                // Track error
                Bundle bundle = new Bundle();
                bundle.putString(context.getString(R.string.analytics_error_text), e.getMessage());
                firebaseAnalytics.logEvent(context.getString(R.string.analytics_add_photo_error), bundle);

                return new ResultParameters(context, null, null, null);
            }
        }

        /* Sets the result intent, launches the ad and finishes the activity */
        @Override
        protected void onPostExecute(ResultParameters resultParameters) {
            // Get the parameters
            Context context = resultParameters.getContext();
            Activity activity = (Activity) context;
            PhotoEntry insertedPhoto = resultParameters.getPhotoEntry();
            InterstitialAd interstitialAd = resultParameters.getInterstitialAd();
            FirebaseAnalytics firebaseAnalytics = resultParameters.getFirebaseAnalytics();

            // If photo insertion was succesful
            if (insertedPhoto != null) {
                // Set the result to return to the details activity
                Intent resultIntent = new Intent();
                resultIntent.putExtra(Keys.PHOTO_ENTRY, insertedPhoto);
                activity.setResult(RESULT_OK, resultIntent);

                // Do not show ads to firebase lab test devices
                if (isTestDevice(context)){
                    activity.finish();
                }
                // Show ad if not test device and ad is loaded
                else if (interstitialAd.isLoaded()) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    boolean adsDisabled = prefs.getBoolean(context.getString(R.string.key_ads_disabled), false);

                    // If the user disabled ads just finish the activity
                    if (adsDisabled)
                        activity.finish();
                    // Otherwise serve and log the ad
                    else {
                        interstitialAd.setAdListener(new AdListener() {
                            @Override
                            public void onAdClosed() {
                                activity.finish();
                            }
                            @Override
                            public void onAdClicked() {
                                super.onAdClicked();
                                firebaseAnalytics.logEvent(context.getString(R.string.analytics_ad_click),null);
                            }
                        });
                        interstitialAd.show();

                        // Send event to firebase
                        firebaseAnalytics.logEvent(context.getString(R.string.analytics_show_ad), null);
                    }
                }
                // Otherwise finish the activity
                // TODO: (update) implement local ad
                else {
                    activity.finish();
                }
            }
            // If photo insertion unsuccessful do nothing
        }

        // Used to filter out pre-launch reports bots clicking on ads
        boolean isTestDevice(Context context) {
            return Boolean.valueOf(Settings.System.getString(context.getContentResolver(), "firebase.test.lab"));
        }
    }


    /* Class to pass references to async task */
    public static class TaskParameters {
        private Context context;
        private String tempPhotoPath;
        private ProjectEntry currentProject;
        private TimeLapseDatabase timeLapseDatabase;
        private InterstitialAd interstitialAd;
        private FirebaseAnalytics firebaseAnalytics;

        public TaskParameters(Context context,
                              String tempPhotoPath,
                              ProjectEntry currentProject,
                              TimeLapseDatabase timeLapseDatabase,
                              InterstitialAd interstitialAd,
                              FirebaseAnalytics firebaseAnalytics){
            this.context = context;
            this.tempPhotoPath = tempPhotoPath;
            this.currentProject = currentProject;
            this.timeLapseDatabase = timeLapseDatabase;
            this.interstitialAd = interstitialAd;
            this.firebaseAnalytics = firebaseAnalytics;
        }
        // Getters
        public Context getContext() { return context; }
        public String getTempPhotoPath() { return tempPhotoPath; }
        public ProjectEntry getCurrentProject() { return currentProject; }
        public TimeLapseDatabase getTimeLapseDatabase() { return timeLapseDatabase; }
        public InterstitialAd getInterstitialAd() { return interstitialAd; }
        public FirebaseAnalytics getFirebaseAnalytics() { return firebaseAnalytics; }
    }

    /* Class to pass result references for async task */
    private static class ResultParameters {
        private Context context;
        private PhotoEntry photoEntry;
        private InterstitialAd interstitialAd;
        private FirebaseAnalytics firebaseAnalytics;

        public ResultParameters(Context context, PhotoEntry photoEntry, InterstitialAd interstitialAd, FirebaseAnalytics firebaseAnalytics){
            this.context = context;
            this.photoEntry = photoEntry;
            this.interstitialAd = interstitialAd;
            this.firebaseAnalytics = firebaseAnalytics;
        }
        // Getters
        public Context getContext() { return context; }
        public PhotoEntry getPhotoEntry() { return photoEntry; }
        public InterstitialAd getInterstitialAd() { return interstitialAd; }
        public FirebaseAnalytics getFirebaseAnalytics() { return firebaseAnalytics; }
    }

    @VisibleForTesting
    public void setmTemporaryPhotoPath(String path){ mTemporaryPhotoPath = path; }

}
