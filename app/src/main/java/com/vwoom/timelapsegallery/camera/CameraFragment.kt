package com.vwoom.timelapsegallery.activities

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.*
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.databinding.FragmentCameraBinding
import com.vwoom.timelapsegallery.details.CameraViewModel
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.InjectorUtils
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.File
import java.util.concurrent.Executors

// Arbitrary number to keep track of permission request
private const val REQUEST_CODE_PERMISSIONS = 10

// Array of all permissions specified in the manifest
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class CameraFragment: Fragment(), LifecycleOwner {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView

    private val args: CameraFragmentArgs by navArgs()

    private val cameraViewModel: CameraViewModel by viewModels {
        InjectorUtils.provideCameraViewModelFactory(requireActivity(), args.project)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentCameraBinding>(inflater, R.layout.fragment_camera, container, false).apply {
            // TODO determine if this apply block is necessary
            //viewModel = cameraViewModel
            lifecycleOwner = viewLifecycleOwner
        }

        viewFinder = binding.viewFinder

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                    activity as Activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        return binding.root
    }


    private fun startCamera() {
        var metrics = DisplayMetrics().also{viewFinder.display.getRealMetrics(it)}
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)

        Log.d(TAG, "$metrics")
        Log.d(TAG, "$screenSize")
        Log.d(TAG, "${activity!!.windowManager.defaultDisplay.rotation}")
        Log.d(TAG, "${viewFinder.display.rotation}")

        // TODO: Implement CameraX operations
        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(CameraX.LensFacing.BACK)
            setTargetResolution(screenSize)
            setTargetRotation(activity!!.windowManager.defaultDisplay.rotation)
        }.build()

        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener {
            // Get all dimensions
            metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
            val previewWidth = metrics.widthPixels
            val previewHeight = metrics.heightPixels
            val width = it.textureSize.width
            val height = it.textureSize.height
            val centerX = viewFinder.width.toFloat() / 2
            val centerY = viewFinder.height.toFloat() / 2

            // Get rotation
            val rotation = when (viewFinder.display.rotation) {
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
            viewFinder.setTransform(matrix)
            viewFinder.surfaceTexture = it.surfaceTexture
        }


        val imageCaptureConfig = ImageCaptureConfig.Builder()
                .apply {
                    setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                    setTargetResolution(screenSize)
                    setTargetRotation(activity!!.windowManager.defaultDisplay.rotation)
                }.build()

        val imageCapture = ImageCapture(imageCaptureConfig)
        activity?.take_picture_fab?.setOnClickListener {
            // TODO handle external files directory better, perhaps as a companion object?
            val externalFilesDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            val file = FileUtils.createTemporaryImageFile(externalFilesDir)

            imageCapture.takePicture(file, executor,
                    object: ImageCapture.OnImageSavedListener{
                        override fun onError(
                                imageCaptureError: ImageCapture.ImageCaptureError,
                                message: String,
                                cause: Throwable?) {
                            viewFinder.post{ Toast.makeText(context, "Capture failed: $message", Toast.LENGTH_LONG).show()}
                            // TODO error log
                            Log.e("Camera Activity", "Capture Failed: $message")
                        }

                        override fun onImageSaved(file: File) {
                            viewFinder.post{ Toast.makeText(context, "Capture success", Toast.LENGTH_LONG).show()}
                            cameraViewModel.handleFile(file, externalFilesDir)
                            findNavController().popBackStack()
                        }
                    })

        }

        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS){
            if (allPermissionsGranted()){
                viewFinder.post { startCamera() }
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