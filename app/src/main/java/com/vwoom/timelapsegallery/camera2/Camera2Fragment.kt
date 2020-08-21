package com.vwoom.timelapsegallery.camera2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.camera2.common.AutoFitTextureView
import com.vwoom.timelapsegallery.camera2.common.OrientationLiveData
import com.vwoom.timelapsegallery.camera2.common.getPreviewOutputSize
import com.vwoom.timelapsegallery.databinding.FragmentCamera2Binding
import com.vwoom.timelapsegallery.di.Injectable
import com.vwoom.timelapsegallery.di.ViewModelFactory
import com.vwoom.timelapsegallery.testing.launchIdling
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class Camera2Fragment : Fragment(), LifecycleOwner, Injectable {
    private val args: Camera2FragmentArgs by navArgs()

    // Camera Fields
    private val cameraId: String by lazy { args.cameraId }
    private val cameraManager: CameraManager by lazy {
        requireContext().applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    private val characteristics: CameraCharacteristics by lazy {
        cameraManager.getCameraCharacteristics(cameraId)
    }
    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler: Handler = Handler(cameraThread.looper)
    private lateinit var viewFinder: AutoFitTextureView
    private lateinit var camera: CameraDevice
    private lateinit var session: CameraCaptureSession
    private lateinit var relativeOrientation: OrientationLiveData
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private var previewSize: Size? = null

    // For matching capture image to preview output
    private var baseWidth: Int = 0
    private var baseHeight: Int = 0

    // ViewModel
    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    private val camera2ViewModel: Camera2ViewModel by viewModels {
        viewModelFactory
    }

    // Take picture functionality
    private var takePictureJob: Job? = null
    private var takePictureFab: FloatingActionButton? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentCamera2Binding
                .inflate(inflater, container, false)
                .apply {
                    lifecycleOwner = viewLifecycleOwner
                }
        viewFinder = binding.cameraPreview
        takePictureFab = binding.takePictureFab

        // If a comparison photo is passed as an argument
        // Set a touch listener to show the quick compare photo
        if (args.photo != null) {
            // Load the photo into the compare image view
            val file = File(args.photo?.photo_url!!)
            Glide.with(requireContext())
                    .load(file).into(binding.previousPhoto)

            // Set up quick compare fab to compare current camera input to previous photo
            // TODO: clickable view accessibility for on touch listener
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
        }
        // Otherwise if no project photo was passed hide the quick compare
        else if (args.photo == null) binding.quickCompareFab.hide()

        // Set a click listener to take the photo, either adding it to an existing project or creating a new project
        takePictureFab?.setOnClickListener {
            // Disable the take picture fab
            it.isEnabled = false
            // Launch the picture as an idling resource job
            takePictureJob = lifecycleScope.launchIdling(Dispatchers.IO) {
                var outputPhoto: FileOutputStream? = null
                try {
                    val externalFilesDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                    val file = FileUtils.createTemporaryImageFile(externalFilesDir)
                    outputPhoto = FileOutputStream(file)

                    // Rotates and scales the bitmap based on the device rotation
                    val matrix = getTransformMatrix(baseWidth, baseHeight)
                    val adjustedBitmap = Bitmap.createBitmap(viewFinder.bitmap, 0, 0, viewFinder.bitmap.width, viewFinder.bitmap.height, matrix, true)
                    adjustedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputPhoto)

                    // Write exif data for image
                    val exif = ExifInterface(file.absolutePath)
                    val timestamp = System.currentTimeMillis()
                    val timeString = TimeUtils.getExifDateTimeFromTimestamp(timestamp)
                    exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, timeString)
                    exif.saveAttributes()

                    // Determine if new project
                    val isNewProject = (args.projectView == null)

                    // For new projects navigate to project detail after insertion
                    if (isNewProject) {
                        val newProjectView = camera2ViewModel.addNewProject(file, externalFilesDir, timestamp)
                        val action = Camera2FragmentDirections.actionCamera2FragmentToDetailsFragment(newProjectView)
                        findNavController().navigate(action)
                    }
                    // For existing projects pop back to the project detail after adding the picture
                    else {
                        camera2ViewModel.addPhotoToProject(file, externalFilesDir, timestamp)
                        findNavController().popBackStack()
                    }
                } catch (e: Exception) {
                    viewFinder.post { Toast.makeText(context, "Capture failed: ${e.message}", Toast.LENGTH_LONG).show() }
                    Timber.d("Take picture exception: ${e.message}")
                } finally {
                    try {
                        outputPhoto?.close()
                    } catch (e: Exception) {
                        Timber.d("Take picture exception in finally block, exception closing photo: ${e.message}")
                    }
                }
                // Re-enable the take picture fab
                it.post { it.isEnabled = true }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        camera2ViewModel.setProjectInfo(args.projectView, args.photo)

        // Set up a texture listener which initializes the camera
        viewFinder.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                Timber.d("onSurfaceTextureAvailable called with to width $width and height $height")
                previewSize = getPreviewOutputSize(
                        viewFinder.display,
                        characteristics,
                        SurfaceHolder::class.java)
                Timber.d("previewSize is width ${previewSize!!.width} and height ${previewSize!!.height}")
                viewFinder.setAspectRatio(previewSize!!.width, previewSize!!.height)

                // Transform the viewfinder to device rotation
                transformImage(width, height)

                // Initialize camera in the viewfinders thread so that size is set
                // Note: camera initialization launches in requestPermissions
                viewFinder.post {
                    // First request the permissions, if permissions cleared or granted then camera is initialized
                    // Initialization occurs after request permission result
                    requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
                }
                baseHeight = height
                baseWidth = width
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
                return false
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
                baseHeight = height
                baseWidth = width
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
            }
        }
        // Set live data for phone orientation
        relativeOrientation = OrientationLiveData(requireContext(), characteristics).apply {
            observe(viewLifecycleOwner, { orientation ->
                Timber.d("Orientation changed to $orientation")
            })
        }
    }

    private fun initializeCamera() = lifecycleScope.launchIdling {
        camera = openCamera(cameraManager, cameraId, cameraHandler)

        // Match size of image output to preview size
        previewSize = getPreviewOutputSize(
                viewFinder.display,
                characteristics,
                SurfaceHolder::class.java)

        // Set the surface of the texture view as a target
        val surfaceTexture = viewFinder.surfaceTexture
        surfaceTexture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
        val previewSurface = Surface(surfaceTexture)
        val targets = listOf(previewSurface)
        session = createCaptureSession(camera, targets, cameraHandler)
        captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(previewSurface)
        }

        // Set the touch focus listener on the viewfinder
        // TODO: handle clickable view accessibility for on touch listener
        @Suppress("ClickableViewAccessibility")
        viewFinder.setOnTouchListener(
                CameraFocusOnTouchHandler(
                        characteristics,
                        captureRequestBuilder,
                        session,
                        cameraHandler))

        // Set the initial request.
        // Note: the touch handler sets repeated request as well on touch
        session.setRepeatingRequest(captureRequestBuilder.build(), null, cameraHandler)
    }

    @SuppressLint("MissingPermission")
    private suspend fun openCamera(
            manager: CameraManager,
            cameraId: String,
            handler: Handler? = null
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) = cont.resume(camera)
            override fun onDisconnected(camera: CameraDevice) {
                Toast.makeText(requireContext(), "Error: Camera disconnected.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
                Timber.d("Camera disconnected")
            }

            override fun onError(camera: CameraDevice, error: Int) {
                val msg = when (error) {
                    ERROR_CAMERA_DEVICE -> "Fatal (device"
                    ERROR_CAMERA_DISABLED -> "Device policy"
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_CAMERA_SERVICE -> "Fatal (service)"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    else -> "Unknown"
                }
                val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                if (cont.isActive) cont.resumeWithException(exc)
            }
        }, handler)
    }

    private suspend fun createCaptureSession(device: CameraDevice, targets: List<Surface>, handler: Handler? = null
    ): CameraCaptureSession = suspendCoroutine { cont ->
        device.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)
            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} session config failed")
                Timber.e(exc)
                cont.resumeWithException(exc)
            }
        }, handler)
    }

    override fun onStop() {
        super.onStop()
        try {
            camera.close()
        } catch (exc: Throwable) {
            Timber.e(exc)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraThread.quitSafely()
    }

    // Transforms the viewfinder to the device orientation
    private fun transformImage(viewWidth: Int, viewHeight: Int) {
        if (previewSize == null || !::viewFinder.isInitialized) return
        val matrix = Matrix()
        val rotation = requireActivity().windowManager.defaultDisplay.rotation
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize!!.height.toFloat(), previewSize!!.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale: Float = (viewHeight.toFloat() / previewSize!!.height)
                    .coerceAtLeast(viewWidth.toFloat() / previewSize!!.width)
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        viewFinder.setTransform(matrix)
    }

    // Returns a transform matrix for rotating the bitmap from the viewfinder on capture
    private fun getTransformMatrix(viewWidth: Int, viewHeight: Int): Matrix {
        val matrix = Matrix()
        val rotation = requireActivity().windowManager.defaultDisplay.rotation
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, viewFinder.height.toFloat(), viewFinder.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            val scale: Float = (viewHeight.toFloat() / viewFinder.height)
                    .coerceAtLeast(viewWidth.toFloat() / viewFinder.width)
            with(matrix) {
                setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
                postScale(scale, scale, centerX, centerY)
                postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
            }
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        return matrix
    }

    // Initializes the camera if permissions granted otherwise notifies user
    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post {
                    initializeCamera()
                }
            } else {
                Toast.makeText(this.requireContext(), "Permissions not granted.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}