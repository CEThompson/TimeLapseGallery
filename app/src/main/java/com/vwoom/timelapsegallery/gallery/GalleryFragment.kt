package com.vwoom.timelapsegallery.gallery

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.transition.Transition
import android.transition.TransitionInflater
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.databinding.FragmentGalleryBinding
import com.vwoom.timelapsegallery.databinding.GalleryRecyclerviewItemBinding
import com.vwoom.timelapsegallery.di.base.BaseFragment
import com.vwoom.timelapsegallery.di.viewmodel.ViewModelFactory
import com.vwoom.timelapsegallery.location.SingleShotLocationProvider
import com.vwoom.timelapsegallery.testing.launchIdling
import com.vwoom.timelapsegallery.utils.PhotoUtils
import com.vwoom.timelapsegallery.weather.WeatherChartDialog
import com.vwoom.timelapsegallery.weather.WeatherDetailsDialog
import com.vwoom.timelapsegallery.weather.WeatherResult
import javax.inject.Inject

// TODO: (deferred) consider optimizing device location for forecasts (location table, get once per day or on forecast sync)
class GalleryFragment : BaseFragment(), GalleryAdapter.GalleryAdapterOnClickHandler {

    private val args: GalleryFragmentArgs by navArgs()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    private val galleryViewModel: GalleryViewModel by viewModels {
        viewModelFactory
    }

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    // Recyclerview
    private var galleryRecyclerView: RecyclerView? = null
    private var galleryAdapter: GalleryAdapter? = null
    private var gridLayoutManager: StaggeredGridLayoutManager? = null

    // UI
    private var binding: FragmentGalleryBinding? = null
    private var toolbar: Toolbar? = null
    private var addProjectFAB: FloatingActionButton? = null
    private var searchCancelFAB: FloatingActionButton? = null
    private var scrollUpFAB: FloatingActionButton? = null
    private var scrollDownFAB: FloatingActionButton? = null

    /* Dialogs */
    // Searching
    private var searchDialog: SearchDialog? = null

    // Weather
    private var weatherChartDialog: WeatherChartDialog? = null
    private var weatherDetailsDialog: WeatherDetailsDialog? = null
    private var weatherLocation: Location? = null

    private var numberOfColumns = PORTRAIT_COLUMN_COUNT
    private var recyclerLastPosition: Int = 0

    // Transitions
    private lateinit var galleryExitTransition: Transition
    private lateinit var galleryReenterTransition: Transition
    private val reenterListener = object : Transition.TransitionListener {
        override fun onTransitionEnd(transition: Transition?) {}
        override fun onTransitionCancel(transition: Transition?) {}
        override fun onTransitionStart(transition: Transition?) {
            val fadeInAnimation = AlphaAnimation(0f, 1f)
            fadeInAnimation.duration = 375
            binding?.galleryRecyclerView?.startAnimation(fadeInAnimation)
        }

        override fun onTransitionPause(transition: Transition?) {}
        override fun onTransitionResume(transition: Transition?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)
        super.onCreate(savedInstanceState)
        galleryReenterTransition = TransitionInflater.from(context).inflateTransition(R.transition.gallery_exit_transition)
        galleryReenterTransition.addListener(reenterListener)
        galleryExitTransition = TransitionInflater.from(context).inflateTransition(R.transition.gallery_exit_transition)
        exitTransition = galleryExitTransition
        reenterTransition = galleryReenterTransition
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentGalleryBinding.inflate(inflater, container, false)

        // Set up options menu
        setHasOptionsMenu(true)
        toolbar = binding?.galleryFragmentToolbar
        toolbar?.title = getString(R.string.app_name)
        (activity as TimeLapseGalleryActivity).setSupportActionBar(toolbar)
        (activity as TimeLapseGalleryActivity).supportActionBar?.setIcon(R.drawable.actionbar_space_between_icon_and_title)

        // Increase columns for horizontal orientation
        numberOfColumns = when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> LANDSCAPE_COLUMN_COUNT
            Configuration.ORIENTATION_PORTRAIT -> PORTRAIT_COLUMN_COUNT
            else -> PORTRAIT_COLUMN_COUNT
        }

        // Set up the adapter for the recycler view
        try {
            val externalFilesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            galleryAdapter = GalleryAdapter(
                    this,
                    externalFilesDir,
                    sharedPreferences.getBoolean(getString(R.string.key_schedule_display), true),
                    sharedPreferences.getBoolean(getString(R.string.key_gif_display), true)
            )
        } catch (e: KotlinNullPointerException) {
            // TODO: (deferred) navigate to layout as failure point for getting external files dir
            Toast.makeText(requireContext(), getString(R.string.error_retrieving_files_dir), Toast.LENGTH_LONG).show()
        }
        // Set up the recycler view
        gridLayoutManager = StaggeredGridLayoutManager(numberOfColumns, StaggeredGridLayoutManager.VERTICAL)
        galleryRecyclerView = binding?.galleryRecyclerView
        galleryRecyclerView?.apply {
            layoutManager = gridLayoutManager
            setHasFixedSize(false)
            adapter = galleryAdapter
            postponeEnterTransition()
        }

