package com.vwoom.timelapsegallery.details

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Parcelable
import android.util.Log
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.activities.AddPhotoActivity
import com.vwoom.timelapsegallery.activities.NewProjectActivity
import com.vwoom.timelapsegallery.data.AppExecutors.Companion.instance
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.TimeLapseDatabase.Companion.getInstance
import com.vwoom.timelapsegallery.data.entry.CoverPhotoEntry
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.data.view.Project
import com.vwoom.timelapsegallery.notification.NotificationUtils
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.Keys
import com.vwoom.timelapsegallery.utils.PhotoUtils
import com.vwoom.timelapsegallery.utils.TimeUtils
import com.vwoom.timelapsegallery.widget.UpdateWidgetService
import java.io.File

class DetailsFragment: Fragment(), DetailsAdapter.DetailsAdapterOnClickHandler {


    // TODO set as bindings
    // Photo Views
    //@BindView(R.id.detail_current_image)
    var mCurrentPhotoImageView: ImageView? = null
    //@BindView(R.id.detail_next_image)
    var mNextPhotoImageView: ImageView? = null
    //@BindView(R.id.details_card_container)
    var mCardView: CardView? = null
    // Progress Indication
    //@BindView(R.id.image_loading_progress)
    var mProgressBar: ProgressBar? = null
    // Recycler View
    //@BindView(R.id.details_recyclerview)
    var mDetailsRecyclerView: RecyclerView? = null
    private var mDetailsAdapter: DetailsAdapter? = null
    // Fabs
    //@BindView(R.id.add_photo_fab)
    var mAddPhotoFab: FloatingActionButton? = null
    //@BindView(R.id.fullscreen_fab)
    var mFullscreenFab: FloatingActionButton? = null
    //@BindView(R.id.play_as_video_fab)
    var mPlayAsVideoFab: FloatingActionButton? = null
    // Photo display
    //@BindView(R.id.details_photo_date_tv)
    var mPhotoDateTv: TextView? = null
    //@BindView(R.id.details_photo_number_tv)
    var mPhotoNumberTv: TextView? = null
    //@BindView(R.id.details_photo_time_tv)
    var mPhotoTimeTv: TextView? = null
    // Project display
    //@BindView(R.id.details_project_timespan_textview)
    var mProjectTimespanTv: TextView? = null
    //@BindView(R.id.details_project_name_text_view)
    var mProjectNameTextView: TextView? = null

    // Database
    private var mTimeLapseDatabase: TimeLapseDatabase? = null
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
    private var mPlayHandler: Handler? = null
    private var mImageIsLoaded = false

    // Swipe listener for image navigation
    private var mOnSwipeTouchListener: OnSwipeTouchListener? = null

    private val TAG = DetailsActivity::class.java.simpleName

    private val REQUEST_ADD_PHOTO = 1

