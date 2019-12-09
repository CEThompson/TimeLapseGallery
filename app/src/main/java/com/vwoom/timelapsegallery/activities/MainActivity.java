package com.vwoom.timelapsegallery.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.app.ActivityOptions;
import android.app.SharedElementCallback;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;

import com.vwoom.timelapsegallery.R;
import com.vwoom.timelapsegallery.adapters.ProjectsAdapter;
import com.vwoom.timelapsegallery.database.view.Project;
import com.vwoom.timelapsegallery.utils.FileUtils;
import com.vwoom.timelapsegallery.utils.Keys;
import com.vwoom.timelapsegallery.viewmodels.MainActivityViewModel;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

    private List<Project> mProjects;

    private int mNumberOfColumns = 3;

    private boolean mFilter;

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

        // Set the icon for the toolbar
        if (getSupportActionBar()!=null)
            getSupportActionBar().setIcon(R.drawable.actionbar_space_between_icon_and_title);

        // Initialize mobile ads
        MobileAds.initialize(this, initializationStatus -> {
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
            Intent newProjectIntent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(newProjectIntent);
        });

        prepareSharedElementTransition();
        mFilter = getIntent().getBooleanExtra(Keys.PROJECT_FILTER_BY_SCHEDULED_TODAY, false);

        // Set up the view model
        setupViewModel();
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

    /* Sets up view models */
    private void setupViewModel(){
        MainActivityViewModel viewModel = ViewModelProviders.of(this).get(MainActivityViewModel.class);

        /* Observe projects */
        viewModel.getProjects().observe(this, (List<Project> projects) -> {
            mProjects = projects;
            mProjectsAdapter.setProjectData(projects);
            mNewProjectFab.show();
        });
    }

    @Override
    public void onClick(Project clickedProject, View sharedElement, String transitionName, int position) {
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

        int returnPosition = mReenterState.getInt(Keys.TRANSITION_POSITION);

        mProjectsRecyclerView.scrollToPosition(returnPosition);
        Log.d(TAG, "shared elements: scrolling to position " + returnPosition);

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
}
