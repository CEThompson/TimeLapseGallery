package com.vwoom.timelapsegallery.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.IOException

object PhotoUtils {
    private val TAG = PhotoUtils::class.java.simpleName

    /* Returns the aspect ratio from the photo path */
    fun getAspectRatioFromImagePath(path: String?): String {
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

    /* Gets exif orientation from bitmap */
    @JvmStatic
    @Throws(IOException::class)
    fun getOrientationFromImagePath(path: String?): Int {
        val exif = ExifInterface(path!!)
        return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }

    /* Rotates a bitmap by its exif orientation */
    @JvmStatic
    @Throws(OutOfMemoryError::class)
    fun rotateBitmap(bitmap: Bitmap, orientation: Int?): Bitmap? {
        if (orientation == null) return null
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> return bitmap
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        // Try to rotate the bitmap
        return try {
            val bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            bmRotated
        } catch (e: OutOfMemoryError) {
            if (e.message != null) Log.e(TAG, e.message!!)
            null
        }
    }

    fun isLandscape(imagePath: String?): Boolean {
        val aspectRatio = getAspectRatioFromImagePath(imagePath)
        val res = aspectRatio.split(":").toTypedArray()
        val width = res[0].toInt()
        val height = res[1].toInt()
        return width > height
    }

    @JvmStatic
    fun decodeSampledBitmapFromPath(path: String?,
                                    reqWidth: Int, reqHeight: Int): Bitmap { // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }

    private fun calculateInSampleSize(
            options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int { // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
// height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight
                    && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}