    // Analytics
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val toolbar: Toolbar = findViewById<Toolbar>(R.id.details_activity_toolbar)
        setSupportActionBar(toolbar)

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true)
            getSupportActionBar().setDisplayShowHomeEnabled(true)
        }

        // Set the title of the activity
        // Set the title of the activity
        setTitle(resources.getString(R.string.project_details))

        // Get the project information from the intent
        // Get the project information from the intent
        mCurrentProject = getIntent().getParcelableExtra(Keys.PROJECT_ENTRY)
        mProjectSchedule = getIntent().getParcelableExtra(Keys.PROJECT_SCHEDULE_ENTRY)

        // Get the database
        // Get the database
        mTimeLapseDatabase = getInstance(this)
        mExternalFilesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // Set up adapter and recycler view
        // Set up adapter and recycler view
        mDetailsAdapter = DetailsAdapter(this, this)
        val linearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        //linearLayoutManager.setStackFromEnd(true);
        //linearLayoutManager.setStackFromEnd(true);
        mDetailsRecyclerView!!.layoutManager = linearLayoutManager
        mDetailsRecyclerView!!.adapter = mDetailsAdapter

        // Set the listener to add a photo to the project
        // Set the listener to add a photo to the project
        mAddPhotoFab!!.setOnClickListener { v: View? ->
            val addPhotoIntent = Intent(this@DetailsActivity, AddPhotoActivity::class.java)
            val lastPhoto: PhotoEntry = getLastPhoto()
            val lastPhotoPath = FileUtils.getPhotoUrl(mExternalFilesDir, mCurrentProject, lastPhoto)
            // Send the path of the last photo and the project id
            addPhotoIntent.putExtra(Keys.PHOTO_PATH, lastPhotoPath)
            addPhotoIntent.putExtra(Keys.PROJECT_ENTRY, mCurrentProject)
            // Start add photo activity for result
            startActivityForResult(addPhotoIntent, REQUEST_ADD_PHOTO)
        }

        // Show the set of images in succession
        // Show the set of images in succession
        mPlayAsVideoFab!!.setOnClickListener { v: View? -> playSetOfImages() }

        // Set a listener to display the image fullscreen
        // Set a listener to display the image fullscreen
        mFullscreenFab!!.setOnClickListener { v: View? -> if (!mPlaying) mFullscreenImageDialog!!.show() }

        // Set a swipe listener for the image
        // Set a swipe listener for the image
        mOnSwipeTouchListener = OnSwipeTouchListener(this)
        mCurrentPhotoImageView!!.setOnTouchListener(mOnSwipeTouchListener)

        if (getIntent() != null) {
            mReturnPosition = getIntent().getIntExtra(Keys.TRANSITION_POSITION, 0)
        }

        // TODO (update) implement pinch zoom on fullscreen image
        // TODO (update) implement pinch zoom on fullscreen image
        initializeFullscreenImageDialog()

        // If restoring state reload the selected photo
        // If restoring state reload the selected photo
        if (savedInstanceState != null) {
            mCurrentPhoto = savedInstanceState.getParcelable<Parcelable>(Keys.PHOTO_ENTRY)
            mReturnPosition = savedInstanceState.getInt(Keys.TRANSITION_POSITION)
            mTransitioned = true
            showPhotoInformation()
            val dialogShowing = savedInstanceState.getBoolean(KEY_DIALOG, false)
            if (dialogShowing) {
                preloadFullscreenImage()
                mFullscreenImageDialog!!.show()
            }
        } else {
            postponeEnterTransition()
        }

        // Initialize fab color
        // Initialize fab color
        mPlayAsVideoFab!!.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@DetailsActivity, R.color.colorGreen))
        mPlayAsVideoFab!!.rippleColor = resources.getColor(R.color.colorGreen)

        // Set the transition name for the image
        // Set the transition name for the image
        val transitionName: String = mCurrentProject.getProject_id() + mCurrentProject.getProject_name()
        mCardView!!.transitionName = transitionName

        setupViewModel(savedInstanceState)

        // Prepare analytics
        // Prepare analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
    }

    override fun onPause() {
        super.onPause()
        // If the activity stops while playing make sure to cancel runnables
        if (mPlaying) stopPlaying()
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
        val photoInformationLayout: LinearLayout = findViewById<LinearLayout>(R.id.photo_information_layout)
        val gradientOverlay: View = findViewById<View>(R.id.details_gradient_overlay)
        val shortAnimationDuration = resources.getInteger(
                android.R.integer.config_shortAnimTime).toLong()
        mFullscreenFab!!.show()
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


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(Keys.PROJECT, mCurrentProject)
        outState.putInt(Keys.TRANSITION_POSITION, mReturnPosition)
        outState.putBoolean(KEY_DIALOG, mFullscreenImageDialog!!.isShowing)
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


    fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = getMenuInflater()
        inflater.inflate(R.menu.details_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                val intent = Intent(this@DetailsActivity, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.delete_photo -> {
                if (mPhotos!!.size() === 1) {
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
            R.id.edit_project -> {
                editProject()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    //Shared elements method
// UI methods
// Update the UI
    fun loadUi(photoEntry: PhotoEntry) { // Set the fullscreen image dialogue to the current photo
        if (!mPlaying) preloadFullscreenImage()
        // Notify the adapter
        mDetailsAdapter!!.setCurrentPhoto(photoEntry)
        // Load the current image
        loadImage(FileUtils.getPhotoUrl(mExternalFilesDir, mCurrentProject, photoEntry))
        // Get info for the current photo
        val timestamp = photoEntry.timestamp
        val photoNumber = mPhotos!!.indexOf(photoEntry) + 1
        val photosInProject: Int = mPhotos.size()
        // Get formatted strings
        val photoNumberString = getString(R.string.details_photo_number_out_of, photoNumber, photosInProject)
        val date = TimeUtils.getDateFromTimestamp(timestamp)
        val time = TimeUtils.getTimeFromTimestamp(timestamp)
        // Set the info
        mPhotoNumberTv!!.text = photoNumberString
        mPhotoDateTv!!.text = date
        mPhotoTimeTv!!.text = time
        val position = mPhotos.indexOf(photoEntry)
        mDetailsRecyclerView!!.scrollToPosition(position)
        mProgressBar!!.progress = photoNumber - 1
    }

    // Loads an image into the main photo view
    private fun loadImage(imagePath: String) {
        mImageIsLoaded = false
        // Get photo info
        val ratio = PhotoUtils.getAspectRatioFromImagePath(imagePath) ?: return
        val isImageLandscape = PhotoUtils.isLandscape(imagePath)
        // Set cardview constraints depending upon if photo is landscape or portrait
        val layoutParams = mCardView!!.layoutParams
        Log.d(FragmentActivity.TAG, "is landscape = $isImageLandscape")
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
        val constraintLayout: ConstraintLayout = findViewById<ConstraintLayout>(R.id.details_current_image_constraint_layout)
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
                        val toast = Toast.makeText(this@DetailsActivity, getString(R.string.error_loading_image), Toast.LENGTH_SHORT)
                        toast.show()
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any, target: Target<Drawable?>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        mNextPhotoImageView!!.visibility = View.VISIBLE
                        Glide.with(this@DetailsActivity)
                                .load(f)
                                .listener(object : RequestListener<Drawable?> {
                                    override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable?>, isFirstResource: Boolean): Boolean {
                                        schedulePostponedTransition()
                                        val toast = Toast.makeText(this@DetailsActivity, getString(R.string.error_loading_image), Toast.LENGTH_SHORT)
                                        toast.show()
                                        return false
                                    }

                                    override fun onResourceReady(resource: Drawable?, model: Any, target: Target<Drawable?>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                                        schedulePostponedTransition()
                                        mNextPhotoImageView!!.visibility = View.INVISIBLE
                                        mImageIsLoaded = true
                                        return false
                                    }
                                })
                                .into(mCurrentPhotoImageView)
                        return false
                    }
                })
                .into(mNextPhotoImageView!!)
    }

    // Loads the set of images concurrently
    private fun playSetOfImages() { // Lazy Initialize handler
        if (mPlayHandler == null) mPlayHandler = Handler()
        if (mCurrentPlayPosition == null) mCurrentPlayPosition = mPhotos!!.indexOf(mCurrentPhoto)
        // If not enough photos give user feedback
        if (mPhotos!!.size() <= 1) {
            Snackbar.make(findViewById(R.id.details_coordinator_layout), R.string.add_more_photos,
                    Snackbar.LENGTH_LONG)
                    .show()
            return
        }
        // If already playing cancel
        if (mPlaying) {
            stopPlaying()
            // Handle UI
            loadUi(mCurrentPhoto!!)
            // Track stop event
            mFirebaseAnalytics!!.logEvent(getString(R.string.analytics_stop_time_lapse), null)
        } else {
            mFullscreenFab!!.hide()
            // Set color of play fab
            mPlayAsVideoFab!!.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@DetailsActivity, R.color.colorRedAccent))
            mPlayAsVideoFab!!.rippleColor = resources.getColor(R.color.colorRedAccent)
            // Set paying true
            mPlaying = true
            // Handle UI
            mPlayAsVideoFab!!.setImageResource(R.drawable.ic_stop_white_24dp)
            // Schedule the runnable for a certain number of ms from now
            val pref = PreferenceManager.getDefaultSharedPreferences(this)
            val playbackIntervalSharedPref = pref.getString(getString(R.string.key_playback_interval), "50")
            val playbackInterval = playbackIntervalSharedPref!!.toInt()
            // If the play position / current photo is at the end, start from the beginning
            if (mCurrentPlayPosition === mPhotos.size() - 1) {
                mCurrentPlayPosition = 0
            } // Otherwise start from wherever it is at
            loadUi(mPhotos[mCurrentPlayPosition])
            mProgressBar!!.progress = mCurrentPlayPosition
            scheduleLoadPhoto(mCurrentPlayPosition, playbackInterval) // Recursively loads the rest of set from beginning
            // Track play button interaction
            mFirebaseAnalytics!!.logEvent(getString(R.string.analytics_play_time_lapse), null)
        }
    }

    // Resets the UI & handles state after playing
    private fun stopPlaying() { // Set color of play fab
        mPlayAsVideoFab!!.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@DetailsActivity, R.color.colorGreen))
        mPlayAsVideoFab!!.rippleColor = resources.getColor(R.color.colorGreen)
        // Set playing false and cancel runnable
        mPlaying = false
        mPlayHandler!!.removeCallbacksAndMessages(null)
        mPlayAsVideoFab!!.setImageResource(R.drawable.ic_play_arrow_white_24dp)
        // Set current photo to position set from playing
        mCurrentPhoto = mPhotos!![mCurrentPlayPosition!!]
        mFullscreenFab!!.show()
    }

    private fun scheduleLoadPhoto(position: Int, interval: Int) {
        mCurrentPlayPosition = position
        val runnable = label@ Runnable {
            // If the position is final position clean up
            if (position == mPhotos!!.size() - 1) {
                mPlaying = false
                mPlayAsVideoFab!!.setImageResource(R.drawable.ic_play_arrow_white_24dp)
                mCurrentPhoto = mPhotos[position]
                mProgressBar!!.progress = position
                mPlayAsVideoFab!!.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this@DetailsActivity, R.color.colorGreen))
                mPlayAsVideoFab!!.rippleColor = resources.getColor(R.color.colorGreen)
                mFullscreenFab!!.show()
                return@label
            }
            // If image is loaded load the next photo
            if (mImageIsLoaded) {
                loadUi(mPhotos[position + 1])
                mProgressBar!!.progress = position + 1
                scheduleLoadPhoto(position + 1, interval)
            } else {
                scheduleLoadPhoto(position, interval)
            }
        }
        mPlayHandler!!.postDelayed(runnable, interval.toLong())
    }

    // Sets the current entry to the clicked photo and loads the image from the entry
    fun onClick(clickedPhoto: PhotoEntry) {
        mCurrentPhoto = clickedPhoto
        mCurrentPlayPosition = mPhotos!!.indexOf(mCurrentPhoto)
        loadUi(mCurrentPhoto!!)
    }

    fun initializeFullscreenImageDialog(){
        // Create the dialog
        // Create the dialog
        mFullscreenImageDialog = Dialog(this, R.style.Theme_AppCompat_Light_NoActionBar_FullScreen)
        mFullscreenImageDialog.setCancelable(false)
        mFullscreenImageDialog.setContentView(R.layout.fullscreen_image)

        mFullscreenImage = mFullscreenImageDialog.findViewById(R.id.fullscreen_image)

        // Get the fabs
        // Get the fabs
        val mFullscreenExitFab: FloatingActionButton = mFullscreenImageDialog.findViewById(R.id.fullscreen_exit_fab)
        val mFullscreenBackFab: FloatingActionButton = mFullscreenImageDialog.findViewById(R.id.fullscreen_back_fab)

        // Display the dialog on clicking the image
        // Display the dialog on clicking the image
        mFullscreenBackFab.setOnClickListener { v: View? -> mFullscreenImageDialog.dismiss() }
        mFullscreenExitFab.setOnClickListener { v: View? -> mFullscreenImageDialog.dismiss() }

        mFullscreenImageDialog.setOnKeyListener {
            dialogInterface, keyCode, event -> if (keyCode == KeyEvent.KEYCODE_BACK) mFullscreenImageDialog.dismiss()
        }

        // Set a listener to change the current photo on swipe
        // Note: this may be preferable as a viewpager instead
        // Set a listener to change the current photo on swipe
// Note: this may be preferable as a viewpager instead
        mFullscreenImage.setOnTouchListener(mOnSwipeTouchListener)
    }

    //Shared elements method
