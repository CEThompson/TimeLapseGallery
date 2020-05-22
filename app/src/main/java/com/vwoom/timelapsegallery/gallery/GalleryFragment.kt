package com.vwoom.timelapsegallery.gallery

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.os.SystemClock
import android.transition.Transition
import android.transition.TransitionInflater
import android.util.Log
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
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
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.databinding.FragmentGalleryBinding
import com.vwoom.timelapsegallery.databinding.GalleryRecyclerviewItemBinding
import com.vwoom.timelapsegallery.location.SingleShotLocationProvider
import com.vwoom.timelapsegallery.utils.InjectorUtils
import com.vwoom.timelapsegallery.utils.PhotoUtils
import com.vwoom.timelapsegallery.weather.WeatherChartDialog
import com.vwoom.timelapsegallery.weather.WeatherDetailsDialog
import com.vwoom.timelapsegallery.weather.WeatherResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

// TODO if network disabled show feedback

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
    private var mSearchActiveFAB: FloatingActionButton? = null

    // Searching
    private var mSearchDialog: SearchDialog? = null
    private var searchJob: Job? = null

    // Weather
    private var mWeatherChartDialog: WeatherChartDialog? = null
    private var mWeatherDetailsDialog: WeatherDetailsDialog? = null
    private var mLocation: Location? = null

    // For scrolling to the end when adding a new project
    private var mPrevProjectsSize: Int? = null

    // For preventing double click crash
    private var mLastClickTime: Long? = null

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
        (activity as TimeLapseGalleryActivity).setSupportActionBar(toolbar)
        toolbar?.title = getString(R.string.app_name)
        (activity as TimeLapseGalleryActivity).supportActionBar?.setIcon(R.drawable.actionbar_space_between_icon_and_title)

        // Increase columns for horizontal orientation
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> mNumberOfColumns = 6
            Configuration.ORIENTATION_PORTRAIT -> mNumberOfColumns = 3
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

        // TODO: (update 1.2) re-evaluate transition after taking pictures of a project, filtered projects do not update immediately

        /*if (mGalleryViewModel.displayedProjectViews.value?.isNotEmpty())
            mGalleryAdapter?.setProjectData(mGalleryViewModel.displayedProjectViews)*/

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

        mSearchActiveFAB = binding?.searchActiveIndicator
        mSearchActiveFAB?.setOnClickListener {
            mGalleryViewModel.userClickedToStopSearch = true
            //mGalleryViewModel.clearSearch()
            mSearchDialog?.clearSearch()
        }

        setupViewModel()

        // Launch with search filter if set from the notification
        if (args.searchLaunchDue && !mGalleryViewModel.userClickedToStopSearch) {
            mGalleryViewModel.searchType = SEARCH_TYPE_DUE_TODAY
        }

        // Hide or show search cancel fab depending upon whether or not the user is searching
        /*if (!userIsNotSearching()) mSearchActiveFAB?.show()
        else mSearchActiveFAB?.hide()*/

        return binding?.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.gallery_fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search_option -> {
                if (mSearchDialog == null) initializeSearchDialog()
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
                    //initializeWeatherDialogs()
                    initializeWeatherChartDialog()
                    executeWithLocation { mGalleryViewModel.getForecast(mLocation) }
                } else if (mGalleryViewModel.weather.value !is WeatherResult.TodaysForecast) {
                    //getForecast()
                    executeWithLocation { mGalleryViewModel.getForecast(mLocation) }
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
            initializeSearchDialog()
            mSearchDialog?.show()
        }

        if (mGalleryViewModel.weatherChartDialogShowing) {
            initializeWeatherChartDialog()
            mWeatherChartDialog?.show()
        }
        if (mGalleryViewModel.weatherDetailsDialogShowing) {
            initializeWeatherDetailsDialog()
            mWeatherDetailsDialog?.show()
        }
    }

    override fun onPause() {
        super.onPause()
        mSearchDialog?.dismiss()
        mWeatherChartDialog?.dismiss()
        mWeatherDetailsDialog?.dismiss()
    }

    override fun onStop() {
        super.onStop()
        searchJob?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mGalleryRecyclerView = null
        mAddProjectFAB = null
        mSearchActiveFAB = null
        mGridLayoutManager = null
        mGalleryAdapter = null
        toolbar = null
        binding = null
    }

    /**
     * UI binding
     */
    private fun setupViewModel() {
        // Observe projects
        mGalleryViewModel.projects.observe(viewLifecycleOwner, Observer { currentProjects ->
            // Detect if we have added a project and scroll to the end
            // If the size of the current list is larger a project has been added
            val projectHasBeenAdded = (mPrevProjectsSize != null && mPrevProjectsSize!! < currentProjects.size)
            if (projectHasBeenAdded) {
                if (mGalleryViewModel.displayedProjectViews.value != null) {
                    val size = mGalleryViewModel.displayedProjectViews.value!!.size
                    mGalleryRecyclerView?.scrollToPosition(size)
                }
            }
            // Keep track of number of projects
            mPrevProjectsSize = currentProjects.size

            // Update the displayed projects in the gallery
            mGalleryViewModel.viewModelScope.launch {
                mGalleryViewModel.displayedProjectViews.value = mGalleryViewModel.filterProjects()
                //mGalleryAdapter?.setProjectData(mGalleryViewModel.displayedProjectViews.value)
            }
        })

        mGalleryViewModel.displayedProjectViews.observe(viewLifecycleOwner, Observer {
            mGalleryAdapter?.setProjectData(it)
        })

        // Watch the tags to update the search dialog
        mGalleryViewModel.tags.observe(viewLifecycleOwner, Observer {
            mSearchDialog?.updateSearchDialog()
        })

        // Observe forecast
        mGalleryViewModel.weather.observe(viewLifecycleOwner, Observer {
            mWeatherChartDialog?.handleWeatherChart(it)
            mWeatherDetailsDialog?.handleWeatherResult(it)
        })

        mGalleryViewModel.search.observe(viewLifecycleOwner, Observer {
            if (it){
                mSearchActiveFAB?.show()
            } else {
                mSearchActiveFAB?.hide()
            }
        })
    }

    /**
     * Search Dialog methods
     */
    private fun initializeSearchDialog() {
        mSearchDialog = SearchDialog(requireContext(), mGalleryViewModel)
        mSearchDialog?.setOnCancelListener { mGalleryViewModel.searchDialogShowing = false }
    }

    private fun initializeWeatherChartDialog() {
        mWeatherChartDialog = WeatherChartDialog(requireContext())
        mWeatherChartDialog?.setOnCancelListener {
            mGalleryViewModel.weatherChartDialogShowing = false
        }
        mWeatherChartDialog?.findViewById<FloatingActionButton>(R.id.sync_weather_data_fab)?.setOnClickListener {
            mGalleryViewModel.weather.value = WeatherResult.Loading
            executeWithLocation {
                try {
                    mGalleryViewModel.updateForecast(mLocation!!)
                } catch (e: KotlinNullPointerException) {
                    mGalleryViewModel.getForecast(null)
                    Toast.makeText(requireContext(), getString(R.string.no_location_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
        mWeatherChartDialog?.findViewById<TextView>(R.id.show_weather_details_tv)?.setOnClickListener {
            mWeatherChartDialog?.cancel()
            if (mWeatherDetailsDialog == null) initializeWeatherDetailsDialog()
            mGalleryViewModel.weatherDetailsDialogShowing = true
            mWeatherDetailsDialog?.show()
        }

        if (mGalleryViewModel.weather.value != null)
            mWeatherChartDialog?.handleWeatherChart(mGalleryViewModel.weather.value!!)
    }

    private fun initializeWeatherDetailsDialog() {
        mWeatherDetailsDialog = WeatherDetailsDialog(requireContext())
        mWeatherDetailsDialog?.setOnCancelListener {
            mGalleryViewModel.weatherDetailsDialogShowing = false
        }
        mWeatherDetailsDialog?.findViewById<FloatingActionButton>(R.id.weather_details_dialog_exit_fab)?.setOnClickListener {
            mWeatherDetailsDialog?.cancel()
        }
        if (mGalleryViewModel.weather.value != null)
            mWeatherDetailsDialog?.handleWeatherResult(mGalleryViewModel.weather.value!!)
    }

    private fun executeWithLocation(toExecute: () -> Unit){
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

    /**
     * User Input
     */

    override fun onClick(clickedProjectView: ProjectView, binding: GalleryRecyclerviewItemBinding, position: Int) {
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
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val recyclerState: Parcelable? = savedInstanceState?.getParcelable(BUNDLE_RECYCLER_LAYOUT)
        mGalleryRecyclerView?.layoutManager?.onRestoreInstanceState(recyclerState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, mGalleryRecyclerView?.layoutManager?.onSaveInstanceState())
    }

    private fun gpsPermissionsGranted() = GPS_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == GPS_REQUEST_CODE_PERMISSIONS && gpsPermissionsGranted()) {
            //getForecast()
            executeWithLocation { mGalleryViewModel.getForecast(mLocation) }
        } else {
            Toast.makeText(this.requireContext(), getString(R.string.permissions_required_for_forecast), Toast.LENGTH_SHORT).show()
            mGalleryViewModel.weather.value = WeatherResult.NoData()
        }
    }

    companion object {
        private var mNumberOfColumns = 3
        private const val BUNDLE_RECYCLER_LAYOUT = "recycler_layout_key"
        private val TAG = GalleryFragment::class.simpleName
        private const val GPS_REQUEST_CODE_PERMISSIONS = 1
        private val GPS_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
