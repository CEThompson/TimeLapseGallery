package com.vwoom.timelapsegallery.detail

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.transition.Transition
import android.transition.TransitionInflater
import android.util.Log
import android.view.*
import android.view.View.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.databinding.FragmentDetailBinding
import com.vwoom.timelapsegallery.detail.dialog.ConversionDialog
import com.vwoom.timelapsegallery.detail.dialog.InfoDialog
import com.vwoom.timelapsegallery.detail.dialog.ScheduleDialog
import com.vwoom.timelapsegallery.detail.dialog.TagDialog
import com.vwoom.timelapsegallery.di.Injectable
import com.vwoom.timelapsegallery.di.ViewModelFactory
import com.vwoom.timelapsegallery.gif.GifUtils
import com.vwoom.timelapsegallery.notification.NotificationUtils
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.PhotoUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils.getProjectEntryFromProjectView
import com.vwoom.timelapsegallery.utils.TimeUtils
import com.vwoom.timelapsegallery.utils.TimeUtils.daysUntilDue
import com.vwoom.timelapsegallery.widget.UpdateWidgetService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import javax.inject.Inject
import kotlin.properties.Delegates

// TODO: (update 1.3) implement pinch zoom on fullscreen image
class DetailFragment : Fragment(), DetailAdapter.DetailAdapterOnClickHandler, Injectable {

    private val args: DetailFragmentArgs by navArgs()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory
    private val detailViewModel: DetailViewModel by viewModels {
        viewModelFactory
    }

    private lateinit var externalFilesDir: File
    private var binding: FragmentDetailBinding? = null
    private var toolbar: Toolbar? = null

    // Photo and project Information
    private lateinit var currentProjectView: ProjectView
    private var photos: List<PhotoEntry> = emptyList()
    private var projectTags: List<TagEntry> = emptyList()
    private var allTags: List<TagEntry> = emptyList()
    private lateinit var currentPhoto: PhotoEntry

    private var currentPlayPosition by Delegates.notNull<Int>()
    private var detailAdapter: DetailAdapter? = null

    // Dialogs
    private var tagDialog: TagDialog? = null
    private var infoDialog: InfoDialog? = null
    private var scheduleDialog: ScheduleDialog? = null
    private var convertDialog: ConversionDialog? = null

    // For playing time lapse
    private var playing = false
    private var imageIsLoaded = false
    private var playbackInterval by Delegates.notNull<Long>()

    // Jobs
    private var playJob: Job? = null
    private var tagJob: Job? = null

    // For fullscreen fragment
    private var photoUrls: Array<String> = arrayOf()

    // Analytics
    private var firebaseAnalytics: FirebaseAnalytics? = null

