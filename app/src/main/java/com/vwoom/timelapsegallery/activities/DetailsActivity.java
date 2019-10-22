package com.vwoom.timelapsegallery.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SharedElementCallback;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.vwoom.timelapsegallery.R;
import com.vwoom.timelapsegallery.adapters.DetailsAdapter;
import com.vwoom.timelapsegallery.database.AppExecutors;
import com.vwoom.timelapsegallery.database.entry.PhotoEntry;
import com.vwoom.timelapsegallery.database.entry.ProjectEntry;
import com.vwoom.timelapsegallery.database.TimeLapseDatabase;
import com.vwoom.timelapsegallery.notification.NotificationUtils;
import com.vwoom.timelapsegallery.utils.FileUtils;
import com.vwoom.timelapsegallery.utils.Keys;
import com.vwoom.timelapsegallery.utils.PhotoUtils;
import com.vwoom.timelapsegallery.utils.TimeUtils;
import com.vwoom.timelapsegallery.viewmodels.DetailsActivityViewModel;
import com.vwoom.timelapsegallery.viewmodels.DetailsViewModelFactory;
import com.vwoom.timelapsegallery.widget.UpdateWidgetService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

//TODO add share to gallery menu option?

public class DetailsActivity extends AppCompatActivity implements DetailsAdapter.DetailsAdapterOnClickHandler {

    // Photo Views
    @BindView(R.id.detail_current_image) ImageView mCurrentPhotoImageView;
    @BindView(R.id.detail_next_image) ImageView mNextPhotoImageView;
    @BindView(R.id.details_card_container) CardView mCardView;

    // Progress Indication
    @BindView(R.id.image_loading_progress) ProgressBar mProgressBar;

    // Recycler View
    @BindView(R.id.details_recyclerview) RecyclerView mDetailsRecyclerView;
    private DetailsAdapter mDetailsAdapter;

    // Fabs
    @BindView(R.id.add_photo_fab) FloatingActionButton mAddPhotoFab;
    @BindView(R.id.fullscreen_fab) FloatingActionButton mFullscreenFab;
    @BindView(R.id.play_as_video_fab) FloatingActionButton mPlayAsVideoFab;

    // Photo display
    @BindView(R.id.details_photo_date_tv) TextView mPhotoDateTv;
    @BindView(R.id.details_photo_number_tv) TextView mPhotoNumberTv;
    @BindView(R.id.details_photo_time_tv) TextView mPhotoTimeTv;

    // Project display
    @BindView(R.id.details_project_timespan_textview) TextView mProjectTimespanTv;
    @BindView(R.id.details_project_name_text_view) TextView mProjectNameTextView;

    // Database
    private TimeLapseDatabase mTimeLapseDatabase;

    // Photo and project Information
    private List<PhotoEntry> mPhotos;
    private PhotoEntry mCurrentPhoto;
    private Integer mCurrentPlayPosition = null;
    private ProjectEntry mCurrentProject;

    // Views for fullscreen dialog
    private Dialog mFullscreenImageDialog;
    private ImageView mFullscreenImage;

    private final static String KEY_DIALOG = "fullscreen_dialog";

    // For playing timelapse
    private boolean mPlaying = false;
    private Handler mPlayHandler;
    private boolean mImageIsLoaded;

    // Swipe listener for image navigation
    private OnSwipeTouchListener mOnSwipeTouchListener;

    private final static String TAG = DetailsActivity.class.getSimpleName();

    private static final int REQUEST_ADD_PHOTO = 1;

    /* Analytics */
    private FirebaseAnalytics mFirebaseAnalytics;

