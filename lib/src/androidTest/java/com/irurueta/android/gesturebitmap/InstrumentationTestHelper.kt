package com.irurueta.android.gesturebitmap

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.abs


/**
 * Utility class for instrumentation tests.
 */
object InstrumentationTestHelper {

    /**
     * Default step size between generated drag touch events (expressed in pixels).
     */
    private const val DEFAULT_DRAG_STEP_SIZE = 10

    private const val DEFAULT_PINCH_DURATION = 500

    private const val EVENT_MIN_INTERVAL = 10

    fun pinch(
        startX1: Int, startY1: Int, startX2: Int, startY2: Int,
        endX1: Int, endY1: Int, endX2: Int, endY2: Int,
        durationMillis: Int = DEFAULT_PINCH_DURATION
    ) {
        var eventTime = SystemClock.uptimeMillis()
        val downTime = SystemClock.uptimeMillis()

        var eventX1 = startX1.toFloat()
        var eventY1 = startY1.toFloat()
        var eventX2 = startX2.toFloat()
        var eventY2 = startY2.toFloat()

        // specify the property for the two touch points
        val pp1 = MotionEvent.PointerProperties()
        pp1.id = 0
        pp1.toolType = MotionEvent.TOOL_TYPE_FINGER
        val pp2 = MotionEvent.PointerProperties()
        pp2.id = 1
        pp2.toolType = MotionEvent.TOOL_TYPE_FINGER

        val properties = arrayOf(pp1, pp2)

        // specify the coordinates of the two touch points
        // NOTE: pressure and size value must be specified to make gesture work
        val pc1 = MotionEvent.PointerCoords()
        pc1.x = eventX1
        pc1.y = eventY1
        pc1.pressure = 1.0f
        pc1.size = 1.0f
        val pc2 = MotionEvent.PointerCoords()
        pc2.x = eventX2
        pc2.y = eventY2
        pc2.pressure = 1.0f
        pc2.size = 1.0f

        val pointerCoords = arrayOf(pc1, pc2)

        // events sequence of pinch gesture
        // 1. send ACTION_DOWN event of one start point
        // 2. send ACTION_POINTER_2_DON of two start points
        // 3. send ACTION_MOVE of two middle points
        // 4. repeat step 3 with updated middle points (x,y) until reach the end points
        // 5. send ACTION_POINTER_2_UP of two end points
        // 6. send ACTION_UP of one end point

        val inst = InstrumentationRegistry.getInstrumentation()

        // step 1
        var event = MotionEvent.obtain(
            downTime, eventTime,
            MotionEvent.ACTION_DOWN, 1, properties,
            pointerCoords, 0, 0, 1.0f, 1.0f, 0, 0, 0, 0
        )
        inst.sendPointerSync(event)

        // step 2
        event = MotionEvent.obtain(
            downTime, eventTime,
            MotionEvent.ACTION_POINTER_2_DOWN, 2, properties,
            pointerCoords, 0, 0, 1.0f, 1.0f, 0, 0, 0, 0
        )
        inst.sendPointerSync(event)

        // step 3, 4
        val numSteps = durationMillis / EVENT_MIN_INTERVAL

        val stepX1 = (endX1 - startX1).toFloat() / numSteps
        val stepY1 = (endY1 - startY1).toFloat() / numSteps
        val stepX2 = (endX2 - startX2).toFloat() / numSteps
        val stepY2 = (endY2 - startY2).toFloat() / numSteps

        for (i in 1..numSteps) {
            // update the move events
            eventTime += EVENT_MIN_INTERVAL
            eventX1 += stepX1
            eventY1 += stepY1
            eventX2 += stepX2
            eventY2 += stepY2

            pc1.x = eventX1
            pc1.y = eventY1
            pc2.x = eventX2
            pc2.y = eventY2

            event = MotionEvent.obtain(
                downTime, eventTime,
                MotionEvent.ACTION_MOVE, 2, properties,
                pointerCoords, 0, 0, 1.0f, 1.0f, 0, 0, 0, 0
            )
            inst.sendPointerSync(event)
        }

        // step 5
        pc1.x = endX1.toFloat()
        pc1.y = endY1.toFloat()
        pc2.x = endX2.toFloat()
        pc2.y = endY2.toFloat()

        eventTime += EVENT_MIN_INTERVAL
        event = MotionEvent.obtain(
            downTime, eventTime,
            MotionEvent.ACTION_POINTER_2_UP, 2, properties,
            pointerCoords, 0, 0, 1.0f, 1.0f, 0, 0, 0, 0
        )
        inst.sendPointerSync(event)

        // step 6
        eventTime += EVENT_MIN_INTERVAL
        event = MotionEvent.obtain(
            downTime, eventTime,
            MotionEvent.ACTION_UP, 1, properties,
            pointerCoords, 0, 0, 1.0f, 1.0f, 0, 0, 0, 0
        )
        inst.sendPointerSync(event)
    }