// UI methods
// TODO (update) implement dual loading solution for smooth transition between fullscreen images
// Pre loads the selected image into the hidden dialogue so that display appears immediate
    private fun preloadFullscreenImage() {
        val path = FileUtils.getPhotoUrl(mExternalFilesDir, mCurrentProject, mCurrentPhoto)
        val current = File(path)
        Glide.with(this)
                .load(current)
                .into(mFullscreenImage!!)
    }

    // Binds project and photos to database
    private fun setupViewModel(savedInstanceState: Bundle?) { //DetailsViewModelFactory factory = new DetailsViewModelFactory(mTimeLapseDatabase, mCurrentProject.getProject_id());
        val factory: DetailsViewModelFactory? = null // TODO fix this
        val viewModel = ViewModelProviders.of(this, factory)
                .get(DetailsViewModel::class.java)
        // Observe the list of photos
        viewModel.photos.observe(this, Observer<List<PhotoEntry?>> { photoEntries: List<PhotoEntry?> ->
            // Save the list of photos
            mPhotos = photoEntries
            // Send the photos to the adapter
            mDetailsAdapter!!.setPhotoData(mPhotos, mCurrentProject)
            // Set current photo to last if none has been selected
            if (savedInstanceState == null || mCurrentPhoto == null) mCurrentPhoto = getLastPhoto()
            // Restore the play position
            mCurrentPlayPosition = mPhotos.indexOf(mCurrentPhoto)
            // Load the ui based on the current photo
            loadUi(mCurrentPhoto!!)
            // Set the date of the project based on the first photo entry
            val (_, firstTimestamp) = mPhotos.get(0)
            val firstProjectDateString = TimeUtils.getShortDateFromTimestamp(firstTimestamp)
            val (_, lastTimestamp) = mPhotos.get(mPhotos.size() - 1)
            val lastProjectDateString = TimeUtils.getShortDateFromTimestamp(lastTimestamp)
            mProjectTimespanTv!!.text = getString(R.string.timespan, firstProjectDateString, lastProjectDateString)
            // Set max for progress bar
            mProgressBar!!.max = mPhotos.size() - 1
        })
        // Observe the current selected project
// Note: this ensures that project data is updated correctly when editing
        viewModel.currentProject.observe(this, Observer { currentProject: Project ->
            mCurrentProject = currentProject
            // mCurrentProject will be null upon deletion
// So when deleting a project the viewmodel attempted to updated a null project causing a crash
// This prevents crashes from occurring
            if (mCurrentProject != null) { // Set project info
                mProjectNameTextView.setText(mCurrentProject.getProject_name())
            }
        })
    }


    //Shared elements method
