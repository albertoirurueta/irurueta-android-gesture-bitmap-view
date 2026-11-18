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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageOrientationTest {

    private fun map(orientation: Int, x: Float, y: Float): FloatArray {
        val matrix = Matrix()
        ImageOrientation.toMatrix(orientation, W, H, matrix)
        val pts = floatArrayOf(x, y)
        matrix.mapPoints(pts)
        return pts
    }

    @Test
    fun uprightSize_swapsAxesOnlyForTransposingOrientations() {
        for (o in listOf(0, 1, 2, 3, 4, 9, -1)) {
            assertEquals(W, ImageOrientation.uprightWidth(o, W, H))
            assertEquals(H, ImageOrientation.uprightHeight(o, W, H))
        }
        for (o in 5..8) {
            assertEquals(H, ImageOrientation.uprightWidth(o, W, H))
            assertEquals(W, ImageOrientation.uprightHeight(o, W, H))
        }
    }

    @Test
    fun toMatrix_cornersMapInsideUprightRectangle() {
        val corners = listOf(0.0f to 0.0f, W.toFloat() to 0.0f, 0.0f to H.toFloat(), W.toFloat() to H.toFloat())
        for (o in 0..9) {
            val uw = ImageOrientation.uprightWidth(o, W, H).toFloat()
            val uh = ImageOrientation.uprightHeight(o, W, H).toFloat()
            for ((x, y) in corners) {
                val p = map(o, x, y)
                assertTrue("orientation $o", p[0] in 0.0f..uw && p[1] in 0.0f..uh)
            }
        }
    }

    @Test
    fun toMatrix_mapsOffCentrePointToExpectedPosition() {
        // stored point (X, Y) = (2, 1) in a 10 x 6 stored image
        val expected = mapOf(
            0 to (2.0f to 1.0f),
            1 to (2.0f to 1.0f),
            2 to (8.0f to 1.0f),
            3 to (8.0f to 5.0f),
            4 to (2.0f to 5.0f),
            5 to (1.0f to 2.0f),
            6 to (5.0f to 2.0f),
            7 to (5.0f to 8.0f),
            8 to (1.0f to 8.0f),
            9 to (2.0f to 1.0f)
        )
        for ((o, e) in expected) {
            val p = map(o, 2.0f, 1.0f)
            assertEquals("orientation $o x", e.first, p[0], 1e-6f)
            assertEquals("orientation $o y", e.second, p[1], 1e-6f)
        }
    }

    private companion object {
        const val W = 10
        const val H = 6
    }
}