    /*Shared Element variables */
    private boolean mIsReturning;
    private int mReturnPosition;
    private boolean mTransitioned = false;
    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            Log.d("SharedElements", "shared element callback firing");
            if (mIsReturning){
                Log.d("SharedElements", "shared elements: detail activity callback firing");
                names.clear();
                sharedElements.clear();

                String transitionName = mCardView.getTransitionName();

                names.add(transitionName);

                sharedElements.put(transitionName, mCardView);
                sharedElements.put(Keys.ADD_FAB_TRANSITION_NAME, mAddPhotoFab);
                Log.d("SharedElements", "shared elements: transition name is " + transitionName);
            }
        }
    };

    /*
     *   Lifecycle
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.details_activity_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Set the title of the activity
        setTitle(getResources().getString(R.string.project_details));

        // Get the project information from the intent
        mCurrentProject = getIntent().getParcelableExtra(Keys.PROJECT_ENTRY);

        // Get the database
        mTimeLapseDatabase = TimeLapseDatabase.getInstance(this);

        // Set up adapter and recycler view
        mDetailsAdapter = new DetailsAdapter(this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        //linearLayoutManager.setStackFromEnd(true);
        mDetailsRecyclerView.setLayoutManager(linearLayoutManager);
        mDetailsRecyclerView.setAdapter(mDetailsAdapter);

        // Set the listener to add a photo to the project
        mAddPhotoFab.setOnClickListener((View v) -> {
            Intent addPhotoIntent = new Intent(DetailsActivity.this, AddPhotoActivity.class);
            PhotoEntry lastPhoto = getLastPhoto();
            String lastPhotoPath = lastPhoto.getUrl();

            // Send the path of the last photo and the project id
            addPhotoIntent.putExtra(Keys.PHOTO_PATH, lastPhotoPath);
            addPhotoIntent.putExtra(Keys.PROJECT_ENTRY, mCurrentProject);

            // Start add photo activity for result
            startActivityForResult(addPhotoIntent, REQUEST_ADD_PHOTO);
        });

        // Show the set of images in succession
        mPlayAsVideoFab.setOnClickListener((View v) -> {
            playSetOfImages();
        });

        // Set a listener to display the image fullscreen
        mFullscreenFab.setOnClickListener( (View v) -> {
            mFullscreenImageDialog.show();
        });

        // Set a swipe listener for the image
        mOnSwipeTouchListener = new OnSwipeTouchListener(this);
        mCurrentPhotoImageView.setOnTouchListener(mOnSwipeTouchListener);

        if (getIntent() != null){
            mReturnPosition = getIntent().getIntExtra(Keys.TRANSITION_POSITION, 0);
        }

        // TODO (update) implement pinch zoom on fullscreen image
        initializeFullscreenImageDialog();

        // If restoring state reload the selected photo
        if (savedInstanceState != null) {
            mCurrentPhoto = savedInstanceState.getParcelable(Keys.PHOTO_ENTRY);
            mReturnPosition = savedInstanceState.getInt(Keys.TRANSITION_POSITION);
            mTransitioned = true;
            showPhotoInformation();
            boolean dialogShowing = savedInstanceState.getBoolean(KEY_DIALOG, false);
            if (dialogShowing){
                preloadFullscreenImage();
                mFullscreenImageDialog.show();
            }
        } else {
            postponeEnterTransition();
        }

        // Initialize fab color
        mPlayAsVideoFab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(DetailsActivity.this, R.color.colorGreen)));
        mPlayAsVideoFab.setRippleColor(getResources().getColor(R.color.colorGreen));

        // Set the transition name for the image
        String transitionName = mCurrentProject.getId() + mCurrentProject.getName();
        mCardView.setTransitionName(transitionName);

        setupViewModel(savedInstanceState);

        // Prepare analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Set up shared element transition
        prepareSharedElementTransition();
        setEnterSharedElementCallback(mCallback);

        // Set listener to show layout items on transition completion
        Transition sharedElementTransition = getWindow().getSharedElementEnterTransition();
        sharedElementTransition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {

            }

            /* Show gradient, info, and fullscreen fab on transition completion*/
            @Override
            public void onTransitionEnd(Transition transition) {
                if (!mIsReturning){
                    showPhotoInformation();
                }
            }

            @Override
            public void onTransitionCancel(Transition transition) {

            }

            @Override
            public void onTransitionPause(Transition transition) {

            }

            @Override
            public void onTransitionResume(Transition transition) {

            }
        });
    }

    private void showPhotoInformation(){
        LinearLayout photoInformationLayout = findViewById(R.id.photo_information_layout);
        View gradientOverlay = findViewById(R.id.details_gradient_overlay);

        long shortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);

        mFullscreenFab.show();

        // Fade in gradient overlay
        gradientOverlay.setAlpha(0f);
        gradientOverlay.setVisibility(View.VISIBLE);
        gradientOverlay.animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration)
                .setListener(null);

        // Fade in photo information
        photoInformationLayout.setAlpha(0f);
        photoInformationLayout.setVisibility(View.VISIBLE);
        photoInformationLayout.animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration)
                .setListener(null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // If the activity stops while playing make sure to cancel runnables
        if (mPlaying) stopPlaying();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_PHOTO && resultCode == RESULT_OK) {
            // On successful addition of photo to project update the project thumbnail and check to update schedule
            if (data != null){

                PhotoEntry resultPhoto = data.getParcelableExtra(Keys.PHOTO_ENTRY);
                updateProjectThumbnail(mTimeLapseDatabase, mCurrentProject, resultPhoto);

                // Set the current photo to null, when null the viewmodel will set to last in set
                mCurrentPhoto = null;

                // Check to update the schedule
                int schedule = mCurrentProject.getSchedule();
                long next = mCurrentProject.getSchedule_next_submission();

                // Update if there is a schedule and the timestamp belongs to today or has elapsed
                if (schedule > 0
                    && (DateUtils.isToday(next) || System.currentTimeMillis() > next)){

                    // update the time
                    next = TimeUtils.getNextScheduledSubmission(next, schedule);

                    // update the database reference
                    final long nextTimestampToSubmit = next;
                    AppExecutors.getInstance().diskIO().execute(() -> {
                        mCurrentProject.setSchedule_next_submission(nextTimestampToSubmit);
                        mTimeLapseDatabase.projectDao().updateProject(mCurrentProject);
                    });

                    UpdateWidgetService.startActionUpdateWidgets(this);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(Keys.PHOTO_ENTRY, mCurrentPhoto);
        outState.putInt(Keys.TRANSITION_POSITION, mReturnPosition);
        outState.putBoolean(KEY_DIALOG, mFullscreenImageDialog.isShowing());
    }

    @Override
    public void onBackPressed() {
        // Use this block for hide animation
        LinearLayout photoInformationLayout = findViewById(R.id.photo_information_layout);
        View gradientOverlay = findViewById(R.id.details_gradient_overlay);
        photoInformationLayout.setVisibility(View.INVISIBLE);
        gradientOverlay.setVisibility(View.INVISIBLE);
        mFullscreenFab.hide();
        supportFinishAfterTransition();
    }

    /*
    *   Options
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.details_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.settings:
                Intent intent = new Intent(DetailsActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.delete_photo:
                if (mPhotos.size()==1){
                    verifyLastPhotoDeletion();
                } else {
                    verifyPhotoDeletion();
                }
                return true;
            case R.id.delete_project:
                verifyProjectDeletion();
                return true;
            case R.id.edit_project:
                editProject();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
    *   Shared elements methods
     */

    @Override
    public void finishAfterTransition() {
        mIsReturning = true;
        Intent data = new Intent();
        data.putExtra(Keys.TRANSITION_POSITION, mReturnPosition);
        data.putExtra(Keys.TRANSITION_NAME, mCurrentProject.getThumbnail_url());
        setResult(RESULT_OK, data);
        super.finishAfterTransition();
    }

    private void prepareSharedElementTransition(){
        Transition layoutTransition = TransitionInflater.from(this)
                .inflateTransition(R.transition.details_transition);
        getWindow().setEnterTransition(layoutTransition);

        Transition transition =
                TransitionInflater.from(this)
                        .inflateTransition(R.transition.image_shared_element_transition);
        getWindow().setSharedElementEnterTransition(transition);

    }

    private void schedulePostponedTransition(){
        if (mTransitioned) return;
        mCurrentPhotoImageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mCurrentPhotoImageView.getViewTreeObserver().removeOnPreDrawListener(this);
                mCurrentPhotoImageView.requestLayout();
                startPostponedEnterTransition();
                mTransitioned = true;
                return true;
            }
        });
    }

    /*
    *   UI methods
     */

    /* Update the UI */
    public void loadUi(PhotoEntry photoEntry){
        // Set the fullscreen image dialogue to the current photo
        if (!mPlaying) preloadFullscreenImage();

        // Notify the adapter
        mDetailsAdapter.setCurrentPhoto(photoEntry);

        // Load the current image
        loadImage(photoEntry.getUrl());

        // Get info for the current photo
        long timestamp = photoEntry.getTimestamp();
        int photoNumber = mPhotos.indexOf(photoEntry)+1;
        int photosInProject = mPhotos.size();

        // Get formatted strings
        String photoNumberString = getString(R.string.details_photo_number_out_of, photoNumber, photosInProject);
        String date = TimeUtils.getDateFromTimestamp(timestamp);
        String time = TimeUtils.getTimeFromTimestamp(timestamp);

        // Set the info
        mPhotoNumberTv.setText(photoNumberString);
        mPhotoDateTv.setText(date);
        mPhotoTimeTv.setText(time);

        int position = mPhotos.indexOf(photoEntry);
        mDetailsRecyclerView.scrollToPosition(position);

        mProgressBar.setProgress(photoNumber-1);
    }

    /* Loads an image into the main photo view */
    private void loadImage(String imagePath){
        mImageIsLoaded = false;

        // Get photo info
        String ratio = PhotoUtils.getAspectRatioFromImagePath(imagePath);
        if (ratio == null) return;

        boolean isImageLandscape = PhotoUtils.isLandscape(imagePath);

        // Set cardview constraints depending upon if photo is landscape or portrait
        ViewGroup.LayoutParams layoutParams = mCardView.getLayoutParams();
        Log.d(TAG, "is landscape = " + isImageLandscape);

        // If landscape set height to wrap content
        // Set width to be measured
        if (isImageLandscape){
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            layoutParams.width = 0;
        }
        // Otherwise image is portrait set height to be measured
        // And wrap content for width
        else {
            layoutParams.height = 0;
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        // Resize the constraint layout
        ConstraintSet constraintSet = new ConstraintSet();
        ConstraintLayout constraintLayout = findViewById(R.id.details_current_image_constraint_layout);

        constraintSet.clone(constraintLayout);
        constraintSet.setDimensionRatio(R.id.detail_current_image, ratio);
        constraintSet.setDimensionRatio(R.id.detail_next_image, ratio);
        constraintSet.applyTo(constraintLayout);

        // TODO (update) streamline code for image loading
        // Load the image
        File f = new File(imagePath);
        Glide.with(this)
                .load(f)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Toast toast = Toast.makeText(DetailsActivity.this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT);
                        toast.show();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        mNextPhotoImageView.setVisibility(View.VISIBLE);
                        Glide.with(DetailsActivity.this)
                                .load(f)
                                .listener(new RequestListener<Drawable>() {
                                    @Override
                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                        schedulePostponedTransition();
                                        Toast toast = Toast.makeText(DetailsActivity.this, getString(R.string.error_loading_image), Toast.LENGTH_SHORT);
                                        toast.show();
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                        schedulePostponedTransition();
                                        mNextPhotoImageView.setVisibility(View.INVISIBLE);
                                        mImageIsLoaded = true;
                                        return false;
                                    }
                                })
                                .into(mCurrentPhotoImageView);
                        return false;
                    }
                })
                .into(mNextPhotoImageView);
    }

    /* Loads the set of images concurrently */
    private void playSetOfImages(){
        // Lazy Initialize handler
        if (mPlayHandler == null) mPlayHandler = new Handler();
        if (mCurrentPlayPosition == null) mCurrentPlayPosition = mPhotos.indexOf(mCurrentPhoto);

        // If not enough photos give user feedback
        if (mPhotos.size()<=1){
            Toast.makeText(this, getString(R.string.add_more_photos), Toast.LENGTH_LONG).show();
            return;
        }

        // If already playing cancel
        if (mPlaying){
            // TODO track stop playing event?

            stopPlaying();
            // Handle UI
            loadUi(mCurrentPhoto);
        }
        // Otherwise play the set of images
        else {
            // TODO track play event?
            // Set color of play fab
            mPlayAsVideoFab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(DetailsActivity.this, R.color.colorRedAccent)));
            mPlayAsVideoFab.setRippleColor(getResources().getColor(R.color.colorRedAccent));

            // Set paying true
            mPlaying = true;

            // Handle UI
            mPlayAsVideoFab.setImageResource(R.drawable.ic_stop_white_24dp);

            // Schedule the runnable for a certain number of ms from now
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            String playbackIntervalSharedPref = pref.getString(getString(R.string.key_playback_interval), "50");
            int playbackInterval = Integer.parseInt(playbackIntervalSharedPref);

            // If the play position / current photo is at the end, start from the beginning
            if (mCurrentPlayPosition == mPhotos.size()-1) {
                mCurrentPlayPosition = 0;
            } // Otherwise start from wherever it is at


            loadUi(mPhotos.get(mCurrentPlayPosition));
            mProgressBar.setProgress(mCurrentPlayPosition);
            scheduleLoadPhoto(mCurrentPlayPosition, playbackInterval); // Recursively loads the rest of set from beginning
        }
    }

    /* Resets the UI & handles state after playing */
    private void stopPlaying(){
        // Set color of play fab
        mPlayAsVideoFab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(DetailsActivity.this, R.color.colorGreen)));
        mPlayAsVideoFab.setRippleColor(getResources().getColor(R.color.colorGreen));

        // Set playing false and cancel runnable
        mPlaying = false;
        mPlayHandler.removeCallbacksAndMessages(null);
        mPlayAsVideoFab.setImageResource(R.drawable.ic_play_arrow_white_24dp);

        // Set current photo to position set from playing
        mCurrentPhoto = mPhotos.get(mCurrentPlayPosition);

    }

    private void scheduleLoadPhoto(int position, int interval){
        mCurrentPlayPosition = position;
        Runnable runnable = () -> {
            // If the position is final position clean up
            if (position == mPhotos.size()-1){
                mPlaying = false;
                mPlayAsVideoFab.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                mCurrentPhoto = mPhotos.get(position);
                mProgressBar.setProgress(position);

                mPlayAsVideoFab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(DetailsActivity.this, R.color.colorGreen)));
                mPlayAsVideoFab.setRippleColor(getResources().getColor(R.color.colorGreen));
                return;
            }

            // If image is loaded load the next photo
            if (mImageIsLoaded){
                loadUi(mPhotos.get(position+1));
                mProgressBar.setProgress(position+1);
                scheduleLoadPhoto(position+1, interval);
            }
            // If the image isn't loaded recheck in interval ms
            else {
                scheduleLoadPhoto(position, interval);
            }
        };
        mPlayHandler.postDelayed(runnable, interval);
    }

    /* Sets the current entry to the clicked photo and loads the image from the entry */
    @Override
    public void onClick(PhotoEntry clickedPhoto) {
        mCurrentPhoto = clickedPhoto;
        mCurrentPlayPosition = mPhotos.indexOf(mCurrentPhoto);
        loadUi(mCurrentPhoto);
    }

    /* Sets up the full screen image dialog for later use*/
    private void initializeFullscreenImageDialog(){
        // Create the dialog
        mFullscreenImageDialog = new Dialog(this, R.style.Theme_AppCompat_Light_NoActionBar_FullScreen);
        mFullscreenImageDialog.setCancelable(false);
        mFullscreenImageDialog.setContentView(R.layout.fullscreen_image);

        mFullscreenImage = mFullscreenImageDialog.findViewById(R.id.fullscreen_image);

        // Get the fabs
        FloatingActionButton mFullscreenExitFab = mFullscreenImageDialog.findViewById(R.id.fullscreen_exit_fab);
        FloatingActionButton mFullscreenBackFab = mFullscreenImageDialog.findViewById(R.id.fullscreen_back_fab);

        // Display the dialog on clicking the image
        mFullscreenBackFab.setOnClickListener((View v) -> {
            mFullscreenImageDialog.dismiss();
        });
        mFullscreenExitFab.setOnClickListener((View v) -> {
            mFullscreenImageDialog.dismiss();
        });

        // Listen for backpress to close dialog
        mFullscreenImageDialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                mFullscreenImageDialog.dismiss();
            }
            return true;
        });

        /* Set a listener to change the current photo on swipe */
        /* Note: this may be preferable as a viewpager instead */
        mFullscreenImage.setOnTouchListener(mOnSwipeTouchListener);
    };

    // TODO (update) implement dual loading solution for smooth transition between fullscreen images
    /* Pre loads the selected image into the hidden dialogue so that display appears immediate */
    private void preloadFullscreenImage(){
        File current = new File(mCurrentPhoto.getUrl());
        Glide.with(this)
                .load(current)
                .into(mFullscreenImage);
    };

    /* Binds project and photos to database */
    private void setupViewModel(@Nullable Bundle savedInstanceState){
        DetailsViewModelFactory factory = new DetailsViewModelFactory(mTimeLapseDatabase, mCurrentProject.getId());
        final DetailsActivityViewModel viewModel = ViewModelProviders.of(this, factory)
                .get(DetailsActivityViewModel.class);

        /* Observe the list of photos */
        viewModel.getPhotos().observe(this, photoEntries -> {
            // Save the list of photos
            mPhotos = photoEntries;

            // Send the photos to the adapter
            mDetailsAdapter.setPhotoData(mPhotos);

            // Set current photo to last if none has been selected
            if (savedInstanceState == null)
                mCurrentPhoto = getLastPhoto();

            // Restore the play position
            mCurrentPlayPosition = mPhotos.indexOf(mCurrentPhoto);

            // Load the ui based on the current photo
            loadUi(mCurrentPhoto);

            // Set the date of the project based on the first photo entry
            PhotoEntry firstPhoto = mPhotos.get(0);
            long firstTimestamp = firstPhoto.getTimestamp();
            String firstProjectDateString = TimeUtils.getShortDateFromTimestamp(firstTimestamp);

            PhotoEntry lastPhoto = mPhotos.get(mPhotos.size()-1);
            long lastTimestamp = lastPhoto.getTimestamp();
            String lastProjectDateString = TimeUtils.getShortDateFromTimestamp(lastTimestamp);

            mProjectTimespanTv.setText(getString(R.string.timespan, firstProjectDateString, lastProjectDateString));

            // Set max for progress bar
            mProgressBar.setMax(mPhotos.size()-1);

        });

        /* Observe the current selected project */
        // Note: this ensures that project data is updated correctly when editing
        viewModel.getCurrentProject().observe(this, currentProject -> {
            mCurrentProject = currentProject;

            // mCurrentProject will be null upon deletion
            // So when deleting a project the viewmodel attempted to updated a null project causing a crash
            // This prevents crashes from occurring
            if (mCurrentProject != null) {
                // Set project info
                mProjectNameTextView.setText(mCurrentProject.getName());
            }
        });
    }

    /* Changes photo on swipe */
    public class OnSwipeTouchListener implements View.OnTouchListener {
        private final GestureDetector gestureDetector;

        public OnSwipeTouchListener (Context ctx){
            gestureDetector = new GestureDetector(ctx, new GestureListener());
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }

        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                boolean result = false;

                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                        result = true;
                    }
                }
                else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom();
                    } else {
                        onSwipeTop();
                    }
                    result = true;
                }

                return result;
            }
        }

        public void onSwipeRight() {
            // Do nothing if currently playing
            if (mPlaying) return;

            // Otherwise adjust the current photo to the next
            int currentIndex = mPhotos.indexOf(mCurrentPhoto);
            if (currentIndex == 0) return;
            mCurrentPhoto = mPhotos.get(currentIndex-1);
            mCurrentPlayPosition = mPhotos.indexOf(mCurrentPhoto);
            loadUi(mCurrentPhoto);
        }

        public void onSwipeLeft() {
            // Do nothing if currently playing
            if (mPlaying) return;

            // Otherwise adjust the current photo to the previous
            int currentIndex = mPhotos.indexOf(mCurrentPhoto);
            if (currentIndex == mPhotos.size()-1) return;
            mCurrentPhoto = mPhotos.get(currentIndex+1);
            mCurrentPlayPosition = mPhotos.indexOf(mCurrentPhoto);
            loadUi(mCurrentPhoto);
        }

        public void onSwipeTop() {
        }

        public void onSwipeBottom() {
        }

    }

    /*
    *   Photo management
     */

    /* Returns the last photo */
    private PhotoEntry getLastPhoto(){
        return mPhotos.get(mPhotos.size()-1);
    }

    /* Deletes the current photo */
    private void deletePhoto(TimeLapseDatabase database, PhotoEntry photoEntry){
        AppExecutors.getInstance().diskIO().execute(() -> {
                // Delete the photo from the file structure
                FileUtils.deletePhoto(this, photoEntry);

                // Delete the photo metadata in the database
                database.photoDao().deletePhoto(photoEntry);
            });

        // Send to analytics
        mFirebaseAnalytics.logEvent(getString(R.string.analytics_delete_photo), null);
    }

    /*
    *   Project management
     */

    /* Deletes the project and recursively deletes files from project folder */
    private void deleteProject(TimeLapseDatabase database, ProjectEntry projectEntry){

        /* Delete project from the database and photos from the file structure */
        AppExecutors.getInstance().diskIO().execute(() -> {
            // Delete the photos from the file structure
            FileUtils.deleteProject(DetailsActivity.this, projectEntry);
            // Delete the project from the database
            database.projectDao().deleteProject(projectEntry);

            /* If project had a schedule ensure widget and notification worker are updated */
            if (projectEntry.getSchedule() != 0) {
                NotificationUtils.scheduleNotificationWorker(this);
                UpdateWidgetService.startActionUpdateWidgets(this);
            }
        });

        // Send to analytics
        Bundle params = new Bundle();
        params.putString("project_name", projectEntry.getName());
        mFirebaseAnalytics.logEvent(getString(R.string.analytics_delete_project), null);
    }

    /* Gets the last photo from the set and sets it as the project thumbnail */
    private void updateProjectThumbnail(TimeLapseDatabase database, ProjectEntry project, PhotoEntry photo){
        AppExecutors.getInstance().diskIO().execute(() -> {
                    project.setThumbnail_url(photo.getUrl());
                    database.projectDao().updateProject(project);
                }
        );
    }

    // TODO (update) recall position after editing project
    /* Edits the current project */
    private void editProject(){
        Intent intent = new Intent(this, NewProjectActivity.class);
        intent.putExtra(Keys.PROJECT_ENTRY, mCurrentProject);
        startActivity(intent);
    }

    /*
    *   Verification
     */

    /* Deletes the current photo after user verification */
    private void verifyPhotoDeletion(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_photo)
                .setMessage(R.string.verify_delete_photo)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, (DialogInterface dialogInterface, int i) -> {
                    // If this photo is the last photo then set the new thumbnail to its previous
                    if (mCurrentPhoto.equals(getLastPhoto())){
                        PhotoEntry newLast = mPhotos.get(mPhotos.size()-2);
                        updateProjectThumbnail(mTimeLapseDatabase, mCurrentProject, newLast);
                    }

                    // Store the entry then nullify the current photo
                    PhotoEntry photoToDelete = mCurrentPhoto;
                    mCurrentPhoto = null;

                    // Delete the photo
                    deletePhoto(mTimeLapseDatabase, photoToDelete);
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    /* If the project has only one photo left deletes the project after verification */
    private void verifyLastPhotoDeletion(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_photo)
                .setMessage(R.string.verify_delete_last_photo)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, (DialogInterface dialogInterface, int i) -> {
                    verifyProjectDeletion();
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    /* Deletes the current project after user verification */
    private void verifyProjectDeletion(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_project)
                .setMessage(R.string.verify_delete_project)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, (DialogInterface dialogInterface, int i) -> {
                        doubleVerifyProjectDeletion();
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    /* Double verifies project deletion */
    private void doubleVerifyProjectDeletion(){
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_project)
                .setMessage(R.string.double_verify_project_deletion)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, (DialogInterface dialogInterface, int i) -> {
                    deleteProject(mTimeLapseDatabase, mCurrentProject);
                    if (mCurrentProject.getSchedule() != 0)
                        NotificationUtils.scheduleNotificationWorker(this);
                    finish();
                })
                .setNegativeButton(android.R.string.no, null).show();
    }
}