// UI methods
// Changes photo on swipe
    class OnSwipeTouchListener(ctx: Context?) : OnTouchListener {
        private val gestureDetector: GestureDetector
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            return gestureDetector.onTouchEvent(event)
        }

        private inner class GestureListener : SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                var result = false
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > Companion.SWIPE_THRESHOLD && Math.abs(velocityX) > Companion.SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                        result = true
                    }
                } else if (Math.abs(diffY) > Companion.SWIPE_THRESHOLD && Math.abs(velocityY) > Companion.SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom()
                    } else {
                        onSwipeTop()
                    }
                    result = true
                }
                return result
            }

            companion object {
                private const val SWIPE_THRESHOLD = 100
                private const val SWIPE_VELOCITY_THRESHOLD = 100
            }
        }

        fun onSwipeRight() { // Do nothing if currently playing
            if (mPlaying) return
            // Otherwise adjust the current photo to the next
            val currentIndex: Int = mPhotos.indexOf(mCurrentPhoto)
            if (currentIndex == 0) return
            mCurrentPhoto = mPhotos.get(currentIndex - 1)
            mCurrentPlayPosition = mPhotos.indexOf(mCurrentPhoto)
            loadUi(mCurrentPhoto)
        }

        fun onSwipeLeft() { // Do nothing if currently playing
            if (mPlaying) return
            // Otherwise adjust the current photo to the previous
            val currentIndex: Int = mPhotos.indexOf(mCurrentPhoto)
            if (currentIndex == mPhotos.size() - 1) return
            mCurrentPhoto = mPhotos.get(currentIndex + 1)
            mCurrentPlayPosition = mPhotos.indexOf(mCurrentPhoto)
            loadUi(mCurrentPhoto)
        }

        fun onSwipeTop() {}
        fun onSwipeBottom() {}

        init {
            gestureDetector = GestureDetector(ctx, GestureListener())
        }
    }

    //Shared elements method