    // Animations for orientation indicator
    private val blinkAnimation: Animation by lazy {
        val animation: Animation = AlphaAnimation(0f, 1f) //to change visibility from visible to invisible
        animation.duration = 400 //.4 second duration for each animation cycle
        animation.repeatCount = Animation.INFINITE //repeating indefinitely
        animation.repeatMode = Animation.REVERSE //animation will start from end point once ended.
        animation
    }
    private val stopAnimation: Animation by lazy {
        val animation: Animation = AlphaAnimation(1f, 0f)
        animation.duration = 0
        animation
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentProjectView = args.clickedProjectView
        playbackInterval = getString(R.string.playback_interval_default).toLong()

        try {
            externalFilesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        } catch (exc: KotlinNullPointerException) {
            // TODO (update 1.2): Investigate potential failures with external files.
            Log.e(TAG, "Couldn't get external files directory.")
            Toast.makeText(requireContext(), "Fatal Error: Could not load external files!", Toast.LENGTH_LONG).show()
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
        val sharedElemTransition = TransitionInflater.from(context).inflateTransition(R.transition.image_shared_element_transition)
        sharedElemTransition.addListener(object : Transition.TransitionListener {
            override fun onTransitionEnd(transition: Transition?) {
                binding?.fullscreenFab?.show()
                val fadeInAnimation = AlphaAnimation(0f, 1f)
                fadeInAnimation.duration = 300
                binding?.photoInformationLayout?.startAnimation(fadeInAnimation)
            }

            override fun onTransitionCancel(transition: Transition?) {
            }

            override fun onTransitionStart(transition: Transition?) {
                binding?.fullscreenFab?.hide()
            }

            override fun onTransitionPause(transition: Transition?) {
            }

            override fun onTransitionResume(transition: Transition?) {
            }
        })
        sharedElementEnterTransition = sharedElemTransition
        sharedElementReturnTransition = sharedElemTransition
        enterTransition = TransitionInflater.from(context).inflateTransition(R.transition.details_enter_transition)
        postponeEnterTransition()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDetailBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

        // Initialize the playback interval from the shared preferences
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val playbackIntervalSharedPref = pref.getString(getString(R.string.key_playback_interval), getString(R.string.playback_interval_default))
        playbackInterval = playbackIntervalSharedPref?.toLong() ?: 50

        // TODO (update 1.3): refactor toolbar to navigation drawer
        // Set up toolbar
        setHasOptionsMenu(true)
        toolbar = binding?.detailsFragmentToolbar
        (activity as TimeLapseGalleryActivity).setSupportActionBar(toolbar)
        toolbar?.title = getString(R.string.project_details)
        (activity as TimeLapseGalleryActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set up adapter and recycler view
        detailAdapter = DetailAdapter(this, externalFilesDir, args.clickedProjectView)
        val linearLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding?.detailsRecyclerview?.layoutManager = linearLayoutManager
        binding?.detailsRecyclerview?.adapter = detailAdapter

        // 1. Initialize the color of the play as video fab
        // NOTE: this is not set in XML because setting by xml seems to lock the value of the color
        binding?.playAsVideoFab?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorGreen))
        binding?.playAsVideoFab?.rippleColor = ContextCompat.getColor(requireContext(), R.color.colorGreen)

        // 2. Set the click listeners
        binding?.addPhotoFab?.setOnClickListener {
            val cameraId = PhotoUtils.findCamera(requireContext())
            if (cameraId == null) {
                Toast.makeText(requireContext(), getString(R.string.no_camera_found), Toast.LENGTH_LONG).show()
            } else {
                val action = DetailFragmentDirections
                        .actionDetailsFragmentToCamera2Fragment(cameraId, detailViewModel.lastPhoto, currentProjectView)
                findNavController().navigate(action)
            }
        }
        binding?.playAsVideoFab?.setOnClickListener { playSetOfImages() }
        binding?.playBackwardsFab?.setOnClickListener { rewindSetOfImages() }

        // Only construct dialogs if they are clicked
        binding?.projectScheduleFab?.setOnClickListener { lazyShowScheduleDialog() }
        binding?.projectTagFab?.setOnClickListener { lazyShowProjectTagDialog() }
        binding?.projectInformationLayout?.projectInformationCardview?.setOnClickListener { lazyShowInfoDialog() }

        binding?.fullscreenFab?.setOnClickListener {
            exitTransition = TransitionInflater.from(context).inflateTransition(R.transition.details_to_fullscreen_transition)
            val action = DetailFragmentDirections.actionDetailsFragmentToFullscreenFragment(detailViewModel.photoIndex, photoUrls)
            val extras = FragmentNavigatorExtras(
                    binding!!.fullscreenFab as View to "fs_exit_fab",
                    binding!!.playAsVideoFab as View to "fs_play_fab")
            findNavController().navigate(action, extras)
        }
        // Set a swipe listener for the image
        val onRightSwipe = { if (!playing) detailViewModel.previousPhoto() }
        val onLeftSwipe = { if (!playing) detailViewModel.nextPhoto() }
        val swipeListener = OnSwipeTouchListener(
                requireContext(),
                onRightSwipe,
                onLeftSwipe)
        @Suppress("ClickableViewAccessibility")
        binding?.detailCurrentImage?.setOnTouchListener(swipeListener)

        // Set the transition tags
        val transitionName = "${currentProjectView.project_id}"
        binding?.detailCurrentImage?.transitionName = transitionName
        binding?.detailsCardContainer?.transitionName = "${transitionName}card"
        binding?.detailsGradientOverlay?.transitionName = "${transitionName}bottomGradient"
        binding?.detailScheduleLayout?.galleryGradientTopDown?.transitionName = "${transitionName}topGradient"
        binding?.detailScheduleLayout?.scheduleDaysUntilDueTv?.transitionName = "${transitionName}due"
        binding?.detailScheduleLayout?.scheduleIndicatorIntervalTv?.transitionName = "${transitionName}interval"
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        detailViewModel.setProject(args.clickedProjectView.project_id)
        setupViewModel()
    }

