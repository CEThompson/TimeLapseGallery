package com.vwoom.timelapsegallery.detail

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
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
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
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
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.databinding.FragmentDetailBinding
import com.vwoom.timelapsegallery.notification.NotificationUtils
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.InjectorUtils
import com.vwoom.timelapsegallery.utils.PhotoUtils
import com.vwoom.timelapsegallery.utils.ProjectUtils.getEntryFromProject
import com.vwoom.timelapsegallery.utils.TimeUtils
import com.vwoom.timelapsegallery.utils.TimeUtils.daysUntilDue
import com.vwoom.timelapsegallery.widget.UpdateWidgetService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import kotlin.math.absoluteValue
import kotlin.properties.Delegates

// TODO: (update 1.2) use NDK to implement converting photo sets to .gif and .mp4/.mov etc
// TODO: (update 1.2) implement pinch zoom on fullscreen image
class DetailFragment : Fragment(), DetailAdapter.DetailAdapterOnClickHandler {

    private var binding: FragmentDetailBinding? = null

    private var mDetailAdapter: DetailAdapter? = null

    // Database
    private var mExternalFilesDir: File? = null

    private var mPlaybackInterval by Delegates.notNull<Long>()

    // Photo and project Information
    private var mPhotos: List<PhotoEntry>? = null
    private var mProjectTags: List<TagEntry>? = null
    private var mAllTags: List<TagEntry>? = null
    private var mCurrentPhoto: PhotoEntry? = null
    private var mCurrentPlayPosition: Int? = null
    private var mCurrentProject: Project? = null

    // Dialogs
    private var mFullscreenImageDialog: Dialog? = null
    private var mTagDialog: Dialog? = null
    private var mInfoDialog: Dialog? = null
    private var mScheduleDialog: Dialog? = null

    // For schedule selection
    private var mNoneSelector: CardView? = null
    private val mDaySelectionViews: ArrayList<CardView> = arrayListOf()
    private val mWeekSelectionViews: ArrayList<CardView> = arrayListOf()

    private var toolbar: Toolbar? = null

    // For playing time lapse
    private var mPlaying = false
    private var mImageIsLoaded = false

    // Jobs
    private var playJob: Job? = null
    private var tagJob: Job? = null


    // Swipe listener for image navigation
    private var mOnSwipeTouchListener: OnSwipeTouchListener? = null

    private val args: DetailFragmentArgs by navArgs()

    private val detailViewModel: DetailViewModel by viewModels {
        InjectorUtils.provideDetailsViewModelFactory(requireActivity(), args.clickedProject)
    }

