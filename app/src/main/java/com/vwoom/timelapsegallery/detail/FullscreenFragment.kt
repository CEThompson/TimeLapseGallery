package com.vwoom.timelapsegallery.detail

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.snackbar.Snackbar
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.databinding.FragmentFullscreenBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.properties.Delegates

class FullscreenFragment : Fragment() {

    private val args: FullscreenFragmentArgs by navArgs()
    private var position by Delegates.notNull<Int>()
    private lateinit var photos: Array<String>
    private var binding: FragmentFullscreenBinding? = null
    private var imageIsLoaded = false
    private var playJob: Job? = null

    private var playing: Boolean = false

    private lateinit var photoFiles: List<File>

    private var playbackInterval by Delegates.notNull<Long>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        position = args.position
        photos = args.photoUrls

        val arrayList = arrayListOf<File>()
        for (url in photos) {
            arrayList.add(File(url))
        }
        photoFiles = arrayList.toList()

        val pref = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val playbackIntervalSharedPref = pref.getString(getString(R.string.key_playback_interval), getString(R.string.playback_interval_default))
        playbackInterval = playbackIntervalSharedPref?.toLong() ?: 50

        val sharedElemTransition = TransitionInflater.from(context).inflateTransition(R.transition.fullscreen_shared_elem_transition)
        sharedElementEnterTransition = sharedElemTransition
        sharedElementReturnTransition = sharedElemTransition
        postponeEnterTransition()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFullscreenBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeFullscreenFragment()
    }

    private fun initializeFullscreenFragment() {
        // Create the dialog
        // Init color of play fab
        binding?.fullscreenPlayFab?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorGreen))
        binding?.fullscreenPlayFab?.rippleColor = ContextCompat.getColor(requireContext(), R.color.colorGreen)
        // Display the dialog on clicking the image
        binding?.fullscreenPlayFab?.setOnClickListener { playSetOfImages() }
        binding?.fullscreenBackFab?.setOnClickListener {
            findNavController().popBackStack()
        }
        binding?.fullscreenExitFab?.setOnClickListener {
            findNavController().popBackStack()
        }

        // Set a listener to change the current photo on swipe
        val swipeListener = OnSwipeTouchListener(requireContext(), { previousPhoto() }, { nextPhoto() })
        // TODO handle clickable view accessibility
        @Suppress("ClickableViewAccessibility")
        binding?.fullscreenImageBottom?.setOnTouchListener(swipeListener)
        loadImagePair()
    }

    // Loads the current position seamlessly
    private fun loadImagePair() {
        val f = photoFiles[position]
        val bottomImage = binding!!.fullscreenImageBottom
        val topImage = binding!!.fullscreenImageTop
        imageIsLoaded = false

        binding?.fullscreenPositionTextview?.text = getString(R.string.details_photo_number_out_of, position+1,photos.size)

        // 1. The first glide call: First load the image into the next image view on top of the current
        Glide.with(this)
                .load(photos[position])
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
                        topImage.visibility = View.VISIBLE
                        // 2. The second glide call: Then load the image into the current image on the bottom
                        Glide.with(requireContext())
                                .load(f)
                                .listener(object : RequestListener<Drawable?> {
                                    override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable?>, isFirstResource: Boolean): Boolean {
                                        val toast = Toast.makeText(requireContext(), getString(R.string.error_loading_image), Toast.LENGTH_SHORT)
                                        toast.show()
                                        startPostponedEnterTransition()
                                        return false
                                    }

                                    override fun onResourceReady(resource: Drawable?, model: Any, target: Target<Drawable?>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                                        // When complete hide the top image
                                        topImage.visibility = View.INVISIBLE
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

    private fun playSetOfImages() {
        // If already playing then stop
        if (playing) {
            stopPlaying()
            return
        }

        // If not enough photos give user feedback
        if (photos.size <= 1) {
            Snackbar.make(binding!!.fullscreenImageBottom, R.string.add_more_photos,
                    Snackbar.LENGTH_LONG)
                    .show()
            return
        }

        // Handle play state
        playing = true
        // Handle UI
        setFabStatePlaying()
        // Override the play position to beginning if currently already at the end
        if (position >= photos.size - 1) {
            position = 0
        }

        // Schedule the recursive sequence
        scheduleLoadPhoto() // Recursively loads the rest of set from beginning
    }

    private fun scheduleLoadPhoto() {
        if (position < 0 || position >= photos.size) {
            position = photos.size - 1
            stopPlaying()
            return
        }

        playJob = lifecycleScope.launch {
            delay(playbackInterval)
            // If image is loaded load the next photo
            if (imageIsLoaded) {
                loadImagePair()
                position++
                scheduleLoadPhoto()
            }
            // Otherwise check again after the interval
            else {
                scheduleLoadPhoto()
            }
        }
    }

    private fun stopPlaying() { // Set color of play fab
        playJob?.cancel()
        playing = false
        setFabStateStopped()
        loadImagePair()
    }

    // Sets the two play buttons to red with a stop icon
    private fun setFabStatePlaying() {
        val fullscreenPlayFab = binding?.fullscreenPlayFab
        fullscreenPlayFab?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorRedAccent))
        fullscreenPlayFab?.rippleColor = ContextCompat.getColor(requireContext(), R.color.colorRedAccent)
        fullscreenPlayFab?.setImageResource(R.drawable.ic_stop_white_24dp)
    }

    // Sets the two play buttons to green with a play icon
    private fun setFabStateStopped() {
        val fullscreenPlayFab = binding?.fullscreenPlayFab
        fullscreenPlayFab?.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorGreen))
        fullscreenPlayFab?.rippleColor = ContextCompat.getColor(requireContext(), R.color.colorGreen)
        fullscreenPlayFab?.setImageResource(R.drawable.ic_play_arrow_white_24dp)
    }

    private fun nextPhoto() {
        if (playing) return
        if (position == photos.size - 1) return
        position++
        loadImagePair()
    }

    private fun previousPhoto() {
        if (playing) return
        if (position == 0) return
        position--
        loadImagePair()
    }

}