    private fun lazyShowInfoDialog() {
        if (infoDialog == null) {
            infoDialog = InfoDialog(requireContext(), detailViewModel, externalFilesDir, currentProjectView)
            infoDialog?.setInfoDialog(currentProjectView)
            infoDialog?.setInfoTags(getTagsText(projectTags))
        }
        infoDialog?.show()
        detailViewModel.infoDialogShowing = true
    }

    private fun lazyShowProjectTagDialog() {
        if (tagDialog == null) {
            tagDialog = TagDialog(requireContext(), detailViewModel, currentProjectView)
            tagDialog?.setProjectTagDialog(allTags, projectTags)
        }
        tagDialog?.show()
        detailViewModel.tagDialogShowing = true
    }

    private fun lazyShowScheduleDialog() {
        if (scheduleDialog == null) {
            scheduleDialog = ScheduleDialog(requireContext(), detailViewModel, externalFilesDir, currentProjectView)
        }
        scheduleDialog?.show()
        detailViewModel.scheduleDialogShowing = true
    }

    private fun lazyShowConvertDialog() {
        if (convertDialog == null) {
            convertDialog = ConversionDialog(requireContext(), detailViewModel, externalFilesDir, currentProjectView)
        }
        convertDialog?.show()
        detailViewModel.convertDialogShowing = true
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.detail_fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Opens the gif conversion dialog
            R.id.convert_to_gif -> {
                lazyShowConvertDialog()
                true
            }
            // Overflow option to share the current photo
            R.id.share_photo -> {
                val photoUrl = ProjectUtils.getProjectPhotoUrl(externalFilesDir,
                        getProjectEntryFromProjectView(currentProjectView),
                        currentPhoto.timestamp)

                if (photoUrl != null) {
                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "image/jpeg"
                        val photoFile = File(photoUrl)
                        Log.d(TAG, photoFile.absolutePath)
                        val photoURI: Uri = FileProvider.getUriForFile(requireContext(),
                                requireContext().applicationContext.packageName.toString() + ".fileprovider",
                                photoFile)
                        putExtra(Intent.EXTRA_STREAM, photoURI)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share Image"))
                }
                true
            }
            // Overflow option to delete the current photo
            R.id.delete_photo -> {
                if (photos.size == 1) {
                    verifyLastPhotoDeletion()
                } else {
                    verifyPhotoDeletion()
                }
                true
            }
            // Overflow option to delete the current project
            R.id.delete_project -> {
                verifyProjectDeletion()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Lifecycle
     */
    override fun onStart() {
        super.onStart()
        binding?.detailsFragmentToolbar?.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        // Restore any showing dialogs
        if (detailViewModel.infoDialogShowing) {
            lazyShowInfoDialog()
        }
        if (detailViewModel.scheduleDialogShowing) {
            lazyShowScheduleDialog()
        }
        if (detailViewModel.tagDialogShowing) {
            lazyShowProjectTagDialog()
        }
        if (detailViewModel.convertDialogShowing) {
            lazyShowConvertDialog()
        }
    }

    override fun onPause() {
        super.onPause()
        // Cancel any jobs
        if (playing) stopPlaying()
        tagJob?.cancel()

        // Dismiss any dialogs
        infoDialog?.dismiss()
        tagDialog?.dismiss()
        scheduleDialog?.dismiss()
        convertDialog?.dismiss()
    }

    override fun onStop() {
        super.onStop()
        binding?.detailsFragmentToolbar?.setNavigationOnClickListener(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        tagDialog = null
        scheduleDialog = null
        infoDialog = null
        convertDialog = null
        detailAdapter = null
        toolbar = null
    }

    /**
     * Photo information and loading methods
     */

    // Animates the information for the current photo to fade in
    private fun fadeInPhotoInformation() {
        if (binding == null) return
        binding!!.detailScheduleLayout.galleryGradientTopDown.animate().alpha(1f)
        binding!!.detailScheduleLayout.scheduleIndicatorIntervalTv.animate().alpha(1f)
        binding!!.detailScheduleLayout.scheduleDaysUntilDueTv.animate().alpha(1f)
        binding!!.photoInformationLayout.animate().alpha(1f)
        binding!!.fullscreenFab.show()
    }

    // Animates the information for the current photo to fade out
    private fun fadeOutPhotoInformation() {
        if (binding == null) return
        binding!!.detailScheduleLayout.galleryGradientTopDown.animate().alpha(0f)
        binding!!.detailScheduleLayout.scheduleIndicatorIntervalTv.animate().alpha(0f)
        binding!!.detailScheduleLayout.scheduleDaysUntilDueTv.animate().alpha(0f)
        binding!!.photoInformationLayout.animate().alpha(0f)
        binding!!.fullscreenFab.hide()
    }

    // Updates the ui to a particular photo entry
    private fun loadUi(photoEntry: PhotoEntry) {
        // Notify the adapter: this updates the detail recycler view red highlight indicator
        //if (!mPlaying)
        detailAdapter?.setCurrentPhoto(photoEntry)

        // Get the image path, handle orientation indicator and load the image
        val imagePath: String? = ProjectUtils.getProjectPhotoUrl(
                externalFilesDir,
                getProjectEntryFromProjectView(currentProjectView),
                photoEntry.timestamp)


        if (!playing) handleOrientationIndicator(imagePath)
        loadImage(imagePath)

        // Update position of progress view and thumbnail
        val position = photos.indexOf(photoEntry)
        val photoNumber = position + 1
        if (!playing)
            binding?.detailsRecyclerview?.scrollToPosition(position)
        binding?.imageLoadingProgress?.progress = position

        // If playing do not update the individual photo info (skip the rest)
        if (playing) return
        // Otherwise update photo information
        else {
            // Get info for the current photo
            val timestamp = photoEntry.timestamp
            val photosInProject: Int = photos.size
            Log.d(TAG, "photoNumber is $photoNumber")
            Log.d(TAG, "photosInProject is $photosInProject")
            // Get formatted strings
            val photoNumberString = getString(R.string.details_photo_number_out_of, photoNumber, photosInProject)
            val date = TimeUtils.getDateFromTimestamp(timestamp)
            val day = TimeUtils.getDayFromTimestamp(timestamp)
            val time = TimeUtils.getTimeFromTimestamp(timestamp)
            // Set the info
            binding?.detailsPhotoNumberTv?.text = photoNumberString
            binding?.detailsPhotoDateTv?.text = date
            binding?.detailsPhotoDayTv?.text = day
            binding?.detailsPhotoTimeTv?.text = time
        }
    }

    // Handles whether or not to display an indicator showing that an image is in the wrong orientation
    private fun handleOrientationIndicator(imagePath: String?) {
        // If no path hide indicator
        if (imagePath == null) {
            binding?.rotationIndicator?.startAnimation(stopAnimation)
            return
        }

        // Otherwise Detect configuration
        val imageIsLandscape = PhotoUtils.isLandscape(imagePath)
        val deviceIsLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // If landscape device and image (true == true) or portrait device and image (false == false)
        // Do not display the indicator
        if (imageIsLandscape == deviceIsLandscape) {
            binding?.rotationIndicator?.startAnimation(stopAnimation)
        }
        // Otherwise show the blinking indicator
        else {
            binding?.rotationIndicator?.startAnimation(blinkAnimation)
        }
    }

    // Loads the passed url
    private fun loadImage(imagePath: String?) {
        imageIsLoaded = false // this set to true after load image pair completes
        // Load the image to the fullscreen dialog if it is showing or to the detail cardview otherwise
        val f = if (imagePath == null) null else File(imagePath)
        loadImagePair(f, binding!!.detailCurrentImage, binding!!.detailNextImage)
    }

    // This function loads an image into a top view, then loads an image into the bottom view and hides the top view
    // This makes 'playing' the images look seamless
    private fun loadImagePair(f: File?, bottomImage: ImageView, topImage: ImageView) {
        // If error with file
        if (f == null) {
            Glide.with(this)
                    .load(R.color.darkImagePlaceHolder)
                    .into(bottomImage)
            startPostponedEnterTransition()
            binding?.errorImage?.visibility = VISIBLE
            if (playing) stopPlaying()
            return
        } else {
            binding?.errorImage?.visibility = INVISIBLE
        }

        // 1. The first glide call: First load the image into the next image view on top of the current
        Glide.with(this)
                .load(f)
                .error(R.drawable.ic_sentiment_very_dissatisfied_white_24dp)
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(e: GlideException?,
                                              model: Any,
                                              target: Target<Drawable?>, isFirstResource: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any, target: Target<Drawable?>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        // Show the just loaded image on the top
                        topImage.visibility = VISIBLE
                        // 2. The second glide call: Then load the image into the current image on the bottom
                        Glide.with(requireContext())
                                .load(f)
                                .error(R.drawable.ic_sentiment_very_dissatisfied_white_24dp)
                                .listener(object : RequestListener<Drawable?> {
                                    override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable?>, isFirstResource: Boolean): Boolean {
                                        startPostponedEnterTransition()
                                        return false
                                    }

                                    override fun onResourceReady(resource: Drawable?, model: Any, target: Target<Drawable?>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                                        // When complete hide the top image
                                        topImage.visibility = INVISIBLE
                                        // Record state
                                        imageIsLoaded = true
                                        // And begin the shared element transition if appropriate
                                        startPostponedEnterTransition()
                                        return false
                                    }
                                })
                                .into(bottomImage)
                        return false
                    }
                })
                .into(topImage)
    }

