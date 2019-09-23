package com.vwoom.timelapsegallery.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.FileProvider;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TimePicker;

import com.bumptech.glide.Glide;

import com.vwoom.timelapsegallery.R;
import com.vwoom.timelapsegallery.analytics.AnalyticsApplication;
import com.vwoom.timelapsegallery.database.AppExecutors;
import com.vwoom.timelapsegallery.database.entry.PhotoEntry;
import com.vwoom.timelapsegallery.database.entry.ProjectEntry;
import com.vwoom.timelapsegallery.database.TimeLapseDatabase;
import com.vwoom.timelapsegallery.notification.NotificationUtils;
import com.vwoom.timelapsegallery.utils.FileUtils;
import com.vwoom.timelapsegallery.utils.Keys;
import com.vwoom.timelapsegallery.utils.PhotoUtils;
import com.vwoom.timelapsegallery.utils.TimeUtils;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.vwoom.timelapsegallery.widget.UpdateWidgetService;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NewProjectActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    @BindView(R.id.submit_new_project_fab) FloatingActionButton mNewProjectFab;
    @BindView(R.id.add_first_photo_fab) FloatingActionButton mFirstPhotoFab;
    @BindView(R.id.project_name_edit_text) EditText mProjectNameEditText;
    @BindView(R.id.notification_schedule_spinner) Spinner mScheduleSpinner;
    @BindView(R.id.project_first_image) ImageView mProjectFirstImage;
    @BindView(R.id.notification_schedule_picker) TimePicker mTimePicker;

    private TimeLapseDatabase mTimeLapseDatabase;
    private ProjectEntry mProjectToEdit;

    /* For spinner */
    private String mScheduleString;

    /* Project Variables */
    private String mName;
    private long mScheduleNextSubmission = System.currentTimeMillis();
    private int mSchedule;

    private String mTemporaryPhotoPath = null;

    private static final int REQUEST_TAKE_PHOTO = 1;

    /* Admob */
    private InterstitialAd mInterstitialAd;

    /* Analytics */
    private Tracker mTracker;

    private static final String TAG = NewProjectActivity.class.getSimpleName();

    // TODO implement tags

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_project);
        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.new_project_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Set up interstitial ad
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        /* Set up the database */
        mTimeLapseDatabase = TimeLapseDatabase.getInstance(this);

        /* Populate the spinner */
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.schedule_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mScheduleSpinner.setAdapter(adapter);
        mScheduleSpinner.setOnItemSelectedListener(this);

        /* Set fab to add initial project photo */
        mFirstPhotoFab.setOnClickListener(view -> takeTemporaryPicture());

        /* Set fab to submit the project */
        mNewProjectFab.setOnClickListener(view -> {
            if (mTemporaryPhotoPath == null) takeTemporaryPicture();
            else submitNewProject();
        });

        mProjectToEdit = getIntent().getParcelableExtra(Keys.PROJECT_ENTRY);

        /* If a project was sent along with the intent set up the activity instead to edit the project */
        if (mProjectToEdit != null){
            // Restore the project info
            mName = mProjectToEdit.getName();
            mSchedule = mProjectToEdit.getSchedule();
            mScheduleNextSubmission = mProjectToEdit.getSchedule_next_submission();
            mTemporaryPhotoPath = mProjectToEdit.getThumbnail_url();

            // Hide the add photo button
            mFirstPhotoFab.hide();

            // Change the listener from new project submission to edit submission
            mNewProjectFab.setOnClickListener( view -> {
                submitProjectEdit();
            });
        }

        /* Restore the instance state if there is one */
        if (savedInstanceState != null){
            // Restore member variables
            mName = savedInstanceState.getString(Keys.PROJECT_NAME);
            mSchedule = savedInstanceState.getInt(Keys.PROJECT_SCHEDULE, 0);
            mTemporaryPhotoPath = savedInstanceState.getString(Keys.TEMP_PATH);
            mScheduleNextSubmission = savedInstanceState.getLong(Keys.CHOSEN_TIME);

            if (mTemporaryPhotoPath != null){
                mFirstPhotoFab.setImageResource(R.drawable.ic_repeat_white_24dp);
            }
        }

        // If new project populate ui will only set schedule spinner to preset 0 or notification NEVER
        populateUi();

        // Prepare analytics
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
    }

    /* Sets edit text, spinner, and picture to the values contained in member variables */
    private void populateUi(){
        // Restore ui
        if (mName != null) mProjectNameEditText.setText(mName);
        mScheduleSpinner.setSelection(mSchedule);
        if (mTemporaryPhotoPath != null) loadImage(mTemporaryPhotoPath);

        int hours = TimeUtils.getHourFromTimestamp(mScheduleNextSubmission);
        int minutes = TimeUtils.getMinutesFromTimestamp(mScheduleNextSubmission);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mTimePicker.setHour(hours);
            mTimePicker.setMinute(minutes);
        } else {
            mTimePicker.setCurrentHour(hours);
            mTimePicker.setCurrentMinute(minutes);
        }
    }

    /* Handles spinner selection */
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        mScheduleString = getResources().getStringArray(R.array.schedule_options)[i];

        // Only show time picker if a schedule is selected
        if (i == 0) mTimePicker.setVisibility(View.GONE);
        else mTimePicker.setVisibility(View.VISIBLE);
    }

    /* If no spinner selection set schedule to NONE */
    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        mScheduleString = getResources().getString(R.string.none);
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
                mTemporaryPhotoPath = tempFile.getAbsolutePath();
            } catch (IOException e) {
                Log.e(TAG, "failure creating file", e);
                // TODO display toast
                // TODO log with crashylitics
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

    /* On returning from the implicit camera intent */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {

            loadImage(mTemporaryPhotoPath);

            mFirstPhotoFab.setImageResource(R.drawable.ic_repeat_white_24dp);

            // Load ad if it isn't loaded
            if (!mInterstitialAd.isLoaded()){
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }
        }
    }

    /* Loads an image into the main photo view */
    private void loadImage(String imagePath){

        // Get photo info
        String ratio = PhotoUtils.getAspectRatioFromImagePath(imagePath);
        if (ratio == null) return;

        // Set cardview constraints depending upon if photo is landscape or portrait
        FrameLayout imageLayout = findViewById(R.id.image_container);
        ViewGroup.LayoutParams layoutParams = imageLayout.getLayoutParams();

        // wrap the content of the photo adjusted to the aspect ratio
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;


        // Resize the constraint layout
        ConstraintSet constraintSet = new ConstraintSet();
        ConstraintLayout constraintLayout = findViewById(R.id.new_project_image_constraint_layout);
        constraintSet.clone(constraintLayout);
        constraintSet.setDimensionRatio(R.id.project_first_image, ratio);
        constraintSet.applyTo(constraintLayout);

        // Load the image
        File f = new File(imagePath);
        Glide.with(this)
                .load(f)
                .into(mProjectFirstImage);

    }

    /* Gathers user input into a project entry object */
    private ProjectEntry gatherUserInput(){
        /* Get the name from the edit text */
        mName = mProjectNameEditText.getText().toString();

        /* Get the schedule from the spinner value */
        mSchedule = TimeUtils.convertScheduleStringToInt(this, mScheduleString);

        /* Get the current timestamp */
        long timestamp = System.currentTimeMillis();

        /* Set the next submission, but if there is a schedule calc the next submission time */
        long pickerTime = getTimestampFromPicker();
        if (pickerTime > System.currentTimeMillis())
            mScheduleNextSubmission = pickerTime;
        else
            mScheduleNextSubmission = getTimestampFromPicker() + TimeUtils.getTimeIntervalFromSchedule(mSchedule);

        /* Return the new project */
        return new ProjectEntry(mName, mTemporaryPhotoPath, mSchedule, mScheduleNextSubmission, timestamp);
    }

    private long getTimestampFromPicker(){
        int hour;
        int minute;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hour = mTimePicker.getHour();
            minute = mTimePicker.getMinute();
        } else {
            hour = mTimePicker.getCurrentHour();
            minute = mTimePicker.getCurrentMinute();
        }

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);

        return c.getTimeInMillis();
    }

    /* Submits the new project to the database */
    private void submitNewProject(){
        /* 1. Get the input */
        ProjectEntry newProject = gatherUserInput();

        /* 2. If the input is valid perform the submission */
        if (validateNewProject(newProject)) {

            /* Execute submission on a disk IO thread */
            AppExecutors.getInstance().diskIO().execute(() -> {

                // Submit the project to the database and get the id
                long projectId = mTimeLapseDatabase.projectDao()
                        .insertProject(newProject);

                ProjectEntry result = mTimeLapseDatabase.projectDao().loadProjectById(projectId);

                // Create the file for the photo
                File finalFile = FileUtils.createFinalFileFromTemp(this, mTemporaryPhotoPath, result, result.getTimestamp());

                // Submit the photo entry for the project
                String photo_path = finalFile.getAbsolutePath();
                PhotoEntry currentPhoto = new PhotoEntry(result.getId(), photo_path, result.getTimestamp());
                mTimeLapseDatabase.photoDao().insertPhoto(currentPhoto);

                // Set the cover photo for the project
                result.setThumbnail_url(photo_path);
                mTimeLapseDatabase.projectDao().updateProject(result);
            });

            // If the new project has a schedule trigger the notification worker to restart
            if (newProject.getSchedule() > 0) {
                NotificationUtils.scheduleNotificationWorker(this);
                UpdateWidgetService.startActionUpdateWidgets(this);
            }

            // Track added project
            mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(getString(R.string.submit_project))
                .setAction(getString(R.string.submitted))
                .setLabel(newProject.getName())
                .build());

            /* Finish the activity */
            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdClosed() {
                        finish();
                    }
                });
                mInterstitialAd.show();
            } else {
                // TODO: implement local ad here
                finish();
            }
        }
    }

    /* Submits a project to edit */
    private void submitProjectEdit(){
        // Gather that data from the inputs and create a project for the edit
        ProjectEntry editedProject = gatherUserInput();

        // Restore id to user submitted project
        editedProject.setId(mProjectToEdit.getId());
        // Restore thumbnail
        editedProject.setThumbnail_url(mProjectToEdit.getThumbnail_url());
        // Restore timestamp
        editedProject.setTimestamp(mProjectToEdit.getTimestamp());

        /* If the project info is valid proceed to update it */
        if (validateEditProject(editedProject)) {
            boolean renameSuccessful;

            /* If the user changed the name of the project rename the directory */
            if (!mProjectToEdit.getName().equals(editedProject.getName())){
                renameSuccessful = FileUtils.renameProject(this, mProjectToEdit, editedProject);
            }
            // Otherwise user did not try to rename the project
            else {
                renameSuccessful = true;
            }

            // If renaming the file directory is not successful handle that here
            if (!renameSuccessful) {
                notifyUserEditFailed();
                return;
            }

            // If the schedule has been changed start the notification worker
            if (mProjectToEdit.getSchedule() != editedProject.getSchedule()) {
                NotificationUtils.scheduleNotificationWorker(this);
                UpdateWidgetService.startActionUpdateWidgets(this);
            }

            /* Submit the edit to the database */
            AppExecutors.getInstance().diskIO().execute(() -> {
                // 1. Update the project
                mTimeLapseDatabase.projectDao().updateProject(editedProject);
            });

            /* Show an add then finish the activity */
            finish();
        }
    }

    /* Reload ad if consumed */
    @Override
    protected void onResume() {
        super.onResume();
        if (!mInterstitialAd.isLoaded()){
            mInterstitialAd.loadAd(new AdRequest.Builder().build());
        }

        // Track activity launch
        mTracker.setScreenName(getString(R.string.new_project_activity));
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    /* Validates new project */
    private boolean validateNewProject(ProjectEntry newProject){
        /* If the name is empty do not progress */
        if (mName.isEmpty()) {
            notifyUserNoName();
            return false;
        }

        /* If the name contains any reserved characters project invalid */
        if (FileUtils.pathContainsReservedCharacter(newProject.getName())) {
            notifyUserInvalidCharacters();
            return false;
        }

        /* If the user has not created a first photo do not validate */
        if (mTemporaryPhotoPath == null) {
            notifyUserNoPicture();
            return false;
        }

        /* Continue on to submit project method */
        return true;
    }

    /* Validates prior to editing */
    private boolean validateEditProject(ProjectEntry editedProject){
        /* If the name is empty do not progress */
        if (editedProject.getName().isEmpty()) {
            notifyUserNoName();
            return false;
        }

        /* Check name of edited project for reserved characters */
        if (FileUtils.pathContainsReservedCharacter(editedProject.getName())) {
            notifyUserInvalidCharacters();
            return false;
        }

        return true;
    }

    /* Notification methods give the user feedback on what went wrong */
    private void notifyUserNoName(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.invalid_name)
                .setMessage(R.string.no_name_message)
                .setIcon(android.R.drawable.ic_dialog_alert).show();
    }

    private void notifyUserInvalidCharacters(){
        new AlertDialog.Builder(this)
        .setTitle(R.string.invalid_name)
                .setMessage(R.string.invalid_characters_message)
                .setIcon(android.R.drawable.ic_dialog_alert).show();
    }

    private void notifyUserNoPicture(){
        new AlertDialog.Builder(this)
        .setTitle(R.string.no_picture)
                .setMessage(R.string.no_picture_message)
                .setIcon(android.R.drawable.ic_dialog_alert).show();
    }

    private void notifyUserEditFailed(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(R.string.edit_failed)
                .setIcon(android.R.drawable.ic_dialog_alert).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Keys.PROJECT_NAME, mName);
        outState.putInt(Keys.PROJECT_SCHEDULE, mSchedule);
        outState.putString(Keys.TEMP_PATH, mTemporaryPhotoPath);
        outState.putLong(Keys.CHOSEN_TIME, mScheduleNextSubmission);
    }
}
