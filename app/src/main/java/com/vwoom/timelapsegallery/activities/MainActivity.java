package com.vwoom.timelapsegallery.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.app.ActivityOptions;
import android.app.SharedElementCallback;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.vwoom.timelapsegallery.R;
import com.vwoom.timelapsegallery.adapters.ProjectsAdapter;
import com.vwoom.timelapsegallery.database.AppExecutors;
import com.vwoom.timelapsegallery.database.TimeLapseDatabase;
import com.vwoom.timelapsegallery.database.entry.PhotoEntry;
import com.vwoom.timelapsegallery.database.entry.ProjectEntry;
import com.vwoom.timelapsegallery.utils.FileUtils;
import com.vwoom.timelapsegallery.utils.Keys;
import com.vwoom.timelapsegallery.utils.ProjectUtils;
import com.vwoom.timelapsegallery.utils.TimeUtils;
import com.vwoom.timelapsegallery.viewmodels.MainActivityViewModel;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements ProjectsAdapter.ProjectsAdapterOnClickHandler {

    @BindView(R.id.add_project_FAB)
    FloatingActionButton mNewProjectFab;

    @BindView(R.id.projects_recycler_view)
    RecyclerView mProjectsRecyclerView;

    private ProjectsAdapter mProjectsAdapter;

    private List<ProjectEntry> mProjects;

    private int mNumberOfColumns = 3;

    private Menu mMenu;

    private boolean mNotificationsEnabled;

    private boolean mFilterByToday;

    /* Analytics */
    private FirebaseAnalytics mFirebaseAnalytics;

    private static final String TAG = MainActivity.class.getSimpleName();

    /* Shared Element Position information */
    private Bundle mReenterState;
    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            // If reenter state contains bundle the activity is returning
            if (mReenterState != null){
                Log.d(TAG, "shared elements: main activity callback firing");

                String transitionName = mReenterState.getString(Keys.TRANSITION_NAME);

                View photoView = mProjectsRecyclerView.findViewWithTag(transitionName);
                Log.d(TAG, "shared elements: transition name is " + transitionName);
                if (photoView != null && transitionName != null){
                    names.clear();
                    names.add(transitionName);

                    sharedElements.clear();
                    sharedElements.put(transitionName, photoView);
                }
                mReenterState = null;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.main_activity_toolbar);
        setSupportActionBar(toolbar);

        // Initialize mobile ads
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        // Increase columns for horizontal orientation
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            mNumberOfColumns = 6;

        // Set up the adapter for the recycler view
        mProjectsAdapter = new ProjectsAdapter(this);

        // Set up the recycler view
        StaggeredGridLayoutManager gridLayoutManager = new StaggeredGridLayoutManager(mNumberOfColumns, StaggeredGridLayoutManager.VERTICAL);
        mProjectsRecyclerView.setLayoutManager(gridLayoutManager);
        mProjectsRecyclerView.setHasFixedSize(false); // adjusting views at runtime
        mProjectsRecyclerView.setAdapter(mProjectsAdapter);

        // Set up click listener to add new projects
        mNewProjectFab.setOnClickListener((View v) ->{
            Intent newProjectIntent = new Intent(MainActivity.this, NewProjectActivity.class);
            startActivity(newProjectIntent);
        });

        prepareSharedElementTransition();
        mFilterByToday = getIntent().getBooleanExtra(Keys.PROJECT_FILTER_BY_SCHEDULED_TODAY, false);

        /* TODO: (update) implement import projects */
        //importProjects();

        // Set up the view model
        setupViewModel();

        // Determine if notifications are enabled
        SharedPreferences sharedPreferences = getSharedPreferences(Keys.PREFERENCES_KEY, Context.MODE_PRIVATE);
        mNotificationsEnabled = sharedPreferences.getBoolean(Keys.NOTIFICATIONS_ENABLED, true);

        // Prepare analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }

    /* Ensure deletion of temporary photo files */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("deletion check", "onStop newProjectActivity firing");
        FileUtils.deleteTempFiles(this); // Make sure to clean up temporary files
    }

    // TODO (update) create menu option to show todays projects
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.settings:
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /* Observes all time lapse projects */
    private void setupViewModel(){
        MainActivityViewModel viewModel = ViewModelProviders.of(this).get(MainActivityViewModel.class);
        viewModel.getProjects().observe(this, (List<ProjectEntry> projectEntries) -> {
                mProjects = projectEntries;

                // If filter by today grab todays projects and set the data on the adapter
                if (mFilterByToday){
                    List<ProjectEntry> todaysProjects = ProjectUtils.getProjectsScheduledToday(mProjects);
                    mProjectsAdapter.setProjectData(todaysProjects);
                }
                // Otherwise set the adapter to projects gathered by the view model
                else {
                    mProjectsAdapter.setProjectData(mProjects);
                }

                // If there are no projects fade in the welcome text view
                TextView welcome = findViewById(R.id.welcome_text_view);
                if (mProjects.size()==0){
                    long shortAnimationDuration = getResources().getInteger(
                            android.R.integer.config_longAnimTime);
                    // Fade in gradient overlay
                    welcome.setAlpha(0f);
                    welcome.setVisibility(View.VISIBLE);
                    welcome.animate()
                            .alpha(1f)
                            .setDuration(shortAnimationDuration)
                            .setListener(null);
                    welcome.setVisibility(View.VISIBLE);
                }
                // Otherwise leave the welcome text view hidden
                else {
                    welcome.setVisibility(View.INVISIBLE);
                }

                // Reveal the new project fab
                mNewProjectFab.show();
        });
    }

    @Override
    public void onClick(ProjectEntry clickedProject, View sharedElement, String transitionName, int position) {
        Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra(Keys.PROJECT_ENTRY, clickedProject);
        intent.putExtra(Keys.TRANSITION_POSITION, position);

        Pair<View, String> p1 = Pair.create((sharedElement), transitionName);
        Pair<View, String> p2 = Pair.create((mNewProjectFab), Keys.ADD_FAB_TRANSITION_NAME);

        // Start the activity with a shared element if lollipop or higher
        Bundle bundle = ActivityOptions
                .makeSceneTransitionAnimation(MainActivity.this,
                        p1,
                        p2)
                .toBundle();

        startActivity(intent, bundle);
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        Log.d(TAG, "shared elements: reentering activity");

        mReenterState = new Bundle(data.getExtras());

        int position = mReenterState.getInt(Keys.TRANSITION_POSITION);

        mProjectsRecyclerView.scrollToPosition(position);
        Log.d(TAG, "shared elements: scrolling to position " + position);

        postponeEnterTransition();

        schedulePostponedTransition();
    }

    private void schedulePostponedTransition(){
        mProjectsRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mProjectsRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                mProjectsRecyclerView.requestLayout();
                startPostponedEnterTransition();
                return true;
            }
        });
    }

    private void prepareSharedElementTransition(){
        Transition transition = TransitionInflater.from(this)
                .inflateTransition(R.transition.image_shared_element_transition);
        getWindow().setSharedElementExitTransition(transition);
        setExitSharedElementCallback(mCallback);
    }

    /* Helper to scan through folders and import projects */
    private void importProjects(){
        Log.d(TAG, "Importing projects");
        AppExecutors.getInstance().diskIO().execute(()->{
            TimeLapseDatabase db = TimeLapseDatabase.getInstance(this);

            File storageDir = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (storageDir != null) {
                File[] files = storageDir.listFiles();

                if (files != null) {
                    for (File child : files) {
                        String url = child.getAbsolutePath();
                        Log.d(TAG, "importing url " + url);

                        String filename = url.substring(url.lastIndexOf("/")+1);
                        Log.d(TAG, "stripping to filename = " + filename);

                        if (!filename.equals(FileUtils.TEMP_FILE_SUBDIRECTORY)) {
                            String id = filename.substring(0, filename.lastIndexOf("_"));
                            Log.d(TAG, "stripping to project id = " + id);
                            String projectName = filename.substring(filename.lastIndexOf("_") + 1);
                            Log.d(TAG, "stripping to project name = " + projectName);
                            File projectDir = new File(storageDir, filename);
                            File[] projectFiles = projectDir.listFiles();

                            if (projectFiles != null) {
                                String firstPhotoPath = projectFiles[0].getAbsolutePath();
                                String lastPhotoPath = projectFiles[projectFiles.length-1].getAbsolutePath();

                                String lastPhotoRelPath = lastPhotoPath.substring(lastPhotoPath.lastIndexOf("/")+1);
                                long lastPhotoTimeStamp = Long.valueOf(lastPhotoRelPath.replaceFirst("[.][^.]+$",""));
                                String firstPhotoRelPath = firstPhotoPath.substring(firstPhotoPath.lastIndexOf("/")+1);
                                long firstPhotoTimestamp = Long.valueOf(firstPhotoRelPath.replaceFirst("[.][^.]+$",""));

                                Log.d(TAG, "first photo path = " + firstPhotoPath);
                                Log.d(TAG, "last photo path = " + lastPhotoPath);

                                Log.d(TAG, "inserting project = " + projectName);
                                ProjectEntry currentProject
                                        = new ProjectEntry(
                                                Long.valueOf(id),
                                        projectName,
                                        firstPhotoPath,
                                        TimeUtils.SCHEDULE_NONE,
                                        lastPhotoTimeStamp,
                                        firstPhotoTimestamp);

                                db.projectDao().insertProject(currentProject);

                                /* import the photos for the project */
                                importProjectPhotos(db, currentProject);
                            }
                        }
                    }
                }
            }

        });
    }

    /* Finds all photos in the project directory and adds any missing photos to the database */
    public void importProjectPhotos(TimeLapseDatabase db, ProjectEntry currentProject){

            Log.d(TAG, "syncing files");
            // Create a list of all photos in the project directory
            List<PhotoEntry> allPhotosInFolder = FileUtils.getPhotosInDirectory(this, currentProject);

            // Create empty list of photos to add
            List<PhotoEntry> photosMissingInDb = new ArrayList<>();

            // Generate a list of photos missing from the database
            if (allPhotosInFolder != null) {
                Log.d(TAG, "checking photos in folder");
                // Loop through all photos in folder
                for (PhotoEntry photo : allPhotosInFolder) {

                    long currentTimestamp = photo.getTimestamp();
                    Log.d(TAG, "checking timestamp " + currentTimestamp);
                    PhotoEntry dbPhoto = db.photoDao().loadPhotoByTimestamp(currentTimestamp, currentProject.getId());

                    Log.d(TAG, "dbPhoto is null = " + (dbPhoto == null));
                    if (dbPhoto == null) photosMissingInDb.add(photo);
                }
            }

            if (photosMissingInDb.size() == 0) return;

            Log.d(TAG, "photos missing from dabatase is " + photosMissingInDb.toString());
            Log.d(TAG, "adding the missing photos");
            // Add the missing photos to the database
            for (PhotoEntry photo: photosMissingInDb){
                db.photoDao().insertPhoto(photo);
            }
    }
}
