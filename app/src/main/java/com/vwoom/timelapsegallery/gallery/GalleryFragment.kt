package com.vwoom.timelapsegallery.gallery

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Typeface
import android.location.Location
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.os.SystemClock
import android.transition.Transition
import android.transition.TransitionInflater
import android.util.Log
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.*
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
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.repository.WeatherResult
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.databinding.FragmentGalleryBinding
import com.vwoom.timelapsegallery.databinding.GalleryRecyclerviewItemBinding
import com.vwoom.timelapsegallery.utils.InjectorUtils
import com.vwoom.timelapsegallery.utils.PhotoUtils
import com.vwoom.timelapsegallery.weather.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.*

// TODO: create gifs or mp4s from photo sets
// TODO: remove all instances of non-null assertion !!
// TODO: increase test coverage, viewmodels? livedata?
// TODO: add dialog to show weather for the next week
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
    private var mSearchDialog: Dialog? = null
    private var searchJob: Job? = null

    // Weather
    private var mWeatherDialog: Dialog? = null
    private var mLocation: Location? = null
    private var mWeatherRecyclerView: RecyclerView? = null
    private var mWeatherAdapter: WeatherAdapter? = null

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

        // TODO handle external files drive failure
        // Set up the adapter for the recycler view
        val externalFilesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        mGalleryAdapter = GalleryAdapter(this, externalFilesDir!!, schedulesDisplayed)

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
        if (mGalleryViewModel.displayedProjectViews.isNotEmpty())
            mGalleryAdapter?.setProjectData(mGalleryViewModel.displayedProjectViews)

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
            clearSearch()
        }

        setupViewModel()

        // Launch with search filter if set from the notification
        if (args.searchLaunchDue && !mGalleryViewModel.userClickedToStopSearch) {
            mGalleryViewModel.searchType = SEARCH_TYPE_DUE_TODAY
        }

        // Hide or show search cancel fab depending upon whether or not the user is searching
        if (!userIsNotSearching()) mSearchActiveFAB?.show()
        else mSearchActiveFAB?.hide()

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
                // TODO if network disabled show feedback
                if (mWeatherDialog == null) initializeWeatherDialog()
                getDeviceLocation()
                mWeatherDialog?.show()
                mGalleryViewModel.weatherDialogShowing = true
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
        if (mGalleryViewModel.weatherDialogShowing){
            initializeWeatherDialog()
            getDeviceLocation()
            mWeatherDialog?.show()
        }
    }

    override fun onPause() {
        super.onPause()
        mSearchDialog?.dismiss()
        mWeatherDialog?.dismiss()
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
                mGalleryRecyclerView?.scrollToPosition(mGalleryViewModel.displayedProjectViews.size)
            }
            // Keep track of number of projects
            mPrevProjectsSize = currentProjects.size

            // Update the displayed projects in the gallery
            mGalleryViewModel.viewModelScope.launch {
                mGalleryViewModel.displayedProjectViews = mGalleryViewModel.filterProjects()
                mGalleryAdapter?.setProjectData(mGalleryViewModel.displayedProjectViews)
            }
        })

        // Watch the tags to update the search dialog
        mGalleryViewModel.tags.observe(viewLifecycleOwner, Observer {
            updateSearchDialog()
        })

        // Observe forecast
        // TODO handle different states of weather data
        mGalleryViewModel.weather.observe(viewLifecycleOwner, Observer{
            handleWeatherChart(it)
        })
    }

    /**
     * Search Dialog methods
     */
    private fun initializeSearchDialog() {
        mSearchDialog = Dialog(requireContext())
        mSearchDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        mSearchDialog?.setContentView(R.layout.dialog_search)
        mSearchDialog?.setOnCancelListener { mGalleryViewModel.searchDialogShowing = false }

        val searchEditText = mSearchDialog?.findViewById<EditText>(R.id.search_edit_text)

        val exitFab = mSearchDialog?.findViewById<FloatingActionButton>(R.id.search_dialog_exit_fab)
        exitFab?.setOnClickListener {
            mGalleryViewModel.searchDialogShowing = false
            mSearchDialog?.dismiss()
        }
        val okDismiss = mSearchDialog?.findViewById<TextView>(R.id.search_dialog_dismiss)
        okDismiss?.setOnClickListener {
            mGalleryViewModel.searchDialogShowing = false
            mSearchDialog?.dismiss()
        }

        val dueTodayCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_due_today_checkbox)
        val dueTomorrowCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_due_tomorrow_checkbox)
        val pendingCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_pending_checkbox)
        val scheduledCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_scheduled_checkbox)
        val unscheduledCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_unscheduled_checkbox)

        searchEditText?.setText(mGalleryViewModel.searchName)   // recover current search term

        searchEditText?.addTextChangedListener {
            val searchName = it.toString().trim()
            mGalleryViewModel.searchName = searchName
            updateSearchFilter()
        }

        // Handle search selection of scheduled / unscheduled projects
        dueTodayCheckBox?.setOnClickListener {
            val checked = (it as CheckBox).isChecked
            if (checked) mGalleryViewModel.searchType = SEARCH_TYPE_DUE_TODAY
            else mGalleryViewModel.searchType = SEARCH_TYPE_NONE
            updateSearchDialogCheckboxes()
            updateSearchFilter()
        }
        dueTomorrowCheckBox?.setOnClickListener {
            val checked = (it as CheckBox).isChecked
            if (checked) mGalleryViewModel.searchType = SEARCH_TYPE_DUE_TOMORROW
            else mGalleryViewModel.searchType = SEARCH_TYPE_NONE
            updateSearchDialogCheckboxes()
            updateSearchFilter()
        }
        pendingCheckBox?.setOnClickListener {
            val checked = (it as CheckBox).isChecked
            if (checked) mGalleryViewModel.searchType = SEARCH_TYPE_PENDING
            else mGalleryViewModel.searchType = SEARCH_TYPE_NONE
            updateSearchDialogCheckboxes()
            updateSearchFilter()
        }
        scheduledCheckBox?.setOnClickListener {
            val checked = (it as CheckBox).isChecked
            if (checked) mGalleryViewModel.searchType = SEARCH_TYPE_SCHEDULED
            else mGalleryViewModel.searchType = SEARCH_TYPE_NONE
            updateSearchDialogCheckboxes()
            updateSearchFilter()
        }
        unscheduledCheckBox?.setOnClickListener {
            val checked = (it as CheckBox).isChecked
            if (checked) mGalleryViewModel.searchType = SEARCH_TYPE_UNSCHEDULED
            else mGalleryViewModel.searchType = SEARCH_TYPE_NONE
            updateSearchDialogCheckboxes()
            updateSearchFilter()
        }
        updateSearchDialog()
    }

    private fun initializeWeatherDialog() {
        mWeatherDialog = Dialog(requireContext())
        mWeatherDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        mWeatherDialog?.setContentView(R.layout.dialog_weather_chart)
        mWeatherDialog?.setOnCancelListener { mGalleryViewModel.weatherDialogShowing = false }

        /*mWeatherDialog?.findViewById<FloatingActionButton>(R.id.weather_chart_exit_fab)?.setOnClickListener {
            mWeatherDialog?.cancel()
        }*/

        mWeatherDialog?.findViewById<FloatingActionButton>(R.id.sync_weather_data_fab)?.setOnClickListener {
            updateWeatherData()
        }
        if (mGalleryViewModel.weather.value != null)
            handleWeatherChart(mGalleryViewModel.weather.value!!)

        // TODO set up weather details
        // Set up the list detail view for the forecast
        /*mWeatherDialog?.findViewById<TextView>(R.id.weather_chart_dismiss)?.setOnClickListener {
            mWeatherDialog?.cancel()
        }*/

        // Set up the weather dialog recycler view
        /*mWeatherRecyclerView = mWeatherDialog?.findViewById(R.id.weather_recycler_view)
        mWeatherAdapter = WeatherAdapter(this)
        val weatherLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        mWeatherRecyclerView?.apply {
            layoutManager = weatherLayoutManager
            setHasFixedSize(false)
            adapter = mWeatherAdapter
        }
        */
    }

    private fun showWeatherSuccess(){
        mWeatherDialog?.findViewById<TextView>(R.id.weather_chart_failure)?.visibility = View.INVISIBLE
        mWeatherDialog?.findViewById<ProgressBar>(R.id.weather_chart_progress)?.visibility = View.INVISIBLE
        mWeatherDialog?.findViewById<LineChart>(R.id.weather_chart)?.visibility = View.VISIBLE
    }

    private fun showWeatherLoading(){
        mWeatherDialog?.findViewById<TextView>(R.id.weather_chart_failure)?.visibility = View.INVISIBLE
        mWeatherDialog?.findViewById<ProgressBar>(R.id.weather_chart_progress)?.visibility = View.VISIBLE
        mWeatherDialog?.findViewById<LineChart>(R.id.weather_chart)?.visibility = View.INVISIBLE
    }

    private fun showWeatherFailure(){
        mWeatherDialog?.findViewById<TextView>(R.id.weather_chart_failure)?.visibility = View.INVISIBLE
        //val progress = mWeatherDialog?.findViewById<ProgressBar>(R.id.weather_chart_progress)
        //Log.d(TAG, "trying to hide progress: ${progress.toString()}")
        mWeatherDialog?.findViewById<ProgressBar>(R.id.weather_chart_progress)?.visibility = View.INVISIBLE
        mWeatherDialog?.findViewById<LineChart>(R.id.weather_chart)?.visibility = View.VISIBLE
    }

    private fun updateWeatherData(){
        mGalleryViewModel.weather.value = WeatherResult.Loading
        Log.d(TAG, "getting device location")
        if (gpsPermissionsGranted()) {
            Log.d(TAG, "permissions granted: requesting single shot location")

            SingleShotLocationProvider.requestSingleUpdate(requireContext(), object : SingleShotLocationProvider.LocationCallback {
                override fun onNewLocationAvailable(location: Location?) {
                    mLocation = location
                    mGalleryViewModel.updateForecast(
                            location?.latitude.toString(),
                            location?.longitude.toString())
                }
            })
        } else {
            Log.d(TAG, "permissions not granted")
            requestPermissions(GPS_PERMISSIONS, GPS_REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun getDeviceLocation(){
        Log.d(TAG, "getting device location")
        if (gpsPermissionsGranted()) {
            Log.d(TAG, "permissions granted: requesting single shot location")

            SingleShotLocationProvider.requestSingleUpdate(requireContext(), object : SingleShotLocationProvider.LocationCallback {
                override fun onNewLocationAvailable(location: Location?) {
                    mLocation = location
                    mGalleryViewModel.getForecast(
                            location?.latitude.toString(),
                            location?.longitude.toString())
                }
            })
        } else {
            Log.d(TAG, "permissions not granted")
            requestPermissions(GPS_PERMISSIONS, GPS_REQUEST_CODE_PERMISSIONS)
        }
    }

    // TODO bind different icons for weather types, cloudy, rainy, clear
    // TODO bind wind conditions
    // TODO set up refresh
    // TODO fix days of week
    // TODO fix clickability / detail list view of periods
    // TODO show the number of projects due per day during the week
    private fun handleWeatherChart(result: WeatherResult<Any>){
        when (result){
            is WeatherResult.Loading -> {
                showWeatherLoading()
            }
            is WeatherResult.Success -> {
                mWeatherDialog?.findViewById<TextView>(R.id.update_status_tv)?.text =
                        (result.data as ForecastResponse).properties.generatedAt
                setWeatherChart(result.data as ForecastResponse)
                showWeatherSuccess()
            }
            is WeatherResult.Failure -> {
                mWeatherDialog?.findViewById<TextView>(R.id.update_status_tv)?.text = //"failed to retrieve weather data, showing cached forecast"
                        result.exception?.localizedMessage ?: "failed to retrieve updated data, showing cached data"
                showWeatherFailure()
            }
            //is WeatherResult.Cached -> {
            //    // TODO handle cached state
           // }

        }
    }

    // TODO calc projects due per day
    private fun setWeatherChart(forecast: ForecastResponse){
        val periods : List<ForecastResponse.Period>? = forecast.properties.periods
        if (periods != null){
            val chart = mWeatherDialog?.findViewById<LineChart>(R.id.weather_chart) ?: return

            //periods = periods.subList(1,periods.size-1)
            // Set the entries for the chart
            val weatherEntries: ArrayList<Entry> = arrayListOf()
            val averages: ArrayList<Entry> = arrayListOf()
            val iconEntries : ArrayList<Entry> = arrayListOf()
            val axisLabels: ArrayList<String> = arrayListOf()

            for (i in periods.indices) {

                weatherEntries.add(Entry(i.toFloat(), periods[i].temperature.toFloat()))

                // Set the label
                //if (periods[i].isDaytime){
                //    axisLabels.add(periods[i].name.substring(0,3).toUpperCase(Locale.getDefault()))
                //}

                // Handle icon per period
                // TODO adjust icons per weather type, clear, rainy, cloudy, etc.
                if (periods[i].isDaytime){
                    iconEntries.add(Entry(i.toFloat(), periods[i].temperature.toFloat()+5f,
                            ContextCompat.getDrawable(requireContext(),R.drawable.ic_wb_sunny_black_24dp)))
                } else {
                    iconEntries.add(Entry(i.toFloat(), periods[i].temperature.toFloat()+5f,
                            ContextCompat.getDrawable(requireContext(),R.drawable.ic_star_black_24dp)))
                }
            }

            // Handle averages
            val start = if (periods[0].isDaytime) 0 else 1
            for (i in start until periods.size-1 step 2){
                //if ( (i+1) !in periods.indices) break
                val avg = (periods[i].temperature.toFloat() + periods[i+1].temperature.toFloat()) / 2f
                averages.add(Entry((i.toFloat()+(i+1).toFloat())/2f, avg))
            }
            if (start == 1) {
                val first = (periods[0].temperature.toFloat() + periods[1].temperature.toFloat()) / 2f
                val entry = Entry(0.5f, first)
                //val entry = Entry(0.5f, averages[0].y)
                averages.add(0,entry)

                val last = (periods[periods.size-1].temperature.toFloat() + periods[periods.size-2].temperature.toFloat()) / 2f
                val lastEntry = Entry(((periods.size-1 + periods.size-2).toFloat() / 2f), last)
                //val lastEntry = Entry(((periods.size-1 + periods.size-2).toFloat() / 2f), averages.last().y)
                averages.add(lastEntry)
            }
            /*for (i in 0 until periods.size-1){
                val avg = (periods[i].temperature.toFloat() + periods[i+1].temperature.toFloat()) / 2f
                averages.add(Entry((i.toFloat()+(i+1).toFloat())/2f, avg))
            }*/

            // Handle labels
            for (i in 0 until periods.size-1 step 1){
                axisLabels.add(periods[i].name.substring(0,3).toUpperCase(Locale.getDefault()))
            }

            Log.d(TAG, axisLabels.toString())
            // Set axis info
            val valueFormatter = object: ValueFormatter(){
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    //return axisLabels[value.toInt()]
                    return if (value.toInt() < axisLabels.size)
                        axisLabels[value.toInt()]
                    else ""
                }
            }

            //if (!periods[0].isDaytime)
            chart.xAxis?.granularity = 1f
            chart.xAxis?.valueFormatter = valueFormatter

            // Hide axis lines
            chart.xAxis?.setDrawGridLines(false)
            chart.xAxis?.setDrawAxisLine(false)
            chart.axisRight?.isEnabled = false
            chart.axisLeft?.isEnabled = false
            chart.description?.isEnabled = false
            //chart?.legend?.isEnabled = false


            // Set the dataSet
            val tempType = if (periods[0].temperatureUnit == "F") WeatherAdapter.FAHRENHEIT else WeatherAdapter.CELSIUS
            val weatherDataSet = LineDataSet(weatherEntries, tempType)
            val iconDataSet = LineDataSet(iconEntries, "Weather Type")
            val avgDataSet = LineDataSet(averages, "Average Temp")
            avgDataSet.setDrawCircles(false)
            avgDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
            avgDataSet.setDrawValues(false)

            weatherDataSet.enableDashedLine(0.8f,1f,0f)
            weatherDataSet.setDrawCircles(false)

            iconDataSet.setDrawIcons(true)
            iconDataSet.setDrawCircles(false)
            iconDataSet.setDrawValues(false)
            iconDataSet.enableDashedLine(0f,1f,0f)
            iconDataSet.color = ContextCompat.getColor(requireContext(), R.color.black)
            // Style the dataSet
            weatherDataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            weatherDataSet.cubicIntensity = .2f
            weatherDataSet.color = ContextCompat.getColor(requireContext(), R.color.colorAccent)


            val tempFormatter = object: ValueFormatter() {
                private val format = DecimalFormat("###,##0")
                override fun getPointLabel(entry: Entry?): String {
                    return format.format(entry?.y)
                }
            }
            weatherDataSet.valueFormatter = tempFormatter
            //dataset.valueTextColor

            weatherDataSet.valueTextSize = 14f

            // Assign the data to the chart
            val lineData = LineData(weatherDataSet, iconDataSet, avgDataSet)
            chart.data = lineData
            chart.invalidate()

            mWeatherDialog?.findViewById<LineChart>(R.id.weather_chart)?.visibility = View.VISIBLE
            mWeatherDialog?.findViewById<ProgressBar>(R.id.weather_chart_progress)?.visibility = View.INVISIBLE
        }
    }

    private fun updateSearchFilter() {
        searchJob?.cancel()
        searchJob = mGalleryViewModel.viewModelScope.launch {
            mGalleryViewModel.displayedProjectViews = mGalleryViewModel.filterProjects()
            mGalleryAdapter?.setProjectData(mGalleryViewModel.displayedProjectViews)
            // show search fab if actively searching
            if (userIsNotSearching())
                mSearchActiveFAB?.hide()
            else
                mSearchActiveFAB?.show()
        }
    }

    private fun userIsNotSearching(): Boolean {
        return mGalleryViewModel.searchTags.isEmpty()
                && mGalleryViewModel.searchType == SEARCH_TYPE_NONE
                && mGalleryViewModel.searchName.isBlank()
    }

    // Updates the dialog with all tags in the database for filtration
    // And updates the state of the checkboxes in the dialog
    private fun updateSearchDialog() {
        if (mSearchDialog == null) return

        // 1. Update the name edit text
        mSearchDialog?.findViewById<EditText>(R.id.search_edit_text)?.setText(mGalleryViewModel.searchName)

        // 2. Update tag state
        updateSearchDialogTags()

        // 3. Update the the checkboxes
        updateSearchDialogCheckboxes()
    }

    // This sets the tags
    private fun updateSearchDialogTags() {
        var tags: List<TagEntry> = listOf()
        if (mGalleryViewModel.tags.value != null) {
            tags = mGalleryViewModel.tags.value!!.sortedBy { it.text.toLowerCase(Locale.getDefault()) }
        }
        // Clear the tag layout
        val tagLayout = mSearchDialog?.findViewById<FlexboxLayout>(R.id.dialog_search_tags_layout)
        val emptyListIndicator = mSearchDialog?.findViewById<TextView>(R.id.empty_tags_label)
        tagLayout?.removeAllViews()
        // Show no tag indicator
        if (tags.isEmpty()) {
            emptyListIndicator?.visibility = View.VISIBLE
            tagLayout?.visibility = View.GONE
        } else {
            tagLayout?.visibility = View.VISIBLE
            emptyListIndicator?.visibility = View.GONE
        }
        // Create the tag views
        for (tag in tags) {
            val tagCheckBox = CheckBox(requireContext())
            tagCheckBox.text = getString(R.string.hashtag, tag.text)
            tagCheckBox.setTypeface(null, Typeface.ITALIC)
            tagCheckBox.alpha = .8f
            tagCheckBox.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTag))
            tagCheckBox.isChecked = mGalleryViewModel.tagSelected(tag)
            tagCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) mGalleryViewModel.searchTags.add(tag)
                else mGalleryViewModel.searchTags.remove(tag)
                updateSearchFilter()
            }
            tagLayout?.addView(tagCheckBox)
        }
    }

    // This sets the checkboxes
    private fun updateSearchDialogCheckboxes() {
        // Update the state of search by schedule layout
        val dueTodayCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_due_today_checkbox)
        val dueTomorrowCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_due_tomorrow_checkbox)
        val pendingCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_pending_checkbox)
        val scheduledCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_scheduled_checkbox)
        val unscheduledCheckBox = mSearchDialog?.findViewById<CheckBox>(R.id.search_unscheduled_checkbox)
        dueTodayCheckBox?.isChecked = mGalleryViewModel.searchType == SEARCH_TYPE_DUE_TODAY
        dueTomorrowCheckBox?.isChecked = mGalleryViewModel.searchType == SEARCH_TYPE_DUE_TOMORROW
        pendingCheckBox?.isChecked = mGalleryViewModel.searchType == SEARCH_TYPE_PENDING
        scheduledCheckBox?.isChecked = mGalleryViewModel.searchType == SEARCH_TYPE_SCHEDULED
        unscheduledCheckBox?.isChecked = mGalleryViewModel.searchType == SEARCH_TYPE_UNSCHEDULED
    }

    // Resets the search parameters and updates the UI
    private fun clearSearch() {
        mGalleryViewModel.searchName = ""
        mGalleryViewModel.searchTags.clear()
        mGalleryViewModel.searchType = SEARCH_TYPE_NONE
        updateSearchFilter()
        updateSearchDialog()
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
            getDeviceLocation()
        } else {
            Toast.makeText(this.requireContext(), "Permissions are required to get localized weather data.", Toast.LENGTH_SHORT).show()
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
