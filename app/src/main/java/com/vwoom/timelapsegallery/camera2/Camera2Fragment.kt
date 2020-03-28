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
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.camera2.common.AutoFitTextureView
import com.vwoom.timelapsegallery.camera2.common.OrientationLiveData
import com.vwoom.timelapsegallery.camera2.common.getPreviewOutputSize
import com.vwoom.timelapsegallery.databinding.FragmentCamera2Binding
import com.vwoom.timelapsegallery.testing.launchIdling
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.InjectorUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// Arbitrary number to keep track of permission request
private const val REQUEST_CODE_PERMISSIONS = 10

// Array of all permissions specified in the manifest
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class Camera2Fragment : Fragment(), LifecycleOwner {

    private val args: Camera2FragmentArgs by navArgs()

    // Camera variables
    private val cameraManager: CameraManager by lazy {
        val context = requireContext().applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private lateinit var cameraId: String

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

    private var baseWidth: Int = 0
    private var baseHeight: Int = 0

    private var takePictureJob: Job? = null
    private val camera2ViewModel: Camera2ViewModel by viewModels {
        InjectorUtils.provideCamera2ViewModelFactory(requireActivity(), args.photo, args.project)
    }
    private var mTakePictureFab: FloatingActionButton? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Set up bindings
        val binding = FragmentCamera2Binding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
        cameraId = args.cameraId
        viewFinder = binding.cameraPreview
        mTakePictureFab = binding.takePictureFab

        // Loads the last photo from a project into the compare view if available
        if (camera2ViewModel.photo != null) {
            val file = File(camera2ViewModel.photo?.photo_url!!)
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

        mTakePictureFab?.setOnClickListener {
            it.isEnabled = false

            var outputPhoto: FileOutputStream? = null
            try {
                val externalFilesDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
                val file = FileUtils.createTemporaryImageFile(externalFilesDir)
                outputPhoto = FileOutputStream(file)

                // Rotates and scales the bitmap based on the device rotation
                val matrix = getTransformMatrix(baseWidth, baseHeight)
                val adjustedBitmap = Bitmap.createBitmap(viewFinder.bitmap, 0, 0, viewFinder.bitmap.width, viewFinder.bitmap.height, matrix, true)
                adjustedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputPhoto)

                // TODO write exif data for saved image
                takePictureJob = lifecycleScope.launchIdling {
                    camera2ViewModel.handleFinalPhotoFile(file, externalFilesDir, ExifInterface.ORIENTATION_NORMAL)
                    findNavController().popBackStack()
                }
            } catch (e: Exception) {
                viewFinder.post { Toast.makeText(context, "Capture failed: ${e.message}", Toast.LENGTH_LONG).show() }
                Log.d(TAG, "Take picture exception: ${e.message}")
            } finally {
                try {
                    outputPhoto?.close()
                } catch (e: Exception) {
                    Log.d(TAG, "Take picture exception in finally block, exception closing photo: ${e.message}")
                }
            }
            it.post { it.isEnabled = true }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewFinder.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
                Log.d(TAG, "onSurfaceTextureAvailable called with to width $width and height $height")
                previewSize = getPreviewOutputSize(
                        viewFinder.display,
                        characteristics,
                        SurfaceHolder::class.java)
                //viewFinder.holder.setFixedSize(previewSize.width, previewSize.height)
                Log.d(TAG, "previewSize is width ${previewSize!!.width} and height ${previewSize!!.height}")
                viewFinder.setAspectRatio(previewSize!!.width, previewSize!!.height)

                // Transforms the viewfinder if device is rotated
                transformImage(width, height)

                // Initialize camera in the viewfinders thread so that size is set
                viewFinder.post {
                    // but first request the permissions, if permissions cleared or granted then camera is initialized
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

        relativeOrientation = OrientationLiveData(requireContext(), characteristics).apply {
            observe(viewLifecycleOwner, Observer { orientation ->
                Log.d(TAG, "Orientation changed to $orientation")
            })
        }
    }

    private fun initializeCamera() = lifecycleScope.launchIdling {
        camera = openCamera(cameraManager, cameraId, cameraHandler)

        // This line would get the true sensor size but for now match the size of the preview view
        // val size = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(pixelFormat).maxBy {it.height * it.width}!!

        // Match size of image output to preview size
        previewSize = getPreviewOutputSize(
                viewFinder.display,
                characteristics,
                SurfaceHolder::class.java)

        val surfaceTexture = viewFinder.surfaceTexture
        surfaceTexture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
        val previewSurface = Surface(surfaceTexture)
        val targets = listOf(previewSurface)
        session = createCaptureSession(camera, targets, cameraHandler)

        captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(previewSurface)
        }

        // Set the touch focus listener on the viewfinder
        @Suppress("ClickableViewAccessibility")
        viewFinder.setOnTouchListener(
                CameraFocusOnTouchHandler(
                        characteristics,
                        captureRequestBuilder,
                        session,
                        cameraHandler))

        // Set the initial request: Note the touch handler sets a repeated request as well likely overriding this
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
                Log.d(TAG, "Camera disconnected")
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
                Log.e(TAG, exc.message, exc)
                cont.resumeWithException(exc)
            }
        }, handler)
    }

    override fun onStop() {
        super.onStop()
        try {
            camera.close()
        } catch (exc: Throwable) {
            Log.e(TAG, "Error closing camera", exc)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraThread.quitSafely()
    }

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

    companion object {
        val TAG = Camera2Fragment::class.java.simpleName
    }

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
}