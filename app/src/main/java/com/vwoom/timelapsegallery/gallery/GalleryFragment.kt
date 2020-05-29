package com.vwoom.timelapsegallery.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.os.SystemClock
import android.transition.Transition
import android.transition.TransitionInflater
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.databinding.FragmentGalleryBinding
import com.vwoom.timelapsegallery.databinding.GalleryRecyclerviewItemBinding
import com.vwoom.timelapsegallery.location.SingleShotLocationProvider
import com.vwoom.timelapsegallery.utils.InjectorUtils
import com.vwoom.timelapsegallery.utils.PhotoUtils
import com.vwoom.timelapsegallery.weather.WeatherChartDialog
import com.vwoom.timelapsegallery.weather.WeatherDetailsDialog
import com.vwoom.timelapsegallery.weather.WeatherResult
import kotlinx.coroutines.launch

// TODO if network / data disabled show feedback on attempt to get weather
// TODO show the number of projects due per day during the week
// TODO: create gifs or mp4s from photo sets
// TODO: increase test coverage
// TODO: optimize getting the device location for forecasts
class GalleryFragment : Fragment(), GalleryAdapter.GalleryAdapterOnClickHandler {

    private val args: GalleryFragmentArgs by navArgs()

    private val mGalleryViewModel: GalleryViewModel by viewModels {
        InjectorUtils.provideGalleryViewModelFactory(requireActivity())
    }

    // Recyclerview
    private var mGalleryRecyclerView: RecyclerView? = null
    private var mGalleryAdapter: GalleryAdapter? = null
    private var mGridLayoutManager: StaggeredGridLayoutManager? = null

    // UI
    private var binding: FragmentGalleryBinding? = null
    private var toolbar: Toolbar? = null
    private var mAddProjectFAB: FloatingActionButton? = null
    private var mSearchCancelFAB: FloatingActionButton? = null

    // Searching
    private var mSearchDialog: SearchDialog? = null

    // Weather
    private var mWeatherChartDialog: WeatherChartDialog? = null
    private var mWeatherDetailsDialog: WeatherDetailsDialog? = null
    private var mLocation: Location? = null

    // For scrolling to the end when adding a new project
    private var mPrevProjectsSize: Int? = null

    // For preventing double click crash
    private var mLastClickTime: Long? = null

    private var mNumberOfColumns = 3

    // Transitions
    private lateinit var galleryExitTransition: Transition
    private lateinit var galleryReenterTransition: Transition
    private val reenterListener = object : Transition.TransitionListener {
        override fun onTransitionEnd(transition: Transition?) {
        }

        override fun onTransitionCancel(transition: Transition?) {
        }

        override fun onTransitionStart(transition: Transition?) {
            val fadeInAnimation = AlphaAnimation(0f, 1f)
            fadeInAnimation.duration = 375
            binding?.galleryRecyclerView?.startAnimation(fadeInAnimation)
        }

        override fun onTransitionPause(transition: Transition?) {
        }

        override fun onTransitionResume(transition: Transition?) {
        }
    }

