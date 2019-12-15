package com.vwoom.timelapsegallery.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.vwoom.timelapsegallery.R
import com.vwoom.timelapsegallery.data.TimeLapseDatabase
import com.vwoom.timelapsegallery.data.entry.CoverPhotoEntry
import com.vwoom.timelapsegallery.data.entry.PhotoEntry
import com.vwoom.timelapsegallery.data.entry.ProjectEntry
import com.vwoom.timelapsegallery.data.entry.ProjectScheduleEntry
import com.vwoom.timelapsegallery.utils.FileUtils
import java.io.File
import java.util.concurrent.Executors

// Arbitrary number to keep track of permission request
private const val REQUEST_CODE_PERMISSIONS = 10

// Array of all permissions specified in the manifest
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class CameraActivity : AppCompatActivity(), LifecycleOwner {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        viewFinder = findViewById(R.id.view_finder)

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Every time provided texture view changes recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
    }

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView

    private fun startCamera() {
        val metrics = DisplayMetrics().also{viewFinder.display.getRealMetrics(it)}
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)

        // TODO use aspect ratio?
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)


        // TODO: Implement CameraX operations
        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(CameraX.LensFacing.BACK)
            setTargetResolution(screenSize)
            setTargetRotation(windowManager.defaultDisplay.rotation)
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
        findViewById<ImageButton>(R.id.take_picture_fab).setOnClickListener {
            // TODO handle external files directory better, perhaps as a companion object?
            val externalFilesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = FileUtils.createTemporaryImageFile(externalFilesDir)

            imageCapture.takePicture(file, executor,
                    object: ImageCapture.OnImageSavedListener{
                        override fun onError(
                                imageCaptureError: ImageCapture.ImageCaptureError,
                                message: String,
                                cause: Throwable?) {
                            viewFinder.post{Toast.makeText(baseContext, "Capture failed: $message", Toast.LENGTH_LONG).show()}
                            // TODO error log
                            Log.e("Camera Activity", "Capture Failed: $message")
                        }

                        override fun onImageSaved(file: File) {
                            viewFinder.post{Toast.makeText(baseContext, "Capture success", Toast.LENGTH_LONG).show()}
                            val db = TimeLapseDatabase.getInstance(baseContext)
                            Log.d("abcde", "inserting project, photo, and cover photo")
                            // Create database entries
                            val timestamp = System.currentTimeMillis()

                            // Create and insert the project
                            val projectEntry = ProjectEntry(null, 0)
                            val project_id = db.projectDao().insertProject(projectEntry)
                            Log.d("abcde", "project id is $project_id")

                            // Update the id for final file creation
                            projectEntry.id = project_id
                            // Copy temp file to project folder file

                            FileUtils.createFinalFileFromTemp(externalFilesDir, file.absolutePath, projectEntry, timestamp)

                            // Create and insert the photo
                            val photoEntry = PhotoEntry(project_id, timestamp)
                            val photo_id = db.photoDao().insertPhoto(photoEntry)
                            Log.d("abcde", "photo entry id $photo_id")
                            Log.d("abcde", "photo entry timestamp ${photoEntry.timestamp}")

                            // Create and insert the cover photo
                            val coverPhotoEntry = CoverPhotoEntry(project_id, photo_id)
                            db.coverPhotoDao().insertPhoto(coverPhotoEntry)
                            Log.d("abcde", "cover photo, project id ${coverPhotoEntry.project_id}")
                            Log.d("abcde", "cover photo, photo id ${coverPhotoEntry.photo_id}")

                            val projectScheduleEntry = ProjectScheduleEntry(project_id, null, null)
                            db.projectScheduleDao().insertProjectSchedule(projectScheduleEntry)
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
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

}