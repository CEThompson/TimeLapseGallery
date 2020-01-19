package com.vwoom.timelapsegallery.detail

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.transition.TransitionInflater
import android.util.Log
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.TimeLapseGalleryActivity
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.databinding.FragmentDetailBinding
import com.vwoom.timelapsegallery.notification.NotificationUtils
import com.vwoom.timelapsegallery.utils.*
import com.vwoom.timelapsegallery.widget.UpdateWidgetService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

// TODO lock down project editing

class DetailFragment : Fragment(), DetailAdapter.DetailAdapterOnClickHandler {

    lateinit var binding: FragmentDetailBinding

    private var mDetailAdapter: DetailAdapter? = null

    // Database
    private var mExternalFilesDir: File? = null

    // Photo and project Information
    private var mPhotos: List<PhotoEntry>? = null
    private var mTags: List<TagEntry>? = null
    private var mSelectedTags: ArrayList<String> = arrayListOf()
    private var mCurrentPhoto: PhotoEntry? = null
    private var mCurrentPlayPosition: Int? = null
    private var mCurrentProject: Project? = null
    private var mProjectSchedule: ProjectScheduleEntry? = null

    // Views for fullscreen dialog
    private var mFullscreenImageDialog: Dialog? = null
    private var mFullscreenImage: ImageView? = null

    private var mProjectInfoDialog: Dialog? = null
    private var mScheduleDialog: Dialog? = null
    private var mEditTagsDialog: Dialog? = null

    // For playing timelapse
    private var mPlaying = false
    private var mImageIsLoaded = false

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

        mExternalFilesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // Prepare analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())

        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(R.transition.image_shared_element_transition)
        sharedElementReturnTransition = TransitionInflater.from(context).inflateTransition(R.transition.image_shared_element_transition)

        enterTransition = TransitionInflater.from(context).inflateTransition(R.transition.details_enter_transition)

