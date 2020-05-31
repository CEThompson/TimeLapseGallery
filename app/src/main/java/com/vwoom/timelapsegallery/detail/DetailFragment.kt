package com.vwoom.timelapsegallery.detail

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.transition.Transition
import android.transition.TransitionInflater
import android.util.Log
import android.view.*
import android.view.View.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
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
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.databinding.FragmentDetailBinding
import com.vwoom.timelapsegallery.notification.NotificationUtils
import com.vwoom.timelapsegallery.utils.*
import com.vwoom.timelapsegallery.utils.ProjectUtils.getProjectEntryFromProjectView
import com.vwoom.timelapsegallery.utils.TimeUtils.daysUntilDue
import com.vwoom.timelapsegallery.widget.UpdateWidgetService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import kotlin.properties.Delegates

// TODO: (update 1.2) use NDK to implement converting photo sets to .gif and .mp4/.mov etc
// TODO: (update 1.2) implement pinch zoom on fullscreen image
class DetailFragment : Fragment(), DetailAdapter.DetailAdapterOnClickHandler {

    private val args: DetailFragmentArgs by navArgs()
    private val detailViewModel: DetailViewModel by viewModels {
        InjectorUtils.provideDetailsViewModelFactory(requireActivity(), args.clickedProjectView)
    }
    private lateinit var mExternalFilesDir: File
    private var binding: FragmentDetailBinding? = null
    private var toolbar: Toolbar? = null

    // Photo and project Information
    private lateinit var mCurrentProjectView: ProjectView
    private var mPhotos: List<PhotoEntry> = emptyList()
    private var mProjectTags: List<TagEntry> = emptyList()
    private var mAllTags: List<TagEntry> = emptyList()
    private lateinit var mCurrentPhoto: PhotoEntry

    private var mCurrentPlayPosition by Delegates.notNull<Int>()
    private var mDetailAdapter: DetailAdapter? = null

    // Dialogs
    private var mTagDialog: Dialog? = null
    private var mInfoDialog: Dialog? = null
    private var mScheduleDialog: Dialog? = null

    // For schedule selection
    private var mNoneSelector: CardView? = null
    private val mDaySelectionViews: ArrayList<CardView> = arrayListOf()
    private val mWeekSelectionViews: ArrayList<CardView> = arrayListOf()

    // For playing time lapse
    private var mPlaying = false
    private var mImageIsLoaded = false
    private var mPlaybackInterval by Delegates.notNull<Long>()

    // Jobs
    private var playJob: Job? = null
    private var tagJob: Job? = null

    // For fullscreen fragment
    private var photoUrls: Array<String> = arrayOf()

    // Analytics
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

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
        mCurrentProjectView = args.clickedProjectView
        mPlaybackInterval = getString(R.string.playback_interval_default).toLong()

        try {
            mExternalFilesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        } catch (exc: KotlinNullPointerException) {
            // TODO (update 1.2): Investigate potential failurs with external files.
            Log.e(TAG, "Couldn't get external files directory.")
            Toast.makeText(requireContext(), "Fatal Error: Could not load external files!", Toast.LENGTH_LONG).show()
        }

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
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
        mPlaybackInterval = playbackIntervalSharedPref?.toLong() ?: 50

