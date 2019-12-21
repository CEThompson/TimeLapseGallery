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
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.navArgs
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.entry.CoverPhotoEntry
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
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
        InjectorUtils.provideCameraViewModelFactory(requireActivity(), args.projectId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewFinder = this.view_finder

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                    activity as Activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Every time provided texture view changes recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
    }

    private fun startCamera() {
        val metrics = DisplayMetrics().also{viewFinder.display.getRealMetrics(it)}
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)

        // TODO use aspect ratio?
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        // TODO: Implement CameraX operations
        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(CameraX.LensFacing.BACK)
            setTargetResolution(screenSize)
            setTargetRotation(activity!!.windowManager.defaultDisplay.rotation)
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener {
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        val imageCaptureConfig = ImageCaptureConfig.Builder()
                .apply {
                    setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                }.build()

        val imageCapture = ImageCapture(imageCaptureConfig)
        activity?.take_picture_fab?.setOnClickListener {
            // TODO handle external files directory better, perhaps as a companion object?
            val externalFilesDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
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
                            val db = TimeLapseDatabase.getInstance(this@CameraFragment.requireContext())
                            Log.d("abcde", "inserting project, photo, and cover photo")
                            // Create database entries
                            val timestamp = System.currentTimeMillis()

                            // Create and insert the project
                            val projectEntry = ProjectEntry(null, 0)
                            val project_id = db?.projectDao()?.insertProject(projectEntry)
                            Log.d("abcde", "project id is $project_id")

                            // Update the id for final file creation
                            projectEntry.id = project_id!!
                            // Copy temp file to project folder file

                            FileUtils.createFinalFileFromTemp(externalFilesDir, file.absolutePath, projectEntry, timestamp)

                            // Create and insert the photo
                            val photoEntry = PhotoEntry(project_id, timestamp)
                            val photo_id = db.photoDao()?.insertPhoto(photoEntry)
                            // Create and insert the cover photo
                            val coverPhotoEntry = CoverPhotoEntry(project_id, photo_id!!)
                            db.coverPhotoDao()?.insertPhoto(coverPhotoEntry)

                            val projectScheduleEntry = ProjectScheduleEntry(project_id, null, null)
                            db.projectScheduleDao()?.insertProjectSchedule(projectScheduleEntry)
                        }
                    })

        }

        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun updateTransform() {
        // TODO: Implement camera viewfinder transformations
        val matrix = Matrix()

        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        val rotationDegrees = when(viewFinder.display.rotation){
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }

        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        viewFinder.setTransform(matrix)
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
}