        postponeEnterTransition()
    }

    override fun onStop() {
        super.onStop()
        if (mFullscreenImageDialog?.isShowing == true) mFullscreenImageDialog?.dismiss()
        binding.detailsFragmentToolbar.setNavigationOnClickListener(null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = FragmentDetailBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

        // Set up toolbar
        setHasOptionsMenu(true)
        val toolbar = binding.detailsFragmentToolbar
        (activity as TimeLapseGalleryActivity).setSupportActionBar(toolbar)
        toolbar.title = getString(R.string.project_details)
        (activity as TimeLapseGalleryActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set up adapter and recycler view
        mDetailAdapter = DetailAdapter(this, requireContext())
        val linearLayoutManager
                = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.detailsRecyclerview.layoutManager = linearLayoutManager
        binding.detailsRecyclerview.adapter = mDetailAdapter

        // Set up fabs:
        // 1. Initialize the color of the play as video fab
        // NOTE: this is not set in XML because setting by xml seems to lock the value of the color
        binding.playAsVideoFab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorGreen))
        binding.playAsVideoFab.rippleColor = ContextCompat.getColor(requireContext(), R.color.colorGreen)


        // 2. Set the click listeners
        binding.addPhotoFab.setOnClickListener {
            // TODO: Determine if there is a better way to handle leaking toolbar references
            (activity as TimeLapseGalleryActivity).setSupportActionBar(null)
            val action = DetailFragmentDirections
                    .actionDetailsFragmentToCameraFragment(detailViewModel.lastPhoto, mCurrentProject)
            findNavController().navigate(action)
        }
        binding.playAsVideoFab.setOnClickListener { playSetOfImages() }
        binding.projectScheduleFab.setOnClickListener { mScheduleDialog?.show() }
        binding.projectTagFab.setOnClickListener { mEditTagsDialog?.show() }
        binding.projectInformationFab?.setOnClickListener {mProjectInfoDialog?.show()}
        binding.fullscreenFab.setOnClickListener { if (!mPlaying) mFullscreenImageDialog?.show() }

        // Set a swipe listener for the image
        mOnSwipeTouchListener = OnSwipeTouchListener(requireContext())
        binding.detailCurrentImage.setOnTouchListener(mOnSwipeTouchListener) // TODO override on perform click
        // TODO implement pinch zoom on fullscreen image

        // Initialize dialogs
        initializeFullscreenImageDialog()
        initializeProjectInformationDialog()
        initializeEditTagsDialog()
        initializeScheduleDialog()

        // Set the transition name for the image
        val imageTransitionName= "${mCurrentProject?.project_id}"
        val cardTransitionName = imageTransitionName + "card"
        binding.detailCurrentImage.transitionName = imageTransitionName
        binding.detailsCardContainer.transitionName = cardTransitionName
        Log.d(TAG, "tracking transition: detail fragment $imageTransitionName & $cardTransitionName")

        setupViewModel()

        showPhotoInformation()

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        binding.detailsFragmentToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onPause() {
        super.onPause()
        playJob?.cancel()
        tagJob?.cancel()
        mProjectInfoDialog?.dismiss()
        mEditTagsDialog?.dismiss()
        mScheduleDialog?.dismiss()
        mFullscreenImageDialog?.dismiss()
    }

    private fun showPhotoInformation() {
        val photoInformationLayout: LinearLayout = binding.photoInformationLayout
        val gradientOverlay: View = binding.detailsGradientOverlay
        val shortAnimationDuration = resources.getInteger(
                android.R.integer.config_shortAnimTime).toLong()
        binding.fullscreenFab.show()
        // Fade in gradient overlay
        gradientOverlay.alpha = 0f
        gradientOverlay.visibility = View.VISIBLE
        gradientOverlay.animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration)
                .setListener(null)
        // Fade in photo information
        photoInformationLayout.alpha = 0f
        photoInformationLayout.visibility = View.VISIBLE
        photoInformationLayout.animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration)
                .setListener(null)
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
                    val photoFile = File(FileUtils.getPhotoUrl(mExternalFilesDir!!, mCurrentProject!!, mCurrentPhoto!!))
                    Log.d(TAG, photoFile.absolutePath)
                    putExtra(Intent.EXTRA_STREAM, Uri.fromFile(photoFile))
                }
                startActivity(Intent.createChooser(shareIntent, "Share Image"))
                true
            }
            R.id.edit_project -> {
                mProjectInfoDialog?.show()
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

    fun loadUi(photoEntry: PhotoEntry) { // Set the fullscreen image dialogue to the current photo
        if (!mPlaying) preloadFullscreenImage()
        // Notify the adapter
        mDetailAdapter?.setCurrentPhoto(photoEntry)
        // Load the current image
        loadImage(FileUtils.getPhotoUrl(mExternalFilesDir!!, mCurrentProject!!, photoEntry))
        // Get info for the current photo
        val timestamp = photoEntry.timestamp
        val photoNumber = mPhotos!!.indexOf(photoEntry) + 1
        val photosInProject: Int = mPhotos!!.size
        // Get formatted strings
        val photoNumberString = getString(R.string.details_photo_number_out_of, photoNumber, photosInProject)
        val date = TimeUtils.getDateFromTimestamp(timestamp)
        val time = TimeUtils.getTimeFromTimestamp(timestamp)
        // Set the info
        binding.detailsPhotoNumberTv.text = photoNumberString
        binding.detailsPhotoDateTv.text = date
        binding.detailsPhotoTimeTv.text = time
        val position = mPhotos!!.indexOf(photoEntry)
        binding.detailsRecyclerview.scrollToPosition(position)
        binding.imageLoadingProgress.progress = photoNumber - 1
    }

    // Loads an image into the main photo view
    private fun loadImage(imagePath: String) {
        mImageIsLoaded = false
        // Get photo info
        val ratio = PhotoUtils.getAspectRatioFromImagePath(imagePath)
        val isImageLandscape = PhotoUtils.isLandscape(imagePath)
        // Set cardview constraints depending upon if photo is landscape or portrait
        val layoutParams = binding.detailsCardContainer.layoutParams

        // If landscape set height to wrap content
        // Set width to be measured
        if (isImageLandscape) {
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams.width = 0
        } else {
            layoutParams.height = 0
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        // Resize the constraint layout
        val constraintSet = ConstraintSet()
        val constraintLayout: ConstraintLayout = binding.detailsCurrentImageConstraintLayout
        constraintSet.clone(constraintLayout)
        constraintSet.setDimensionRatio(R.id.detail_current_image, ratio)
        constraintSet.setDimensionRatio(R.id.detail_next_image, ratio)
        constraintSet.applyTo(constraintLayout)
        // TODO (update) streamline code for image loading
        // Load the image

        val f = File(imagePath)

        Glide.with(this)
                .load(f)
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable?>, isFirstResource: Boolean): Boolean {
                        val toast = Toast.makeText(requireContext(), getString(R.string.error_loading_image), Toast.LENGTH_SHORT)
                        toast.show()
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any, target: Target<Drawable?>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        binding.detailNextImage.visibility = View.VISIBLE
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
                                        startPostponedEnterTransition()
                                        binding.detailNextImage.visibility = View.INVISIBLE
                                        mImageIsLoaded = true
                                        return false
                                    }
                                })
                                .into(binding.detailCurrentImage)
                        return false
                    }
                })
                .into(binding.detailNextImage)
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
            Snackbar.make(binding.detailsCoordinatorLayout, R.string.add_more_photos,
                    Snackbar.LENGTH_LONG)
                    .show()
            return
        }

        // Handle UI
        binding.fullscreenFab.hide()
        binding.playAsVideoFab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorRedAccent))
        binding.playAsVideoFab.rippleColor = ContextCompat.getColor(requireContext(), R.color.colorRedAccent)
        binding.playAsVideoFab.setImageResource(R.drawable.ic_stop_white_24dp)

        // Handle play state
        mPlaying = true
        mCurrentPlayPosition = mPhotos!!.indexOf(mCurrentPhoto)

        // Get the playback interval from the shared preferences
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val playbackIntervalSharedPref = pref.getString(getString(R.string.key_playback_interval), "50")
        val playbackInterval = playbackIntervalSharedPref!!.toLong()

        // Override the play position to beginning if currently already at the end
        if (mCurrentPlayPosition == mPhotos!!.size - 1) {
            mCurrentPlayPosition = 0
            detailViewModel.setPhoto(mPhotos!![0])
        }

        // Actually schedule the sequence via recursive function
        binding.imageLoadingProgress.progress = mCurrentPlayPosition!!
        scheduleLoadPhoto(mCurrentPlayPosition!!, playbackInterval) // Recursively loads the rest of set from beginning

        // Track play button interaction
        mFirebaseAnalytics!!.logEvent(getString(R.string.analytics_play_time_lapse), null)
    }

    // Resets the UI & handles state after playing
    private fun stopPlaying() { // Set color of play fab
        playJob?.cancel()
        mPlaying = false

        binding.playAsVideoFab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorGreen))
        binding.playAsVideoFab.rippleColor = ContextCompat.getColor(requireContext(), R.color.colorGreen)
        binding.playAsVideoFab.setImageResource(R.drawable.ic_play_arrow_white_24dp)
        binding.fullscreenFab.show()
    }

    var playJob: Job? = null
    var tagJob: Job? = null

    private fun scheduleLoadPhoto(position: Int, interval: Long) {
        Log.e("DetailsFragment", "schedule loading position $position")
        if (position < 0 || position >= mPhotos!!.size) {
            mPlaying = false
            binding.playAsVideoFab.setImageResource(R.drawable.ic_play_arrow_white_24dp)
            binding.imageLoadingProgress.progress = position
            binding.playAsVideoFab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorGreen))
            binding.playAsVideoFab.rippleColor = ContextCompat.getColor(requireContext(), R.color.colorGreen)
            binding.fullscreenFab.show()
            return
        }

        mCurrentPlayPosition = position

        playJob = detailViewModel.viewModelScope.launch {
            delay(interval)
            // If image is loaded load the next photo
            if (mImageIsLoaded) {
                detailViewModel.nextPhoto()
                binding.imageLoadingProgress.progress = position + 1
                scheduleLoadPhoto(position + 1, interval)
            } else {
                scheduleLoadPhoto(position, interval)
            }
        }
    }

    override fun onClick(clickedPhoto: PhotoEntry) {
        detailViewModel.setPhoto(clickedPhoto)
    }

    fun initializeScheduleDialog(){
        mScheduleDialog = Dialog(requireContext())
        mScheduleDialog?.setContentView(R.layout.dialog_schedule)
    }

    fun initializeEditTagsDialog(){
        mEditTagsDialog = Dialog(requireContext())
        mEditTagsDialog?.setContentView(R.layout.dialog_edit_tags)

        val addTagTextView = mEditTagsDialog?.findViewById<TextView>(R.id.dialog_edit_tags_add_tag)
        val deleteTextView = mEditTagsDialog?.findViewById<TextView>(R.id.dialog_edit_tags_delete)

        addTagTextView?.setOnClickListener { addTag() }
        deleteTextView?.setOnClickListener { deleteTags() }
    }

    fun initializeProjectInformationDialog(){
        mProjectInfoDialog = Dialog(requireContext())
        mProjectInfoDialog?.setContentView(R.layout.dialog_project_information)

        // Get Views
        val editTagsTextView = mProjectInfoDialog?.findViewById<TextView>(R.id.dialog_project_info_edit_tags)
        val editNameFab = mProjectInfoDialog?.findViewById<FloatingActionButton>(R.id.edit_project_name_FAB)
        val editScheduleFab = mProjectInfoDialog?.findViewById<FloatingActionButton>(R.id.edit_schedule_FAB)
        val verifyFab = mProjectInfoDialog?.findViewById<FloatingActionButton>(R.id.dialog_verify_fab)

        // Set fab colors
        editNameFab?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
        editScheduleFab?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
        verifyFab?.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorGreen))


        // Set click listeners
        editTagsTextView?.setOnClickListener {
            mEditTagsDialog?.show()
        }
        editNameFab?.setOnClickListener {
            editName()
        }
        editScheduleFab?.setOnClickListener {
            mScheduleDialog?.show()
        }
        verifyFab?.setOnClickListener {
            mProjectInfoDialog?.dismiss()
        }
    }

    fun initializeFullscreenImageDialog() {
        // Create the dialog
        mFullscreenImageDialog = Dialog(requireContext(), R.style.Theme_AppCompat_Light_NoActionBar_FullScreen)
        mFullscreenImageDialog?.setCancelable(false)
        mFullscreenImageDialog?.setContentView(R.layout.fullscreen_image)

        mFullscreenImage = mFullscreenImageDialog?.findViewById(R.id.fullscreen_image)

        // Get the fabs
        val mFullscreenExitFab: FloatingActionButton? = mFullscreenImageDialog?.findViewById(R.id.fullscreen_exit_fab)
        val mFullscreenBackFab: FloatingActionButton? = mFullscreenImageDialog?.findViewById(R.id.fullscreen_back_fab)

        // Display the dialog on clicking the image
        mFullscreenBackFab?.setOnClickListener { mFullscreenImageDialog?.dismiss() }
        mFullscreenExitFab?.setOnClickListener { mFullscreenImageDialog?.dismiss() }

        mFullscreenImageDialog?.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) mFullscreenImageDialog?.dismiss()
            true
        }

        // Set a listener to change the current photo on swipe
        // Note: this may be preferable as a viewpager instead
        mFullscreenImage?.setOnTouchListener(mOnSwipeTouchListener)
    }

    //Shared elements method
    // UI methods
    // TODO (update) implement dual loading solution for smooth transition between fullscreen images
    // Pre loads the selected image into the hidden dialogue so that display appears immediate
    private fun preloadFullscreenImage() {
        if (mCurrentPhoto == null) return
        val path = FileUtils.getPhotoUrl(mExternalFilesDir!!, mCurrentProject!!, mCurrentPhoto!!)
        val current = File(path)
        Glide.with(this)
                .load(current)
                .into(mFullscreenImage!!)
    }

    // Binds project and photos to database
    private fun setupViewModel() {
        // Observe the current selected project
        detailViewModel.currentProject.observe(this, Observer { currentProject: Project ->
            mCurrentProject = currentProject

            // Set id
            binding.projectInformationLayout?.detailsProjectId?.text = mCurrentProject?.project_id.toString()
            val projectInfoDialogId = mProjectInfoDialog?.findViewById<TextView>(R.id.dialog_project_info_id_label)
            projectInfoDialogId?.text = getString(R.string.project_id_label, currentProject.project_id)

            // Set the dialog name
            val projectInfoNameTv = mProjectInfoDialog?.findViewById<TextView>(R.id.dialog_project_info_name)

            // Set name for both the dialog and the project info card view
            val name = mCurrentProject?.project_name
            if (name == null || name.isEmpty()) {
                // Set the layout card view
                binding.projectInformationLayout?.detailsProjectNameTextView?.text = getString(R.string.unnamed)
                binding.projectInformationLayout?.detailsProjectNameTextView
                        ?.setTypeface(binding.projectInformationLayout?.detailsProjectNameTextView?.typeface, Typeface.ITALIC)
                binding.projectInformationLayout?.detailsProjectNameTextView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey))
                // Set the dialog
                projectInfoNameTv?.text = getString(R.string.unnamed)

            }
            else {
                // Set the card view
                binding.projectInformationLayout?.detailsProjectNameTextView?.text = name
                binding.projectInformationLayout?.detailsProjectNameTextView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                binding.projectInformationLayout?.detailsProjectNameTextView
                        ?.setTypeface(binding.projectInformationLayout?.detailsProjectNameTextView?.typeface, Typeface.BOLD)
                // Set the dialog
                projectInfoNameTv?.text = currentProject.project_name
            }

            // Set the dialog schedule information
            if (currentProject.schedule_time == null) {
                mProjectInfoDialog?.findViewById<TextView>(R.id.dialog_edit_schedule_textview_description)?.text = getString(R.string.none)
            } else {
                mProjectInfoDialog?.findViewById<TextView>(R.id.dialog_edit_schedule_textview_description)
                        ?.text = TimeUtils.getDateFromTimestamp(currentProject.schedule_time)
            }

        })

        // Observe the list of photos
        detailViewModel.photos.observe(this, Observer<List<PhotoEntry>> { photoEntries: List<PhotoEntry> ->
            // Save the list of photos
            mPhotos = photoEntries

            // Set the last photo whenever the list of photos changes
            if (mPhotos != null && mPhotos?.size != 0){
                val lastPhotoEntry: PhotoEntry = mPhotos?.get(mPhotos!!.size-1)!!

                // If the last photo in the set changes update to the current photo
                if (lastPhotoEntry.id != detailViewModel.lastPhoto?.photo_id) {
                    detailViewModel.currentPhoto.value = lastPhotoEntry
                }

                // Set the cover photo to the last in the set
                detailViewModel.setCoverPhoto(lastPhotoEntry)

                // Set the last photo
                detailViewModel.setLastPhotoByEntry(mExternalFilesDir!!, mCurrentProject!!, lastPhotoEntry)
            }

            // Send the photos to the adapter
            mDetailAdapter?.setPhotoData(mPhotos, mCurrentProject)

            // Restore the play position
            mCurrentPlayPosition = mPhotos?.indexOf(mCurrentPhoto)

            // Set the date of the project based on the first photo entry
            val firstTimestamp = mPhotos?.get(0)?.timestamp
            val firstProjectDateString = TimeUtils.getShortDateFromTimestamp(firstTimestamp!!)
            val lastTimestamp = mPhotos?.get(mPhotos!!.size - 1)?.timestamp
            val lastProjectDateString = TimeUtils.getShortDateFromTimestamp(lastTimestamp!!)
            binding.projectInformationLayout?.detailsProjectTimespanTextview?.text = getString(R.string.timespan, firstProjectDateString, lastProjectDateString)

            // Set max for progress bar
            binding.imageLoadingProgress.max = mPhotos!!.size - 1

            // If current photo isn't set, set it to the last photo
            if (detailViewModel.currentPhoto.value == null) {
                mCurrentPhoto = mPhotos!!.get(mPhotos!!.size-1)
                detailViewModel.currentPhoto.value = mCurrentPhoto
            }
        })

        // Load the ui based on the current photo
        detailViewModel.currentPhoto.observe(this, Observer { currentPhoto: PhotoEntry? ->
            mCurrentPhoto = currentPhoto
            if (currentPhoto != null) {
                loadUi(currentPhoto)
            }
        })

        // Observe the tags
        detailViewModel.tags.observe(this, Observer<List<ProjectTagEntry>> { tagEntries: List<ProjectTagEntry> ->
            tagJob = detailViewModel.viewModelScope.launch {
                // Update project information dialog
                mTags = detailViewModel.getTags(tagEntries).sortedBy { it.tag }

                val taglayout = mEditTagsDialog?.findViewById<LinearLayout>(R.id.dialog_edit_tags_taglayout)
                taglayout?.removeAllViews()
                var text = ""
                for (tag in mTags!!){
                    // Create the project information display text
                    text = text.plus("#${tag.tag}  ")

                    // Set up views for deleting tags
                    val checkBox = CheckBox(requireContext())
                    checkBox.text = getString(R.string.hashtag, tag.tag)
                    taglayout?.addView(checkBox)

                    checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                        val checkBoxTag = tag.tag
                        if (isChecked){
                            mSelectedTags.add(checkBoxTag)
                        } else {
                            mSelectedTags.remove(checkBoxTag)
                        }
                    }
                }
                val tags = mProjectInfoDialog?.findViewById<TextView>(R.id.dialog_information_tags)
                tags?.text = text

                // Update edit tag dialog
            }
        })
    }

    // Changes photo on swipe
    inner class OnSwipeTouchListener(ctx: Context?) : OnTouchListener {
        private val gestureDetector: GestureDetector
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            return gestureDetector.onTouchEvent(event)
        }

        private inner class GestureListener : SimpleOnGestureListener() {

            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                var result = false
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                        result = true
                    }
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

    private fun deleteTags(){
        Log.d(TAG, mSelectedTags.toString())
        detailViewModel.deleteTags(mSelectedTags, mCurrentProject!!)
        mSelectedTags = arrayListOf() // reset list
    }

    private fun addTag(){
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        AlertDialog.Builder(requireContext())
                .setTitle("Add Tag")
                .setView(input)
                .setPositiveButton(android.R.string.yes) { _, _: Int ->
                    val tagText = input.text.toString()
                    detailViewModel.addTag(tagText, mCurrentProject!!)
                }
                .setNegativeButton(android.R.string.no, null).show()
    }

    private fun editName(){
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        AlertDialog.Builder(requireContext())
                .setTitle("Edit Name")
                .setView(input)
                .setPositiveButton(android.R.string.yes) { _, _: Int ->
                    val nameText = input.text.toString()
                    detailViewModel.updateProjectName(mExternalFilesDir!!, nameText, mCurrentProject!!)
                }
                .setNegativeButton(android.R.string.no, null).show()
    }

    private fun editSchedule(){
        // TODO implement scheduling
    }

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
                    if (mCurrentProject?.schedule_time != null && mCurrentProject?.interval_days != null) {
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