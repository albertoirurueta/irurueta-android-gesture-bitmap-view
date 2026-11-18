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

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class MetricTransformationUpdaterTest {

    @Test
    fun scaleAt_setsScaleAndKeepsRotation() {
        val parameters = MetricTransformationParameters(2.0, ROTATION, 10.0, 20.0)

        MetricTransformationUpdater.scaleAt(parameters, 4.0, PIVOT_X, PIVOT_Y)

        assertEquals(4.0, parameters.scale, TOLERANCE)
        assertEquals(ROTATION, parameters.rotationAngle, TOLERANCE)
    }

    @Test
    fun scaleAt_keepsPivotPointFixed() {
        val parameters = MetricTransformationParameters(2.0, ROTATION, 10.0, 20.0)
        // point of the image displayed at the pivot before scaling
        val source = unmap(parameters, PIVOT_X.toDouble(), PIVOT_Y.toDouble())

        MetricTransformationUpdater.scaleAt(parameters, 5.0, PIVOT_X, PIVOT_Y)

        val mapped = map(parameters, source.first, source.second)
        assertEquals(PIVOT_X.toDouble(), mapped.first, TOLERANCE)
        assertEquals(PIVOT_Y.toDouble(), mapped.second, TOLERANCE)
    }

    @Test
    fun scaleAt_withSameScale_keepsTranslation() {
        val parameters = MetricTransformationParameters(2.0, ROTATION, 10.0, 20.0)

        MetricTransformationUpdater.scaleAt(parameters, 2.0, PIVOT_X, PIVOT_Y)

        assertEquals(10.0, parameters.horizontalTranslation, TOLERANCE)
        assertEquals(20.0, parameters.verticalTranslation, TOLERANCE)
    }

    @Test
    fun rotateAt_setsRotationAndKeepsScale() {
        val parameters = MetricTransformationParameters(2.0, ROTATION, 10.0, 20.0)

        MetricTransformationUpdater.rotateAt(parameters, NEW_ROTATION, PIVOT_X, PIVOT_Y)

        assertEquals(NEW_ROTATION.toDouble(), parameters.rotationAngle, TOLERANCE)
        assertEquals(2.0, parameters.scale, TOLERANCE)
    }

    @Test
    fun rotateAt_keepsPivotPointFixed() {
        val parameters = MetricTransformationParameters(2.0, ROTATION, 10.0, 20.0)
        val source = unmap(parameters, PIVOT_X.toDouble(), PIVOT_Y.toDouble())

        MetricTransformationUpdater.rotateAt(parameters, NEW_ROTATION, PIVOT_X, PIVOT_Y)

        val mapped = map(parameters, source.first, source.second)
        assertEquals(PIVOT_X.toDouble(), mapped.first, TOLERANCE)
        assertEquals(PIVOT_Y.toDouble(), mapped.second, TOLERANCE)
    }

    @Test
    fun rotateAt_withSameRotation_keepsTranslation() {
        val parameters = MetricTransformationParameters(2.0, ROTATION, 10.0, 20.0)

        MetricTransformationUpdater.rotateAt(parameters, ROTATION.toFloat(), PIVOT_X, PIVOT_Y)

        assertEquals(10.0, parameters.horizontalTranslation, TOLERANCE)
        assertEquals(20.0, parameters.verticalTranslation, TOLERANCE)
    }

    // applies [s*R t] to a point
    private fun map(p: MetricTransformationParameters, x: Double, y: Double): Pair<Double, Double> {
        val c = cos(p.rotationAngle)
        val s = sin(p.rotationAngle)
        return Pair(
            p.scale * (c * x - s * y) + p.horizontalTranslation,
            p.scale * (s * x + c * y) + p.verticalTranslation
        )
    }

    // applies the inverse of [s*R t] to a point
    private fun unmap(p: MetricTransformationParameters, x: Double, y: Double): Pair<Double, Double> {
        val c = cos(p.rotationAngle)
        val s = sin(p.rotationAngle)
        val dx = (x - p.horizontalTranslation) / p.scale
        val dy = (y - p.verticalTranslation) / p.scale
        return Pair(c * dx + s * dy, -s * dx + c * dy)
    }

    private companion object {
        const val ROTATION = 0.3
        const val NEW_ROTATION = 1.1f
        const val PIVOT_X = 150.0f
        const val PIVOT_Y = 80.0f
        const val TOLERANCE = 1.0e-4
    }
}