// UI methods
//Photo management
// Returns the last photo
    private fun getLastPhoto(): PhotoEntry? {
        return mPhotos!![mPhotos!!.size() - 1]
    }

    //Deletes the current photo
    private fun deletePhoto(database: TimeLapseDatabase, project: Project, photoEntry: PhotoEntry) {
        instance!!.diskIO().execute {
            // Delete the photo from the file structure
            FileUtils.deletePhoto(mExternalFilesDir, project, photoEntry)
            // Delete the photo metadata in the database
            database.photoDao().deletePhoto(photoEntry)
        }
        // Send to analytics
        mFirebaseAnalytics!!.logEvent(getString(R.string.analytics_delete_photo), null)
    }

    // management


    // management
// Deletes the project and recursively deletes files from project folder
    private fun deleteProject(database: TimeLapseDatabase, project: Project) { // Delete project from the database and photos from the file structure
        instance!!.diskIO().execute {
            val projectEntry = database.projectDao().loadProjectById(project.project_id)
            // Delete the photos from the file structure
            FileUtils.deleteProject(this@DetailsActivity, projectEntry)
            val (_, schedule_time, interval_days) = database.projectScheduleDao().loadScheduleByProjectId(projectEntry!!.id)
            // Delete the project from the database
//TODO delete project :: database.projectDao().deleteProject(projectEntry);
// If project had a schedule ensure widget and notification worker are updated
            if (schedule_time != null && interval_days != null) {
                NotificationUtils.scheduleNotificationWorker(this)
                UpdateWidgetService.startActionUpdateWidgets(this)
            }
        }
        // Send to analytics
        val params = Bundle()
        params.putString("project_name", project.project_name)
        mFirebaseAnalytics!!.logEvent(getString(R.string.analytics_delete_project), null)
    }

    // Gets the last photo from the set and sets it as the project thumbnail
    private fun updateProjectThumbnail(database: TimeLapseDatabase, project: Project, photo: PhotoEntry) {
        instance!!.diskIO().execute {
            val coverPhotoEntry = CoverPhotoEntry(project.project_id, photo.id)
            database.coverPhotoDao().insertPhoto(coverPhotoEntry)
        }
    }

    // TODO (update) recall position after editing project
