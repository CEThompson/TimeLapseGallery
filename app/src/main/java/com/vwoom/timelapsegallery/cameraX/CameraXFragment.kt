package com.vwoom.timelapsegallery.cameraX

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.util.concurrent.ListenableFuture
import com.vwoom.timelapsegallery.databinding.FragmentCameraXBinding
import com.vwoom.timelapsegallery.di.ViewModelFactory
import com.vwoom.timelapsegallery.utils.FileUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject

// Arbitrary number to keep track of permission request
private const val REQUEST_CODE_PERMISSIONS = 10


// Array of all permissions specified in the manifest
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)


// TODO: (deferred) hunt down memory leak
// TODO (deferred): investigate cameraX for possible future usage
class CameraXFragment : Fragment(), LifecycleOwner {

    private var mTakePictureFab: FloatingActionButton? = null
    private var previewView: PreviewView? = null
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private val executor = Executors.newSingleThreadExecutor()
    private var preview: Preview? = null

    private var takePictureJob: Job? = null

    private val args: CameraXFragmentArgs by navArgs()

    //@Inject
    // todo inject viewmodel factory if using camera x
    lateinit var viewModelFactory: ViewModelFactory
    private val cameraViewModel: CameraXViewModel by viewModels {
        viewModelFactory
        //InjectorUtils.provideCameraXViewModelFactory(requireActivity(), args.photo, args.projectView)
    }

    private var mLastClickTime: Long? = null

    override fun onStop() {
        super.onStop()
        takePictureJob?.cancel()
        executor.shutdown()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        @Suppress("ClickableViewAccessibility")
        previewView?.setOnTouchListener(null)
        previewView = null
        preview?.setSurfaceProvider(null)
        preview = null
        mTakePictureFab = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentCameraXBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

        previewView = binding.cameraPreview
        mTakePictureFab = binding.takePictureFab
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        // Request camera permissions & start on success
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        setUi(binding)

        return binding.root
    }

    private fun setUi(binding: FragmentCameraXBinding) {
        // Loads the last photo from a project into the compare view if available
        if (cameraViewModel.photo != null) {
            val file = File(cameraViewModel.photo?.photo_url!!)
            Glide.with(requireContext())
                    .load(file).into(binding.previousPhoto)
        }

        // Set up quick compare function
        @Suppress("ClickableViewAccessibility")
        binding.quickCompareFab.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.previousPhoto.visibility = View.VISIBLE
                    true
                }
                MotionEvent.ACTION_UP -> {
                    binding.previousPhoto.visibility = View.INVISIBLE
                    true
                }
                else -> false
            }
        }

        // If no project photo was passed hide the quick compare
        if (args.photo == null) binding.quickCompareFab.hide()
    }

    private fun startCamera() {
        val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

        preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

        // Build the image capture use case and attach button click listener
        val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()

        mTakePictureFab?.setOnClickListener {
            val externalFilesDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            val photoFile = FileUtils.createTemporaryImageFile(externalFilesDir)

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                    .build()

            imageCapture.takePicture(outputOptions, executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exception: ImageCaptureException) {
                            val msg = "Photo capture failed: ${exception.localizedMessage}"
                            Log.e(TAG, msg, exception)
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        }

                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            takePictureJob = cameraViewModel.viewModelScope.launch {
                                cameraViewModel.handleFile(photoFile, externalFilesDir)
                                findNavController().popBackStack()
                            }
                        }
                    })
        }

        val display = requireActivity().windowManager.defaultDisplay
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture, preview)
            preview?.setSurfaceProvider(previewView?.createSurfaceProvider())
            setUpTapToFocus(display, cameraSelector, camera)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun setUpTapToFocus(display: Display, cameraSelector: CameraSelector, camera: Camera) {
        @Suppress("ClickableViewAccessibility")
        previewView?.setOnTouchListener { _, event ->
            // Only allow metering action once per second
            if (mLastClickTime != null && SystemClock.elapsedRealtime() - mLastClickTime!! < 1000) return@setOnTouchListener false
            mLastClickTime = SystemClock.elapsedRealtime()

            val factory = DisplayOrientedMeteringPointFactory(
                    display,
                    cameraSelector,
                    previewView!!.width.toFloat(), previewView!!.height.toFloat())
            val point = factory.createPoint(event.x, event.y)
            val action = FocusMeteringAction.Builder(point).build()
            camera.cameraControl.startFocusAndMetering(action)
            return@setOnTouchListener true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        val TAG = CameraXFragment::class.java.simpleName
    }
}