        // Set up toolbar
        setHasOptionsMenu(true)
        toolbar = binding?.detailsFragmentToolbar
        (activity as TimeLapseGalleryActivity).setSupportActionBar(toolbar)
        toolbar?.title = getString(R.string.project_details)
        (activity as TimeLapseGalleryActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set up adapter and recycler view
        mDetailAdapter = DetailAdapter(this, mExternalFilesDir)
        val linearLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding?.detailsRecyclerview?.layoutManager = linearLayoutManager
        binding?.detailsRecyclerview?.adapter = mDetailAdapter

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
                        .actionDetailsFragmentToCamera2Fragment(cameraId, detailViewModel.lastPhoto, mCurrentProjectView)
                findNavController().navigate(action)
            }
        }
        binding?.playAsVideoFab?.setOnClickListener { playSetOfImages() }
        binding?.projectScheduleFab?.setOnClickListener {
            if (mScheduleDialog == null) initializeScheduleDialog()
            mScheduleDialog?.show()
            detailViewModel.scheduleDialogShowing = true
        }
        binding?.projectTagFab?.setOnClickListener {
            if (mTagDialog == null) initializeTagDialog()
            mTagDialog?.show()
            detailViewModel.tagDialogShowing = true
        }
        binding?.projectInformationLayout?.projectInformationCardview?.setOnClickListener {
            if (mInfoDialog == null) initializeInfoDialog()
            mInfoDialog?.show()
            detailViewModel.infoDialogShowing = true
        }
        binding?.fullscreenFab?.setOnClickListener {
            exitTransition = TransitionInflater.from(context).inflateTransition(R.transition.details_to_fullscreen_transition)
            val action = DetailFragmentDirections.actionDetailsFragmentToFullscreenFragment(detailViewModel.photoIndex, photoUrls)
            val extras = FragmentNavigatorExtras(
                    binding!!.fullscreenFab as View to "fs_exit_fab",
                    binding!!.playAsVideoFab as View to "fs_play_fab")
            findNavController().navigate(action, extras)
        }
        // Set a swipe listener for the image
        val onRightSwipe = { if (!mPlaying) detailViewModel.previousPhoto() }
        val onLeftSwipe = { if (!mPlaying) detailViewModel.nextPhoto() }
        val swipeListener = OnSwipeTouchListener(
                requireContext(),
                onRightSwipe,
                onLeftSwipe)
        @Suppress("ClickableViewAccessibility")
        binding?.detailCurrentImage?.setOnTouchListener(swipeListener)

        // Set the transition tags
        val transitionName = "${mCurrentProjectView.project_id}"
        binding?.detailCurrentImage?.transitionName = transitionName
        binding?.detailsCardContainer?.transitionName = "${transitionName}card"
        binding?.detailsGradientOverlay?.transitionName = "${transitionName}bottomGradient"
        binding?.detailScheduleLayout?.galleryGradientTopDown?.transitionName = "${transitionName}topGradient"
        binding?.detailScheduleLayout?.scheduleDaysUntilDueTv?.transitionName = "${transitionName}due"
        binding?.detailScheduleLayout?.scheduleIndicatorIntervalTv?.transitionName = "${transitionName}interval"

        // Finally set up the observables
        setupViewModel()

        return binding?.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.detail_fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.share -> {
                val photoUrl = ProjectUtils.getProjectPhotoUrl(mExternalFilesDir,
                        getProjectEntryFromProjectView(mCurrentProjectView),
                        mCurrentPhoto.timestamp)
                if (photoUrl != null)
                {
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
            R.id.delete_photo -> {
                if (mPhotos.size == 1) {
                    verifyLastPhotoDeletion()
                } else {
                    verifyPhotoDeletion()
                }
                true
            }
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
            if (mInfoDialog == null) initializeInfoDialog()
            mInfoDialog?.show()
        }
        if (detailViewModel.scheduleDialogShowing) {
            if (mScheduleDialog == null) initializeScheduleDialog()
            mScheduleDialog?.show()
        }
        if (detailViewModel.tagDialogShowing) {
            if (mTagDialog == null) initializeTagDialog()
            mTagDialog?.show()
        }
    }

    override fun onPause() {
        super.onPause()
        // Cancel any jobs
        if (mPlaying) stopPlaying()
        tagJob?.cancel()

        // Dismiss any dialogs
        mInfoDialog?.dismiss()
        mTagDialog?.dismiss()
        mScheduleDialog?.dismiss()
    }

    override fun onStop() {
        super.onStop()
        binding?.detailsFragmentToolbar?.setNavigationOnClickListener(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        mTagDialog = null
        mScheduleDialog = null
        mInfoDialog = null
        mDetailAdapter = null
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
            mDetailAdapter?.setCurrentPhoto(photoEntry)

        // Get the image path, handle orientation indicator and load the image
        val imagePath: String? = ProjectUtils.getProjectPhotoUrl(
                mExternalFilesDir,
                getProjectEntryFromProjectView(mCurrentProjectView),
                photoEntry.timestamp)


        if (!mPlaying) handleOrientationIndicator(imagePath)
        loadImage(imagePath)

        // Update position of progress view and thumbnail
        val position = mPhotos.indexOf(photoEntry)
        val photoNumber = position + 1
        if (!mPlaying)
            binding?.detailsRecyclerview?.scrollToPosition(position)
        binding?.imageLoadingProgress?.progress = position

        // If playing do not update the individual photo info (skip the rest)
        if (mPlaying) return
        // Otherwise update photo information
        else {
            // Get info for the current photo
            val timestamp = photoEntry.timestamp
            val photosInProject: Int = mPhotos.size
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
        if (imagePath == null){
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
        mImageIsLoaded = false // this set to true after load image pair completes
        // Load the image to the fullscreen dialog if it is showing or to the detail cardview otherwise
        val f = if (imagePath==null) null else File(imagePath)
        loadImagePair(f, binding!!.detailCurrentImage, binding!!.detailNextImage)
    }

    // TODO: (update 1.2) re-evaluate and speed up image loading
    // This function loads an image into a top view, then loads an image into the bottom view and hides the top view
    // This makes 'playing' the images look seamless
    private fun loadImagePair(f: File?, bottomImage: ImageView, topImage: ImageView) {
        // If error with file
        if (f==null){
            Glide.with(this)
                    .load(R.color.darkImagePlaceHolder)
                    .into(bottomImage)
            startPostponedEnterTransition()
            binding?.errorImage?.visibility = VISIBLE
            if (mPlaying) stopPlaying()
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
                                        mImageIsLoaded = true
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
        if (mPlaying) {
            stopPlaying()
            mFirebaseAnalytics!!.logEvent(getString(R.string.analytics_stop_time_lapse), null)
            return
        }
        // If not enough photos give user feedback
        if (mPhotos.size <= 1) {
            Snackbar.make(binding!!.detailsCoordinatorLayout, R.string.add_more_photos,
                    Snackbar.LENGTH_LONG)
                    .show()
            return
        }

        // Handle play state
        mPlaying = true
        mCurrentPlayPosition = mPhotos.indexOf(mCurrentPhoto)
        // Handle UI
        fadeOutPhotoInformation()
        setFabStatePlaying()
        // Override the play position to beginning if currently already at the end
        if (mCurrentPlayPosition == mPhotos.size - 1) {
            mCurrentPlayPosition = 0
            detailViewModel.setPhoto(mPhotos[0])
        }

        // Schedule the recursive sequence
        binding?.imageLoadingProgress?.progress = mCurrentPlayPosition
        scheduleLoadPhoto(mCurrentPlayPosition) // Recursively loads the rest of set from beginning

        // Track play button interaction
        mFirebaseAnalytics!!.logEvent(getString(R.string.analytics_play_time_lapse), null)
    }

    // Sets the two play buttons to red with a stop icon
    private fun setFabStatePlaying() {
        binding?.playAsVideoFab?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorRedAccent))
        binding?.playAsVideoFab?.rippleColor = ContextCompat.getColor(requireContext(), R.color.colorRedAccent)
        binding?.playAsVideoFab?.setImageResource(R.drawable.ic_stop_white_24dp)
    }

    // Sets the two play buttons to green with a play icon
    private fun setFabStateStopped() {
        binding?.playAsVideoFab?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorGreen))
        binding?.playAsVideoFab?.rippleColor = ContextCompat.getColor(requireContext(), R.color.colorGreen)
        binding?.playAsVideoFab?.setImageResource(R.drawable.ic_play_arrow_white_24dp)
    }

    // Resets the UI & handles state after playing
    private fun stopPlaying() {
        playJob?.cancel()
        mPlaying = false
        binding?.imageLoadingProgress?.progress = mCurrentPlayPosition
        setFabStateStopped()
        loadUi(mCurrentPhoto)
        fadeInPhotoInformation()
    }

    // Sets a coroutine to load the next photo every 50 ms, or whatever has been chosen from the shared preferences
    private fun scheduleLoadPhoto(position: Int) {
        Log.d("DetailsFragment", "schedule loading position $position")
        if (position < 0 || position >= mPhotos.size) {
            stopPlaying()
            return
        }
        mCurrentPlayPosition = position
        playJob = detailViewModel.viewModelScope.launch {
            delay(mPlaybackInterval)
            // If image is loaded load the next photo
            if (mImageIsLoaded) {
                detailViewModel.nextPhoto()
                binding?.imageLoadingProgress?.progress = position + 1
                scheduleLoadPhoto(position + 1)
            }
            // Otherwise check again after the interval
            else {
                scheduleLoadPhoto(position)
            }
        }
    }

    /**
     * Dialog Initialization Methods
     */
    private fun initializeInfoDialog() {
        mInfoDialog = Dialog(requireContext())
        mInfoDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        mInfoDialog?.setContentView(R.layout.dialog_project_information)
        mInfoDialog?.setOnCancelListener { detailViewModel.infoDialogShowing = false }
        // Get Views
        val editNameButton = mInfoDialog?.findViewById<FloatingActionButton>(R.id.edit_project_name_button)
        // Set fab colors
        editNameButton?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
        editNameButton?.setOnClickListener { verifyEditName() }
        val tagsTextView = mInfoDialog?.findViewById<TextView>(R.id.dialog_information_tags)
        tagsTextView?.setOnClickListener {
            if (mTagDialog == null) initializeTagDialog()
            mTagDialog?.show()
            detailViewModel.tagDialogShowing = true
        }
        val infoOkTextView = mInfoDialog?.findViewById<TextView>(R.id.dialog_info_dismiss)
        infoOkTextView?.setOnClickListener {
            mInfoDialog?.dismiss()
            detailViewModel.infoDialogShowing = false
        }
        val exitFab = mInfoDialog?.findViewById<FloatingActionButton>(R.id.project_info_exit_fab)
        exitFab?.setOnClickListener {
            mInfoDialog?.dismiss()
            detailViewModel.infoDialogShowing = false
        }
        setInfoDialog()
        setInfoTags()
    }

    private fun initializeTagDialog() {
        mTagDialog = Dialog(requireContext())
        mTagDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        mTagDialog?.setContentView(R.layout.dialog_project_tag)
        mTagDialog?.setOnCancelListener { detailViewModel.tagDialogShowing = false }
        // set add tag fab
        val editText = mTagDialog?.findViewById<EditText>(R.id.add_tag_dialog_edit_text)
        val addTagFab = mTagDialog?.findViewById<FloatingActionButton>(R.id.add_tag_fab)
        addTagFab?.setOnClickListener {
            val tagText = editText?.text.toString().trim()
            when {
                tagText.isEmpty() -> {
                    return@setOnClickListener
                }
                tagText.contains(' ') -> showTagValidationAlertDialog(getString(R.string.invalid_tag_one_word))
                tagText.length > 14 -> showTagValidationAlertDialog(getString(R.string.invalid_tag_length))
                else -> {
                    detailViewModel.addTag(tagText, mCurrentProjectView)
                    mFirebaseAnalytics?.logEvent(getString(R.string.analytics_add_tag), null)
                    editText?.text?.clear()
                }
            }
        }
        val dismissView = mTagDialog?.findViewById<TextView>(R.id.dialog_project_tag_dismiss)
        dismissView?.setOnClickListener {
            mTagDialog?.dismiss()
            detailViewModel.tagDialogShowing = false
        }
        val exitFab = mTagDialog?.findViewById<FloatingActionButton>(R.id.project_tag_exit_fab)
        exitFab?.setOnClickListener {
            mTagDialog?.dismiss()
            detailViewModel.tagDialogShowing = false
        }
        setProjectTagDialog()
    }

    private fun initializeScheduleDialog() {
        if (mScheduleDialog != null) return

        mScheduleDialog = Dialog(requireContext())
        mScheduleDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        mScheduleDialog?.setContentView(R.layout.dialog_schedule)
        mScheduleDialog?.setOnCancelListener { detailViewModel.scheduleDialogShowing = false }

        // Set up selector for no schedule
        val noneLayout = mScheduleDialog?.findViewById<FrameLayout>(R.id.dialog_schedule_none_layout)
        noneLayout?.removeAllViews()
        mNoneSelector = layoutInflater.inflate(R.layout.dialog_schedule_selector, noneLayout, false) as CardView
        val noneTv = mNoneSelector?.findViewById<TextView>(R.id.selector_child_tv)
        noneTv?.text = getString(R.string.unscheduled)
        mNoneSelector?.contentDescription = getString(R.string.content_description_schedule_selector_none)
        noneLayout?.addView(mNoneSelector)
        mNoneSelector?.setOnClickListener {
            detailViewModel.setSchedule(mExternalFilesDir, mCurrentProjectView, 0)
            mFirebaseAnalytics?.logEvent(getString(R.string.analytics_delete_schedule), null)
        }

        // Set up days selection
        val daysLayout = mScheduleDialog?.findViewById<FlexboxLayout>(R.id.dialog_schedule_days_selection_layout)
        daysLayout?.removeAllViews()
        for (dayInterval in 1..6) {
            // a selection layout for each interval
            //val textView = TextView(requireContext())
            val selectionLayout: CardView = layoutInflater.inflate(R.layout.dialog_schedule_selector, daysLayout, false) as CardView
            val textView = selectionLayout.findViewById<TextView>(R.id.selector_child_tv)
            textView.text = dayInterval.toString()
            selectionLayout.setOnClickListener {
                detailViewModel.setSchedule(mExternalFilesDir, mCurrentProjectView, dayInterval)
                mFirebaseAnalytics?.logEvent(getString(R.string.analytics_add_schedule_days), null)
            }
            daysLayout?.addView(selectionLayout)
            selectionLayout.contentDescription = getString(R.string.content_description_schedule_selector_days, dayInterval)
            mDaySelectionViews.add(selectionLayout)
        }

        // Set up weeks selection
        val weeksLayout = mScheduleDialog?.findViewById<FlexboxLayout>(R.id.dialog_schedule_weeks_selection_layout)
        weeksLayout?.removeAllViews()
        for (weekInterval in 1..4) {
            val selectionLayout: CardView = layoutInflater.inflate(R.layout.dialog_schedule_selector, daysLayout, false) as CardView
            val textView = selectionLayout.findViewById<TextView>(R.id.selector_child_tv)
            textView.text = weekInterval.toString()
            selectionLayout.setOnClickListener {
                detailViewModel.setSchedule(mExternalFilesDir, mCurrentProjectView, weekInterval * 7)
                mFirebaseAnalytics?.logEvent(getString(R.string.analytics_add_schedule_weeks), null)
            }
            weeksLayout?.addView(selectionLayout)
            selectionLayout.contentDescription = getString(R.string.content_description_schedule_selector_weeks, weekInterval)
            mWeekSelectionViews.add(selectionLayout)
        }

        // Set up custom input
        mScheduleDialog?.findViewById<EditText>(R.id.custom_schedule_input)?.addTextChangedListener {
            val interval = it.toString()
            if (interval.isNotEmpty()) {
                detailViewModel.setSchedule(mExternalFilesDir, mCurrentProjectView, interval.toInt())
                mFirebaseAnalytics?.logEvent(getString(R.string.analytics_add_schedule_custom), null)
            } else {
                detailViewModel.setSchedule(mExternalFilesDir, mCurrentProjectView, 0)
                mFirebaseAnalytics?.logEvent(getString(R.string.analytics_delete_schedule), null)
            }
        }

        // Set up dismissing the dialog
        val okTextView = mScheduleDialog?.findViewById<TextView>(R.id.dialog_schedule_dismiss)
        okTextView?.setOnClickListener {
            mScheduleDialog?.dismiss()
            detailViewModel.scheduleDialogShowing = false
        }
        val exitFab = mScheduleDialog?.findViewById<FloatingActionButton>(R.id.schedule_dialog_exit_fab)
        exitFab?.setOnClickListener {
            mScheduleDialog?.dismiss()
            detailViewModel.scheduleDialogShowing = false
        }

        // Update UI to current schedule
        setScheduleInformation()
    }

    /**
     * UI binding
     */
    // Bind the ui to observables
    private fun setupViewModel() {
        // Observe the project for name and schedule changes
        detailViewModel.projectView.observe(viewLifecycleOwner, Observer { projectView: ProjectView? ->
            if (projectView==null) return@Observer
            mCurrentProjectView = projectView
            mDetailAdapter?.setProject(mCurrentProjectView)
            // This updates the project information card, project info dialog,
            // schedule layout over the image and the schedule dialog

            // Set the ui for the project information layout cardview
            // 1. Set the ID
            binding?.projectInformationLayout?.detailsProjectId?.text = mCurrentProjectView.project_id.toString()
            // 2. Set the name, handle appropriately if no name specified
            val name = mCurrentProjectView.project_name
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
            if (mCurrentProjectView.interval_days == 0) {
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
                        mCurrentProjectView.interval_days.toString()

                val daysUntilDue = daysUntilDue(mCurrentProjectView)
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
            setInfoDialog()
            // And update the fields in the schedule dialog
            setScheduleInformation()

        })

        // Observe the list of photos
        // This keeps track of the last photo in the set and ensures that photo is the cover photo
        // This also keeps track of the list of photos to pass to the details recycler view
        // Lastly this updates UI showing the date range for photos and the progress bar max
        detailViewModel.photos.observe(viewLifecycleOwner, Observer { photoEntries: List<PhotoEntry> ->
            // Update the recycler view
            mPhotos = photoEntries
            mDetailAdapter?.submitList(photoEntries)

            // Bind cover photo to the end of the list always
            val lastPhotoEntry = photoEntries.last()
            detailViewModel.setLastPhotoByEntry(mExternalFilesDir, mCurrentProjectView, lastPhotoEntry)
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
            val added: Boolean = (currentSize > previousSize)

            lateinit var newCurrentPhoto: PhotoEntry
            // If added set to the last photo
            if (added) {
                newCurrentPhoto = lastPhotoEntry
                detailViewModel.photoIndex = newMaxIndex
            }
            // If deleted, set current photo
            else {
                newCurrentPhoto = photoEntries[detailViewModel.photoIndex]
            }

            // Restore the play position
            mCurrentPlayPosition = mPhotos.indexOf(newCurrentPhoto)
            // Load the current photo
            detailViewModel.currentPhoto.value = newCurrentPhoto
            // Make sure to save the position of the max index in the view model
            detailViewModel.maxIndex = newMaxIndex

            // Processes photo urls for the fullscreen fragment
            lifecycleScope.launch {
                val list = arrayListOf<String>()
                for (photo in photoEntries) {
                    val photoUrl = ProjectUtils.getProjectPhotoUrl(
                            mExternalFilesDir,
                            getProjectEntryFromProjectView(mCurrentProjectView),
                            photo.timestamp)
                    if (photoUrl==null){
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
                mCurrentPhoto = currentPhoto
                loadUi(currentPhoto)
            }
        })

        // Observe the tags for the project
        // This updates the tags in the dialog, sets the tags in the project info card layout,
        // Lastly this writes the sorted tags to the tags text file in the meta directory
        detailViewModel.projectTags.observe(viewLifecycleOwner, Observer { projectTagEntries: List<ProjectTagEntry> ->
            tagJob = detailViewModel.viewModelScope.launch {
                // 1. Get the Tag Entries from the Project Tag Entries sorted
                mProjectTags = detailViewModel.getTags(projectTagEntries)
                        .sortedBy { it.text.toLowerCase(Locale.getDefault()) }

                // 2. Set the tags for the project tag dialog
                setProjectTagDialog()

                // 3. Set the tags in the info dialog and get the string representing the tags
                val tagsText = setInfoTags()
                // 4. Use the string to set the tags in the project info card view unless there are none
                if (mProjectTags.isEmpty()) {
                    binding?.projectInformationLayout?.detailsProjectTagsTextview?.visibility = GONE
                } else {
                    binding?.projectInformationLayout?.detailsProjectTagsTextview?.text = tagsText
                    binding?.projectInformationLayout?.detailsProjectTagsTextview?.visibility = VISIBLE
                }

                // 5. Lastly write the list of tags to the text file (overwriting any previously)
                FileUtils.writeProjectTagsFile(mExternalFilesDir, mCurrentProjectView.project_id, mProjectTags)
            }
        })

        // Observe all the tags in the database
        // This is used to set up the list of all the tags in the project tag dialog
        // So that the user may simply click on a tag to add them to a project
        detailViewModel.tags.observe(viewLifecycleOwner, Observer { tagEntries: List<TagEntry> ->
            mAllTags = tagEntries.sortedBy { it.text.toLowerCase(Locale.getDefault()) }
            setProjectTagDialog()
        })
    }

    // This updates the tags in the project info dialog
    // This is distinct from set info dialog so that tags may be updated separately
    private fun setInfoTags(): String {
        var tagsText = ""
        for (tag in mProjectTags) {
            // Concatenate a string for non-interactive output
            tagsText = tagsText.plus("#${tag.text}  ")
        }
        val tagsTextView = mInfoDialog?.findViewById<TextView>(R.id.dialog_information_tags)
        if (tagsText.isEmpty()) tagsTextView?.text = getString(R.string.none)
        else tagsTextView?.text = tagsText
        return tagsText
    }

    // This updates the rest of the info in the project info dialog
    // Name, id, schedule, etc.
    private fun setInfoDialog() {
        if (mInfoDialog == null) return
        // Set info dialog fields
        val projectInfoDialogId = mInfoDialog?.findViewById<TextView>(R.id.dialog_project_info_id_field)
        projectInfoDialogId?.text = mCurrentProjectView.project_id.toString()
        val projectInfoNameTv = mInfoDialog?.findViewById<TextView>(R.id.dialog_project_info_name)
        if (mCurrentProjectView.project_name == null || mCurrentProjectView.project_name!!.isEmpty()) {
            projectInfoNameTv?.text = getString(R.string.unnamed)
        } else projectInfoNameTv?.text = mCurrentProjectView.project_name
        if (mCurrentProjectView.interval_days == 0) {
            mInfoDialog?.findViewById<TextView>(R.id.info_dialog_schedule_description)?.text = getString(R.string.none)
        } else {
            mInfoDialog?.findViewById<TextView>(R.id.info_dialog_schedule_description)?.text =
                    getString(R.string.every_x_days, mCurrentProjectView.interval_days.toString())
        }
    }

    // This sets the tags in the project info dialog
    // This creates text views for all tags in the database, but if the tags
    // belong to the project then they are styled appropriately for user feedback
    private fun setProjectTagDialog() {
        if (mTagDialog == null) return
        val availableTagsLayout = mTagDialog?.findViewById<FlexboxLayout>(R.id.project_tag_dialog_available_tags_layout)
        availableTagsLayout?.removeAllViews()
        // Set up the available tags in the project information dialog
        val instructionTv = mTagDialog?.findViewById<TextView>(R.id.tag_deletion_instructions)
        if (mAllTags.isEmpty()) {
            instructionTv?.text = getString(R.string.tag_start_instruction)
        } else {
            instructionTv?.text = getString(R.string.tag_deletion_instruction)
            // Add the tags to the layout
            for (tagEntry in mAllTags) {
                // Inflate the tag and set its text
                val textView: TextView = layoutInflater.inflate(R.layout.tag_text_view, availableTagsLayout, false) as TextView
                textView.text = getString(R.string.hashtag, tagEntry.text)

                // Style depending upon whether or not this particular project is tagged
                val tagInProject: Boolean = mProjectTags.contains(tagEntry)
                if (tagInProject) {
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTag))
                    textView.setOnClickListener { detailViewModel.deleteTagFromProject(tagEntry, mCurrentProjectView) }
                } else {
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey))
                    textView.setOnClickListener { detailViewModel.addTag(tagEntry.text, mCurrentProjectView) }
                }

                // Set tag deletion
                textView.setOnLongClickListener {
                    verifyTagDeletion(tagEntry)
                    true
                }

                // Add the view to the flex box layout
                availableTagsLayout?.addView(textView)
            }
        }
    }

    // Updates UI of schedule dialog to current schedule
    private fun setScheduleInformation() {
        if (mScheduleDialog == null) return
        val colorSelected = R.color.colorPrimary
        val colorDefault = R.color.colorSubtleAccent
        val defaultElevation = 2f
        val selectedElevation = 6f

        val currentInterval = mCurrentProjectView.interval_days
        if (currentInterval == 0) {
            mNoneSelector?.setCardBackgroundColor(ContextCompat.getColor(requireContext(), colorSelected))
            mNoneSelector?.elevation = selectedElevation
        } else {
            mNoneSelector?.setCardBackgroundColor(ContextCompat.getColor(requireContext(), colorDefault))
            mNoneSelector?.elevation = defaultElevation
        }
        for (selector in mDaySelectionViews) {
            selector.setCardBackgroundColor(ContextCompat.getColor(requireContext(), colorDefault))
            selector.elevation = defaultElevation
            val selectorTv = selector.findViewById<TextView>(R.id.selector_child_tv)
            if (selectorTv.text == currentInterval.toString()) {
                selector.setCardBackgroundColor(ContextCompat.getColor(requireContext(), colorSelected))
                selector.elevation = selectedElevation
            }
        }
        for (selector in mWeekSelectionViews) {
            selector.setCardBackgroundColor(ContextCompat.getColor(requireContext(), colorDefault))
            selector.elevation = defaultElevation
            val selectorTv = selector.findViewById<TextView>(R.id.selector_child_tv)
            val currentWeekIntervalToDays = selectorTv.text.toString().toInt() * 7
            if (currentWeekIntervalToDays == currentInterval) {
                selector.setCardBackgroundColor(ContextCompat.getColor(requireContext(), colorSelected))
                selector.elevation = selectedElevation
            }
        }
        val scheduleOutput = mScheduleDialog?.findViewById<TextView>(R.id.dialog_schedule_result)
        if (mCurrentProjectView.interval_days == 0) scheduleOutput?.text = getString(R.string.none)
        else scheduleOutput?.text = getString(R.string.every_x_days, mCurrentProjectView.interval_days.toString())
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
                    detailViewModel.deleteCurrentPhoto(mExternalFilesDir)
                    mFirebaseAnalytics?.logEvent(getString(R.string.analytics_delete_photo), null)
                }
                .setNegativeButton(android.R.string.no, null).show()
    }

    // Ensures the user wishes to delete the selected tag
    private fun verifyTagDeletion(tagEntry: TagEntry) {
        AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_tag, tagEntry.text))
                .setMessage(getString(R.string.verify_delete_tag, tagEntry.text))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { _, _: Int ->
                    // If this photo is the last photo then set the new thumbnail to its previous
                    detailViewModel.deleteTagFromDatabase(tagEntry)
                    mFirebaseAnalytics?.logEvent(getString(R.string.analytics_delete_tag), null)
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
                    detailViewModel.deleteCurrentProject(mExternalFilesDir)
                    // If current project had a schedule remove the notification and update widgets
                    if (mCurrentProjectView.interval_days != 0) {
                        NotificationUtils.scheduleNotificationWorker(requireContext())
                        UpdateWidgetService.startActionUpdateWidgets(requireContext())
                    }
                    sharedElementReturnTransition = null

                    // Log project deletion to analytics
                    mFirebaseAnalytics?.logEvent(getString(R.string.analytics_delete_project), null)

                    findNavController().popBackStack()
                }
                .setNegativeButton(android.R.string.no, null).show()
    }

    // Verifies the user wishes to rename the project
    // This will rename the folder the images are written to.
    private fun verifyEditName() {
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.contentDescription = getString(R.string.content_description_edit_text_project_name)
        AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.edit_name))
                .setView(input)
                .setPositiveButton(android.R.string.yes) { _, _: Int ->
                    val nameText = input.text.toString().trim()
                    detailViewModel.updateProjectName(mExternalFilesDir, nameText, mCurrentProjectView)
                }
                .setNegativeButton(android.R.string.no, null).show()
    }

    // Gives user feedback on tags
    private fun showTagValidationAlertDialog(message: String) {
        AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.invalid_tag))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _: Int ->
                }.show()
    }

    companion object {
        private val TAG = DetailFragment::class.java.simpleName
    }
}