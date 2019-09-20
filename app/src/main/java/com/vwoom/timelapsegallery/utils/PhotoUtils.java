package com.vwoom.timelapsegallery.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;

public final class PhotoUtils {

    private static final String TAG = PhotoUtils.class.getSimpleName();

    /* Returns the aspect ratio from the photo path */
    public static String getAspectRatioFromImagePath(String path){
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bmOptions);

        int imageHeight = bmOptions.outHeight;
        int imageWidth = bmOptions.outWidth;

        String aspectRatio = imageWidth + ":" + imageHeight;

        // If the image is rotated modify the aspect ratio
        Integer orientation = getOrientationFromImagePath(path);
        if (orientation == null) return null;

        // Modify aspect ratio in case of landscape orientation
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_270:
            case ExifInterface.ORIENTATION_ROTATE_90:
                aspectRatio = imageHeight + ":" + imageWidth;
                break;
        }

        return aspectRatio;
    }

    /* Gets exif orientation from bitmap */
    public static Integer getOrientationFromImagePath(String path){
        Integer orientation = null;
        try {
            ExifInterface exif = new ExifInterface(path);
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException e) { Log.d(TAG, e.getMessage());}

       return orientation;
    }

    /* Rotates a bitmap by its exif orientation */
    public static Bitmap rotateBitmap(Bitmap bitmap, Integer orientation){
        if (orientation == null) return null;
        Matrix matrix = new Matrix();
        // Determine the rotation matrix
        switch (orientation){
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        // Try to rotate the bitmap
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isLandscape(String imagePath){
        String aspectRatio = getAspectRatioFromImagePath(imagePath);
        String[] res = aspectRatio.split(":");
        int width = Integer.parseInt(res[0]);
        int height = Integer.parseInt(res[1]);
        return width > height;
    }

    public static Bitmap decodeSampledBitmapFromPath(String path,
                                                     int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(path, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
