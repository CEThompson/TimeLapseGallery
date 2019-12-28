package com.vwoom.timelapsegallery.details

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.transition.TransitionInflater
import android.util.Log
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
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
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.databinding.FragmentDetailsBinding
import com.vwoom.timelapsegallery.utils.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class DetailsFragment : Fragment(), DetailsAdapter.DetailsAdapterOnClickHandler {

    lateinit var binding: FragmentDetailsBinding

    private var mDetailsAdapter: DetailsAdapter? = null

    // Database
    private var mExternalFilesDir: File? = null

    // Photo and project Information
    private var mPhotos: List<PhotoEntry>? = null
    private var mCurrentPhoto: PhotoEntry? = null
    private var mCurrentPlayPosition: Int? = null
    private var mCurrentProject: Project? = null
    private var mProjectSchedule: ProjectScheduleEntry? = null

    // Views for fullscreen dialog
    private var mFullscreenImageDialog: Dialog? = null
    private var mFullscreenImage: ImageView? = null

    private val KEY_DIALOG = "fullscreen_dialog"

    // For playing timelapse
    private var mPlaying = false
    private var mImageIsLoaded = false

    // Swipe listener for image navigation
    private var mOnSwipeTouchListener: OnSwipeTouchListener? = null

    private val args: DetailsFragmentArgs by navArgs()

    private val detailsViewModel: DetailsViewModel by viewModels {
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

        postponeEnterTransition()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.detailsFragmentToolbar.setNavigationOnClickListener(null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        // TODO determine if this apply block is necessary
        binding = DataBindingUtil.inflate<FragmentDetailsBinding>(inflater, R.layout.fragment_details, container, false).apply {
            //viewModel = cameraViewModel
            lifecycleOwner = viewLifecycleOwner
        }

        // Set up toolbar
        setHasOptionsMenu(true)
        val toolbar = binding.detailsFragmentToolbar
        (activity as TimeLapseGalleryActivity).setSupportActionBar(toolbar)
        toolbar?.title = getString(R.string.project_details)
        (activity as TimeLapseGalleryActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // Set up adapter and recycler view
        mDetailsAdapter = DetailsAdapter(this, requireContext())
        val linearLayoutManager
                = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.detailsRecyclerview.layoutManager = linearLayoutManager
        binding.detailsRecyclerview.adapter = mDetailsAdapter

        // Set the listener to add a photo to the project
        binding.addPhotoFab.setOnClickListener {
            val action = DetailsFragmentDirections
                    .actionDetailsFragmentToCameraFragment(detailsViewModel.lastPhoto, mCurrentProject)
            findNavController().navigate(action)
        }

        // Show the set of images in succession
        binding.playAsVideoFab.setOnClickListener {
            playSetOfImages()
        }

        // Set a listener to display the image fullscreen
        binding.fullscreenFab.setOnClickListener { if (!mPlaying) mFullscreenImageDialog?.show() }

        // Set a swipe listener for the image
        mOnSwipeTouchListener = OnSwipeTouchListener(requireContext())
        binding.detailCurrentImage.setOnTouchListener(mOnSwipeTouchListener) // todo override on perform click


        // TODO (update) implement pinch zoom on fullscreen image
        initializeFullscreenImageDialog()

        // Initialize fab color
        binding.playAsVideoFab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorGreen))
        binding.playAsVideoFab.rippleColor = resources.getColor(R.color.colorGreen)

        // Set the transition name for the image
        val imageTransitionName= "${mCurrentProject?.project_id}"
        val cardTransitionName = imageTransitionName + "card"
        binding.detailCurrentImage.transitionName = imageTransitionName
        binding.detailsCardContainer.transitionName = cardTransitionName

        setupViewModel()

        showPhotoInformation()

        return binding.root
    }


    override fun onPause() {
        super.onPause()
        playJob?.cancel()
        // If the activity stops while playing make sure to cancel runnables
        // TODO handle playing
        //if (mPlaying) stopPlaying()
    }

    // TODO handle return from camera fragment
    /*
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_PHOTO && resultCode == Activity.RESULT_OK) { // On successful addition of photo to project update the project thumbnail and check to update schedule
            if (data != null) {
                val resultPhoto: PhotoEntry = data.getParcelableExtra(Keys.PHOTO_ENTRY)
                updateProjectThumbnail(mTimeLapseDatabase, mCurrentProject, resultPhoto)
                // Set the current photo to null, when null the viewmodel will set to last in set
                mCurrentPhoto = null
                if (mProjectSchedule == null) return
                // update the schedule
                val schedule: Int = mProjectSchedule.getInterval_days()
                var next: Long = mProjectSchedule.getSchedule_time()
                // Update if there is a schedule and the timestamp belongs to today or has elapsed
                if (schedule > 0
                        && (DateUtils.isToday(next) || System.currentTimeMillis() > next)) { // update the time
                    next = TimeUtils.getNextScheduledSubmission(next, schedule)
                    // update the database reference
                    val nextTimestampToSubmit = next
                    instance!!.diskIO().execute {
                        mProjectSchedule.setSchedule_time(nextTimestampToSubmit)
                        mTimeLapseDatabase!!.projectScheduleDao().updateProjectSchedule(mProjectSchedule!!)
                    }
                    UpdateWidgetService.startActionUpdateWidgets(this)
                }
            }
        }
    }
    */


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


    /* TODO handle back press
    fun onBackPressed() { // Use this block for hide animation
        val photoInformationLayout: LinearLayout = findViewById<LinearLayout>(R.id.photo_information_layout)
        val gradientOverlay: View = findViewById<View>(R.id.details_gradient_overlay)
        photoInformationLayout.visibility = View.INVISIBLE
        gradientOverlay.visibility = View.INVISIBLE
        mFullscreenFab!!.hide()
        supportFinishAfterTransition()
    }
     */

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.details_fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                val action = DetailsFragmentDirections.actionDetailsFragmentToSettingsFragment()
                findNavController().navigate(action)
                true
            }
            R.id.delete_photo -> {
                // TODO handle photo deletion through viewmodel
                if (mPhotos?.size === 1) {
                    //verifyLastPhotoDeletion()
                } else {
                    //verifyPhotoDeletion()
                }
                true
            }
            R.id.delete_project -> {
                //verifyProjectDeletion()
                true
            }
            R.id.edit_project -> {
                //editProject()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    fun loadUi(photoEntry: PhotoEntry) { // Set the fullscreen image dialogue to the current photo
        if (!mPlaying) preloadFullscreenImage()
        // Notify the adapter
        mDetailsAdapter?.setCurrentPhoto(photoEntry)
        // Load the current image
        loadImage(FileUtils.getPhotoUrl(mExternalFilesDir, mCurrentProject, photoEntry))
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
        val ratio = PhotoUtils.getAspectRatioFromImagePath(imagePath) ?: return
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
                                        //schedulePostponedTransition() TODO schedule transition?
                                        startPostponedEnterTransition()
                                        val toast = Toast.makeText(requireContext(), getString(R.string.error_loading_image), Toast.LENGTH_SHORT)
                                        toast.show()
                                        return false
                                    }

                                    override fun onResourceReady(resource: Drawable?, model: Any, target: Target<Drawable?>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                                        //schedulePostponedTransition() TODO schedule transition?
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


    // Loads the set of images concurrently
    private fun playSetOfImages() { // Lazy Initialize handler
        if (mPlaying) {
            stopPlaying()
            mFirebaseAnalytics!!.logEvent(getString(R.string.analytics_stop_time_lapse), null)
            return
        }

        mCurrentPlayPosition = mPhotos!!.indexOf(mCurrentPhoto)

        // If not enough photos give user feedback
        if (mPhotos!!.size <= 1) {
            Snackbar.make(binding.detailsCoordinatorLayout, R.string.add_more_photos,
                    Snackbar.LENGTH_LONG)
                    .show()
            return
        }

        binding.fullscreenFab.hide()
        // Set color of play fab
        binding.playAsVideoFab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorRedAccent))
        binding.playAsVideoFab.rippleColor = resources.getColor(R.color.colorRedAccent)
        // Set paying true
        mPlaying = true
        // Handle UI
        binding.playAsVideoFab.setImageResource(R.drawable.ic_stop_white_24dp)

        // Schedule the runnable for a certain number of ms from now
        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val playbackIntervalSharedPref = pref.getString(getString(R.string.key_playback_interval), "50")
        val playbackInterval = playbackIntervalSharedPref!!.toLong()

        // If the play position / current photo is at the end, start from the beginning
        if (mCurrentPlayPosition == mPhotos!!.size - 1) {
            mCurrentPlayPosition = 0
            detailsViewModel.setPhoto(mPhotos!![0])
        }

        // Otherwise start from wherever it is at
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
        binding.playAsVideoFab.rippleColor = resources.getColor(R.color.colorGreen)
        binding.playAsVideoFab.setImageResource(R.drawable.ic_play_arrow_white_24dp)
        binding.fullscreenFab.show()
    }

    var playJob: Job? = null

    // TODO convert these runnables into coroutine chain?
    private fun scheduleLoadPhoto(position: Int, interval: Long) {
        Log.e("DetailsFragment", "schedule loading position $position")
        if (position < 0 || position >= mPhotos!!.size) {
            mPlaying = false
            binding.playAsVideoFab.setImageResource(R.drawable.ic_play_arrow_white_24dp)
            binding.imageLoadingProgress.progress = position
            binding.playAsVideoFab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorGreen))
            binding.playAsVideoFab.rippleColor = resources.getColor(R.color.colorGreen)
            binding.fullscreenFab.show()
            return
        }

        mCurrentPlayPosition = position

        playJob = detailsViewModel.viewModelScope.launch {
            delay(interval)
            // If image is loaded load the next photo
            if (mImageIsLoaded) {
                detailsViewModel.nextPhoto()
                binding.imageLoadingProgress.progress = position + 1
                scheduleLoadPhoto(position + 1, interval)
            } else {
                scheduleLoadPhoto(position, interval)
            }
        }
    }

    override fun onClick(clickedPhoto: PhotoEntry) {
        detailsViewModel.setPhoto(clickedPhoto)
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
        mFullscreenBackFab?.setOnClickListener { v: View? -> mFullscreenImageDialog?.dismiss() }
        mFullscreenExitFab?.setOnClickListener { v: View? -> mFullscreenImageDialog?.dismiss() }

        mFullscreenImageDialog?.setOnKeyListener { dialogInterface, keyCode, event ->
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
        val path = FileUtils.getPhotoUrl(mExternalFilesDir, mCurrentProject, mCurrentPhoto)
        val current = File(path)
        Glide.with(this)
                .load(current)
                .into(mFullscreenImage!!)
    }

    // Binds project and photos to database
    private fun setupViewModel() {

        // Observe the current selected project
        detailsViewModel.currentProject.observe(this, Observer { currentProject: Project ->
            mCurrentProject = currentProject

            val name = mCurrentProject?.project_name
            if (name == null)
                binding.detailsProjectNameTextView.setText(mCurrentProject?.project_id.toString())
            else {
                val projectIdentification = getString(R.string.project_identification, mCurrentProject?.project_id, mCurrentProject?.project_name)
                binding.detailsProjectNameTextView.setText(projectIdentification)
            }
        })

        // Observe the list of photos
        detailsViewModel.photos.observe(this, Observer<List<PhotoEntry>> { photoEntries: List<PhotoEntry> ->
            // Save the list of photos
            mPhotos = photoEntries

            // convert the last photoentry in mPhotos into a photo abstraction and store in viewmodel to send when the add photo fab is clicked
            if (mPhotos != null && mPhotos?.size != 0){
                val lastPhotoEntry: PhotoEntry = mPhotos?.get(mPhotos!!.size-1)!!
                detailsViewModel.setLastPhotoByEntry(mExternalFilesDir!!, mCurrentProject!!, lastPhotoEntry)
            }

            // Send the photos to the adapter
            mDetailsAdapter?.setPhotoData(mPhotos, mCurrentProject)

            // Set current photo to last if none has been selected
            //if (mCurrentPhoto == null) mCurrentPhoto = getLastPhoto()

            // Restore the play position
            mCurrentPlayPosition = mPhotos?.indexOf(mCurrentPhoto)

            // Set the date of the project based on the first photo entry
            val firstTimestamp = mPhotos?.get(0)?.timestamp
            val firstProjectDateString = TimeUtils.getShortDateFromTimestamp(firstTimestamp!!)
            val lastTimestamp = mPhotos?.get(mPhotos!!.size - 1)?.timestamp
            val lastProjectDateString = TimeUtils.getShortDateFromTimestamp(lastTimestamp!!)
            binding.detailsProjectTimespanTextview.text = getString(R.string.timespan, firstProjectDateString, lastProjectDateString)

            // Set max for progress bar
            binding.imageLoadingProgress.max = mPhotos!!.size - 1

            // If current isn't set, set it to last
            if (detailsViewModel.currentPhoto.value == null) {
                mCurrentPhoto = mPhotos!!.get(mPhotos!!.size-1)
                detailsViewModel.currentPhoto.value = mCurrentPhoto
            }
        })

        // Load the ui based on the current photo
        detailsViewModel.currentPhoto.observe(this, Observer { currentPhoto: PhotoEntry? ->
            mCurrentPhoto = currentPhoto
            if (currentPhoto != null) {
                loadUi(currentPhoto)
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
        fun onSwipeRight() = detailsViewModel.previousPhoto()
        fun onSwipeLeft() = detailsViewModel.nextPhoto()

        init {
            gestureDetector = GestureDetector(ctx, GestureListener())
        }
    }

    /*
    //Deletes the current photo
    private fun deletePhoto(photoEntry: PhotoEntry) {
        detailsViewModel.deletePhoto(photoEntry)
        mFirebaseAnalytics!!.logEvent(getString(R.string.analytics_delete_photo), null)
    }

    // Deletes the project and recursively deletes files from project folder
    private fun deleteProject(project: Project) { // Delete project from the database and photos from the file structure
        detailsViewModel.deleteProject(project)

        if (project.schedule_time != null && project.interval_days != null) {
            NotificationUtils.scheduleNotificationWorker(requireContext())
            UpdateWidgetService.startActionUpdateWidgets(requireContext())
        }

        // Send to analytics
        val params = Bundle()
        params.putString("project_name", project.project_name)
        mFirebaseAnalytics!!.logEvent(getString(R.string.analytics_delete_project), null)
    }

    // Gets the last photo from the set and sets it as the project thumbnail
    private fun updateProjectThumbnail(photoEntry: PhotoEntry) {
        detailsViewModel.setCoverPhoto(photoEntry)
    }

    //Edits the current project
    private fun editProject() {
        // TODO handle editing project with dialog
    }

    private fun verifyPhotoDeletion() {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_photo)
                .setMessage(R.string.verify_delete_photo)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { dialogInterface: DialogInterface?, i: Int ->
                    // If this photo is the last photo then set the new thumbnail to its previous
                    detailsViewModel.deleteCurrentPhoto()
                }
                .setNegativeButton(android.R.string.no, null).show()
    }

    private fun verifyLastPhotoDeletion() {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_photo)
                .setMessage(R.string.verify_delete_last_photo)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { dialogInterface: DialogInterface?, i: Int -> verifyProjectDeletion() }
                .setNegativeButton(android.R.string.no, null).show()
    }

    // Deletes the current project after user verification
    private fun verifyProjectDeletion() {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_project)
                .setMessage(R.string.verify_delete_project)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { dialogInterface: DialogInterface?, i: Int -> doubleVerifyProjectDeletion() }
                .setNegativeButton(android.R.string.no, null).show()
    }

    // Double verifies project deletion
    private fun doubleVerifyProjectDeletion() {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_project)
                .setMessage(R.string.double_verify_project_deletion)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { dialogInterface: DialogInterface?, i: Int ->
                    detailsViewModel.deleteCurrentProject()
                    NotificationUtils.scheduleNotificationWorker(requireContext())
                }
                .setNegativeButton(android.R.string.no, null).show()
    }
    */

}