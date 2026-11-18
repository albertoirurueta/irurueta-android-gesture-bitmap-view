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

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Computations limiting how far a displayed image can be scrolled on one axis, so that it remains
 * visible, including the tension (resistance) applied when overscrolling.
 */
internal object ScrollLimits {

    /**
     * Computes minimum allowed start (left or top) coordinate of displayed bitmap on one axis.
     *
     * @param rectSize size of displayed bitmap on the axis.
     * @param viewSize size of the view on the axis.
     * @param endMargin scroll margin at the end (right or bottom) of the axis.
     * @return minimum allowed start coordinate.
     */
    fun minAllowedStart(rectSize: Float, viewSize: Float, endMargin: Float): Float {
        return if (rectSize <= viewSize) {
            // bitmap fits within the view, it must remain centered (up to margin)
            (viewSize - rectSize) / 2.0f - endMargin
        } else {
            // bitmap end can not leave an empty space larger than margin
            viewSize - endMargin - rectSize
        }
    }

    /**
     * Computes maximum allowed start (left or top) coordinate of displayed bitmap on one axis.
     *
     * @param rectSize size of displayed bitmap on the axis.
     * @param viewSize size of the view on the axis.
     * @param startMargin scroll margin at the start (left or top) of the axis.
     * @return maximum allowed start coordinate.
     */
    fun maxAllowedStart(rectSize: Float, viewSize: Float, startMargin: Float): Float {
        return if (rectSize <= viewSize) {
            // bitmap fits within the view, it must remain centered (up to margin)
            (viewSize - rectSize) / 2.0f + startMargin
        } else {
            // bitmap start can not leave an empty space larger than margin
            startMargin
        }
    }

    /**
     * Limits requested variation of a coordinate so that it remains within provided range.
     *
     * @param current current value of coordinate.
     * @param delta requested variation.
     * @param minValue minimum allowed value.
     * @param maxValue maximum allowed value.
     * @param snap true to always end within the allowed range. If false and current value is
     * already out of range (e.g. because scale or rotation have changed), only variations
     * approaching the allowed range are accepted, and never beyond requested amount.
     * @param softMinValue minimum value allowed without scroll margin (tension starts below it).
     * @param softMaxValue maximum value allowed without scroll margin (tension starts above it).
     * @param tensionEnabled true to apply tension when overscrolling, false otherwise.
     * @param tension amount of tension applied when overscrolling.
     * @return allowed variation.
     */
    fun limitDelta(
        current: Float,
        delta: Float,
        minValue: Float,
        maxValue: Float,
        snap: Boolean,
        softMinValue: Float,
        softMaxValue: Float,
        tensionEnabled: Boolean,
        tension: Float
    ): Float {
        val target = current + delta
        return when {
            snap -> max(minValue, min(target, maxValue)) - current

            current in minValue..maxValue -> {
                val allowed = tensionDelta(
                    current, delta, softMinValue, softMaxValue, minValue, maxValue, tensionEnabled, tension
                )
                max(minValue, min(current + allowed, maxValue)) - current
            }

            current > maxValue -> if (delta < 0.0f) max(target, minValue) - current else 0.0f
            else -> if (delta > 0.0f) min(target, maxValue) - current else 0.0f
        }
    }

    /**
     * Applies tension (resistance) to a requested variation when it moves the coordinate further
     * beyond the un-margined bounds, so that the resistance grows with the overscroll (as
     * configured by [tension]) until it reaches the margin.
     *
     * @param current current value of coordinate, within the margin-expanded range.
     * @param delta requested variation.
     * @param softMinValue minimum value without margin.
     * @param softMaxValue maximum value without margin.
     * @param minValue minimum value including margin.
     * @param maxValue maximum value including margin.
     * @param tensionEnabled true to apply tension, false to return the requested variation.
     * @param tension amount of tension: 1 is linear, larger values are stiffer and 0 disables it.
     * @return variation after applying tension, or the requested one if tension does not apply.
     */
    fun tensionDelta(
        current: Float,
        delta: Float,
        softMinValue: Float,
        softMaxValue: Float,
        minValue: Float,
        maxValue: Float,
        tensionEnabled: Boolean,
        tension: Float
    ): Float {
        if (!tensionEnabled) {
            return delta
        }

        val overscroll: Float
        val margin: Float
        if (delta > 0.0f) {
            overscroll = max(0.0f, current - softMaxValue)
            margin = maxValue - softMaxValue
        } else {
            overscroll = max(0.0f, softMinValue - current)
            margin = softMinValue - minValue
        }
        if (overscroll <= 0.0f || margin <= 0.0f) {
            return delta
        }

        val resistance = (1.0f - overscroll / margin).coerceIn(0.0f, 1.0f).pow(tension)
        return delta * resistance
    }
}
