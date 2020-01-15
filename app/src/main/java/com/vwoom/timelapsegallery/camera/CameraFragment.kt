package com.vwoom.timelapsegallery.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Toast
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.TextureViewMeteringPointFactory
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
import com.vwoom.timelapsegallery.databinding.FragmentCameraBinding
import com.vwoom.timelapsegallery.detail.CameraViewModel
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.InjectorUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

// Arbitrary number to keep track of permission request
private const val REQUEST_CODE_PERMISSIONS = 10

// Array of all permissions specified in the manifest
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

// TODO camera x is somehow leaking: hunt it down

class CameraFragment: Fragment(), LifecycleOwner {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private val executor = Executors.newSingleThreadExecutor()
    private var cameraPreview: PreviewView? = null
    private var takePictureJob: Job? = null

    private val args: CameraFragmentArgs by navArgs()

    private val cameraViewModel: CameraViewModel by viewModels {
        InjectorUtils.provideCameraViewModelFactory(requireActivity(), args.photo, args.project)
    }

    private var mTakePictureFab: FloatingActionButton? = null

    private var mCameraSelector: CameraSelector? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentCameraBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

        cameraPreview = binding.cameraPreview
        mTakePictureFab = binding.takePictureFab

        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        // Request camera permissions
        if (allPermissionsGranted()) {
            cameraPreview?.post { startCamera() }
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Loads the last photo from the project into the compare view
        // If a project has been passed as a parameter
        if (cameraViewModel.photo != null){
            val file = File(cameraViewModel.photo?.photo_url!!)
            Glide.with(requireContext())
                    .load(file).into(binding.previousPhoto)
        }

        // TODO: Override perform click for on touch listener
        binding.quickCompareFab.setOnTouchListener { _, event ->
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

        if (args.photo == null) binding.quickCompareFab.hide()

        return binding.root
    }

    override fun onStop() {
        super.onStop()
        takePictureJob?.cancel()
    }

    private fun startCamera() {

        /* TODO determine update preview output for camera X alpha 08
        preview.setOnPreviewOutputUpdateListener {
            // Get all dimensions
            Log.d(TAG, "preview output update listener firing")
            metrics = DisplayMetrics().also { cameraPreview?.display!!.getRealMetrics(it) }
            val previewWidth = metrics.widthPixels
            val previewHeight = metrics.heightPixels
            val width = it.textureSize.width
            val height = it.textureSize.height
            val centerX = cameraPreview?.width!!.toFloat() / 2
            val centerY = cameraPreview?.height!!.toFloat() / 2

            // Get rotation
            val rotation = when (cameraPreview?.display!!.rotation) {
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

            // TODO optimize this transform to match precisely the captured image
            if (rotation!=0)
                matrix.postScale(
                        previewWidth.toFloat() / height,
                        previewHeight.toFloat() / width,
                        centerX,
                        centerY
                )

            // Assign transformation to view
            cameraPreview?.setTransform(matrix)
            cameraPreview?.surfaceTexture = it.surfaceTexture
        }
         */
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
            //cameraProvider.bindToLifecycle(requireParentFragment(), cameraSelector)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    fun bindPreview(cameraProvider: ProcessCameraProvider) {
        var metrics = DisplayMetrics().also{cameraPreview?.display!!.getRealMetrics(it)}
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)

        // TODO: Implement CameraX on touch focus
        val preview = Preview.Builder().apply {
            setTargetResolution(screenSize)
            setTargetRotation(activity!!.windowManager.defaultDisplay.rotation)
        }.build()

        preview.previewSurfaceProvider = cameraPreview?.previewSurfaceProvider

        val imageCapture = ImageCapture.Builder()
                .apply {
                    setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    setTargetResolution(screenSize)
                    setTargetRotation(activity!!.windowManager.defaultDisplay.rotation)
                }.build()

        setTakePictureFab(imageCapture)

        val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
    }

    fun setTakePictureFab(imageCapture: ImageCapture) {
        mTakePictureFab?.setOnClickListener {
            val externalFilesDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            val file = FileUtils.createTemporaryImageFile(externalFilesDir)
            imageCapture.takePicture(file, executor, object: ImageCapture.OnImageSavedCallback{
                override fun onError(imageCaptureError: Int, message: String, cause: Throwable?) {
                    cameraPreview?.post{ Toast.makeText(context, "Capture failed: $message", Toast.LENGTH_LONG).show()}
                    Log.e(TAG, "Capture Failed: $message")
                }
                override fun onImageSaved(file: File) {
                    cameraPreview?.post{ Toast.makeText(context, "Capture success", Toast.LENGTH_LONG).show()}

                    takePictureJob = cameraViewModel.viewModelScope.launch {
                        async {cameraViewModel.handleFile(file, externalFilesDir)}.await()
                        findNavController().popBackStack()
                    }
                }
            })
        }
    }

    /*
    private fun setUpTapToFocus() {

        cameraPreview?.setOnTouchListener { _, event ->

            if (event.action != MotionEvent.ACTION_UP) {

                return@setOnTouchListener false

            }

            val factory = TextureViewMeteringPointFactory(cameraPreview!!)

            val point = factory.createPoint(event.x, event.y)

            val action = FocusMeteringAction.Builder.from(point).build()

            cameraControl.startFocusAndMetering(action)

            return@setOnTouchListener true

        }
    }*/

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS){
            if (allPermissionsGranted()){
                cameraPreview?.post {
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