        galleryRecyclerView?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                startPostponedEnterTransition()
                galleryRecyclerView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
            }
        })

        // Set up navigation to add new projects
        addProjectFAB = binding?.addProjectFAB
        addProjectFAB?.setOnClickListener {
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

        searchCancelFAB = binding?.searchActiveIndicator
        searchCancelFAB?.setOnClickListener {
            galleryViewModel.userClickedToStopSearch = true
            galleryViewModel.clearSearch()
            searchDialog?.updateSearchDialog()
        }

        scrollUpFAB = binding?.scrollUpFAB
        scrollUpFAB?.setOnClickListener {
            // TODO: (deferred) show position of gallery in a scroll bar like display
            // TODO: (deferred) listen to scroll position and hide up and down scroll fabs as necessary
            galleryRecyclerView?.scrollToPosition(0)
        }

        scrollDownFAB = binding?.scrollDownFAB
        scrollDownFAB?.setOnClickListener {
            galleryRecyclerView?.scrollToPosition(recyclerLastPosition)
        }

        // Set observers
        setupViewModel()

        // Launch with search filter if set from the notification
        if (args.searchLaunchDue && !galleryViewModel.userClickedToStopSearch) {
            galleryViewModel.searchType = SearchType.DueToday
            galleryViewModel.setSearch()
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
                if (searchDialog == null) {
                    searchDialog = SearchDialog(requireContext(), galleryViewModel)
                }
                searchDialog?.show()
                galleryViewModel.searchDialogShowing = true
                true
            }
            R.id.settings_option -> {
                val action = GalleryFragmentDirections.actionGalleryFragmentToSettingsFragment()
                findNavController().navigate(action)
                true
            }
            R.id.weather_option -> {
                // Request permissions before handling dialog
                if (gpsPermissionsGranted()) {
                    showWeatherDialog()
                } else {
                    requestPermissions(GPS_PERMISSIONS, GPS_REQUEST_CODE_PERMISSIONS)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showWeatherDialog() {
        if (weatherChartDialog == null) {
            initializeWeatherChartDialog()
        }
        weatherChartDialog?.show()
        galleryViewModel.weatherChartDialogShowing = true
        checkForWeatherUpdate()
    }

    private fun checkForWeatherUpdate() {
        if (galleryViewModel.weather.value is WeatherResult.TodaysForecast
                || galleryViewModel.weather.value is WeatherResult.Loading) return
        getLocationAndExecute {
            galleryViewModel.getForecast(weatherLocation)
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-enable the recycler view
        galleryRecyclerView?.isEnabled = true

        // Restore any dialogs
        if (galleryViewModel.searchDialogShowing) {
            if (searchDialog == null)
                searchDialog = SearchDialog(requireContext(), galleryViewModel)
            searchDialog?.show()
        }
        if (galleryViewModel.weatherChartDialogShowing) {
            showWeatherDialog()
        }
        if (galleryViewModel.weatherDetailsDialogShowing) {
            if (weatherDetailsDialog == null)
                weatherDetailsDialog = WeatherDetailsDialog(requireContext(), galleryViewModel)
            weatherDetailsDialog?.show()
        }
    }

    override fun onPause() {
        super.onPause()
        searchDialog?.dismiss()
        weatherChartDialog?.dismiss()
        weatherDetailsDialog?.dismiss()
    }

    // TODO: find better way to handle binding
    override fun onDestroyView() {
        super.onDestroyView()
        galleryRecyclerView = null
        addProjectFAB = null
        scrollDownFAB = null
        scrollUpFAB = null
        searchCancelFAB = null
        gridLayoutManager = null
        galleryAdapter = null
        toolbar = null
        binding = null
        searchDialog = null
        weatherDetailsDialog = null
        weatherChartDialog = null
    }

    /**
     * UI binding
     */
    private fun setupViewModel() {
        // Observe the entire list projects in the database
        galleryViewModel.projects.observe(viewLifecycleOwner, {
            // Update the displayed projects by filtering all projects
            // Note: default filter is none and currentProjects will simply display
            galleryViewModel.filterProjects()
        })

        // Observe the projects to be displayed after filtration
        galleryViewModel.displayedProjectViews.observe(viewLifecycleOwner, {
            galleryAdapter?.submitList(it)
            recyclerLastPosition = it.size - 1
            if (it.size > SHOW_SCROLLING_FABS_AMOUNT) showScrollingFabs()
            else hideScrollingFabs()
        })

        // Observe the search state
        // This will hide and display the search cancel fab if search filter options are not default (no tags, no search string, no due dates)
        galleryViewModel.search.observe(viewLifecycleOwner, {
            if (it) searchCancelFAB?.show() else searchCancelFAB?.hide()
        })

        // Observe the weather response saved in the database
        // This may be WeatherResponse.NoData, WeatherResponse.Loading, WeatherResponse.Cached, WeatherResponse.TodaysForecast
        galleryViewModel.weather.observe(viewLifecycleOwner, {
            // Display the data from the response in the dialogs
            weatherChartDialog?.handleWeatherChart(it)
            weatherDetailsDialog?.handleWeatherResult(it)
        })

        // Watch the tags to update the search dialog
        galleryViewModel.tags.observe(viewLifecycleOwner, {
            searchDialog?.updateSearchDialog()
        })
    }

    private fun hideScrollingFabs() {
        scrollDownFAB?.hide()
        scrollUpFAB?.hide()
    }

    private fun showScrollingFabs() {
        scrollDownFAB?.show()
        scrollUpFAB?.show()
    }

    /**
     * Search Dialog methods
     */
    private fun initializeWeatherChartDialog() {
        if (weatherChartDialog != null) return
        weatherChartDialog = WeatherChartDialog(requireContext(), galleryViewModel)
        // Set click listener to get device location and forecast, otherwise get local cache if available
        weatherChartDialog?.findViewById<FloatingActionButton>(R.id.sync_weather_data_fab)?.setOnClickListener {
            getLocationAndExecute {
                try {
                    galleryViewModel.updateForecast(weatherLocation!!)
                } catch (e: KotlinNullPointerException) {
                    galleryViewModel.getForecast(null)
                    Toast.makeText(requireContext(), getString(R.string.no_location_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
        // Set click listener to show the details dialog
        weatherChartDialog?.findViewById<TextView>(R.id.show_weather_details_tv)?.setOnClickListener {
            weatherChartDialog?.cancel()
            if (weatherDetailsDialog == null) {
                weatherDetailsDialog = WeatherDetailsDialog(requireContext(), galleryViewModel)
            }
            galleryViewModel.weatherDetailsDialogShowing = true
            weatherDetailsDialog?.show()
        }

        // Get / update the forecast
        getLocationAndExecute {
            galleryViewModel.getForecast(weatherLocation)
        }

    }

    // Gets the device location and executes a function passed as a parameter
    private fun getLocationAndExecute(toExecute: () -> Unit) {
        val lm: LocationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Determine if network or gps are enabled
        val gpsEnabled = try {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            false
        }
        val networkEnabled = try {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }

        if (!gpsEnabled && !networkEnabled) {
            galleryViewModel.getForecast(null)
            Toast.makeText(requireContext(), getString(R.string.enable_location_resources), Toast.LENGTH_LONG).show()
        }
        // Otherwise request a single shot location
        // and invoke the passed in function (forecast get or update) on success
        else {
            galleryViewModel.viewModelScope.launchIdling {
                SingleShotLocationProvider.requestSingleUpdate(requireContext(), object : SingleShotLocationProvider.LocationCallback {
                    override fun onNewLocationAvailable(location: Location?) {
                        this@GalleryFragment.weatherLocation = location
                        toExecute.invoke()
                    }
                })
            }
        }

    }

    // Navigates to the clicked project
    override fun onClick(clickedProjectView: ProjectView, binding: GalleryRecyclerviewItemBinding, position: Int) {
        // Only execute if recycler view is enabled
        if (galleryRecyclerView?.isEnabled == false) return
        // Disable the recycler view to prevent double clicks
        else galleryRecyclerView?.isEnabled = false

        // Restore the exit transition if it has been nullified by navigating to the add project fab
        if (exitTransition == null)
            exitTransition = galleryExitTransition
        if (reenterTransition == null) reenterTransition = galleryReenterTransition

        // Navigate to the detail fragment
        val action = GalleryFragmentDirections.actionGalleryFragmentToDetailsFragment(clickedProjectView)
        val extras = FragmentNavigatorExtras(
                addProjectFAB as View to getString(R.string.key_add_transition),
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
        galleryRecyclerView?.layoutManager?.onRestoreInstanceState(recyclerState)
    }

    // Save the scroll state of the recycler view
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, galleryRecyclerView?.layoutManager?.onSaveInstanceState())
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
            showWeatherDialog()
        } else {
            binding?.let {
                val snackBar = Snackbar.make(it.mainLayout, getString(R.string.permissions_required_for_forecast),
                        Snackbar.LENGTH_LONG)
                snackBar.setAction(getString(R.string.allow)) { requestPermissions(GPS_PERMISSIONS, GPS_REQUEST_CODE_PERMISSIONS) }
                snackBar.show()
            }
            galleryViewModel.setForecastNoData()
        }
    }

    companion object {
        private const val SHOW_SCROLLING_FABS_AMOUNT = 10
        private const val LANDSCAPE_COLUMN_COUNT = 6
        private const val PORTRAIT_COLUMN_COUNT = 3
        private const val BUNDLE_RECYCLER_LAYOUT = "recycler_layout_key"
        private const val GPS_REQUEST_CODE_PERMISSIONS = 1
        private val GPS_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    }
}