    // Loads the set of images in sequence
    private fun playSetOfImages() {
        // If already playing then stop
        if (playing) {
            stopPlaying()
            firebaseAnalytics!!.logEvent(getString(R.string.analytics_stop_time_lapse), null)
            return
        }
        // If not enough photos give user feedback
        if (photos.size <= 1) {
            Snackbar.make(binding!!.detailsCoordinatorLayout, R.string.add_more_photos,
                    Snackbar.LENGTH_LONG)
                    .show()
            return
        }

        // Handle play state
        playing = true
        currentPlayPosition = photos.indexOf(currentPhoto)
        // Handle UI
        fadeOutPhotoInformation()
        setFabStatePlaying(backward = false)
        // Override the play position to beginning if currently already at the end
        if (currentPlayPosition == photos.size - 1) {
            currentPlayPosition = 0
            detailViewModel.setPhoto(photos[0])
        }

        // Schedule the recursive sequence
        binding?.imageLoadingProgress?.progress = currentPlayPosition
        scheduleNextPhoto(currentPlayPosition) // Recursively loads the rest of set from beginning

        // Track play button interaction
        firebaseAnalytics!!.logEvent(getString(R.string.analytics_play_time_lapse), null)
    }

    // Loads the set of images backwards
    private fun rewindSetOfImages() {
        // If already playing then stop
        if (playing) {
            stopPlaying()
            firebaseAnalytics!!.logEvent(getString(R.string.analytics_stop_time_lapse), null)
            return
        }
        // If not enough photos give user feedback
        if (photos.size <= 1) {
            Snackbar.make(binding!!.detailsCoordinatorLayout, R.string.add_more_photos,
                    Snackbar.LENGTH_LONG)
                    .show()
            return
        }

        // Handle play state
        playing = true
        currentPlayPosition = photos.indexOf(currentPhoto)
        // Handle UI
        fadeOutPhotoInformation()
        setFabStatePlaying(true)

        // Rewind from the end if currently at the beginning
        if (currentPlayPosition == 0) {
            currentPlayPosition = photos.size - 1
            detailViewModel.setPhoto(photos[photos.size - 1])
        }

        // Schedule the recursive sequence
        binding?.imageLoadingProgress?.progress = currentPlayPosition
        scheduleRewindPhoto(currentPlayPosition) // Recursively loads the rest of set from beginning
    }


