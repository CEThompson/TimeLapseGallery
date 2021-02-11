package com.vwoom.timelapsegallery.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.exifinterface.media.ExifInterface
import java.io.IOException

// TODO: reassess usage of this class
object PhotoUtils {

    // Returns a back facing camera that is available to the phone
    fun findBackFacingCamera(context: Context): String? {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        for (id in cameraManager.cameraIdList) {
            val currentCameraCharacteristics = cameraManager.getCameraCharacteristics(id)
            if (currentCameraCharacteristics
                            .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                return id
            }
        }
        return null
    }

    // Returns the aspect ratio from the photo path
    fun getAspectRatioFromImagePath(path: String): String {
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, bmOptions)
        val imageHeight = bmOptions.outHeight
        val imageWidth = bmOptions.outWidth
        var aspectRatio = "$imageWidth:$imageHeight"
        // If the image is rotated modify the aspect ratio
        val orientation: Int
        orientation = try {
            getOrientationFromImagePath(path)
        } catch (e: IOException) {
            return aspectRatio
        }
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_ROTATE_90 -> aspectRatio = "$imageHeight:$imageWidth"
        }
        return aspectRatio
    }

    // Gets exif orientation from bitmap
    @JvmStatic
    @Throws(IOException::class)
    fun getOrientationFromImagePath(path: String): Int {
        val exif = ExifInterface(path)
        return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }

    // Determines if photo is wider than it is tall, used as shorthand for landscape
    fun isImageLandscape(imagePath: String): Boolean {
        val aspectRatio = getAspectRatioFromImagePath(imagePath)
        val res = aspectRatio.split(":").toTypedArray()
        val width = res[0].toInt()
        val height = res[1].toInt()
        return width > height
    }
}