    /**
     * Makes a drag or scroll gesture from a given start point towards provided destination point.
     * @param toX horizontal position where gesture will end.
     * @param toY vertical position where gesture will end.
     * @param stepSize step size in pixels between generated touch events.
     */
    fun drag(
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int,
        stepSize: Int = DEFAULT_DRAG_STEP_SIZE
    ) {
        val stepCountX = abs((fromX - toX) / stepSize)
        val stepCountY = abs((fromY - toY) / stepSize)
        val stepCount = (stepCountX + stepCountY) / 2

        val inst = InstrumentationRegistry.getInstrumentation()

        val downTime = SystemClock.uptimeMillis()
        var eventTime = SystemClock.uptimeMillis()

        val xStep = (toX - fromX).toFloat() / stepCount
        val yStep = (toY - fromY).toFloat() / stepCount

        var x = fromX.toFloat()
        var y = fromY.toFloat()

        var event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
        inst.sendPointerSync(event)
        inst.waitForIdleSync()

        for (i in 1..stepCount) {
            x += xStep
            y += yStep
            eventTime = SystemClock.uptimeMillis()
            event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, x, y, 0)
            inst.sendPointerSync(event)
            inst.waitForIdleSync()
        }

        eventTime = SystemClock.uptimeMillis()
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0)
        inst.sendPointerSync(event)
        inst.waitForIdleSync()
    }

    /**
     * Generates a double tap touch event at the center of provided view.
     *
     * @param v view where double tap will be made.
     */
    fun doubleTap(v: View) {
        doubleTap((v.left + v.right) / 2, (v.top + v.bottom) / 2)
    }

    /**
     * Generates a double tap touch event at provided x,y coordinates on the screen.
     * @param x horizontal coordinate.
     * @param y vertical coordinate.
     */
    private fun doubleTap(x: Int, y: Int) {
        val durationBetweenDoubleTap = (ViewConfiguration.getDoubleTapTimeout() / 1.5f).toLong()

        tap(x, y)
        Thread.sleep(durationBetweenDoubleTap)
        tap(x, y)
    }

    /**
     * Generates a touch tap at provided x,y coordinates on the screen.
     * @param x horizontal coordinate.
     * @param y vertical coordinate.
     */
    private fun tap(x: Int, y: Int) {
        touchDown(x, y)
        touchUp(x, y)
    }

    /**
     * Creates a touch event where one finger is pressed against the screen
     * at provided x,y position.
     * @param x horizontal coordinate.
     * @param y vertical coordinate.
     */
    private fun touchDown(x: Int, y: Int) {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()

        val event = MotionEvent.obtain(
            downTime,
            eventTime,
            MotionEvent.ACTION_DOWN,
            x.toFloat(),
            y.toFloat(),
            0
        )
        sendMotionEvent(event)
    }

    /**
     * Creates a touch event where one finger is raised from the screen at
     * provided x,y position.
     * @param x horizontal coordinate.
     * @param y vertical coordinate.
     */
    private fun touchUp(x: Int, y: Int) {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()

        val event = MotionEvent.obtain(
            downTime,
            eventTime,
            MotionEvent.ACTION_UP,
            x.toFloat(),
            y.toFloat(),
            0
        )
        sendMotionEvent(event)
    }

    /**
     * Sends a touch event.
     * @param event touch event to be sent.
     */
    private fun sendMotionEvent(event: MotionEvent) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.sendPointerSync(event)
    }
}