//Edits the current project
    private fun editProject() {
        val intent = Intent(this, NewProjectActivity::class.java)
        intent.putExtra(Keys.PROJECT_ENTRY, mCurrentProject)
        startActivity(intent)
    }

    //Shared elements method
// UI methods
//Photo management
//Verification
// Deletes the current photo after user verification
    private fun verifyPhotoDeletion() {
        AlertDialog.Builder(this)
                .setTitle(R.string.delete_photo)
                .setMessage(R.string.verify_delete_photo)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { dialogInterface: DialogInterface?, i: Int ->
                    // If this photo is the last photo then set the new thumbnail to its previous
                    if (mCurrentPhoto!!.equals(getLastPhoto())) {
                        val newLast = mPhotos!![mPhotos!!.size() - 2]
                        updateProjectThumbnail(mTimeLapseDatabase!!, mCurrentProject!!, newLast)
                    }
                    // Store the entry then nullify the current photo
                    val photoToDelete = mCurrentPhoto!!
                    mCurrentPhoto = null
                    // Delete the photo
                    deletePhoto(mTimeLapseDatabase!!, mCurrentProject!!, photoToDelete)
                }
                .setNegativeButton(android.R.string.no, null).show()
    }

    // If the project has only one photo left deletes the project after verification
    private fun verifyLastPhotoDeletion() {
        AlertDialog.Builder(this)
                .setTitle(R.string.delete_photo)
                .setMessage(R.string.verify_delete_last_photo)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { dialogInterface: DialogInterface?, i: Int -> verifyProjectDeletion() }
                .setNegativeButton(android.R.string.no, null).show()
    }

    // Deletes the current project after user verification
    private fun verifyProjectDeletion() {
        AlertDialog.Builder(this)
                .setTitle(R.string.delete_project)
                .setMessage(R.string.verify_delete_project)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { dialogInterface: DialogInterface?, i: Int -> doubleVerifyProjectDeletion() }
                .setNegativeButton(android.R.string.no, null).show()
    }

    // Double verifies project deletion
    private fun doubleVerifyProjectDeletion() {
        AlertDialog.Builder(this)
                .setTitle(R.string.delete_project)
                .setMessage(R.string.double_verify_project_deletion)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes) { dialogInterface: DialogInterface?, i: Int ->
                    deleteProject(mTimeLapseDatabase!!, mCurrentProject!!)
                    if (mProjectSchedule != null) NotificationUtils.scheduleNotificationWorker(this)
                    finish()
                }
                .setNegativeButton(android.R.string.no, null).show()
    }

}