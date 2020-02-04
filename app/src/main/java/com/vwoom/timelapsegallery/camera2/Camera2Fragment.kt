package com.vwoom.timelapsegallery.camera2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.vwoom.timelapsegallery.databinding.FragmentCamera2Binding
import com.vwoom.timelapsegallery.detail.CameraXViewModel
import com.vwoom.timelapsegallery.utils.InjectorUtils
import kotlinx.coroutines.Job
import java.io.File

// Arbitrary number to keep track of permission request
private const val REQUEST_CODE_PERMISSIONS = 10

// Array of all permissions specified in the manifest
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

// TODO camera x is somehow leaking: hunt it down

class Camera2Fragment : Fragment(), LifecycleOwner {

    private val cameraManager: CameraManager by lazy {
        val context = requireContext().applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    // TODO implement camera characteristics?
    private lateinit var camera2Preview: TextureView
    private var takePictureJob: Job? = null

    private val args: Camera2FragmentArgs by navArgs()

    private val cameraViewModel: CameraXViewModel by viewModels {
        InjectorUtils.provideCameraXViewModelFactory(requireActivity(), args.photo, args.project)
    }

    private var mTakePictureFab: FloatingActionButton? = null

    private var cameraCaptureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null

    private var previewSize: Size? = null

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            setupCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            cameraDevice = null
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Set up bindings
        val binding = FragmentCamera2Binding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
        camera2Preview = binding.cameraPreview
        mTakePictureFab = binding.takePictureFab

        // Loads the last photo from a project into the compare view if available
        if (cameraViewModel.photo != null) {
            val file = File(cameraViewModel.photo?.photo_url!!)
            Glide.with(requireContext())
                    .load(file).into(binding.previousPhoto)
        }

        // TODO: Override perform click for on touch listener
        // Set up quick compare function
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

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            if (camera2Preview.isAvailable) {
                setupCamera()
            } else {
                camera2Preview.surfaceTextureListener = surfaceTextureListener
            }
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

    }

    override fun onStop() {
        super.onStop()
        takePictureJob?.cancel()
        closeCamera()
        closeBackgroundThread()
    }

    fun closeCamera() {
        cameraCaptureSession?.close()
        cameraCaptureSession = null
        cameraDevice?.close()
        cameraDevice = null
    }

    fun closeBackgroundThread() {
        cameraThread?.quitSafely()
        cameraThread = null
        cameraHandler = null
    }

    fun openBackGroundThread() {
        cameraThread = HandlerThread("camera_handler")
        cameraThread?.start()
        cameraHandler = Handler(cameraThread!!.looper)
    }

    fun setupCamera() {
        openBackGroundThread()
        lateinit var cameraIdToSet: String
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)

                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    val streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    previewSize = streamConfigurationMap?.getOutputSizes(SurfaceTexture::class.java)!![0]
                    cameraIdToSet = cameraId
                }
            }
            Log.d(TAG, "opening camera")
            Log.d(TAG, "camera id $cameraIdToSet")
            cameraManager.openCamera(cameraIdToSet, stateCallback, cameraHandler)
        } catch (e: CameraAccessException) {
            Log.d(TAG, "Camera access exception ${e.message}")
        } catch (s: SecurityException) {
            Log.d(TAG, "Camera security exception ${s.message}")
        }
    }

    // TODO set take picture for camera 2
    /*
    fun setTakePictureFab(imageCapture: ImageCapture) {
        mTakePictureFab?.setOnClickListener {
            val externalFilesDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
            val file = FileUtils.createTemporaryImageFile(externalFilesDir)
            imageCapture.takePicture(file, executor, object : ImageCapture.OnImageSavedCallback {
                override fun onError(imageCaptureError: Int, message: String, cause: Throwable?) {
                    camera2Preview?.post { Toast.makeText(context, "Capture failed: $message", Toast.LENGTH_LONG).show() }
                    Log.e(TAG, "Capture Failed: $message")
                }

                override fun onImageSaved(file: File) {
                    camera2Preview?.post { Toast.makeText(context, "Capture success", Toast.LENGTH_LONG).show() }

                    takePictureJob = cameraViewModel.viewModelScope.launch {
                        async { cameraViewModel.handleFile(file, externalFilesDir) }.await()
                        findNavController().popBackStack()
                    }
                }
            })
        }
    }
    */

    private fun createPreviewSession() {
        Log.d(TAG,"creating preview session")
        try {
            val surfaceTexture = camera2Preview.surfaceTexture
            surfaceTexture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)

            val previewSurface = Surface(surfaceTexture)

            val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder?.addTarget(previewSurface)

            Log.d(TAG,"creating capture session")
            Log.d(TAG,"camera device is null is ${cameraDevice == null}")
            cameraDevice?.createCaptureSession(listOf(previewSurface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    Log.d(TAG,"on configured")
                    if (cameraDevice == null) return
                    try {
                        val captureRequest = captureRequestBuilder?.build()
                        Log.d(TAG,"capture request ${captureRequest.toString()}")
                        cameraCaptureSession = session
                        cameraCaptureSession?.setRepeatingRequest(captureRequest!!, null, cameraHandler)
                    } catch (e: CameraAccessException) {
                        Log.d(TAG, "Camera access exception ${e.message}")
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.d(TAG, "Camera configuration failed")
                }
            }, cameraHandler)
        } catch (e: CameraAccessException) {
            Log.d(TAG, "Camera access exception ${e.message}")
        }
    }
    // TODO implement tap to focus
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
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                camera2Preview?.post {
                    Toast.makeText(this.requireContext(), "Permissions granted, firing up the camera.", Toast.LENGTH_SHORT).show()
                    setupCamera()
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
        val TAG = Camera2Fragment::class.java.simpleName
    }
}