    /**
     * Lifecycle
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        galleryReenterTransition = TransitionInflater.from(context).inflateTransition(R.transition.gallery_exit_transition)
        galleryReenterTransition.addListener(reenterListener)
        galleryExitTransition = TransitionInflater.from(context).inflateTransition(R.transition.gallery_exit_transition)
        exitTransition = galleryExitTransition
        reenterTransition = galleryReenterTransition
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentGalleryBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

        // Set up options menu
        setHasOptionsMenu(true)
        toolbar = binding?.galleryFragmentToolbar
        toolbar?.title = getString(R.string.app_name)
        // TODO: refactor so that toolbar does not violate inversion of control
        // TODO: convert to navigation drawer
        (activity as TimeLapseGalleryActivity).setSupportActionBar(toolbar)
        (activity as TimeLapseGalleryActivity).supportActionBar?.setIcon(R.drawable.actionbar_space_between_icon_and_title)

        // Increase columns for horizontal orientation
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> mNumberOfColumns = HORIZONTAL_COLUMN_COUNT
            Configuration.ORIENTATION_PORTRAIT -> mNumberOfColumns = VERTICAL_COLUMN_COUNT
        }

        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val schedulesDisplayed = preferences.getBoolean(getString(R.string.key_schedule_display), true)

        // Set up the adapter for the recycler view
        try {
            val externalFilesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            mGalleryAdapter = GalleryAdapter(this, externalFilesDir, schedulesDisplayed)
        } catch (e: KotlinNullPointerException){
            // TODO: set up analytics to track external files drive failure
            Toast.makeText(requireContext(), getString(R.string.error_retrieving_files_dir), Toast.LENGTH_LONG).show()
        }
        // Set up the recycler view
        mGridLayoutManager = StaggeredGridLayoutManager(mNumberOfColumns, StaggeredGridLayoutManager.VERTICAL)
        mGalleryRecyclerView = binding?.galleryRecyclerView
        mGalleryRecyclerView?.apply {
            layoutManager = mGridLayoutManager
            setHasFixedSize(false)
            adapter = mGalleryAdapter
            postponeEnterTransition()
        }

        mGalleryRecyclerView?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                startPostponedEnterTransition()
                mGalleryRecyclerView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
            }
        })

        // Set up navigation to add new projects
        mAddProjectFAB = binding?.addProjectFAB
        mAddProjectFAB?.setOnClickListener {
            // Skip the gallery fade out transition When navigating to the camera
            exitTransition = null
            reenterTransition = null
            // Send the camera ID to the camera fragment or notify the user if no camera available
            val cameraId = PhotoUtils.findCamera(requireContext())
            if (cameraId == null) {
                Toast.makeText(requireContext(), getString(R.string.no_camera_found), Toast.LENGTH_LONG).show()
            } else {
                val action = GalleryFragmentDirections.actionGalleryFragmentToCamera2Fragment(cameraId, null, null)
                findNavController().navigate(action)
            }
        }

        mSearchCancelFAB = binding?.searchActiveIndicator
        mSearchCancelFAB?.setOnClickListener {
            mGalleryViewModel.userClickedToStopSearch = true
            mSearchDialog?.clearSearch()
        }

        setupViewModel()

        // Launch with search filter if set from the notification
        if (args.searchLaunchDue && !mGalleryViewModel.userClickedToStopSearch) {
            mGalleryViewModel.searchType = SEARCH_TYPE_DUE_TODAY
        }

        return binding?.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.gallery_fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search_option -> {
                if (mSearchDialog == null) {
                    mSearchDialog = SearchDialog(requireContext(), mGalleryViewModel)
                }
                mSearchDialog?.show()
                mGalleryViewModel.searchDialogShowing = true
                true
            }
            R.id.settings_option -> {
                val action = GalleryFragmentDirections.actionGalleryFragmentToSettingsFragment()
                findNavController().navigate(action)
                true
            }
            R.id.weather_option -> {
                if (mWeatherChartDialog == null) {
                    initializeWeatherChartDialog()
                    getLocationAndExecute { mGalleryViewModel.getForecast(mLocation) }
                } else if (mGalleryViewModel.weather.value !is WeatherResult.TodaysForecast) {
                    getLocationAndExecute { mGalleryViewModel.getForecast(mLocation) }
                }
                mWeatherChartDialog?.show()
                mGalleryViewModel.weatherChartDialogShowing = true
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        if (mGalleryViewModel.searchDialogShowing) {
            mSearchDialog = SearchDialog(requireContext(), mGalleryViewModel)
            mSearchDialog?.show()
        }
        if (mGalleryViewModel.weatherChartDialogShowing) {
            initializeWeatherChartDialog()
            mWeatherChartDialog?.show()
        }
        if (mGalleryViewModel.weatherDetailsDialogShowing) {
            mWeatherDetailsDialog = WeatherDetailsDialog(requireContext(), mGalleryViewModel)
            mWeatherDetailsDialog?.show()
        }
    }

    override fun onPause() {
        super.onPause()
        mSearchDialog?.dismiss()
        mWeatherChartDialog?.dismiss()
        mWeatherDetailsDialog?.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mGalleryRecyclerView = null
        mAddProjectFAB = null
        mSearchCancelFAB = null
        mGridLayoutManager = null
        mGalleryAdapter = null
        toolbar = null
        binding = null
    }

    /**
     * UI binding
     */
    // TODO: (update 1.2) re-evaluate transition after taking pictures of a project, filtered projects do not update immediately
    private fun setupViewModel() {
        // Observe the entire list projects in the database
        // Note: this does not bind directly to displayed projects
        mGalleryViewModel.projects.observe(viewLifecycleOwner, Observer { currentProjects ->
            // 1. Detect if we have added a project and scroll to the end
            val projectHasBeenAdded = (mPrevProjectsSize != null && mPrevProjectsSize!! < currentProjects.size)
            if (projectHasBeenAdded) {
                if (mGalleryViewModel.displayedProjectViews.value != null) {
                    val size = mGalleryViewModel.displayedProjectViews.value!!.size
                    mGalleryRecyclerView?.scrollToPosition(size)
                }
            }
            mPrevProjectsSize = currentProjects.size // Keep track of number of projects for comparison to previous

            // 2. Set the displayed projects by the current filter
            // Note: default filter is none and will display currentProjects
            mGalleryViewModel.filterProjects()
            /*mGalleryViewModel.viewModelScope.launch {
                mGalleryViewModel._displayedProjectViews.value = mGalleryViewModel.filterProjects()
            }*/
        })

        // Observe the projects to be displayed after filtration
        mGalleryViewModel.displayedProjectViews.observe(viewLifecycleOwner, Observer {
            mGalleryAdapter?.setProjectData(it)
        })

        // Observe the search state
        // This will hide and display the search cancel fab if search filter options are not default (no tags, no search string, no due dates)
        mGalleryViewModel.search.observe(viewLifecycleOwner, Observer {
            if (it) mSearchCancelFAB?.show() else mSearchCancelFAB?.hide()
        })

        // Observe the weather response saved in the database
        // This may be WeatherResponse.NoData, WeatherResponse.Loading, WeatherResponse.Cached, WeatherResponse.TodaysForecast
        mGalleryViewModel.weather.observe(viewLifecycleOwner, Observer {
            // Display the data from the reponse in the dialogs
            mWeatherChartDialog?.handleWeatherChart(it)
            mWeatherDetailsDialog?.handleWeatherResult(it)
        })

        // Watch the tags to update the search dialog
        mGalleryViewModel.tags.observe(viewLifecycleOwner, Observer {
            mSearchDialog?.updateSearchDialog()
        })
    }

