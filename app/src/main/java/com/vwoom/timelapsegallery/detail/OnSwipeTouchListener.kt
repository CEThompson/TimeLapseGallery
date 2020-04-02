package com.vwoom.timelapsegallery.detail

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.absoluteValue

class OnSwipeTouchListener(ctx: Context?, private val onSwipeRight: () -> Unit, private val onSwipeLeft: () -> Unit) : View.OnTouchListener {
    private val gestureDetector: GestureDetector

    @Suppress("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        private val swipeThreshold = 100
        private val swipeVelocityThreshold = 100
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            var result = false
            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x
            if (diffX.absoluteValue > diffY.absoluteValue)
                if (diffX.absoluteValue > swipeThreshold && velocityX.absoluteValue > swipeVelocityThreshold) {
                    if (diffX > 0) {
                        onSwipeRight()
                    } else {
                        onSwipeLeft()
                    }
                    result = true
                }
            return result
        }
    }

    fun onSwipeRight() = onSwipeRight.invoke()
    fun onSwipeLeft() = onSwipeLeft.invoke()

    init {
        gestureDetector = GestureDetector(ctx, GestureListener())
    }
}
