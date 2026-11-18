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
import com.irurueta.android.testutils.getPrivateProperty
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class RotationGestureDetectorTest {

    @After
    fun afterTest() {
        clearAllMocks()
        unmockkAll()
    }

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

        val event = mockk<MotionEvent>(relaxed = true)
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
        val event1 = mockk<MotionEvent>(relaxed = true)
        every { event1.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { event1.getPointerId(0) }.returns(POINTER_ID_1)

        assertFalse(detector.onTouchEvent(event1))

        // send 2nd event to set 2nd pointer ID
        val event2 = mockk<MotionEvent>(relaxed = true)
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

        val event = mockk<MotionEvent>(relaxed = true)
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

        val event1 = mockk<MotionEvent>(relaxed = true)
        every { event1.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { event1.getPointerId(0) }.returns(POINTER_ID_1)

        assertFalse(detector.onTouchEvent(event1))

        var pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_1, pointerId1)

        var pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(INVALID_POINTER_ID, pointerId2)

        val event2 = mockk<MotionEvent>(relaxed = true)
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
        val event1 = mockk<MotionEvent>(relaxed = true)
        every { event1.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { event1.getPointerId(0) }.returns(POINTER_ID_1)

        assertFalse(detector.onTouchEvent(event1))

        // send 2nd event to set 2nd pointer ID
        val event2 = mockk<MotionEvent>(relaxed = true)
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
        val event3 = mockk<MotionEvent>(relaxed = true)
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
        val event1 = mockk<MotionEvent>(relaxed = true)
        every { event1.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { event1.getPointerId(0) }.returns(POINTER_ID_1)

        assertFalse(detector.onTouchEvent(event1))

        // send 2nd event to set 2nd pointer ID
        val event2 = mockk<MotionEvent>(relaxed = true)
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
        val event3 = mockk<MotionEvent>(relaxed = true)
        every { event3.actionMasked }.returns(MotionEvent.ACTION_UP)

        assertFalse(detector.onTouchEvent(event3))

        pointerId1 = detector.getPrivateProperty("pointerId1")
        assertEquals(INVALID_POINTER_ID, pointerId1)

        pointerId2 = detector.getPrivateProperty("pointerId2")
        assertEquals(INVALID_POINTER_ID, pointerId2)
    }

    @Test
    fun onTouchEvent_whenActionPointerUp_setsInitialRotationValues() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>()
        val detector = RotationGestureDetector(listener)

        // send 1st event to set 1st pointer ID
        val event1 = mockk<MotionEvent>(relaxed = true)
        every { event1.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { event1.getPointerId(0) }.returns(POINTER_ID_1)

        assertFalse(detector.onTouchEvent(event1))

        // send 2nd event to set 2nd pointer ID
        val event2 = mockk<MotionEvent>(relaxed = true)
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
        val event3 = mockk<MotionEvent>(relaxed = true)
        every { event3.actionMasked }.returns(MotionEvent.ACTION_POINTER_UP)
        every { event3.actionIndex }.returns(POINTER_INDEX_2)
        every { event3.pointerCount }.returns(2)
        every { event3.getPointerId(POINTER_INDEX_1) }.returns(POINTER_ID_1)
        every { event3.getPointerId(POINTER_INDEX_2) }.returns(POINTER_ID_2)

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
        val event1 = mockk<MotionEvent>(relaxed = true)
        every { event1.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { event1.getPointerId(0) }.returns(POINTER_ID_1)

        assertFalse(detector.onTouchEvent(event1))

        // send 2nd event to set 2nd pointer ID
        val event2 = mockk<MotionEvent>(relaxed = true)
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
        val event3 = mockk<MotionEvent>(relaxed = true)
        every { event3.actionMasked }.returns(MotionEvent.ACTION_CANCEL)

        assertFalse(detector.onTouchEvent(event3))

        pointerId1 = detector.getPrivateProperty("pointerId1")
        assertEquals(INVALID_POINTER_ID, pointerId1)

        pointerId2 = detector.getPrivateProperty("pointerId2")
        assertEquals(INVALID_POINTER_ID, pointerId2)
    }

    @Test
    fun onTouchEvent_whenFirstPointerIsLifted_keepsRemainingPointerAndRestartsWithNewOne() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>()
        every { listener.onRotation(any(), any(), any(), any()) }.returns(true)
        val detector = RotationGestureDetector(listener)

        startTwoFingerGesture(detector)

        // 1st pointer is lifted while 2nd one remains on screen
        val event3 = mockk<MotionEvent>(relaxed = true)
        every { event3.actionMasked }.returns(MotionEvent.ACTION_POINTER_UP)
        every { event3.actionIndex }.returns(POINTER_INDEX_1)
        every { event3.pointerCount }.returns(2)
        every { event3.getPointerId(POINTER_INDEX_1) }.returns(POINTER_ID_1)
        every { event3.getPointerId(POINTER_INDEX_2) }.returns(POINTER_ID_2)

        assertFalse(detector.onTouchEvent(event3))

        // remaining pointer becomes the 1st one
        var pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_2, pointerId1)
        var pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(INVALID_POINTER_ID, pointerId2)

        // moving with a single finger does not notify any rotation
        val event4 = mockk<MotionEvent>(relaxed = true)
        every { event4.actionMasked }.returns(MotionEvent.ACTION_MOVE)
        assertFalse(detector.onTouchEvent(event4))
        verify(exactly = 0) { listener.onRotation(any(), any(), any(), any()) }

        // a new finger touches the screen
        val event5 = mockk<MotionEvent>(relaxed = true)
        every { event5.actionMasked }.returns(MotionEvent.ACTION_POINTER_DOWN)
        every { event5.actionIndex }.returns(1)
        every { event5.getPointerId(1) }.returns(POINTER_ID_3)
        every { event5.findPointerIndex(POINTER_ID_2) }.returns(0)
        every { event5.findPointerIndex(POINTER_ID_3) }.returns(1)
        every { event5.getX(0) }.returns(0.0f)
        every { event5.getY(0) }.returns(0.0f)
        every { event5.getX(1) }.returns(0.0f)
        every { event5.getY(1) }.returns(100.0f)

        assertFalse(detector.onTouchEvent(event5))

        pointerId1 = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_2, pointerId1)
        pointerId2 = detector.getPrivateProperty("pointerId2")
        assertEquals(POINTER_ID_3, pointerId2)

        // angle is restarted for the new pair of fingers
        assertEquals(detector.initialAngle, detector.currentAngle, 0.0f)
        assertEquals((Math.PI / 2.0).toFloat(), detector.initialAngle, 0.0f)
    }

    @Test
    fun onTouchEvent_whenTrackedPointerIsLiftedWithThreeFingers_tracksRemainingPairWithoutJump() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>()
        every { listener.onRotation(any(), any(), any(), any()) }.returns(true)
        val detector = RotationGestureDetector(listener)

        startTwoFingerGesture(detector)

        // 3rd finger touches the screen and is ignored
        val event3 = mockk<MotionEvent>(relaxed = true)
        every { event3.actionMasked }.returns(MotionEvent.ACTION_POINTER_DOWN)
        assertFalse(detector.onTouchEvent(event3))

        var pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_1, pointerId1)
        var pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(POINTER_ID_2, pointerId2)

        // 2nd finger is lifted, 1st and 3rd fingers remain
        val event4 = mockk<MotionEvent>(relaxed = true)
        every { event4.actionMasked }.returns(MotionEvent.ACTION_POINTER_UP)
        every { event4.actionIndex }.returns(1)
        every { event4.pointerCount }.returns(3)
        every { event4.getPointerId(0) }.returns(POINTER_ID_1)
        every { event4.getPointerId(1) }.returns(POINTER_ID_2)
        every { event4.getPointerId(2) }.returns(POINTER_ID_3)
        every { event4.findPointerIndex(POINTER_ID_1) }.returns(0)
        every { event4.findPointerIndex(POINTER_ID_3) }.returns(2)
        every { event4.getX(0) }.returns(0.0f)
        every { event4.getY(0) }.returns(0.0f)
        every { event4.getX(2) }.returns(-100.0f)
        every { event4.getY(2) }.returns(0.0f)

        assertFalse(detector.onTouchEvent(event4))

        pointerId1 = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_1, pointerId1)
        pointerId2 = detector.getPrivateProperty("pointerId2")
        assertEquals(POINTER_ID_3, pointerId2)

        // moving without changing finger positions reports no rotation change
        val event5 = mockk<MotionEvent>(relaxed = true)
        every { event5.actionMasked }.returns(MotionEvent.ACTION_MOVE)
        every { event5.findPointerIndex(POINTER_ID_1) }.returns(0)
        every { event5.findPointerIndex(POINTER_ID_3) }.returns(1)
        every { event5.getX(0) }.returns(0.0f)
        every { event5.getY(0) }.returns(0.0f)
        every { event5.getX(1) }.returns(-100.0f)
        every { event5.getY(1) }.returns(0.0f)

        assertTrue(detector.onTouchEvent(event5))
        verify(exactly = 1) { listener.onRotation(detector, 0.0f, -50.0f, 0.0f) }
    }

    @Test
    fun onTouchEvent_whenTrackedPointerIsMissingOnMove_makesNoAction() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>()
        val detector = RotationGestureDetector(listener)

        startTwoFingerGesture(detector)

        val event3 = mockk<MotionEvent>(relaxed = true)
        every { event3.actionMasked }.returns(MotionEvent.ACTION_MOVE)
        every { event3.findPointerIndex(POINTER_ID_1) }.returns(-1)
        every { event3.findPointerIndex(POINTER_ID_2) }.returns(0)

        assertFalse(detector.onTouchEvent(event3))
        verify { listener wasNot Called }
    }

    @Test
    fun onTouchEvent_whenUnknownAction_returnsFalseWithoutChanges() {
        val detector = RotationGestureDetector()

        startTwoFingerGesture(detector)

        val event = mockk<MotionEvent>(relaxed = true)
        every { event.actionMasked }.returns(MotionEvent.ACTION_OUTSIDE)

        assertFalse(detector.onTouchEvent(event))

        val pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_1, pointerId1)
        val pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(POINTER_ID_2, pointerId2)
    }

    @Test
    fun onTouchEvent_whenPointerDownAndNoFingerTracked_tracksFirstFinger() {
        val detector = RotationGestureDetector()

        val event = mockk<MotionEvent>(relaxed = true)
        every { event.actionMasked }.returns(MotionEvent.ACTION_POINTER_DOWN)
        every { event.actionIndex }.returns(ACTION_INDEX)
        every { event.getPointerId(ACTION_INDEX) }.returns(POINTER_ID_2)

        assertFalse(detector.onTouchEvent(event))

        val pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_2, pointerId1)
        val pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(INVALID_POINTER_ID, pointerId2)
    }

    @Test
    fun onTouchEvent_whenSecondTrackedPointerIsMissingOnMove_makesNoAction() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>()
        val detector = RotationGestureDetector(listener)

        startTwoFingerGesture(detector)

        val event = mockk<MotionEvent>(relaxed = true)
        every { event.actionMasked }.returns(MotionEvent.ACTION_MOVE)
        every { event.findPointerIndex(POINTER_ID_1) }.returns(0)
        every { event.findPointerIndex(POINTER_ID_2) }.returns(-1)

        assertFalse(detector.onTouchEvent(event))
        verify { listener wasNot Called }
    }

    @Test
    fun onTouchEvent_whenMoveAndNoListener_returnsFalse() {
        val detector = RotationGestureDetector()

        startTwoFingerGesture(detector)

        val event = mockk<MotionEvent>(relaxed = true)
        every { event.actionMasked }.returns(MotionEvent.ACTION_MOVE)
        every { event.findPointerIndex(POINTER_ID_1) }.returns(0)
        every { event.findPointerIndex(POINTER_ID_2) }.returns(1)
        every { event.getX(0) }.returns(0.0f)
        every { event.getY(0) }.returns(0.0f)
        every { event.getX(1) }.returns(0.0f)
        every { event.getY(1) }.returns(100.0f)

        assertFalse(detector.onTouchEvent(event))
        assertEquals(0.0f, detector.pivotX, 0.0f)
        assertEquals(50.0f, detector.pivotY, 0.0f)
    }

    @Test
    fun onTouchEvent_whenUntrackedPointerIsLifted_keepsTrackedPointers() {
        val detector = RotationGestureDetector()

        startTwoFingerGesture(detector)

        val event = mockk<MotionEvent>(relaxed = true)
        every { event.actionMasked }.returns(MotionEvent.ACTION_POINTER_UP)
        every { event.actionIndex }.returns(2)
        every { event.getPointerId(2) }.returns(POINTER_ID_3)

        assertFalse(detector.onTouchEvent(event))

        val pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_1, pointerId1)
        val pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(POINTER_ID_2, pointerId2)
    }

    @Test
    fun onTouchEvent_whenOnlyTrackedPointerIsLiftedAndAnotherRemains_tracksRemainingPointer() {
        val detector = RotationGestureDetector()

        val event1 = mockk<MotionEvent>(relaxed = true)
        every { event1.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { event1.getPointerId(0) }.returns(POINTER_ID_1)
        assertFalse(detector.onTouchEvent(event1))

        // 1st finger is lifted while an untracked finger remains
        val event2 = mockk<MotionEvent>(relaxed = true)
        every { event2.actionMasked }.returns(MotionEvent.ACTION_POINTER_UP)
        every { event2.actionIndex }.returns(0)
        every { event2.pointerCount }.returns(2)
        every { event2.getPointerId(0) }.returns(POINTER_ID_1)
        every { event2.getPointerId(1) }.returns(POINTER_ID_3)

        assertFalse(detector.onTouchEvent(event2))

        val pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_3, pointerId1)
        val pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(INVALID_POINTER_ID, pointerId2)
    }

    @Test
    fun onTouchEvent_whenOnlyTrackedPointerIsLiftedAndNoneRemains_stopsTracking() {
        val detector = RotationGestureDetector()

        val event1 = mockk<MotionEvent>(relaxed = true)
        every { event1.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { event1.getPointerId(0) }.returns(POINTER_ID_1)
        assertFalse(detector.onTouchEvent(event1))

        val event2 = mockk<MotionEvent>(relaxed = true)
        every { event2.actionMasked }.returns(MotionEvent.ACTION_POINTER_UP)
        every { event2.actionIndex }.returns(0)
        every { event2.pointerCount }.returns(1)
        every { event2.getPointerId(0) }.returns(POINTER_ID_1)

        assertFalse(detector.onTouchEvent(event2))

        val pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(INVALID_POINTER_ID, pointerId1)
        val pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(INVALID_POINTER_ID, pointerId2)
    }

    @Test
    fun onTouchEvent_whenSecondFingerIsNotFoundOnPointerDown_discardsSecondFinger() {
        val detector = RotationGestureDetector()

        val event1 = mockk<MotionEvent>(relaxed = true)
        every { event1.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { event1.getPointerId(0) }.returns(POINTER_ID_1)
        assertFalse(detector.onTouchEvent(event1))

        val event2 = mockk<MotionEvent>(relaxed = true)
        every { event2.actionMasked }.returns(MotionEvent.ACTION_POINTER_DOWN)
        every { event2.actionIndex }.returns(ACTION_INDEX)
        every { event2.getPointerId(ACTION_INDEX) }.returns(POINTER_ID_2)
        every { event2.findPointerIndex(POINTER_ID_1) }.returns(0)
        every { event2.findPointerIndex(POINTER_ID_2) }.returns(-1)

        assertFalse(detector.onTouchEvent(event2))

        val pointerId1: Int? = detector.getPrivateProperty("pointerId1")
        assertEquals(POINTER_ID_1, pointerId1)
        val pointerId2: Int? = detector.getPrivateProperty("pointerId2")
        assertEquals(INVALID_POINTER_ID, pointerId2)
    }

    @Test
    fun angularVelocity_whenClockwiseRotation_isPositive() {
        val detector = RotationGestureDetector()
        assertEquals(0.0f, detector.angularVelocity, 0.0f)

        startGesture(detector, 0.0f, 0L)
        rotate(detector, 0.0f, 0.1f, 5, 10L)

        // 0.1 rad every 10 ms
        assertEquals(10.0f, detector.angularVelocity, 0.01f)
    }

    @Test
    fun angularVelocity_whenCounterClockwiseRotation_isNegative() {
        val detector = RotationGestureDetector()

        startGesture(detector, 0.0f, 0L)
        rotate(detector, 0.0f, -0.05f, 5, 20L)

        // -0.05 rad every 20 ms
        assertEquals(-2.5f, detector.angularVelocity, 0.01f)
    }

    @Test
    fun angularVelocity_whenAngleWrapsAroundPi_usesWrappedDelta() {
        val detector = RotationGestureDetector()
        val start = PI.toFloat() - 0.05f

        startGesture(detector, start, 0L)
        rotate(detector, start, 0.1f, 4, 10L)

        assertEquals(10.0f, detector.angularVelocity, 0.05f)

        val negativeDetector = RotationGestureDetector()
        val negativeStart = -PI.toFloat() + 0.05f
        startGesture(negativeDetector, negativeStart, 0L)
        rotate(negativeDetector, negativeStart, -0.1f, 4, 10L)

        assertEquals(-10.0f, negativeDetector.angularVelocity, 0.05f)
    }

    @Test
    fun angularVelocity_whenGestureIsHeld_isZero() {
        val detector = RotationGestureDetector()

        startGesture(detector, 0.0f, 0L)
        rotate(detector, 0.0f, 0.0f, 5, 10L)

        assertEquals(0.0f, detector.angularVelocity, 0.0f)
    }

    @Test
    fun angularVelocity_whenEventTimeDoesNotAdvance_isZero() {
        val detector = RotationGestureDetector()

        startGesture(detector, 0.0f, 0L)
        rotate(detector, 0.0f, 0.1f, 3, 10L)
        assertTrue(detector.angularVelocity > 0.0f)

        detector.onTouchEvent(moveEvent(0.4f, 30L))

        assertEquals(0.0f, detector.angularVelocity, 0.0f)
    }

    @Test
    fun angularVelocity_whenNoisyEvent_isSmoothed() {
        val detector = RotationGestureDetector()

        startGesture(detector, 0.0f, 0L)
        rotate(detector, 0.0f, 0.1f, 5, 10L)
        // one sample moving twice as fast
        detector.onTouchEvent(moveEvent(0.5f + 0.2f, 60L))

        val velocity = detector.angularVelocity
        assertTrue(velocity > 10.0f)
        assertTrue(velocity < 20.0f)
    }

    @Test
    fun angularVelocity_whenNewTrackingStarts_isReset() {
        val detector = RotationGestureDetector()

        startGesture(detector, 0.0f, 0L)
        rotate(detector, 0.0f, 0.1f, 3, 10L)
        assertTrue(detector.angularVelocity > 0.0f)

        // start a new gesture
        detector.onTouchEvent(upEvent(40L))
        startGesture(detector, 0.0f, 100L)

        assertEquals(0.0f, detector.angularVelocity, 0.0f)
    }

    @Test
    fun onRotationEnd_whenActionPointerUp_notifiesOnceWithVelocity() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>(relaxed = true)
        every { listener.onRotation(any(), any(), any(), any()) }.returns(true)
        val detector = RotationGestureDetector(listener)

        startGesture(detector, 0.0f, 0L)
        rotate(detector, 0.0f, 0.1f, 5, 10L)

        val velocity = slot<Float>()
        detector.onTouchEvent(pointerUpEvent(55L))
        // later up event must not notify again
        detector.onTouchEvent(upEvent(60L))

        verify(exactly = 1) { listener.onRotationEnd(detector, capture(velocity), any(), any()) }
        assertEquals(10.0f, velocity.captured, 0.01f)
        assertEquals(0.0f, detector.angularVelocity, 0.0f)
    }

    @Test
    fun onRotationEnd_whenActionUp_notifiesOnceWithVelocityAndPivot() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>(relaxed = true)
        every { listener.onRotation(any(), any(), any(), any()) }.returns(true)
        val detector = RotationGestureDetector(listener)

        startGesture(detector, 0.0f, 0L)
        rotate(detector, 0.0f, -0.1f, 5, 10L)
        val pivotX = detector.pivotX
        val pivotY = detector.pivotY

        detector.onTouchEvent(upEvent(55L))
        detector.onTouchEvent(upEvent(56L))

        val velocity = slot<Float>()
        verify(exactly = 1) {
            listener.onRotationEnd(detector, capture(velocity), pivotX, pivotY)
        }
        assertEquals(-10.0f, velocity.captured, 0.01f)
    }

    @Test
    fun onRotationEnd_whenActionCancel_notifiesZeroVelocity() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>(relaxed = true)
        every { listener.onRotation(any(), any(), any(), any()) }.returns(true)
        val detector = RotationGestureDetector(listener)

        startGesture(detector, 0.0f, 0L)
        rotate(detector, 0.0f, 0.1f, 5, 10L)

        val cancel = mockk<MotionEvent>(relaxed = true)
        every { cancel.actionMasked }.returns(MotionEvent.ACTION_CANCEL)
        every { cancel.eventTime }.returns(55L)
        detector.onTouchEvent(cancel)
        detector.onTouchEvent(cancel)

        verify(exactly = 1) { listener.onRotationEnd(detector, 0.0f, any(), any()) }
    }

    @Test
    fun onRotationEnd_whenReleasedLongAfterLastMove_notifiesZeroVelocity() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>(relaxed = true)
        every { listener.onRotation(any(), any(), any(), any()) }.returns(true)
        val detector = RotationGestureDetector(listener)

        startGesture(detector, 0.0f, 0L)
        rotate(detector, 0.0f, 0.1f, 5, 10L)

        detector.onTouchEvent(upEvent(500L))

        verify(exactly = 1) { listener.onRotationEnd(detector, 0.0f, any(), any()) }
    }

    @Test
    fun onRotationEnd_whenNoRotationMovement_isNotNotified() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>(relaxed = true)
        val detector = RotationGestureDetector(listener)

        startGesture(detector, 0.0f, 0L)
        detector.onTouchEvent(pointerUpEvent(10L))
        startGesture(detector, 0.0f, 20L)
        detector.onTouchEvent(upEvent(30L))
        startGesture(detector, 0.0f, 40L)
        val cancel = mockk<MotionEvent>(relaxed = true)
        every { cancel.actionMasked }.returns(MotionEvent.ACTION_CANCEL)
        detector.onTouchEvent(cancel)

        verify(exactly = 0) { listener.onRotationEnd(any(), any(), any(), any()) }
    }

    @Test
    fun onRotationEnd_whenOnlyOneFingerTracked_isNotNotified() {
        val listener = mockk<RotationGestureDetector.OnRotationGestureListener>(relaxed = true)
        val detector = RotationGestureDetector(listener)

        val down = mockk<MotionEvent>(relaxed = true)
        every { down.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { down.getPointerId(0) }.returns(POINTER_ID_1)
        detector.onTouchEvent(down)
        detector.onTouchEvent(upEvent(10L))

        verify(exactly = 0) { listener.onRotationEnd(any(), any(), any(), any()) }
    }

    @Test
    fun onRotationEnd_whenListenerDoesNotOverride_doesNotFail() {
        val listener = object : RotationGestureDetector.OnRotationGestureListener {
            var rotations = 0

            override fun onRotation(
                gestureDetector: RotationGestureDetector,
                angle: Float,
                pivotX: Float,
                pivotY: Float
            ): Boolean {
                rotations++
                return true
            }
        }
        val detector = RotationGestureDetector(listener)

        startGesture(detector, 0.0f, 0L)
        rotate(detector, 0.0f, 0.1f, 3, 10L)
        detector.onTouchEvent(upEvent(40L))

        assertEquals(3, listener.rotations)
    }

    private fun fingerAngleEvent(action: Int, angle: Float, time: Long): MotionEvent {
        val event = mockk<MotionEvent>(relaxed = true)
        every { event.actionMasked }.returns(action)
        every { event.actionIndex }.returns(ACTION_INDEX)
        every { event.eventTime }.returns(time)
        every { event.pointerCount }.returns(2)
        every { event.getPointerId(0) }.returns(POINTER_ID_1)
        every { event.getPointerId(1) }.returns(POINTER_ID_2)
        every { event.findPointerIndex(POINTER_ID_1) }.returns(POINTER_INDEX_1)
        every { event.findPointerIndex(POINTER_ID_2) }.returns(POINTER_INDEX_2)
        every { event.getX(POINTER_INDEX_1) }.returns(0.0f)
        every { event.getY(POINTER_INDEX_1) }.returns(0.0f)
        every { event.getX(POINTER_INDEX_2) }.returns(100.0f * cos(angle))
        every { event.getY(POINTER_INDEX_2) }.returns(100.0f * sin(angle))
        return event
    }

    private fun moveEvent(angle: Float, time: Long) =
        fingerAngleEvent(MotionEvent.ACTION_MOVE, angle, time)

    private fun upEvent(time: Long) = fingerAngleEvent(MotionEvent.ACTION_UP, 0.0f, time)

    private fun pointerUpEvent(time: Long) =
        fingerAngleEvent(MotionEvent.ACTION_POINTER_UP, 0.0f, time)

    private fun startGesture(detector: RotationGestureDetector, angle: Float, time: Long) {
        val down = mockk<MotionEvent>(relaxed = true)
        every { down.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { down.getPointerId(0) }.returns(POINTER_ID_1)
        detector.onTouchEvent(down)
        detector.onTouchEvent(fingerAngleEvent(MotionEvent.ACTION_POINTER_DOWN, angle, time))
    }

    /**
     * Sends [steps] move events, each rotating [step] radians and [interval] ms after the previous.
     */
    private fun rotate(
        detector: RotationGestureDetector,
        startAngle: Float,
        step: Float,
        steps: Int,
        interval: Long
    ) {
        var angle = startAngle
        var time = 0L
        for (i in 1..steps) {
            angle += step
            time += interval
            detector.onTouchEvent(moveEvent(angle, time))
        }
    }

    private fun startTwoFingerGesture(detector: RotationGestureDetector) {
        val event1 = mockk<MotionEvent>(relaxed = true)
        every { event1.actionMasked }.returns(MotionEvent.ACTION_DOWN)
        every { event1.getPointerId(0) }.returns(POINTER_ID_1)

        assertFalse(detector.onTouchEvent(event1))

        val event2 = mockk<MotionEvent>(relaxed = true)
        every { event2.actionMasked }.returns(MotionEvent.ACTION_POINTER_DOWN)
        every { event2.actionIndex }.returns(ACTION_INDEX)
        every { event2.getPointerId(ACTION_INDEX) }.returns(POINTER_ID_2)
        every { event2.findPointerIndex(POINTER_ID_1) }.returns(POINTER_INDEX_1)
        every { event2.getX(POINTER_INDEX_1) }.returns(0.0f)
        every { event2.getY(POINTER_INDEX_1) }.returns(0.0f)
        every { event2.findPointerIndex(POINTER_ID_2) }.returns(POINTER_INDEX_2)
        every { event2.getX(POINTER_INDEX_2) }.returns(100.0f)
        every { event2.getY(POINTER_INDEX_2) }.returns(0.0f)

        assertFalse(detector.onTouchEvent(event2))
    }

    private companion object {
        const val INVALID_POINTER_ID = -1

        const val POINTER_ID_1 = 0

        const val POINTER_ID_2 = 1

        const val POINTER_ID_3 = 2

        const val ACTION_INDEX = 1

        const val POINTER_INDEX_1 = 0

        const val POINTER_INDEX_2 = 1
    }
}