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
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrollLimitsTest {

    @Test
    fun minAllowedStart_whenImageFitsInView_returnsCenteredStartMinusMargin() {
        assertEquals(20.0f, ScrollLimits.minAllowedStart(50.0f, 100.0f, 5.0f), 0.0f)
    }

    @Test
    fun minAllowedStart_whenImageIsLargerThanView_returnsStartLeavingOnlyMargin() {
        assertEquals(-105.0f, ScrollLimits.minAllowedStart(200.0f, 100.0f, 5.0f), 0.0f)
    }

    @Test
    fun maxAllowedStart_whenImageFitsInView_returnsCenteredStartPlusMargin() {
        assertEquals(30.0f, ScrollLimits.maxAllowedStart(50.0f, 100.0f, 5.0f), 0.0f)
    }

    @Test
    fun maxAllowedStart_whenImageIsLargerThanView_returnsMargin() {
        assertEquals(5.0f, ScrollLimits.maxAllowedStart(200.0f, 100.0f, 5.0f), 0.0f)
    }

    @Test
    fun limitDelta_whenSnapping_endsWithinRange() {
        assertEquals(-40.0f, limit(current = 50.0f, delta = 100.0f, snap = true), 0.0f)
        assertEquals(-160.0f, limit(current = 50.0f, delta = -500.0f, snap = true), 0.0f)
        assertEquals(3.0f, limit(current = 0.0f, delta = 3.0f, snap = true), 0.0f)
    }

    @Test
    fun limitDelta_whenWithinRangeWithoutTension_clampsToRange() {
        assertEquals(10.0f, limit(current = 0.0f, delta = 30.0f), 0.0f)
        assertEquals(-5.0f, limit(current = 0.0f, delta = -5.0f), 0.0f)
    }

    @Test
    fun limitDelta_whenAboveRange_onlyAcceptsVariationsApproachingRange() {
        assertEquals(0.0f, limit(current = 50.0f, delta = 5.0f), 0.0f)
        assertEquals(-5.0f, limit(current = 50.0f, delta = -5.0f), 0.0f)
        assertEquals(-160.0f, limit(current = 50.0f, delta = -500.0f), 0.0f)
    }

    @Test
    fun limitDelta_whenBelowRange_onlyAcceptsVariationsApproachingRange() {
        assertEquals(0.0f, limit(current = -200.0f, delta = -5.0f), 0.0f)
        assertEquals(5.0f, limit(current = -200.0f, delta = 5.0f), 0.0f)
        assertEquals(210.0f, limit(current = -200.0f, delta = 500.0f), 0.0f)
    }

    @Test
    fun limitDelta_withoutMargins_acceptsOnlyVariationsWithinOrApproachingRange() {
        fun limit(current: Float, delta: Float, snap: Boolean): Float =
            ScrollLimits.limitDelta(current, delta, 0.0f, 10.0f, snap, 0.0f, 10.0f, true, 1.0f)

        // in range: variation is limited to allowed range
        assertEquals(2.0f, limit(5.0f, 2.0f, false), 0.0f)
        assertEquals(5.0f, limit(5.0f, 20.0f, false), 0.0f)
        assertEquals(-5.0f, limit(5.0f, -20.0f, false), 0.0f)

        // above maximum
        assertEquals(0.0f, limit(15.0f, 3.0f, false), 0.0f)
        assertEquals(-3.0f, limit(15.0f, -3.0f, false), 0.0f)
        assertEquals(-15.0f, limit(15.0f, -30.0f, false), 0.0f)

        // below minimum
        assertEquals(0.0f, limit(-5.0f, -3.0f, false), 0.0f)
        assertEquals(3.0f, limit(-5.0f, 3.0f, false), 0.0f)
        assertEquals(15.0f, limit(-5.0f, 30.0f, false), 0.0f)

        // snapping always ends within range
        assertEquals(-5.0f, limit(15.0f, 3.0f, true), 0.0f)
        assertEquals(5.0f, limit(-5.0f, -3.0f, true), 0.0f)
    }

    @Test
    fun limitDelta_whenOverscrollingWithTension_reducesVariation() {
        // 5 pixels beyond the soft maximum, with a margin of 10: half of the variation is kept
        val allowed = limit(current = 5.0f, delta = 2.0f, tensionEnabled = true)

        assertEquals(1.0f, allowed, TOLERANCE)
    }

    @Test
    fun tensionDelta_whenDisabledOrWithinSoftRange_returnsRequestedVariation() {
        assertEquals(2.0f, tension(current = 5.0f, delta = 2.0f, enabled = false), 0.0f)
        assertEquals(2.0f, tension(current = -10.0f, delta = 2.0f), 0.0f)
        assertEquals(-2.0f, tension(current = -10.0f, delta = -2.0f), 0.0f)
    }

    @Test
    fun tensionDelta_whenThereIsNoMargin_returnsRequestedVariation() {
        val allowed = ScrollLimits.tensionDelta(
            5.0f, 2.0f, SOFT_MIN, SOFT_MAX, SOFT_MIN, SOFT_MAX, true, 1.0f
        )

        assertEquals(2.0f, allowed, 0.0f)
    }

    @Test
    fun tensionDelta_whenOverscrollingBelowSoftMinimum_reducesVariation() {
        // 5 pixels below the soft minimum, with a margin of 10
        assertEquals(-1.0f, tension(current = SOFT_MIN - 5.0f, delta = -2.0f), TOLERANCE)
    }

    @Test
    fun tensionDelta_withLargerTension_isStiffer() {
        val linear = tension(current = 5.0f, delta = 2.0f, amount = 1.0f)
        val stiff = tension(current = 5.0f, delta = 2.0f, amount = 2.0f)
        val none = tension(current = 5.0f, delta = 2.0f, amount = 0.0f)

        assertTrue(stiff < linear)
        assertEquals(2.0f, none, 0.0f)
    }

    private fun limit(
        current: Float,
        delta: Float,
        snap: Boolean = false,
        tensionEnabled: Boolean = false
    ) = ScrollLimits.limitDelta(
        current, delta, MIN, MAX, snap, SOFT_MIN, SOFT_MAX, tensionEnabled, 1.0f
    )

    private fun tension(
        current: Float,
        delta: Float,
        enabled: Boolean = true,
        amount: Float = 1.0f
    ) = ScrollLimits.tensionDelta(current, delta, SOFT_MIN, SOFT_MAX, MIN, MAX, enabled, amount)

    private companion object {
        const val SOFT_MIN = -100.0f
        const val SOFT_MAX = 0.0f
        const val MIN = -110.0f
        const val MAX = 10.0f
        const val TOLERANCE = 1.0e-5f
    }
}