    // Sets the two play buttons to red with a stop icon
    private fun setFabStatePlaying(backward: Boolean) {
        binding?.playAsVideoFab?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorRedAccent))
        binding?.playAsVideoFab?.rippleColor = ContextCompat.getColor(requireContext(), R.color.colorRedAccent)
        binding?.playAsVideoFab?.setImageResource(R.drawable.ic_stop_white_24dp)

        if (backward)
            binding?.playBackwardsFab?.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorGreen))
    }

    // Sets the two play buttons to green with a play icon
    private fun setFabStateStopped() {
        binding?.playAsVideoFab?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorGreen))
        binding?.playAsVideoFab?.rippleColor = ContextCompat.getColor(requireContext(), R.color.colorGreen)
        binding?.playAsVideoFab?.setImageResource(R.drawable.ic_play_arrow_white_24dp)

        binding?.playBackwardsFab?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorSubtleAccent))

    }

    // Resets the UI & handles state after playing
    private fun stopPlaying() {
        playJob?.cancel()
        playing = false
        binding?.imageLoadingProgress?.progress = currentPlayPosition
        setFabStateStopped()
        loadUi(currentPhoto)
        fadeInPhotoInformation()
    }

    // Sets a coroutine to load the next photo every 50 ms, or whatever has been chosen from the shared preferences
    private fun scheduleNextPhoto(position: Int) {
        Log.d("DetailsFragment", "schedule loading position $position")
        if (position < 0 || position >= photos.size) {
            stopPlaying()
            return
        }
        currentPlayPosition = position
        playJob = detailViewModel.viewModelScope.launch {
            delay(playbackInterval)
            // If image is loaded load the next photo
            if (imageIsLoaded) {
                detailViewModel.nextPhoto()
                binding?.imageLoadingProgress?.progress = position + 1
                scheduleNextPhoto(position + 1)
            }
            // Otherwise check again after the interval
            else {
                scheduleNextPhoto(position)
            }
        }
    }

    // Sets a coroutine to load the previous photo every 50 ms, or whatever has been chosen from the shared preferences
    private fun scheduleRewindPhoto(position: Int) {
        Log.d("DetailsFragment", "schedule loading position $position")
        if (position < 0 || position >= photos.size) {
            stopPlaying()
            return
        }
        currentPlayPosition = position
        playJob = detailViewModel.viewModelScope.launch {
            delay(playbackInterval)
            // If image is loaded load the next photo
            if (imageIsLoaded) {
                detailViewModel.previousPhoto()
                binding?.imageLoadingProgress?.progress = position - 1
                scheduleRewindPhoto(position - 1)
            }
            // Otherwise check again after the interval
            else {
                scheduleRewindPhoto(position)
            }
        }
    }


    /**
     * UI binding
     */
    // Bind the ui to observables
    private fun setupViewModel() {
        // Observe the project for name and schedule changes
        detailViewModel.projectView.observe(viewLifecycleOwner, Observer { projectView: ProjectView? ->
            if (projectView == null) return@Observer
            currentProjectView = projectView
            detailAdapter?.setProject(currentProjectView)
            // This updates the project information card, project info dialog,
            // schedule layout over the image and the schedule dialog

            // Set the ui for the project information layout cardview
            // 1. Set the ID
            binding?.projectInformationLayout?.detailsProjectId?.text = currentProjectView.project_id.toString()
            // 2. Set the name, handle appropriately if no name specified
            val name = currentProjectView.project_name
            // Style for no name
            if (name == null || name.isEmpty()) {
                binding?.projectInformationLayout
                        ?.detailsProjectNameTextView
                        ?.text = getString(R.string.unnamed)
                binding?.projectInformationLayout
                        ?.detailsProjectNameTextView
                        ?.setTypeface(binding!!.projectInformationLayout.detailsProjectNameTextView.typeface, Typeface.ITALIC)
                binding?.projectInformationLayout
                        ?.detailsProjectNameTextView
                        ?.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey))
            }
            // Otherwise style for a set name
            else {
                binding?.projectInformationLayout
                        ?.detailsProjectNameTextView
                        ?.text = name
                binding?.projectInformationLayout
                        ?.detailsProjectNameTextView
                        ?.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                binding?.projectInformationLayout
                        ?.detailsProjectNameTextView
                        ?.setTypeface(binding!!.projectInformationLayout.detailsProjectNameTextView.typeface, Typeface.BOLD)
            }

            // 3. Set the schedule information
            // If there isn't a schedule set the color of the fab to white and hide the layout
            if (currentProjectView.interval_days == 0) {
                binding?.projectScheduleFab?.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
                binding?.detailScheduleLayout?.scheduleLayout?.visibility = INVISIBLE
            }
            // Otherwise set the color of the schedule fab to an accent and show the schedule layout
            else {
                binding?.projectScheduleFab?.backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorYellow))
                // Show the interval of the schedule
                binding?.detailScheduleLayout?.scheduleIndicatorIntervalTv?.text =
                        currentProjectView.interval_days.toString()

                val daysUntilDue = daysUntilDue(currentProjectView)
                // Show how many days until project is due
                binding?.detailScheduleLayout?.scheduleDaysUntilDueTv?.text =
                        daysUntilDue.toString()

                // Style schedule layout depending upon whether or not the project is due
                if (daysUntilDue <= 0) {
                    binding?.detailScheduleLayout?.scheduleDaysUntilDueTv
                            ?.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorSubtleRedAccent))
                    binding?.detailScheduleLayout?.scheduleIndicatorIntervalTv
                            ?.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_schedule_indicator_due_24dp, 0, 0, 0)
                } else {
                    binding?.detailScheduleLayout?.scheduleDaysUntilDueTv
                            ?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    binding?.detailScheduleLayout?.scheduleIndicatorIntervalTv
                            ?.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_schedule_indicator_pending_24dp, 0, 0, 0)
                }

                // Show the layout
                binding?.detailScheduleLayout?.scheduleLayout?.visibility = VISIBLE
            }
            // Also update the fields in the info dialog
            infoDialog?.setInfoDialog(projectView)
            // And update the fields in the schedule dialog
            scheduleDialog?.setScheduleInformation(projectView)
        })

        // Observe the list of photos
        // This keeps track of the last photo in the set and ensures that photo is the cover photo
        // This also keeps track of the list of photos to pass to the details recycler view
        // Lastly this updates UI showing the date range for photos and the progress bar max
        detailViewModel.photos.observe(viewLifecycleOwner, Observer { photoEntries: List<PhotoEntry> ->
            // Entries should only be possibly empty if deleting the project
            if (photoEntries.isEmpty()) return@Observer

            // Update the recycler view
            photos = photoEntries
            detailAdapter?.submitList(photoEntries)

            // Bind cover photo to the end of the list always
            var lastPhotoEntry = photoEntries.last()
            detailViewModel.setLastPhotoByEntry(externalFilesDir, currentProjectView, lastPhotoEntry)
            detailViewModel.setCoverPhoto(lastPhotoEntry)
            // Update the UI based upon the range of photo dates
            val firstTimestamp = photoEntries[0].timestamp
            val firstProjectDateString = TimeUtils.getShortDateFromTimestamp(firstTimestamp)
            val lastTimestamp = photoEntries.last().timestamp
            val lastProjectDateString = TimeUtils.getShortDateFromTimestamp(lastTimestamp)
            binding?.projectInformationLayout
                    ?.detailsProjectTimespanTextview?.text = getString(R.string.timespan, firstProjectDateString, lastProjectDateString)
            binding?.imageLoadingProgress?.max = photoEntries.size - 1


            // Now figure out whether or not a photo was added or deleted to determine the new current photo
            val newMaxIndex = photoEntries.size - 1
            val oldMaxIndex = detailViewModel.maxIndex
            val previousSize = oldMaxIndex + 1
            val currentSize = photoEntries.size
            val added: Boolean = if (oldMaxIndex == Integer.MAX_VALUE) false else (currentSize > previousSize)

            detailViewModel.photoIndex = newMaxIndex

            // TODO: convert these to appropriate boolean mutable live data fields to observe
            // If added set to the last photo
            if (added) {
                // Update gif if exists and preference is set to auto-update
                val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
                val updateGifs = pref.getBoolean(getString(R.string.key_gif_auto_convert), true)
                if (updateGifs) {
                    val gif = GifUtils.getGifForProject(externalFilesDir, getProjectEntryFromProjectView(currentProjectView))
                    if (gif != null) {
                        GifUtils.scheduleGifWorker(requireContext())
                    }
                }
            }
            // If deleted, set current photo
            else {
                lastPhotoEntry = photoEntries[detailViewModel.photoIndex]
            }

            // Restore the play position
            currentPlayPosition = photos.indexOf(lastPhotoEntry)
            // Load the current photo
            detailViewModel.currentPhoto.value = lastPhotoEntry
            // Make sure to save the position of the max index in the view model
            detailViewModel.maxIndex = newMaxIndex

            // Processes photo urls for the fullscreen fragment
            lifecycleScope.launch {
                val list = arrayListOf<String>()
                for (photo in photoEntries) {
                    val photoUrl = ProjectUtils.getProjectPhotoUrl(
                            externalFilesDir,
                            getProjectEntryFromProjectView(currentProjectView),
                            photo.timestamp)
                    if (photoUrl == null) {
                        Log.d(TAG, "error loading timestamp ${photo.timestamp}")
                        continue
                    }
                    list.add(photoUrl)
                }
                photoUrls = list.map { it }.toTypedArray()
            }
        })

        // Observes the currently selected photo
        // This loads the image and timestamp information based on the current photo
        detailViewModel.currentPhoto.observe(viewLifecycleOwner, Observer { currentPhoto: PhotoEntry? ->
            if (currentPhoto != null) {
                this.currentPhoto = currentPhoto
                loadUi(currentPhoto)
            }
        })

        // Observe the tags for the project
        // This updates the tags in the dialog, sets the tags in the project info card layout,
        // Lastly this writes the sorted tags to the tags text file in the meta directory
        detailViewModel.projectTags.observe(viewLifecycleOwner, Observer { projectTagEntries: List<ProjectTagEntry> ->
            tagJob = detailViewModel.viewModelScope.launch {
                // 1. Get the Tag Entries from the Project Tag Entries sorted
                projectTags = detailViewModel.getTags(projectTagEntries)
                        .sortedBy { it.text.toLowerCase(Locale.getDefault()) }

                // 2. Set the tags for the project tag dialog
                tagDialog?.setProjectTagDialog(allTags, projectTags)

                // 3. Get the string representing the tags and set the info dialog
                val tagsText = getTagsText(projectTags)
                infoDialog?.setInfoTags(tagsText)

                // 4. Use the string to set the tags in the project info card view unless there are none
                if (projectTags.isEmpty()) {
                    binding?.projectInformationLayout?.detailsProjectTagsTextview?.visibility = GONE
                } else {
                    binding?.projectInformationLayout?.detailsProjectTagsTextview?.text = tagsText
                    binding?.projectInformationLayout?.detailsProjectTagsTextview?.visibility = VISIBLE
                }

                // 5. Lastly write the list of tags to the text file (overwriting any previously)
                FileUtils.writeProjectTagsFile(externalFilesDir, currentProjectView.project_id, projectTags)
            }
        })

        // Observe all the tags in the database
        // This is used to set up the list of all the tags in the project tag dialog
        // So that the user may simply click on a tag to add them to a project
        detailViewModel.tags.observe(viewLifecycleOwner, Observer { tagEntries: List<TagEntry> ->
            allTags = tagEntries.sortedBy { it.text.toLowerCase(Locale.getDefault()) }
            tagDialog?.setProjectTagDialog(allTags, projectTags)
        })
    }

    // Gets the text from a list of tags handling empty case
    private fun getTagsText(tags: List<TagEntry>): String {
        var tagsText = ""
        for (tag in tags) {
            // Concatenate a string for non-interactive output
            tagsText = tagsText.plus("#${tag.text}  ")
        }
        return if (tagsText.isEmpty()) requireContext().getString(R.string.none)
        else tagsText
    }

    /**
     * User input
     */

    // Sets the current photo from clicking on the bottom recycler view
    override fun onClick(clickedPhoto: PhotoEntry) {
        detailViewModel.setPhoto(clickedPhoto)
    }

    /**
     * Dialog verifications
     */
    // Ensures the user wishes to delete the current photo
    private fun verifyPhotoDeletion() {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_photo)
                .setMessage(R.string.verify_delete_photo)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { _, _: Int ->
                    detailViewModel.deleteCurrentPhoto(externalFilesDir)
                    firebaseAnalytics?.logEvent(getString(R.string.analytics_delete_photo), null)
                }
                .setNegativeButton(android.R.string.no, null).show()
    }

    // If the photo is the last in the project, directs the user to deleting the project instead
    private fun verifyLastPhotoDeletion() {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_photo)
                .setMessage(R.string.verify_delete_last_photo)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { _, _: Int ->
                    verifyProjectDeletion()
                }
                .setNegativeButton(android.R.string.no, null).show()
    }

    // Deletes the current project after user verification
    private fun verifyProjectDeletion() {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_project)
                .setMessage(R.string.verify_delete_project)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { _, _: Int ->
                    doubleVerifyProjectDeletion()
                }
                .setNegativeButton(android.R.string.no, null).show()
    }

    // Provides an additional layer of verification for project deletion
    private fun doubleVerifyProjectDeletion() {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_project)
                .setMessage(R.string.double_verify_project_deletion)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { _, _: Int ->
                    detailViewModel.deleteCurrentProject(externalFilesDir, currentProjectView)
                    // If current project had a schedule remove the notification and update widgets
                    if (currentProjectView.interval_days != 0) {
                        NotificationUtils.scheduleNotificationWorker(requireContext())
                        UpdateWidgetService.startActionUpdateWidgets(requireContext())
                    }
                    sharedElementReturnTransition = null

                    // Log project deletion to analytics
                    firebaseAnalytics?.logEvent(getString(R.string.analytics_delete_project), null)

                    findNavController().popBackStack()
                }
                .setNegativeButton(android.R.string.no, null).show()
    }

    companion object {
        private val TAG = DetailFragment::class.java.simpleName
    }
}