    // Analytics
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mCurrentProject = args.clickedProject
        mPlaybackInterval = getString(R.string.playback_interval_default).toLong()
        mExternalFilesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())
        val sharedElemTransition = TransitionInflater.from(context).inflateTransition(R.transition.image_shared_element_transition)
        sharedElemTransition.addListener(object: Transition.TransitionListener{
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

    private fun fadeInPhotoInformation() {
        if (binding == null) return
        binding!!.detailScheduleLayout.galleryGradientTopDown.animate().alpha(1f)
        binding!!.detailScheduleLayout.scheduleIndicatorIntervalTv.animate().alpha(1f)
        binding!!.detailScheduleLayout.scheduleDaysUntilDueTv.animate().alpha(1f)
        binding!!.photoInformationLayout.animate().alpha(1f)
        binding!!.fullscreenFab.show()
    }

    private fun fadeOutPhotoInformation() {
        if (binding == null) return
        binding!!.detailScheduleLayout.galleryGradientTopDown.animate().alpha(0f)
        binding!!.detailScheduleLayout.scheduleIndicatorIntervalTv.animate().alpha(0f)
        binding!!.detailScheduleLayout.scheduleDaysUntilDueTv.animate().alpha(0f)
        binding!!.photoInformationLayout.animate().alpha(0f)
        binding!!.fullscreenFab.hide()
    }

    override fun onStart() {
        super.onStart()
        binding?.detailsFragmentToolbar?.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        if (detailViewModel.fullscreenDialogShowing){
            if (mFullscreenImageDialog == null) {
                initializeFullscreenImageDialog()
            }
            mFullscreenImageDialog?.show()
        }
        if (detailViewModel.infoDialogShowing){
            if (mInfoDialog == null) initializeInfoDialog()
            mInfoDialog?.show()
        }
        if (detailViewModel.scheduleDialogShowing){
            if (mScheduleDialog == null) initializeScheduleDialog()
            mScheduleDialog?.show()
        }
        if (detailViewModel.tagDialogShowing){
            if (mTagDialog == null) initializeTagDialog()
            mTagDialog?.show()
        }
    }

    override fun onPause() {
        super.onPause()
        // Cancel any jobs
        playJob?.cancel()
        tagJob?.cancel()

        // Dismiss any dialogs
        mInfoDialog?.dismiss()
        mFullscreenImageDialog?.dismiss()
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
        mFullscreenImageDialog = null
        mDetailAdapter = null
        toolbar = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDetailBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

        // Get the playback interval from the shared preferences
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val playbackIntervalSharedPref = pref.getString(getString(R.string.key_playback_interval), getString(R.string.playback_interval_default))
        mPlaybackInterval = playbackIntervalSharedPref!!.toLong()

        // Set up toolbar
        setHasOptionsMenu(true)
        toolbar = binding?.detailsFragmentToolbar
        (activity as TimeLapseGalleryActivity).setSupportActionBar(toolbar)
        toolbar?.title = getString(R.string.project_details)
        (activity as TimeLapseGalleryActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set up adapter and recycler view
        mDetailAdapter = DetailAdapter(this, mExternalFilesDir!!)
        val linearLayoutManager
                = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding?.detailsRecyclerview?.layoutManager = linearLayoutManager
        binding?.detailsRecyclerview?.adapter = mDetailAdapter

        // 1. Initialize the color of the play as video fab
        // NOTE: this is not set in XML because setting by xml seems to lock the value of the color
        binding?.playAsVideoFab?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorGreen))
        binding?.playAsVideoFab?.rippleColor = ContextCompat.getColor(requireContext(), R.color.colorGreen)

        // 2. Set the click listeners
        binding?.addPhotoFab?.setOnClickListener {
            val cameraId = PhotoUtils.findCamera(requireContext())
            if (cameraId == null){
                Toast.makeText(requireContext(), getString(R.string.no_camera_found), Toast.LENGTH_LONG).show()
            } else {
                val action = DetailFragmentDirections
                        .actionDetailsFragmentToCamera2Fragment(cameraId, detailViewModel.lastPhoto, mCurrentProject)
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
        binding?.projectInformationFab?.setOnClickListener {
            if (mInfoDialog == null) initializeInfoDialog()
            mInfoDialog?.show()
            detailViewModel.infoDialogShowing = true
        }
        binding?.fullscreenFab?.setOnClickListener {
            if (mFullscreenImageDialog == null) initializeFullscreenImageDialog()

            if (!mPlaying) {
                mFullscreenImageDialog?.show()
                detailViewModel.fullscreenDialogShowing = true
            }
        }

        // Set a swipe listener for the image
        mOnSwipeTouchListener = OnSwipeTouchListener(requireContext())
        @Suppress("ClickableViewAccessibility")
        binding?.detailCurrentImage?.setOnTouchListener(mOnSwipeTouchListener)

        // Set the transition name for the image
        val transitionName= "${mCurrentProject!!.project_id}"
        val cardTransitionName = "${transitionName}card"
        val bottomTransitionName = "${transitionName}bottomGradient"
        val topTransitionName = "${transitionName}topGradient"
        val dueTransitionName = "${transitionName}due"
        val intervalTransitionName = "${transitionName}interval"
        binding?.detailCurrentImage?.transitionName = transitionName
        binding?.detailsCardContainer?.transitionName = cardTransitionName
        binding?.detailsGradientOverlay?.transitionName = bottomTransitionName
        binding?.detailScheduleLayout?.galleryGradientTopDown?.transitionName = topTransitionName
        binding?.detailScheduleLayout?.scheduleDaysUntilDueTv?.transitionName = dueTransitionName
        binding?.detailScheduleLayout?.scheduleIndicatorIntervalTv?.transitionName = intervalTransitionName

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
                val shareIntent: Intent = Intent().apply{
                    action = Intent.ACTION_SEND
                    type = "image/jpeg"
                    val photoFile = File(FileUtils
                            .getPhotoUrl(
                                    mExternalFilesDir!!,
                                    getEntryFromProject(mCurrentProject!!),
                                    mCurrentPhoto!!.timestamp))
                    Log.d(TAG, photoFile.absolutePath)
                    putExtra(Intent.EXTRA_STREAM, Uri.fromFile(photoFile))
                }
                startActivity(Intent.createChooser(shareIntent, "Share Image"))
                true
            }
            R.id.delete_photo -> {
                if (mPhotos?.size == 1) {
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

    private fun loadUi(photoEntry: PhotoEntry) { // Set the fullscreen image dialogue to the current photo
        if (!mPlaying) setFullscreenImage()
        // Notify the adapter: this updates the detail recycler view red highlight indicator
        mDetailAdapter?.setCurrentPhoto(photoEntry)

        // Get the image path, handle orientation indicator and load the image
        val imagePath = FileUtils.getPhotoUrl(mExternalFilesDir!!, getEntryFromProject(mCurrentProject!!), photoEntry.timestamp)
        if (!mPlaying) handleOrientationIndicator(imagePath)
        loadImage(imagePath)

        // Update position of progress view and thumbnail
        val position = mPhotos!!.indexOf(photoEntry)
        val photoNumber = position + 1
        binding?.detailsRecyclerview?.scrollToPosition(position)
        binding?.imageLoadingProgress?.progress = position

        // If playing do not update the individual photo info (skip the rest)
        if (mPlaying) return
        // Otherwise update photo information
        else {
            // Get info for the current photo
            val timestamp = photoEntry.timestamp
            val photosInProject: Int = mPhotos!!.size
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


    private fun handleOrientationIndicator(imagePath: String) {
        // Detect configuration
        val imageIsLandscape = PhotoUtils.isLandscape(imagePath)
        val deviceIsLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // If landscape device and image (true == true) or portrait device and image (false == false)
        // Do not display the indicator
        if (imageIsLandscape == deviceIsLandscape)  {
            binding?.rotationIndicator?.startAnimation(stopAnimation)
        }
        // Otherwise show the blinking indicator
        else {
            binding?.rotationIndicator?.startAnimation(blinkAnimation)
        }
    }

    // Loads an image into the main photo view
    private fun loadImage(imagePath: String) {
        mImageIsLoaded = false // this set to true after load image pair completes
        // Load the image
        val f = File(imagePath)
        loadImagePair(f, binding!!.detailCurrentImage, binding!!.detailNextImage)
    }

    // TODO: (update 1.2) re-evaluate and speed up image loading
    // This function loads an image into a top view, then loads an image into the bottom view and hides the top view
    // This makes 'playing' the images look seamless
    private fun loadImagePair(f: File, bottomImage: ImageView, topImage: ImageView) {
        // 1. First load the image into the next image view on top of the current
        Glide.with(this)
                .load(f)
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(e: GlideException?,
                                              model: Any,
                                              target: Target<Drawable?>, isFirstResource: Boolean): Boolean {
                        val toast = Toast.makeText(requireContext(), getString(R.string.error_loading_image), Toast.LENGTH_SHORT)
                        toast.show()
                        return false
                    }
                    override fun onResourceReady(resource: Drawable?, model: Any, target: Target<Drawable?>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        // Show the just loaded image on the top
                        topImage.visibility = VISIBLE
                        // 2. Then load the image into the current image on the bottom
                        Glide.with(requireContext())
                                .load(f)
                                .listener(object : RequestListener<Drawable?> {
                                    override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable?>, isFirstResource: Boolean): Boolean {
                                        startPostponedEnterTransition()
                                        val toast = Toast.makeText(requireContext(), getString(R.string.error_loading_image), Toast.LENGTH_SHORT)
                                        toast.show()
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
        if (mPhotos!!.size <= 1) {
            Snackbar.make(binding!!.detailsCoordinatorLayout, R.string.add_more_photos,
                    Snackbar.LENGTH_LONG)
                    .show()
            return
        }

        // Handle play state
        mPlaying = true
        mCurrentPlayPosition = mPhotos!!.indexOf(mCurrentPhoto)

        // Handle UI
        fadeOutPhotoInformation()
        binding?.playAsVideoFab?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorRedAccent))
        binding?.playAsVideoFab?.rippleColor = ContextCompat.getColor(requireContext(), R.color.colorRedAccent)
        binding?.playAsVideoFab?.setImageResource(R.drawable.ic_stop_white_24dp)


        // Override the play position to beginning if currently already at the end
        if (mCurrentPlayPosition == mPhotos!!.size - 1) {
            mCurrentPlayPosition = 0
            detailViewModel.setPhoto(mPhotos!![0])
        }

        // Actually schedule the sequence via recursive function
        binding?.imageLoadingProgress?.progress = mCurrentPlayPosition!!
        scheduleLoadPhoto(mCurrentPlayPosition!!) // Recursively loads the rest of set from beginning

        // Track play button interaction
        mFirebaseAnalytics!!.logEvent(getString(R.string.analytics_play_time_lapse), null)
    }

    // Resets the UI & handles state after playing
    private fun stopPlaying() { // Set color of play fab
        playJob?.cancel()
        mPlaying = false
        binding?.imageLoadingProgress?.progress = mCurrentPlayPosition ?: 0
        binding?.playAsVideoFab?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorGreen))
        binding?.playAsVideoFab?.rippleColor = ContextCompat.getColor(requireContext(), R.color.colorGreen)
        binding?.playAsVideoFab?.setImageResource(R.drawable.ic_play_arrow_white_24dp)

        fadeInPhotoInformation()
    }

    private fun scheduleLoadPhoto(position: Int) {
        Log.d("DetailsFragment", "schedule loading position $position")
        if (position < 0 || position >= mPhotos!!.size) {
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
            } else {
                scheduleLoadPhoto(position)
            }
        }
    }

    override fun onClick(clickedPhoto: PhotoEntry) {
        detailViewModel.setPhoto(clickedPhoto)
    }

    /**
     * Dialog Methods
     */
    private fun initializeInfoDialog(){
        mInfoDialog = Dialog(requireContext())
        mInfoDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        mInfoDialog?.setContentView(R.layout.dialog_project_information)
        mInfoDialog?.setOnCancelListener { detailViewModel.infoDialogShowing = false }
        // Get Views
        val editNameButton = mInfoDialog?.findViewById<FloatingActionButton>(R.id.edit_project_name_button)
        // Set fab colors
        editNameButton?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
        editNameButton?.setOnClickListener { editName() }
        val tagsTextView = mInfoDialog?.findViewById<TextView>(R.id.dialog_information_tags)
        tagsTextView?.setOnClickListener {
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
    private fun editName(){
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.edit_name))
                .setView(input)
                .setPositiveButton(android.R.string.yes) { _, _: Int ->
                    val nameText = input.text.toString().trim()
                    detailViewModel.updateProjectName(mExternalFilesDir!!, nameText, mCurrentProject!!)
                }
                .setNegativeButton(android.R.string.no, null).show()
    }
    private fun initializeTagDialog(){
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
                tagText.isEmpty() -> {return@setOnClickListener}
                tagText.contains(' ') -> showTagValidationAlertDialog(getString(R.string.invalid_tag_one_word))
                tagText.length > 14 -> showTagValidationAlertDialog(getString(R.string.invalid_tag_length))
                else -> {
                    detailViewModel.addTag(tagText, mCurrentProject!!)
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
    private fun showTagValidationAlertDialog(message: String) {
        AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.invalid_tag))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _: Int ->
                }.show()
    }
    private fun initializeScheduleDialog(){
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
        noneLayout?.addView(mNoneSelector)
        mNoneSelector?.setOnClickListener {
            detailViewModel.setSchedule(mExternalFilesDir!!, mCurrentProject!!, 0)
        }

        // Set up days selection
        val daysLayout = mScheduleDialog?.findViewById<FlexboxLayout>(R.id.dialog_schedule_days_selection_layout)
        daysLayout?.removeAllViews()
        for (dayInterval in 1..6){
            // a selection layout for each interval
            //val textView = TextView(requireContext())
            val selectionLayout: CardView = layoutInflater.inflate(R.layout.dialog_schedule_selector, daysLayout,false) as CardView
            val textView = selectionLayout.findViewById<TextView>(R.id.selector_child_tv)
            textView.text = dayInterval.toString()
            selectionLayout.setOnClickListener {
                detailViewModel.setSchedule(mExternalFilesDir!!, mCurrentProject!!, dayInterval)
            }
            daysLayout?.addView(selectionLayout)
            mDaySelectionViews.add(selectionLayout)
        }

        // Set up weeks selection
        val weeksLayout = mScheduleDialog?.findViewById<FlexboxLayout>(R.id.dialog_schedule_weeks_selection_layout)
        weeksLayout?.removeAllViews()
        for (weekInterval in 1..4){
            val selectionLayout: CardView = layoutInflater.inflate(R.layout.dialog_schedule_selector, daysLayout,false) as CardView
            val textView = selectionLayout.findViewById<TextView>(R.id.selector_child_tv)
            textView.text = weekInterval.toString()
            selectionLayout.setOnClickListener {
                detailViewModel.setSchedule(mExternalFilesDir!!,mCurrentProject!!, weekInterval*7)
            }
            weeksLayout?.addView(selectionLayout)
            mWeekSelectionViews.add(selectionLayout)
        }

        // Set up custom input
        mScheduleDialog?.findViewById<EditText>(R.id.custom_schedule_input)?.addTextChangedListener {
            val interval = it.toString()
            if (interval.isNotEmpty()){
                detailViewModel.setSchedule(mExternalFilesDir!!, mCurrentProject!!, interval.toInt())
            } else {
                detailViewModel.setSchedule(mExternalFilesDir!!, mCurrentProject!!, 0)
            }
        }

        // Dismiss
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
    private fun initializeFullscreenImageDialog() {
        // Create the dialog
        mFullscreenImageDialog = Dialog(requireContext(), R.style.Theme_AppCompat_Light_NoActionBar_FullScreen)
        mFullscreenImageDialog?.setCancelable(false)
        mFullscreenImageDialog?.setContentView(R.layout.dialog_fullscreen_image)
        val mFullscreenExitFab: FloatingActionButton? = mFullscreenImageDialog?.findViewById(R.id.fullscreen_exit_fab)
        val mFullscreenBackFab: FloatingActionButton? = mFullscreenImageDialog?.findViewById(R.id.fullscreen_back_fab)
        // Display the dialog on clicking the image
        mFullscreenBackFab?.setOnClickListener {
            mFullscreenImageDialog?.dismiss()
            detailViewModel.fullscreenDialogShowing = false
        }
        mFullscreenExitFab?.setOnClickListener {
            mFullscreenImageDialog?.dismiss()
            detailViewModel.fullscreenDialogShowing = false
        }
        mFullscreenImageDialog?.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                mFullscreenImageDialog?.dismiss()
                detailViewModel.fullscreenDialogShowing = false
            }
            true
        }
        // Set a listener to change the current photo on swipe
        val fullscreenImage = mFullscreenImageDialog?.findViewById<ImageView>(R.id.fullscreen_image_bottom)
        @Suppress("ClickableViewAccessibility")
        fullscreenImage?.setOnTouchListener(mOnSwipeTouchListener)
        setFullscreenImage()
    }

    /**
    * UI methods
    */

    // Pre loads the selected image into the hidden dialogue so that display appears immediate
    private fun setFullscreenImage() {
        if (mFullscreenImageDialog == null) return
        if (mCurrentPhoto == null) return
        val path = FileUtils.getPhotoUrl(
                mExternalFilesDir!!,
                getEntryFromProject(mCurrentProject!!),
                mCurrentPhoto!!.timestamp)
        val current = File(path)
        val bottomImage = mFullscreenImageDialog?.findViewById<ImageView>(R.id.fullscreen_image_bottom)
        val topImage = mFullscreenImageDialog?.findViewById<ImageView>(R.id.fullscreen_image_top)
        if (bottomImage != null && topImage != null)
            loadImagePair(current, bottomImage, topImage)
    }
    
    private fun setupViewModel() {
        // Observe the current selected project
        // This updates the project information card, project info dialog,
        // schedule layout over the image and the schedule dialog
        detailViewModel.currentProject.observe(viewLifecycleOwner, Observer { currentProject: Project ->
            mCurrentProject = currentProject

            // Set the ui for the project information layout cardview
            // 1. Set the ID
            binding?.projectInformationLayout?.detailsProjectId?.text = mCurrentProject?.project_id.toString()
            // 2. Set the name, handle appropriately if no name specified
            val name = mCurrentProject?.project_name
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
            if (currentProject.interval_days == 0) {
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
                        currentProject.interval_days.toString()

                val daysUntilDue = daysUntilDue(mCurrentProject!!)
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
            // 1. Set the last photo to pass to the camera fragment.
            // Note: this is because photo passes the last photo url along with it as a parcelable
            val lastPhotoEntry = photoEntries.last()
            detailViewModel.setLastPhotoByEntry(mExternalFilesDir!!, mCurrentProject!!, lastPhotoEntry)

            // Detect if the last photo changed from previous assignment
            val lastPhotoChanged = (mPhotos!=null && lastPhotoEntry != mPhotos!!.last())

            // NOTE: setting mPhotos here because of a timing issue,
            // otherwise the current photo listener fires triggering a method which uses mPhotos incorrect value
            // causing the photo number to read wrong on return to fragment from the camera
            mPhotos = photoEntries

            // If the last photo changed we want to update it as the current photo
            // and make sure to set it as the cover photo
            if (lastPhotoChanged) {
                // Set the current photo to the last
                mCurrentPhoto = lastPhotoEntry
                detailViewModel.currentPhoto.value = lastPhotoEntry

                // Set the cover photo to the last in the set
                // This handles changing cover photo on deletion and addition
                detailViewModel.setCoverPhoto(lastPhotoEntry)
            }

            // 2. Send the list of photos to the adapter
            mDetailAdapter?.setPhotoData(mPhotos, mCurrentProject)
            // Restore the play position
            mCurrentPlayPosition = mPhotos?.indexOf(mCurrentPhoto)

            // 3. Update the project info card view to show the range of dates the photos represent
            val firstTimestamp = mPhotos?.get(0)?.timestamp
            val firstProjectDateString = TimeUtils.getShortDateFromTimestamp(firstTimestamp!!)
            val lastTimestamp = mPhotos?.get(mPhotos!!.size - 1)?.timestamp
            val lastProjectDateString = TimeUtils.getShortDateFromTimestamp(lastTimestamp!!)
            binding?.projectInformationLayout
                    ?.detailsProjectTimespanTextview?.text = getString(R.string.timespan, firstProjectDateString, lastProjectDateString)

            // 4. Update the progress bar
            binding?.imageLoadingProgress?.max = mPhotos!!.size - 1

            // 5. Lastly if current photo isn't set for some reason, set it to the last photo
            if (detailViewModel.currentPhoto.value == null) {
                mCurrentPhoto = mPhotos!![mPhotos!!.size-1]
                detailViewModel.currentPhoto.value = mCurrentPhoto
            }
        })

        // Observes the currently selected photo
        // This loads the image and timestamp information based on the current photo
        detailViewModel.currentPhoto.observe(viewLifecycleOwner, Observer { currentPhoto: PhotoEntry? ->
            mCurrentPhoto = currentPhoto
            if (currentPhoto != null) {
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
                if (mProjectTags!!.isEmpty()) {
                    binding?.projectInformationLayout?.detailsProjectTagsTextview?.visibility = GONE
                }
                else {
                    binding?.projectInformationLayout?.detailsProjectTagsTextview?.text = tagsText
                    binding?.projectInformationLayout?.detailsProjectTagsTextview?.visibility = VISIBLE
                }

                // 5. Lastly write the list of tags to the text file (overwriting any previously)
                FileUtils.writeProjectTagsFile(mExternalFilesDir!!, mCurrentProject!!.project_id, mProjectTags!!)
            }
        })

        // Observe all the tags in the database
        // This is used to set up the list of all the tags in the project tag dialog
        // So that the user may simply click on a tag to add them to a project
        detailViewModel.tags.observe(viewLifecycleOwner, Observer {tagEntries: List<TagEntry> ->
            mAllTags = tagEntries.sortedBy { it.text.toLowerCase(Locale.getDefault()) }
            setProjectTagDialog()
        })
    }
    // This updates the tags in the project info dialog
    // This is distinct from set info dialog so that tags may be updated separately
    private fun setInfoTags(): String {
        var tagsText = ""
        for (tag in mProjectTags!!) {
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
        projectInfoDialogId?.text = mCurrentProject!!.project_id.toString()
        val projectInfoNameTv = mInfoDialog?.findViewById<TextView>(R.id.dialog_project_info_name)
        if (mCurrentProject?.project_name == null || mCurrentProject?.project_name!!.isEmpty()) {
            projectInfoNameTv?.text = getString(R.string.unnamed)
        } else projectInfoNameTv?.text = mCurrentProject!!.project_name
        if (mCurrentProject!!.interval_days == 0) {
            mInfoDialog?.findViewById<TextView>(R.id.info_dialog_schedule_description)?.text = getString(R.string.none)
        } else {
            mInfoDialog?.findViewById<TextView>(R.id.info_dialog_schedule_description)?.text =
                    getString(R.string.every_x_days, mCurrentProject!!.interval_days.toString())
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
        if (mAllTags != null) {
            val instructionTv = mTagDialog?.findViewById<TextView>(R.id.tag_deletion_instructions)
            if (mAllTags!!.isEmpty()){
                instructionTv?.text = getString(R.string.tag_start_instruction)
            } else {
                instructionTv?.text = getString(R.string.tag_deletion_instruction)
                // Add the tags to the layout
                for (tagEntry in mAllTags!!) {
                    // Inflate the tag and set its text
                    val textView: TextView = layoutInflater.inflate(R.layout.tag_text_view, availableTagsLayout, false) as TextView
                    textView.text = getString(R.string.hashtag, tagEntry.text)

                    // Style depending upon whether or not this particular project is tagged
                    val tagInProject: Boolean = mProjectTags?.contains(tagEntry) ?: false
                    if (tagInProject) {
                        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTag))
                        textView.setOnClickListener { detailViewModel.deleteTagFromProject(tagEntry, mCurrentProject!!) }
                    }
                    else {
                        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey))
                        textView.setOnClickListener { detailViewModel.addTag(tagEntry.text, mCurrentProject!!) }
                    }

                    // Set tag deletion
                    textView.setOnLongClickListener {
                        verifyTagDeletion(tagEntry)
                        true
                    }

                    // Add the view to the flexbox layout
                    availableTagsLayout?.addView(textView)
                }
            }
        }
    }
    // Updates UI of schedule dialog to current schedule
    private fun setScheduleInformation(){
        if (mScheduleDialog == null) return
        val colorSelected = R.color.colorPrimary
        val colorDefault = R.color.colorSubtleAccent
        val defaultElevation = 2f
        val selectedElevation = 6f

        val currentInterval = mCurrentProject!!.interval_days
        if (currentInterval == 0){
            mNoneSelector?.setCardBackgroundColor(ContextCompat.getColor(requireContext(), colorSelected))
            mNoneSelector?.elevation = selectedElevation
        } else {
            mNoneSelector?.setCardBackgroundColor(ContextCompat.getColor(requireContext(), colorDefault))
            mNoneSelector?.elevation = defaultElevation
        }
        for (selector in mDaySelectionViews){
            selector.setCardBackgroundColor(ContextCompat.getColor(requireContext(), colorDefault))
            selector.elevation = defaultElevation
            val selectorTv = selector.findViewById<TextView>(R.id.selector_child_tv)
            if (selectorTv.text == currentInterval.toString()) {
                selector.setCardBackgroundColor(ContextCompat.getColor(requireContext(), colorSelected))
                selector.elevation = selectedElevation
            }
        }
        for (selector in mWeekSelectionViews){
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
        if (mCurrentProject!!.interval_days==0) scheduleOutput?.text = getString(R.string.none)
        else scheduleOutput?.text = getString(R.string.every_x_days, mCurrentProject!!.interval_days.toString())
    }

    // Changes photo on swipe
    inner class OnSwipeTouchListener(ctx: Context?) : OnTouchListener {
        private val gestureDetector: GestureDetector
        @Suppress("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            return gestureDetector.onTouchEvent(event)
        }
        private inner class GestureListener : SimpleOnGestureListener() {
            private val swipeThreshold = 100
            private val swipeVelocityThreshold = 100
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }
            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                var result = false
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (diffX.absoluteValue > diffY.absoluteValue)
                    if (diffX.absoluteValue > swipeThreshold && velocityX.absoluteValue > swipeVelocityThreshold) {
                        if (diffX > 0) { onSwipeRight() }
                        else { onSwipeLeft() }
                        result = true
                    }
                return result
            }
        }
        fun onSwipeRight() = detailViewModel.previousPhoto()
        fun onSwipeLeft() = detailViewModel.nextPhoto()
        init {
            gestureDetector = GestureDetector(ctx, GestureListener())
        }
    }

    /**
     * Dialog verifications
     */
    private fun verifyPhotoDeletion() {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_photo)
                .setMessage(R.string.verify_delete_photo)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { _, _: Int ->
                    // If this photo is the last photo then set the new thumbnail to its previous
                    detailViewModel.deleteCurrentPhoto(mExternalFilesDir!!)
                }
                .setNegativeButton(android.R.string.no, null).show()
    }
    private fun verifyTagDeletion(tagEntry: TagEntry){
        AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_tag, tagEntry.text))
                .setMessage(getString(R.string.verify_delete_tag, tagEntry.text))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { _, _: Int ->
                    // If this photo is the last photo then set the new thumbnail to its previous
                    detailViewModel.deleteTagFromRepo(tagEntry)
                }
                .setNegativeButton(android.R.string.no, null).show()
    }
    private fun verifyLastPhotoDeletion() {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_photo)
                .setMessage(R.string.verify_delete_last_photo)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { _, _: Int ->
                    verifyProjectDeletion() }
                .setNegativeButton(android.R.string.no, null).show()
    }
    // Deletes the current project after user verification
    private fun verifyProjectDeletion() {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_project)
                .setMessage(R.string.verify_delete_project)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { _, _: Int ->
                    doubleVerifyProjectDeletion() }
                .setNegativeButton(android.R.string.no, null).show()
    }
    // Double verifies project deletion
    private fun doubleVerifyProjectDeletion() {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_project)
                .setMessage(R.string.double_verify_project_deletion)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { _, _: Int ->
                    detailViewModel.deleteCurrentProject(mExternalFilesDir!!)
                    // If current project had a schedule remove the notification and update widgets
                    if (mCurrentProject?.interval_days != 0) {
                        NotificationUtils.scheduleNotificationWorker(requireContext())
                        UpdateWidgetService.startActionUpdateWidgets(requireContext())
                    }
                    findNavController().popBackStack()
                }
                .setNegativeButton(android.R.string.no, null).show()
    }

    companion object {
        private val TAG = DetailFragment::class.java.simpleName
    }
}