    /**
     * Search Dialog methods
     */
    private fun initializeWeatherChartDialog() {
        mWeatherChartDialog = WeatherChartDialog(requireContext(), mGalleryViewModel)
        // Set click listener to get device location and forecast, otherwise get local cache if available
        mWeatherChartDialog?.findViewById<FloatingActionButton>(R.id.sync_weather_data_fab)?.setOnClickListener {
            //mGalleryViewModel.weather.value = WeatherResult.Loading
            getLocationAndExecute {
                try {
                    mGalleryViewModel.updateForecast(mLocation!!)
                } catch (e: KotlinNullPointerException) {
                    mGalleryViewModel.getForecast(null)
                    Toast.makeText(requireContext(), getString(R.string.no_location_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
        // Set click listener to show the details dialog
        mWeatherChartDialog?.findViewById<TextView>(R.id.show_weather_details_tv)?.setOnClickListener {
            mWeatherChartDialog?.cancel()
            if (mWeatherDetailsDialog == null) {
                mWeatherDetailsDialog = WeatherDetailsDialog(requireContext(), mGalleryViewModel)
            }
            mGalleryViewModel.weatherDetailsDialogShowing = true
            mWeatherDetailsDialog?.show()
        }
    }

    // Gets the device location and executes a function passed as a parameter
    private fun getLocationAndExecute(toExecute: () -> Unit){
        if (gpsPermissionsGranted()) {
            SingleShotLocationProvider.requestSingleUpdate(requireContext(), object : SingleShotLocationProvider.LocationCallback {
                override fun onNewLocationAvailable(location: Location?) {
                    mLocation = location
                    toExecute.invoke()
                }
            })
        } else {
            requestPermissions(GPS_PERMISSIONS, GPS_REQUEST_CODE_PERMISSIONS)
        }
    }

    // Navigates to the clicked project
    override fun onClick(clickedProjectView: ProjectView, binding: GalleryRecyclerviewItemBinding, position: Int) {
        // TODO: refactor to click enabling / disabling, theres a way to do this rather than tracking click time
        // Prevents multiple clicks
        if (mLastClickTime != null && SystemClock.elapsedRealtime() - mLastClickTime!! < 250) return
        mLastClickTime = SystemClock.elapsedRealtime()

        // Restore the exit transition if it has been nullified by navigating to the add project fab
        if (exitTransition == null)
            exitTransition = galleryExitTransition
        if (reenterTransition == null) reenterTransition = galleryReenterTransition

        // Navigate to the detail fragment
        val action = GalleryFragmentDirections.actionGalleryFragmentToDetailsFragment(clickedProjectView, position)
        val extras = FragmentNavigatorExtras(
                mAddProjectFAB as View to getString(R.string.key_add_transition),
                binding.projectImage to binding.projectImage.transitionName,
                binding.projectCardView to binding.projectCardView.transitionName,
                binding.galleryBottomGradient to binding.galleryBottomGradient.transitionName,
                binding.galleryScheduleLayout.galleryGradientTopDown to binding.galleryScheduleLayout.galleryGradientTopDown.transitionName,
                binding.galleryScheduleLayout.scheduleDaysUntilDueTv to binding.galleryScheduleLayout.scheduleDaysUntilDueTv.transitionName,
                binding.galleryScheduleLayout.scheduleIndicatorIntervalTv to binding.galleryScheduleLayout.scheduleIndicatorIntervalTv.transitionName
        )
        findNavController().navigate(action, extras)
    }

    /**
     * Recycler View State
     */
    // Restore the scroll state of the recycler view
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val recyclerState: Parcelable? = savedInstanceState?.getParcelable(BUNDLE_RECYCLER_LAYOUT)
        mGalleryRecyclerView?.layoutManager?.onRestoreInstanceState(recyclerState)
    }
    // Save the scroll state of the recycler view
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, mGalleryRecyclerView?.layoutManager?.onSaveInstanceState())
    }

    /**
     * Permissions
     */
    // Check if gps permissions have been granted
    private fun gpsPermissionsGranted() = GPS_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    // Get the location and forecast on permission granted
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == GPS_REQUEST_CODE_PERMISSIONS && gpsPermissionsGranted()) {
            getLocationAndExecute { mGalleryViewModel.getForecast(mLocation) }
        } else {
            Toast.makeText(this.requireContext(), getString(R.string.permissions_required_for_forecast), Toast.LENGTH_SHORT).show()
            mGalleryViewModel.forecastDenied()
        }
    }

    companion object {
        private const val HORIZONTAL_COLUMN_COUNT = 6
        private const val VERTICAL_COLUMN_COUNT = 3
        private const val BUNDLE_RECYCLER_LAYOUT = "recycler_layout_key"
        private const val GPS_REQUEST_CODE_PERMISSIONS = 1
        private val GPS_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
