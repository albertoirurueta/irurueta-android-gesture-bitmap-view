/*
 * Copyright (C) 2025 Alberto Irurueta Carro (alberto@irurueta.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irurueta.android.gesturebitmap

import android.view.MotionEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.atan2
import kotlin.random.Random

class RotationGestureDetectorTest {

    @Test
    fun constructor_whenNoListener_setsExpectedValues() {
        val detector = RotationGestureDetector()

        // check
        assertNull(detector.listener)
        assertEquals(0.0f, detector.initialX1, 0.0f)
        assertEquals(0.0f, detector.initialY1, 0.0f)
        assertEquals(0.0f, detector.initialX2, 0.0f)
        assertEquals(0.0f, detector.initialY2, 0.0f)
        assertEquals(0.0f, detector.currentX1, 0.0f)
        assertEquals(0.0f, detector.currentY1, 0.0f)
        assertEquals(0.0f, detector.currentX2, 0.0f)
        assertEquals(0.0f, detector.currentY2, 0.0f)
        assertEquals(0.0f, detector.initialAngle, 0.0f)
        assertEquals(0.0f, detector.currentAngle, 0.0f)
        assertEquals(0.0f, detector.deltaAngleStart, 0.0f)
        assertEquals(0.0f, detector.deltaAngle, 0.0f)
        assertEquals(0.0f, detector.pivotX, 0.0f)
        assertEquals(0.0f, detector.pivotY, 0.0f)

        val pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(INVALID_POINTER_ID, pointerId1)

        val pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(INVALID_POINTER_ID, pointerId2)
    }

    @Test
    fun constructor_whenListener_setsExpectedValues() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>()
        val detector = RotationGestureDetector(listener)

        // check
        assertSame(listener, detector.listener)
        assertEquals(0.0f, detector.initialX1, 0.0f)
        assertEquals(0.0f, detector.initialY1, 0.0f)
        assertEquals(0.0f, detector.initialX2, 0.0f)
        assertEquals(0.0f, detector.initialY2, 0.0f)
        assertEquals(0.0f, detector.currentX1, 0.0f)
        assertEquals(0.0f, detector.currentY1, 0.0f)
        assertEquals(0.0f, detector.currentX2, 0.0f)
        assertEquals(0.0f, detector.currentY2, 0.0f)
        assertEquals(0.0f, detector.initialAngle, 0.0f)
        assertEquals(0.0f, detector.currentAngle, 0.0f)
        assertEquals(0.0f, detector.deltaAngleStart, 0.0f)
        assertEquals(0.0f, detector.deltaAngle, 0.0f)
        assertEquals(0.0f, detector.pivotX, 0.0f)
        assertEquals(0.0f, detector.pivotY, 0.0f)

        val pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(INVALID_POINTER_ID, pointerId1)

        val pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(INVALID_POINTER_ID, pointerId2)
    }

    @Test
    fun listener_getSetsExpectedValue() {
        val detector = RotationGestureDetector()

        // check default value
        assertNull(detector.listener)

        // set new value
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>()
        detector.listener = listener

        // check
        assertSame(listener, detector.listener)
    }

    @Test
    fun onTouchEvent_whenNoEvent_returnsWithoutChanges() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>()
        val detector = RotationGestureDetector(listener)

        assertFalse(detector.onTouchEvent(null))

        assertEquals(0.0f, detector.initialX1, 0.0f)
        assertEquals(0.0f, detector.initialY1, 0.0f)
        assertEquals(0.0f, detector.initialX2, 0.0f)
        assertEquals(0.0f, detector.initialY2, 0.0f)
        assertEquals(0.0f, detector.currentX1, 0.0f)
        assertEquals(0.0f, detector.currentY1, 0.0f)
        assertEquals(0.0f, detector.currentX2, 0.0f)
        assertEquals(0.0f, detector.currentY2, 0.0f)
        assertEquals(0.0f, detector.initialAngle, 0.0f)
        assertEquals(0.0f, detector.currentAngle, 0.0f)
        assertEquals(0.0f, detector.deltaAngleStart, 0.0f)
        assertEquals(0.0f, detector.deltaAngle, 0.0f)
        assertEquals(0.0f, detector.pivotX, 0.0f)
        assertEquals(0.0f, detector.pivotY, 0.0f)

        val pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(INVALID_POINTER_ID, pointerId1)

        val pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(INVALID_POINTER_ID, pointerId2)
    }

    @Test
    fun onTouchEvent_when1stFingerActionDown_returnsWithoutChanges() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>()
        val detector = RotationGestureDetector(listener)

        val event = mockk<MotionEvent>()
        every { event.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { event.getPointerId(0) }.returns(POINTER_ID_1)

        assertFalse(detector.onTouchEvent(event))

        assertEquals(0.0f, detector.initialX1, 0.0f)
        assertEquals(0.0f, detector.initialY1, 0.0f)
        assertEquals(0.0f, detector.initialX2, 0.0f)
        assertEquals(0.0f, detector.initialY2, 0.0f)
        assertEquals(0.0f, detector.currentX1, 0.0f)
        assertEquals(0.0f, detector.currentY1, 0.0f)
        assertEquals(0.0f, detector.currentX2, 0.0f)
        assertEquals(0.0f, detector.currentY2, 0.0f)
        assertEquals(0.0f, detector.initialAngle, 0.0f)
        assertEquals(0.0f, detector.currentAngle, 0.0f)
        assertEquals(0.0f, detector.deltaAngleStart, 0.0f)
        assertEquals(0.0f, detector.deltaAngle, 0.0f)
        assertEquals(0.0f, detector.pivotX, 0.0f)
        assertEquals(0.0f, detector.pivotY, 0.0f)

        val pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_1, pointerId1)

        val pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(INVALID_POINTER_ID, pointerId2)
    }

    @Test
    fun onTouchEvent_when2ndFingerActionDown_setsInitialRotationValues() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>()
        val detector = RotationGestureDetector(listener)

        // send 1st event to set 1st pointer ID
        val event1 = mockk<MotionEvent>()
        every { event1.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { event1.getPointerId(0) }.returns(POINTER_ID_1)

        assertFalse(detector.onTouchEvent(event1))

        // send 2nd event to set 2nd pointer ID
        val event2 = mockk<MotionEvent>()
        every { event2.actionMasked }.returns(MotionEvent.ACTION_POINTER_DOWN)
        every { event2.actionIndex }.returns(ACTION_INDEX)
        every { event2.getPointerId(ACTION_INDEX) }.returns(POINTER_ID_2)
        every { event2.findPointerIndex(POINTER_ID_1) }.returns(POINTER_INDEX_1)
        val x1 = Random.Default.nextFloat()
        every { event2.getX(POINTER_INDEX_1) }.returns(x1)
        val y1 = Random.Default.nextFloat()
        every { event2.getY(POINTER_INDEX_1) }.returns(y1)
        every { event2.findPointerIndex(POINTER_ID_2) }.returns(POINTER_INDEX_2)
        val x2 = Random.Default.nextFloat()
        every { event2.getX(POINTER_INDEX_2) }.returns(x2)
        val y2 = Random.Default.nextFloat()
        every { event2.getY(POINTER_INDEX_2) }.returns(y2)

        assertFalse(detector.onTouchEvent(event2))

        val pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_1, pointerId1)

        val pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(POINTER_ID_2, pointerId2)

        assertEquals(x1, detector.initialX1, 0.0f)
        assertEquals(y1, detector.initialY1, 0.0f)
        assertEquals(x2, detector.initialX2, 0.0f)
        assertEquals(y2, detector.initialY2, 0.0f)
        val pivotX = 0.5f * (x1 + x2)
        assertEquals(pivotX, detector.pivotX, 0.0f)
        val pivotY = 0.5f * (y1 + y2)
        assertEquals(pivotY, detector.pivotY, 0.0f)

        val angleBetweenFingers = atan2(y2 - y1, x2 - x1)
        assertEquals(angleBetweenFingers, detector.initialAngle, 0.0f)
        assertEquals(angleBetweenFingers, detector.currentAngle, 0.0f)
        assertEquals(0.0f, detector.deltaAngleStart, 0.0f)
        assertEquals(0.0f, detector.deltaAngle, 0.0f)
    }

    @Test
    fun onTouchEvent_whenInvalidPointer1ActionMove_returnsWithoutChanges() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>()
        val detector = RotationGestureDetector(listener)

        val event = mockk<MotionEvent>()
        every { event.actionMasked }.returns(MotionEvent.ACTION_MOVE)

        assertFalse(detector.onTouchEvent(event))

        assertEquals(0.0f, detector.initialX1, 0.0f)
        assertEquals(0.0f, detector.initialY1, 0.0f)
        assertEquals(0.0f, detector.initialX2, 0.0f)
        assertEquals(0.0f, detector.initialY2, 0.0f)
        assertEquals(0.0f, detector.currentX1, 0.0f)
        assertEquals(0.0f, detector.currentY1, 0.0f)
        assertEquals(0.0f, detector.currentX2, 0.0f)
        assertEquals(0.0f, detector.currentY2, 0.0f)
        assertEquals(0.0f, detector.initialAngle, 0.0f)
        assertEquals(0.0f, detector.currentAngle, 0.0f)
        assertEquals(0.0f, detector.deltaAngleStart, 0.0f)
        assertEquals(0.0f, detector.deltaAngle, 0.0f)
        assertEquals(0.0f, detector.pivotX, 0.0f)
        assertEquals(0.0f, detector.pivotY, 0.0f)

        val pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(INVALID_POINTER_ID, pointerId1)

        val pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(INVALID_POINTER_ID, pointerId2)
    }

    @Test
    fun onTouchEvent_whenInvalidPointer2ActionMove_returnsWithoutChanges() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>()
        val detector = RotationGestureDetector(listener)

        val event1 = mockk<MotionEvent>()
        every { event1.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { event1.getPointerId(0) }.returns(POINTER_ID_1)

        assertFalse(detector.onTouchEvent(event1))

        var pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_1, pointerId1)

        var pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(INVALID_POINTER_ID, pointerId2)

        val event2 = mockk<MotionEvent>()
        every { event2.actionMasked }.returns(MotionEvent.ACTION_MOVE)

        assertFalse(detector.onTouchEvent(event2))

        assertEquals(0.0f, detector.initialX1, 0.0f)
        assertEquals(0.0f, detector.initialY1, 0.0f)
        assertEquals(0.0f, detector.initialX2, 0.0f)
        assertEquals(0.0f, detector.initialY2, 0.0f)
        assertEquals(0.0f, detector.currentX1, 0.0f)
        assertEquals(0.0f, detector.currentY1, 0.0f)
        assertEquals(0.0f, detector.currentX2, 0.0f)
        assertEquals(0.0f, detector.currentY2, 0.0f)
        assertEquals(0.0f, detector.initialAngle, 0.0f)
        assertEquals(0.0f, detector.currentAngle, 0.0f)
        assertEquals(0.0f, detector.deltaAngleStart, 0.0f)
        assertEquals(0.0f, detector.deltaAngle, 0.0f)
        assertEquals(0.0f, detector.pivotX, 0.0f)
        assertEquals(0.0f, detector.pivotY, 0.0f)

        pointerId1 = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_1, pointerId1)

        pointerId2 = detector.getPrivateProperty("pointerId2")
        assertEquals(INVALID_POINTER_ID, pointerId2)
    }

    @Test
    fun onTouchEvent_whenValidActionMove_updatesPivotCurrentAngleAndDeltaAngles() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>()
        every { listener.onRotation(any(), any(), any(), any()) }.returns(true)

        val detector = RotationGestureDetector(listener)

        // send 1st event to set 1st pointer ID
        val event1 = mockk<MotionEvent>()
        every { event1.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { event1.getPointerId(0) }.returns(POINTER_ID_1)

        assertFalse(detector.onTouchEvent(event1))

        // send 2nd event to set 2nd pointer ID
        val event2 = mockk<MotionEvent>()
        every { event2.actionMasked }.returns(MotionEvent.ACTION_POINTER_DOWN)
        every { event2.actionIndex }.returns(ACTION_INDEX)
        every { event2.getPointerId(ACTION_INDEX) }.returns(POINTER_ID_2)
        every { event2.findPointerIndex(POINTER_ID_1) }.returns(POINTER_INDEX_1)
        val x1 = Random.Default.nextFloat()
        every { event2.getX(POINTER_INDEX_1) }.returns(x1)
        val y1 = Random.Default.nextFloat()
        every { event2.getY(POINTER_INDEX_1) }.returns(y1)
        every { event2.findPointerIndex(POINTER_ID_2) }.returns(POINTER_INDEX_2)
        val x2 = Random.Default.nextFloat()
        every { event2.getX(POINTER_INDEX_2) }.returns(x2)
        val y2 = Random.Default.nextFloat()
        every { event2.getY(POINTER_INDEX_2) }.returns(y2)

        assertFalse(detector.onTouchEvent(event2))

        var pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_1, pointerId1)

        var pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(POINTER_ID_2, pointerId2)

        assertEquals(x1, detector.initialX1, 0.0f)
        assertEquals(y1, detector.initialY1, 0.0f)
        assertEquals(x2, detector.initialX2, 0.0f)
        assertEquals(y2, detector.initialY2, 0.0f)
        var pivotX = 0.5f * (x1 + x2)
        assertEquals(pivotX, detector.pivotX, 0.0f)
        var pivotY = 0.5f * (y1 + y2)
        assertEquals(pivotY, detector.pivotY, 0.0f)

        val angleBetweenFingers = atan2(y2 - y1, x2 - x1)
        assertEquals(angleBetweenFingers, detector.initialAngle, 0.0f)
        assertEquals(angleBetweenFingers, detector.currentAngle, 0.0f)
        assertEquals(0.0f, detector.deltaAngleStart, 0.0f)
        assertEquals(0.0f, detector.deltaAngle, 0.0f)

        // send 3rd event to move points
        val event3 = mockk<MotionEvent>()
        every { event3.actionMasked }.returns(MotionEvent.ACTION_MOVE)
        every { event3.findPointerIndex(POINTER_ID_1) }.returns(POINTER_INDEX_1)
        val x1b = Random.Default.nextFloat()
        every { event3.getX(POINTER_INDEX_1) }.returns(x1b)
        val y1b = Random.Default.nextFloat()
        every { event3.getY(POINTER_INDEX_1) }.returns(y1b)
        every { event3.findPointerIndex(POINTER_ID_2) }.returns(POINTER_INDEX_2)
        val x2b = Random.Default.nextFloat()
        every { event3.getX(POINTER_INDEX_2) }.returns(x2b)
        val y2b = Random.Default.nextFloat()
        every { event3.getY(POINTER_INDEX_2) }.returns(y2b)

        assertTrue(detector.onTouchEvent(event3))

        pointerId1 = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_1, pointerId1)

        pointerId2 = detector.getPrivateProperty("pointerId2")
        assertEquals(POINTER_ID_2, pointerId2)

        assertEquals(x1, detector.initialX1, 0.0f)
        assertEquals(y1, detector.initialY1, 0.0f)
        assertEquals(x2, detector.initialX2, 0.0f)
        assertEquals(y2, detector.initialY2, 0.0f)
        assertEquals(x1b, detector.currentX1, 0.0f)
        assertEquals(y1b, detector.currentY1, 0.0f)
        assertEquals(x2b, detector.currentX2, 0.0f)
        assertEquals(y2b, detector.currentY2, 0.0f)
        pivotX = 0.5f * (x1b + x2b)
        assertEquals(pivotX, detector.pivotX, 0.0f)
        pivotY = 0.5f * (y1b + y2b)
        assertEquals(pivotY, detector.pivotY, 0.0f)

        assertEquals(angleBetweenFingers, detector.initialAngle, 0.0f)
        val angleBetweenFingers2 = atan2(y2b - y1b, x2b - x1b)
        assertEquals(angleBetweenFingers2, detector.currentAngle, 0.0f)
        assertEquals(angleBetweenFingers2 - angleBetweenFingers, detector.deltaAngleStart, 0.0f)
        assertEquals(angleBetweenFingers2 - angleBetweenFingers, detector.deltaAngle, 0.0f)

        verify(exactly = 1) {
            listener.onRotation(
                detector,
                detector.deltaAngle,
                detector.pivotX,
                detector.pivotY
            )
        }
    }

    @Test
    fun onTouchEvent_whenActionUp_setsInitialRotationValues() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>()
        val detector = RotationGestureDetector(listener)

        // send 1st event to set 1st pointer ID
        val event1 = mockk<MotionEvent>()
        every { event1.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { event1.getPointerId(0) }.returns(POINTER_ID_1)

        assertFalse(detector.onTouchEvent(event1))

        // send 2nd event to set 2nd pointer ID
        val event2 = mockk<MotionEvent>()
        every { event2.actionMasked }.returns(MotionEvent.ACTION_POINTER_DOWN)
        every { event2.actionIndex }.returns(ACTION_INDEX)
        every { event2.getPointerId(ACTION_INDEX) }.returns(POINTER_ID_2)
        every { event2.findPointerIndex(POINTER_ID_1) }.returns(POINTER_INDEX_1)
        val x1 = Random.Default.nextFloat()
        every { event2.getX(POINTER_INDEX_1) }.returns(x1)
        val y1 = Random.Default.nextFloat()
        every { event2.getY(POINTER_INDEX_1) }.returns(y1)
        every { event2.findPointerIndex(POINTER_ID_2) }.returns(POINTER_INDEX_2)
        val x2 = Random.Default.nextFloat()
        every { event2.getX(POINTER_INDEX_2) }.returns(x2)
        val y2 = Random.Default.nextFloat()
        every { event2.getY(POINTER_INDEX_2) }.returns(y2)

        assertFalse(detector.onTouchEvent(event2))

        var pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_1, pointerId1)

        var pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(POINTER_ID_2, pointerId2)

        // send 3rd event to raise 1st pointer
        val event3 = mockk<MotionEvent>()
        every { event3.actionMasked }.returns(MotionEvent.ACTION_UP)

        assertFalse(detector.onTouchEvent(event3))

        pointerId1 = detector.getPrivateProperty("pointerId1")
        assertEquals(INVALID_POINTER_ID, pointerId1)

        pointerId2 = detector.getPrivateProperty("pointerId2")
        assertEquals(POINTER_ID_2, pointerId2)
    }

    @Test
    fun onTouchEvent_whenActionPointerUp_setsInitialRotationValues() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>()
        val detector = RotationGestureDetector(listener)

        // send 1st event to set 1st pointer ID
        val event1 = mockk<MotionEvent>()
        every { event1.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { event1.getPointerId(0) }.returns(POINTER_ID_1)

        assertFalse(detector.onTouchEvent(event1))

        // send 2nd event to set 2nd pointer ID
        val event2 = mockk<MotionEvent>()
        every { event2.actionMasked }.returns(MotionEvent.ACTION_POINTER_DOWN)
        every { event2.actionIndex }.returns(ACTION_INDEX)
        every { event2.getPointerId(ACTION_INDEX) }.returns(POINTER_ID_2)
        every { event2.findPointerIndex(POINTER_ID_1) }.returns(POINTER_INDEX_1)
        val x1 = Random.Default.nextFloat()
        every { event2.getX(POINTER_INDEX_1) }.returns(x1)
        val y1 = Random.Default.nextFloat()
        every { event2.getY(POINTER_INDEX_1) }.returns(y1)
        every { event2.findPointerIndex(POINTER_ID_2) }.returns(POINTER_INDEX_2)
        val x2 = Random.Default.nextFloat()
        every { event2.getX(POINTER_INDEX_2) }.returns(x2)
        val y2 = Random.Default.nextFloat()
        every { event2.getY(POINTER_INDEX_2) }.returns(y2)

        assertFalse(detector.onTouchEvent(event2))

        var pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_1, pointerId1)

        var pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(POINTER_ID_2, pointerId2)

        // send 3rd event to raise 2nd pointer
        val event3 = mockk<MotionEvent>()
        every { event3.actionMasked }.returns(MotionEvent.ACTION_POINTER_UP)

        assertFalse(detector.onTouchEvent(event3))

        pointerId1 = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_1, pointerId1)

        pointerId2 = detector.getPrivateProperty("pointerId2")
        assertEquals(INVALID_POINTER_ID, pointerId2)
    }

    @Test
    fun onTouchEvent_whenActionCancel_setsInitialRotationValues() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>()
        val detector = RotationGestureDetector(listener)

        // send 1st event to set 1st pointer ID
        val event1 = mockk<MotionEvent>()
        every { event1.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { event1.getPointerId(0) }.returns(POINTER_ID_1)

        assertFalse(detector.onTouchEvent(event1))

        // send 2nd event to set 2nd pointer ID
        val event2 = mockk<MotionEvent>()
        every { event2.actionMasked }.returns(MotionEvent.ACTION_POINTER_DOWN)
        every { event2.actionIndex }.returns(ACTION_INDEX)
        every { event2.getPointerId(ACTION_INDEX) }.returns(POINTER_ID_2)
        every { event2.findPointerIndex(POINTER_ID_1) }.returns(POINTER_INDEX_1)
        val x1 = Random.Default.nextFloat()
        every { event2.getX(POINTER_INDEX_1) }.returns(x1)
        val y1 = Random.Default.nextFloat()
        every { event2.getY(POINTER_INDEX_1) }.returns(y1)
        every { event2.findPointerIndex(POINTER_ID_2) }.returns(POINTER_INDEX_2)
        val x2 = Random.Default.nextFloat()
        every { event2.getX(POINTER_INDEX_2) }.returns(x2)
        val y2 = Random.Default.nextFloat()
        every { event2.getY(POINTER_INDEX_2) }.returns(y2)

        assertFalse(detector.onTouchEvent(event2))

        var pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_1, pointerId1)

        var pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(POINTER_ID_2, pointerId2)

        // send 3rd event to cancel touch event
        val event3 = mockk<MotionEvent>()
        every { event3.actionMasked }.returns(MotionEvent.ACTION_CANCEL)

        assertFalse(detector.onTouchEvent(event3))

        pointerId1 = detector.getPrivateProperty("pointerId1")
        assertEquals(INVALID_POINTER_ID, pointerId1)

        pointerId2 = detector.getPrivateProperty("pointerId2")
        assertEquals(INVALID_POINTER_ID, pointerId2)
    }

    private companion object {
        const val INVALID_POINTER_ID = -1

        const val POINTER_ID_1 = 0

        const val POINTER_ID_2 = 1

        const val ACTION_INDEX = 1

        const val POINTER_INDEX_1 = 0

        const val POINTER_INDEX_2 = 1
    }
}