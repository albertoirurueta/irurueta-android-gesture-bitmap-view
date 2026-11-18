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

import android.graphics.Matrix
import android.graphics.RectF
import com.irurueta.android.gesturebitmap.GestureBitmapView.DisplayType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BaseMatrixCalculatorTest {

    @Test
    fun compute_withNone_returnsIdentity() {
        assertTrue(compute(DisplayType.NONE).isIdentity)
    }

    @Test
    fun compute_withFitIfBiggerAndSmallerImage_returnsIdentity() {
        assertTrue(compute(DisplayType.FIT_IF_BIGGER, SMALL, SMALL).isIdentity)
    }

    @Test
    fun compute_withFitIfBiggerAndBiggerImage_fitsAndCentersImage() {
        // image is twice as wide as the view: scaled by 0.5 and centered vertically
        assertRect(0.0f, 25.0f, 200.0f, 75.0f, mapped(DisplayType.FIT_IF_BIGGER))
    }

    @Test
    fun compute_withFitXTop_fitsWidthAndAlignsToTop() {
        assertRect(0.0f, 0.0f, 200.0f, 50.0f, mapped(DisplayType.FIT_X_TOP))
    }

    @Test
    fun compute_withFitXBottom_fitsWidthAndAlignsToBottom() {
        assertRect(0.0f, 50.0f, 200.0f, 100.0f, mapped(DisplayType.FIT_X_BOTTOM))
    }

    @Test
    fun compute_withFitXCenter_fitsWidthAndCentersVertically() {
        assertRect(0.0f, 25.0f, 200.0f, 75.0f, mapped(DisplayType.FIT_X_CENTER))
    }

    @Test
    fun compute_withFitYLeft_fitsHeightAndAlignsToLeft() {
        assertRect(0.0f, 0.0f, 400.0f, 100.0f, mapped(DisplayType.FIT_Y_LEFT))
    }

    @Test
    fun compute_withFitYRight_fitsHeightAndAlignsToRight() {
        assertRect(-200.0f, 0.0f, 200.0f, 100.0f, mapped(DisplayType.FIT_Y_RIGHT))
    }

    @Test
    fun compute_withFitYCenter_fitsHeightAndCentersHorizontally() {
        assertRect(-100.0f, 0.0f, 300.0f, 100.0f, mapped(DisplayType.FIT_Y_CENTER))
    }

    @Test
    fun compute_withCenterCrop_fillsViewAndCentersImage() {
        assertRect(-100.0f, 0.0f, 300.0f, 100.0f, mapped(DisplayType.CENTER_CROP))
    }

    private fun compute(
        displayType: DisplayType,
        imageWidth: Int = IMAGE_WIDTH,
        imageHeight: Int = IMAGE_HEIGHT
    ): Matrix {
        val result = Matrix()
        // start from a matrix that is not the identity, to check it is overwritten
        result.setScale(3.0f, 3.0f)
        BaseMatrixCalculator.compute(
            imageWidth, imageHeight, VIEW_WIDTH, VIEW_HEIGHT, displayType, result
        )
        return result
    }

    private fun mapped(displayType: DisplayType): RectF {
        val rect = RectF(0.0f, 0.0f, IMAGE_WIDTH.toFloat(), IMAGE_HEIGHT.toFloat())
        compute(displayType).mapRect(rect)
        return rect
    }

    private fun assertRect(left: Float, top: Float, right: Float, bottom: Float, actual: RectF) {
        assertEquals(left, actual.left, TOLERANCE)
        assertEquals(top, actual.top, TOLERANCE)
        assertEquals(right, actual.right, TOLERANCE)
        assertEquals(bottom, actual.bottom, TOLERANCE)
    }

    private companion object {
        const val IMAGE_WIDTH = 400
        const val IMAGE_HEIGHT = 100
        const val SMALL = 50
        const val VIEW_WIDTH = 200.0f
        const val VIEW_HEIGHT = 100.0f
        const val TOLERANCE = 1.0e-3f
    }
}
