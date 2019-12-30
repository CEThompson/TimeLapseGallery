package com.vwoom.timelapsegallery.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.os.Bundle
import android.os.Environment
import android.transition.TransitionInflater
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.databinding.FragmentCameraBinding
import com.vwoom.timelapsegallery.details.CameraViewModel
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.InjectorUtils
import java.io.File
import java.util.concurrent.Executors

// Arbitrary number to keep track of permission request
private const val REQUEST_CODE_PERMISSIONS = 10

// Array of all permissions specified in the manifest
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class CameraFragment: Fragment(), LifecycleOwner {

    private val executor = Executors.newSingleThreadExecutor()
    private var viewFinder: TextureView? = null

    private val args: CameraFragmentArgs by navArgs()

    private val cameraViewModel: CameraViewModel by viewModels {
        InjectorUtils.provideCameraViewModelFactory(requireActivity(), args.photo, args.project)
    }

    private var mTakePictureFab: FloatingActionButton? = null

    private var mPreview: Preview? = null

    override fun onDestroyView() {
        super.onDestroyView()
        mPreview = null
        mTakePictureFab = null
        viewFinder = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentCameraBinding>(inflater, R.layout.fragment_camera, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

        viewFinder = binding.viewFinder
        mTakePictureFab = binding.takePictureFab

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder?.post { startCamera() }
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // TODO set up last project photo
        if (cameraViewModel.photo != null){
            val file = File(cameraViewModel.photo?.photo_url!!)
            Glide.with(requireContext())
                    .load(file).into(binding.previousPhoto)
        }

        binding.quickCompareFab.setOnTouchListener { v, event ->
            when (event.action){
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

        // TODO set up photo verification?
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(R.transition.image_shared_element_transition)
        sharedElementReturnTransition = TransitionInflater.from(context).inflateTransition(R.transition.image_shared_element_transition)

        return binding.root
    }

    override fun onStop() {
        super.onStop()
        mPreview?.removePreviewOutputListener()
    }



    private fun startCamera() {
        var metrics = DisplayMetrics().also{viewFinder?.display!!.getRealMetrics(it)}
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)

        Log.d(TAG, "$metrics")
        Log.d(TAG, "$screenSize")
        Log.d(TAG, "${activity!!.windowManager.defaultDisplay.rotation}")
        Log.d(TAG, "${viewFinder?.display!!.rotation}")

        // TODO: Implement CameraX operations
        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(CameraX.LensFacing.BACK)
            setTargetResolution(screenSize)
            setTargetRotation(activity!!.windowManager.defaultDisplay.rotation)
        }.build()

        mPreview = Preview(previewConfig)
        mPreview?.setOnPreviewOutputUpdateListener {
            // Get all dimensions
            metrics = DisplayMetrics().also { viewFinder?.display!!.getRealMetrics(it) }
            val previewWidth = metrics.widthPixels
            val previewHeight = metrics.heightPixels
            val width = it.textureSize.width
            val height = it.textureSize.height
            val centerX = viewFinder?.width!!.toFloat() / 2
            val centerY = viewFinder?.height!!.toFloat() / 2

            // Get rotation
            val rotation = when (viewFinder?.display!!.rotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> throw IllegalStateException()
            }
            val matrix = Matrix()
            // Rotate matrix
            matrix.postRotate(-rotation.toFloat(), centerX, centerY)
            // Scale matrix
            matrix.postScale(
                    previewWidth.toFloat() / height,
                    previewHeight.toFloat() / width,
                    centerX,
                    centerY
            )
            // Assign transformation to view
            viewFinder?.setTransform(matrix)
            viewFinder?.surfaceTexture = it.surfaceTexture
        }


        val imageCaptureConfig = ImageCaptureConfig.Builder()
                .apply {
                    setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                    setTargetResolution(screenSize)
                    setTargetRotation(activity!!.windowManager.defaultDisplay.rotation)
                }.build()

        val imageCapture = ImageCapture(imageCaptureConfig)
        mTakePictureFab?.setOnClickListener {
            // TODO handle external files directory better, perhaps as a companion object?
            val externalFilesDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            val file = FileUtils.createTemporaryImageFile(externalFilesDir)
            imageCapture.takePicture(file, executor,
                    object: ImageCapture.OnImageSavedListener{
                        override fun onError(
                                imageCaptureError: ImageCapture.ImageCaptureError,
                                message: String,
                                cause: Throwable?) {
                            viewFinder?.post{ Toast.makeText(context, "Capture failed: $message", Toast.LENGTH_LONG).show()}
                            Log.e(TAG, "Capture Failed: $message")
                        }

                        override fun onImageSaved(file: File) {
                            viewFinder?.post{ Toast.makeText(context, "Capture success", Toast.LENGTH_LONG).show()}
                            cameraViewModel.handleFile(file, externalFilesDir)
                            // TODO create job to handle adding a new project or handle adding a photo to a project
                            // TODO navigate back on job finish
                            //findNavController().popBackStack()
                        }
                    })

        }

        CameraX.bindToLifecycle(this, mPreview, imageCapture)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS){
            if (allPermissionsGranted()){
                viewFinder?.post {
                    Toast.makeText(this.requireContext(), "Permissions granted, firing up the camera.", Toast.LENGTH_SHORT).show()
                    startCamera()
                }
            } else {
                Toast.makeText(this.requireContext(), "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        val TAG = CameraFragment::class.java.simpleName
    }
}