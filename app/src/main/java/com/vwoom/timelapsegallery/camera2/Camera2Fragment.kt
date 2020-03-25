package com.vwoom.timelapsegallery.camera2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.*
import android.util.Log
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
import com.vwoom.timelapsegallery.camera2.common.AutoFitSurfaceView
import com.vwoom.timelapsegallery.camera2.common.OrientationLiveData
import com.vwoom.timelapsegallery.camera2.common.computeExifOrientation
import com.vwoom.timelapsegallery.camera2.common.getPreviewOutputSize
import com.vwoom.timelapsegallery.databinding.FragmentCamera2Binding
import com.vwoom.timelapsegallery.testing.launchIdling
import com.vwoom.timelapsegallery.utils.FileUtils
import com.vwoom.timelapsegallery.utils.InjectorUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// Arbitrary number to keep track of permission request
private const val REQUEST_CODE_PERMISSIONS = 10

// Array of all permissions specified in the manifest
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

// TODO: Clean up code, look into image quality difference between this and previous implementation
class Camera2Fragment : Fragment(), LifecycleOwner {

    private val args: Camera2FragmentArgs by navArgs()

    // Variables from camera 2 basic
    private val cameraManager: CameraManager by lazy {
        val context = requireContext().applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    // TODO figure out best way to get the camera ID
    private val cameraId by lazy {
        lateinit var id: String
        for (cameraId in cameraManager.cameraIdList) {
            val currentCameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            if (currentCameraCharacteristics
                            .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                id = cameraId
                break
            }
        }
        id
    }
    private val pixelFormat = ImageFormat.JPEG // TODO could later support ImageFormat.RAW_SENSOR or ImageFormat.DEPTH_JPEG
    private val characteristics: CameraCharacteristics by lazy {
        cameraManager.getCameraCharacteristics(cameraId)
    }
    private lateinit var imageReader: ImageReader // Used as buffer for camera still shot
    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler: Handler = Handler(cameraThread.looper)
    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
    private val imageReaderHandler = Handler(imageReaderThread.looper)
    private lateinit var viewFinder: AutoFitSurfaceView
    private lateinit var camera: CameraDevice
    private lateinit var session: CameraCaptureSession
    private lateinit var relativeOrientation: OrientationLiveData


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
            // TODO consider using view model scope rather than lifecycle scope
            takePictureJob = lifecycleScope.launchIdling {
                takePhoto().use { result ->
                    Log.d(TAG, "resulting take photo")
                    // Output into temp file
                    val output = saveResult(result)

                    if (output.extension == "jpg") {
                        val exif = ExifInterface(output.absolutePath)
                        exif.setAttribute(ExifInterface.TAG_ORIENTATION, result.orientation.toString())
                        exif.saveAttributes()
                    }
                    Log.d(TAG, "checking for output: ${output.absolutePath}")

                    // Send to view model to handle final file and return to gallery
                    val externalFilesDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    camera2ViewModel.handleFile(output, externalFilesDir!!)
                    findNavController().popBackStack()
                }
            }
            it.post { it.isEnabled = true }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewFinder.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceDestroyed(holder: SurfaceHolder?) = Unit
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) = Unit
            override fun surfaceCreated(holder: SurfaceHolder?) {
                val previewSize = getPreviewOutputSize(
                        viewFinder.display,
                        characteristics,
                        SurfaceHolder::class.java)
                viewFinder.holder.setFixedSize(previewSize.width, previewSize.height)
                viewFinder.setAspectRatio(previewSize.width, previewSize.height)

                // Initialize camera in the viewfinders thread so that size is set
                viewFinder.post {
                    // but first request the permissions, if permissions cleared or granted then camera is initialized
                    requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
                }
            }
        })

        relativeOrientation = OrientationLiveData(requireContext(), characteristics).apply {
            observe(viewLifecycleOwner, Observer { orientation ->
                Log.d(TAG, "Orientation changed to $orientation")
            })
        }
    }

    private fun initializeCamera() = lifecycleScope.launchIdling {
        camera = openCamera(cameraManager, cameraId, cameraHandler)

        // TODO setting pixel format to ImageFormat.JPEG for now, could also be ImageFormat.RAW_SENSOR && ImageFormat.DEPTH_JPEG
        // This block gets true camera output size
        //val size = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
        //        .getOutputSizes(pixelFormat).maxBy {it.height * it.width}!!

        // Match size of image output to preview size
        val previewSize = getPreviewOutputSize(
                viewFinder.display,
                characteristics,
                SurfaceHolder::class.java)
        imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, pixelFormat, IMAGE_BUFFER_SIZE)

        val targets = listOf(viewFinder.holder.surface, imageReader.surface) // where the camera will output frames
        session = createCaptureSession(camera, targets, cameraHandler)

        val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(viewFinder.holder.surface)
        }

        // TODO set up tap to focus
        //setTapToFocus()

        session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)

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
                // TODO handle disconnected camera
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

    private suspend fun takePhoto(): CombinedCaptureResult = suspendCoroutine {
        @Suppress("ControlFlowWithEmptyBody")
        while (imageReader.acquireNextImage() != null) {
        }

        val imageQueue = ArrayBlockingQueue<Image>(IMAGE_BUFFER_SIZE)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireNextImage()
            imageQueue.add(image)
        }, imageReaderHandler)

        val captureRequest = session.device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
            addTarget(imageReader.surface)
        }

        session.capture(captureRequest.build(), object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult) {
                super.onCaptureCompleted(session, request, result)
                val resultTimestamp = result.get(CaptureResult.SENSOR_TIMESTAMP)
                Log.d(TAG, "Capture result received: $resultTimestamp")

                val exc = TimeoutException("Image dequeuing took too long")
                // TODO changed cont to it here, figure out what this means
                val timeoutRunnable = Runnable { it.resumeWithException(exc) }
                imageReaderHandler.postDelayed(timeoutRunnable, IMAGE_CAPTURE_TIMEOUT_MILLIS)

                // TODO changed cont to it here, figure out what this means
                @Suppress("BlockingMethodInNonBlockingContext")
                lifecycleScope.launch(it.context) {
                    while (true) {
                        val image = imageQueue.take()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                image.format != ImageFormat.DEPTH_JPEG &&
                                image.timestamp != resultTimestamp) continue
                        Log.d(TAG, "Matching image dequeued: ${image.timestamp}")

                        imageReaderHandler.removeCallbacks(timeoutRunnable)
                        imageReader.setOnImageAvailableListener(null, null)

                        Log.d(TAG, "imageQueue $imageQueue")
                        while (imageQueue.size > 0) {
                            imageQueue.take().close()
                        }

                        val rotation = relativeOrientation.value ?: 0
                        val mirrored = characteristics.get(CameraCharacteristics.LENS_FACING) ==
                                CameraCharacteristics.LENS_FACING_FRONT
                        val exifOrientation = computeExifOrientation(rotation, mirrored)

                        Log.d(TAG, "about to it resume")
                        // TODO changed cont to it here, figure out what this means
                        it.resume(CombinedCaptureResult(
                                image, result, exifOrientation, imageReader.imageFormat))
                        Log.d(TAG, "done with it resume")
                        break
                    }
                }
            }
        }, cameraHandler)
    }

    private suspend fun saveResult(result: CombinedCaptureResult): File = suspendCoroutine { cont ->
        val externalFilesDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        when (result.format) {
            ImageFormat.JPEG, ImageFormat.DEPTH_JPEG -> {
                val buffer = result.image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
                try {
                    val output = FileUtils.createTemporaryImageFile(externalFilesDir)
                    FileOutputStream(output).use { it.write(bytes) }
                    cont.resume(output)
                } catch (exc: IOException) {
                    Log.e(TAG, "Unable to write JPEG image to file", exc)
                    cont.resumeWithException(exc)
                }
            }
            ImageFormat.RAW_SENSOR -> {
                val dngCreator = DngCreator(characteristics, result.metadata)
                try {
                    val output = FileUtils.createTemporaryImageFile(externalFilesDir)
                    FileOutputStream(output).use { dngCreator.writeImage(it, result.image) }
                    cont.resume(output)
                } catch (exc: IOException) {
                    Log.e(TAG, "Unable to write DNG image to file", exc)
                    cont.resumeWithException(exc)
                }
            }
            else -> {
                val exc = RuntimeException("Unknown image format: ${result.image.format}")
                cont.resumeWithException(exc)
            }
        }
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
        imageReaderThread.quitSafely()
    }

    companion object {
        val TAG = Camera2Fragment::class.java.simpleName
        private const val IMAGE_BUFFER_SIZE: Int = 3
        private const val IMAGE_CAPTURE_TIMEOUT_MILLIS: Long = 5000

        data class CombinedCaptureResult(
                val image: Image,
                val metadata: CaptureResult,
                val orientation: Int,
                val format: Int
        ) : Closeable {
            override fun close() = image.close()
        }

    }

    fun setTapToFocus(captureRequestBuilder: CaptureRequest.Builder){
        @Suppress("ClickableViewAccessibility")
        viewFinder.setOnTouchListener(
                CameraFocusOnTouchHandler(
                characteristics,
                captureRequestBuilder,
                session,
                cameraHandler))
    }
    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post {
                    //Toast.makeText(this.requireContext(), "Permissions granted, firing up the camera.", Toast.LENGTH_SHORT).show()
                    initializeCamera()
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
}