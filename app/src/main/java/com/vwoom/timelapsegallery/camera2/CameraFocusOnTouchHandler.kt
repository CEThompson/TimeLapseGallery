package com.vwoom.timelapsegallery.camera2

import android.annotation.SuppressLint
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.params.MeteringRectangle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import kotlin.math.max

class CameraFocusOnTouchHandler(
        private val mCameraCharacteristics: CameraCharacteristics,
        private val mPreviewRequestBuilder: CaptureRequest.Builder,
        private val mCaptureSession: CameraCaptureSession,
        private val mBackgroundHandler: Handler
) : OnTouchListener {
    private var mManualFocusEngaged = false
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean { //Override in your touch-enabled view (this can be different than the view you use for displaying the cam preview)
        val actionMasked = motionEvent.actionMasked
        if (actionMasked != MotionEvent.ACTION_DOWN) {
            return false
        }
        if (mManualFocusEngaged) {
            Log.d(TAG, "Manual focus already engaged")
            return true
        }
        val sensorArraySize = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        //TODO: here I just flip x,y, but this needs to correspond with the sensor orientation (via SENSOR_ORIENTATION)
        val y = (motionEvent.x / view.width.toFloat() * sensorArraySize!!.height().toFloat()).toInt()
        val x = (motionEvent.y / view.height.toFloat() * sensorArraySize.width().toFloat()).toInt()
        val halfTouchWidth = 50 //(int)motionEvent.getTouchMajor(); //TODO: this doesn't represent actual touch size in pixel. Values range in [3, 10]...
        val halfTouchHeight = 50 //(int)motionEvent.getTouchMinor();
        val focusAreaTouch = MeteringRectangle(max(x - halfTouchWidth, 0),
                max(y - halfTouchHeight, 0),
                halfTouchWidth * 2,
                halfTouchHeight * 2,
                MeteringRectangle.METERING_WEIGHT_MAX - 1)
        val captureCallbackHandler: CaptureCallback = object : CaptureCallback() {
            override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                super.onCaptureCompleted(session, request, result)
                mManualFocusEngaged = false
                if (request.tag === "FOCUS_TAG") { //the focus trigger is complete - resume repeating (preview surface will get frames), clear AF trigger
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, null)
                    try {
                        mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
                super.onCaptureFailed(session, request, failure)
                Log.e(TAG, "Manual AF failure: $failure")
                mManualFocusEngaged = false
            }
        }
        //first stop the existing repeating request
        try {
            mCaptureSession.stopRepeating()
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        //cancel any existing AF trigger (repeated touches, etc.)
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
        try {
            mCaptureSession.capture(mPreviewRequestBuilder.build(), captureCallbackHandler, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        //Now add a new AF trigger with focus region
        if (isMeteringAreaAFSupported) {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(focusAreaTouch))
        }
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
        mPreviewRequestBuilder.setTag("FOCUS_TAG") //we'll capture this later for resuming the preview
        //then we ask for a single request (not repeating!)
        try {
            mCaptureSession.capture(mPreviewRequestBuilder.build(), captureCallbackHandler, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        mManualFocusEngaged = true
        return true
    }

    private val isMeteringAreaAFSupported: Boolean
        get() {
            val value = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)
            return if (value != null) {
                value >= 1
            } else {
                false
            }
        }

    companion object {
        private const val TAG = "FocusOnTouchHandler"
    }

}