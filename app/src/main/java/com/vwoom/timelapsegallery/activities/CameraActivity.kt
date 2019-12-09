package com.vwoom.timelapsegallery.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import com.vwoom.timelapsegallery.database.TimeLapseDatabase
import com.vwoom.timelapsegallery.database.entry.CoverPhotoEntry
import com.vwoom.timelapsegallery.database.entry.PhotoEntry
import com.vwoom.timelapsegallery.database.entry.ProjectEntry
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
                    setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                }.build()

        val imageCapture = ImageCapture(imageCaptureConfig)
        findViewById<ImageButton>(R.id.take_picture_fab).setOnClickListener {
            val file = FileUtils.createTemporaryImageFile(this)

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

                            // Create database entries
                            val timestamp = System.currentTimeMillis()
                            val projectEntry = ProjectEntry(null, 0)
                            val photoEntry = PhotoEntry(projectEntry.id, timestamp)
                            val coverPhotoEntry = CoverPhotoEntry(projectEntry.id, photoEntry.id)

                            // Copy temp file to project folder file
                            FileUtils.createFinalFileFromTemp(baseContext, file.absolutePath, projectEntry, timestamp)

                            // Get and insert into the database
                            val db = TimeLapseDatabase.getInstance(baseContext)
                            db.projectDao().insertProject(projectEntry)
                            db.photoDao().insertPhoto(photoEntry)
                            db.coverPhotoDao().insertPhoto(coverPhotoEntry)
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