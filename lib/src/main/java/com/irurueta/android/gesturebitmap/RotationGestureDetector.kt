package com.irurueta.android.gesturebitmap

import android.view.MotionEvent
import kotlin.math.atan2

/**
 * Gesture detector to handle rotation gestures with two fingers.
 *
 * @property listener listener to handle events genrated by this gesture detector.
 */
class RotationGestureDetector(var listener: OnRotationGestureListener? = null) {

    /**
     * Initial X coordinate of 1st finger.
     */
    var initialX1 = 0.0f
        private set

    /**
     * Initial Y coordinate of 1st finger.
     */
    var initialY1 = 0.0f
        private set

    /**
     * Initial X coordinate of 2nd finger.
     */
    var initialX2 = 0.0f
        private set

    /**
     * Initial Y coordinate of 2nd finger.
     */
    var initialY2 = 0.0f
        private set

    /**
     * Current X coordinate of 1st finger.
     */
    var currentX1 = 0.0f
        private set

    /**
     * Current Y coordinate of 1st finger.
     */
    var currentY1 = 0.0f
        private set

    /**
     * Current X coordinate of 2nd finger.
     */
    var currentX2 = 0.0f
        private set

    /**
     * Current Y coordinate of 2nd finger.
     */
    var currentY2 = 0.0f
        private set

    /**
     * Angle between both fingers at their initial position.
     */
    var initialAngle = 0.0f
        private set

    /**
     * Angle between both fingers at their current position.
     */
    var currentAngle = 0.0f
        private set

    /**
     * Amount of angle variation between initial and current positions.
     */
    var deltaAngleStart = 0.0f
        private set

    /**
     * Angle variation respect previous touch event.
     */
    var deltaAngle = 0.0f
        private set

    /**
     * X coordinate of pivot point.
     * The pivot point is located between both fingers and is a point where rotation
     * should not have any effect.
     */
    var pivotX = 0.0f
        private set

    /**
     * Y coordinate of pivot point.
     */
    var pivotY = 0.0f
        private set

    /**
     * Id for 1st finger.
     */
    private var pointerId1 = INVALID_POINTER_ID

    /**
     * Id for 2nd finger.
     */
    private var pointerId2 = INVALID_POINTER_ID

    /**
     * Method to call when a touch event occurs.
     *
     * @param event touch event that has occurred.
     * @return true if the event should be considered to be handled, false otherwise.
     */
    fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 1st finger touch
                pointerId1 = event.getPointerId(0)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // 2nd finger touch
                pointerId2 = event.getPointerId(event.actionIndex)

                // initial coordinates for 1st finger
                val index1 = event.findPointerIndex(pointerId1)
                initialX1 = event.getX(index1)
                initialY1 = event.getY(index1)

                // initial coordinates for 2nd finger
                val index2 = event.findPointerIndex(pointerId2)
                initialX2 = event.getX(index2)
                initialY2 = event.getY(index2)

                pivotX = midPoint(initialX1, initialX2)
                pivotY = midPoint(initialY1, initialY2)

                initialAngle = angleBetweenFingers(initialX1, initialY1, initialX2, initialY2)
                currentAngle = initialAngle
            }
            MotionEvent.ACTION_MOVE -> {
                // update current finger positions
                if (pointerId1 != INVALID_POINTER_ID && pointerId2 != INVALID_POINTER_ID) {
                    // current coordinates for 1st finger
                    val index1 = event.findPointerIndex(pointerId1)
                    currentX1 = event.getX(index1)
                    currentY1 = event.getY(index1)

                    // current coordinates for 2nd finger
                    val index2 = event.findPointerIndex(pointerId2)
                    currentX2 = event.getX(index2)
                    currentY2 = event.getY(index2)

                    // update pivot
                    pivotX = midPoint(currentX1, currentX2)
                    pivotY = midPoint(currentY1, currentY2)

                    val prevAngle = currentAngle
                    currentAngle = angleBetweenFingers(currentX1, currentY1, currentX2, currentY2)

                    deltaAngleStart = currentAngle - initialAngle
                    deltaAngle = currentAngle - prevAngle

                    return (listener?.onRotation(this, deltaAngle, pivotX, pivotY)) ?: false
                }
            }
            MotionEvent.ACTION_UP -> {
                pointerId1 = INVALID_POINTER_ID
            }
            MotionEvent.ACTION_POINTER_UP -> {
                pointerId2 = INVALID_POINTER_ID
            }
            MotionEvent.ACTION_CANCEL -> {
                pointerId1 = INVALID_POINTER_ID
                pointerId2 = INVALID_POINTER_ID
            }
        }

        return false
    }

    /**
     * Returns the angle of the line joining 2 fingers.
     *
     * @param x1 x coordinate of 1st finger.
     * @param y1 y coordinate of 1st finger.
     * @param x2 x coordinate of 2nd finger.
     * @param y2 y coordinate of 2nd finger.
     */
    private fun angleBetweenFingers(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val diffX = x2 - x1
        val diffY = y2 - y1
        return atan2(diffY, diffX)
    }

    /**
     * Computes the middle point between two points.
     *
     * @param a 1st point.
     * @param b 2nd point.
     * @return middle point.
     */
    private fun midPoint(a: Float, b: Float): Float {
        return (a + b) / 2.0f
    }

    private companion object {
        /**
         * Id for an invalid pointer (finger).
         */
        const val INVALID_POINTER_ID = -1
    }

    /**
     * Listener to notify rotation events.
     */
    interface OnRotationGestureListener {

        /**
         * Called when rotation is detected.
         *
         * @param gestureDetector gesture detector that raised the event.
         * @param angle amount of angle variation between consecutive touch events expressed
         * in radians.
         * @param pivotX x coordinate of pivot point.
         * @param pivotY y coordinate of pivot point.
         * @return true if event has been handled, false otherwise.
         */
        fun onRotation(
            gestureDetector: RotationGestureDetector,
            angle: Float, pivotX: Float, pivotY: Float
        ): Boolean
    }
}