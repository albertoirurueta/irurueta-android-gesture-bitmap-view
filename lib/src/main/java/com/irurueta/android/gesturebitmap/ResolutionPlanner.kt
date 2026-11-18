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
import android.graphics.Rect
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Pure helper functions deciding which resolution levels and which detail patches are needed to
 * display a large image.
 *
 * Sample sizes are always powers of two, where a sample size of N means that each bitmap pixel
 * covers N x N pixels of the stored image. Functions in this object hold no state and make no use
 * of Android state other than the [Matrix] and [Rect] instances provided by the caller.
 */
internal object ResolutionPlanner {

    /**
     * Fraction of the visible region size added on each side of a patch so that small pans do not
     * require decoding a new patch.
     */
    const val OVERSCAN = 0.25f

    /**
     * Number of bytes used by each pixel of a bitmap (ARGB_8888).
     */
    const val BYTES_PER_PIXEL = 4

    /**
     * Number of values needed to store the four corners of the view.
     */
    const val CORNER_VALUES = 8

    /**
     * Result of choosing which cached level must be drawn.
     *
     * @property sample sample size of the chosen cached level.
     * @property needsPatch true when no cached level is fine enough for the required sample size,
     * so a detail patch is needed to display the image sharply.
     */
    data class LevelChoice(val sample: Int, val needsPatch: Boolean)

    /**
     * Computes the sample size of the preview bitmap.
     *
     * The preview keeps at least one bitmap pixel per view pixel when the image is displayed at
     * base scale (fitted inside the view).
     *
     * @param imageW width of the stored image in pixels. Must be positive.
     * @param imageH height of the stored image in pixels. Must be positive.
     * @param viewW width of the view in pixels. Must be positive.
     * @param viewH height of the view in pixels. Must be positive.
     * @return largest power of two sample size keeping at least one bitmap pixel per view pixel
     * (at least 1).
     * @throws IllegalArgumentException if any argument is not positive.
     */
    fun previewSampleSize(imageW: Int, imageH: Int, viewW: Int, viewH: Int): Int {
        require(imageW > 0 && imageH > 0) { "image size must be positive" }
        require(viewW > 0 && viewH > 0) { "view size must be positive" }
        val baseScale = min(viewW.toFloat() / imageW, viewH.toFloat() / imageH)
        return requiredSampleSize(baseScale)
    }

    /**
     * Computes the sample size needed to display an image sharply at a given display scale.
     *
     * @param displayScale number of view pixels per stored image pixel. Must be positive.
     * @return largest power of two less than or equal to `1 / displayScale` (at least 1).
     * @throws IllegalArgumentException if [displayScale] is not a positive finite number.
     */
    fun requiredSampleSize(displayScale: Float): Int {
        require(displayScale > 0.0f && displayScale.isFinite()) {
            "scale must be positive and finite"
        }
        val ratio = 1.0f / displayScale
        var sample = 1
        while (sample <= Int.MAX_VALUE / 4 && sample * 2 <= ratio) {
            sample *= 2
        }
        return sample
    }

    /**
     * Computes the sample sizes of the levels to keep cached, from coarsest to finest.
     *
     * The first element is the preview sample size, which is increased as needed so the preview
     * fits within the maximum bitmap size. Each following element is half of the previous one, and
     * is included only while the running total of bitmap bytes (preview included) stays within
     * the budget and the bitmap sides stay within the maximum bitmap size.
     *
     * @param imageW width of the stored image in pixels. Must be positive.
     * @param imageH height of the stored image in pixels. Must be positive.
     * @param previewSample requested sample size of the preview. Must be a positive power of two.
     * @param budgetBytes memory budget for all cached levels in bytes. 0 keeps the preview only.
     * @param maxBitmapW maximum width of a bitmap in pixels. Must be positive.
     * @param maxBitmapH maximum height of a bitmap in pixels. Must be positive.
     * @return list of sample sizes, never empty, ordered from coarsest to finest.
     * @throws IllegalArgumentException if any size is not positive, [previewSample] is not a
     * power of two or [budgetBytes] is negative.
     */
    fun cachedLevelSampleSizes(
        imageW: Int,
        imageH: Int,
        previewSample: Int,
        budgetBytes: Long,
        maxBitmapW: Int,
        maxBitmapH: Int
    ): List<Int> {
        require(imageW > 0 && imageH > 0) { "image size must be positive" }
        require(maxBitmapW > 0 && maxBitmapH > 0) { "maximum bitmap size must be positive" }
        require(previewSample > 0 && previewSample and (previewSample - 1) == 0) {
            "preview sample must be a power of two"
        }
        require(budgetBytes >= 0) { "budget must not be negative" }

        var sample = previewSample
        while (!fits(imageW, imageH, sample, maxBitmapW, maxBitmapH) && sample <= Int.MAX_VALUE / 2) {
            sample *= 2
        }
        val result = mutableListOf(sample)
        var total = bytes(imageW, imageH, sample)
        while (sample > 1) {
            sample /= 2
            if (!fits(imageW, imageH, sample, maxBitmapW, maxBitmapH)) {
                break
            }
            total += bytes(imageW, imageH, sample)
            if (total > budgetBytes) {
                break
            }
            result.add(sample)
        }
        return result
    }

    /**
     * Chooses which of the available cached levels must be drawn.
     *
     * @param availableSamples sample sizes of the available levels, in any order. Must not be
     * empty.
     * @param requiredSample sample size needed to display the image sharply.
     * @return the coarsest available sample less than or equal to [requiredSample], or the finest
     * available sample with `needsPatch = true` when none is fine enough.
     * @throws IllegalArgumentException if [availableSamples] is empty.
     */
    fun levelToDraw(availableSamples: List<Int>, requiredSample: Int): LevelChoice {
        val choice = levelSampleToDraw(availableSamples, requiredSample)
        return LevelChoice(abs(choice), choice < 0)
    }

    /**
     * Chooses which of the available cached levels must be drawn, without allocating.
     *
     * @param availableSamples sample sizes of the available levels, in any order. Must not be
     * empty.
     * @param requiredSample sample size needed to display the image sharply.
     * @return the coarsest available sample less than or equal to [requiredSample], or the finest
     * available sample negated when none is fine enough, meaning that a patch is needed.
     * @throws IllegalArgumentException if [availableSamples] is empty.
     */
    fun levelSampleToDraw(availableSamples: List<Int>, requiredSample: Int): Int {
        require(availableSamples.isNotEmpty()) { "at least one level is required" }
        var coarsestFineEnough = 0
        var finest = Int.MAX_VALUE
        for (i in availableSamples.indices) {
            val sample = availableSamples[i]
            if (sample <= requiredSample && sample > coarsestFineEnough) {
                coarsestFineEnough = sample
            }
            if (sample < finest) {
                finest = sample
            }
        }
        return if (coarsestFineEnough > 0) coarsestFineEnough else -finest
    }

    /**
     * Computes the region of the stored image a detail patch must decode.
     *
     * The view rectangle is mapped through [inverseMatrix] to stored image coordinates, its
     * bounding box is intersected with the image, expanded by [OVERSCAN] on each side, snapped
     * outwards to multiples of the sample size and clamped to the image. When the region exceeds
     * [maxPixels] the overscan is dropped first and then the sample size is doubled until it fits.
     *
     * @param inverseMatrix matrix mapping view coordinates to stored image coordinates (inverse of
     * display matrix times orientation matrix).
     * @param viewW width of the view in pixels.
     * @param viewH height of the view in pixels.
     * @param storedW width of the stored image in pixels.
     * @param storedH height of the stored image in pixels.
     * @param sample requested sample size (power of two).
     * @param maxPixels maximum number of bitmap pixels of the patch. Must be positive.
     * @param result rectangle where the region is stored in stored image coordinates. It is
     * emptied when the image is not visible.
     * @param points array of at least 8 values used as working storage, so that callers on a hot
     * path can avoid allocations.
     * @return sample size actually used, which is at least [sample].
     * @throws IllegalArgumentException if [sample] or [maxPixels] are not positive.
     */
    fun patchRegion(
        inverseMatrix: Matrix,
        viewW: Int,
        viewH: Int,
        storedW: Int,
        storedH: Int,
        sample: Int,
        maxPixels: Long,
        result: Rect,
        points: FloatArray = FloatArray(CORNER_VALUES)
    ): Int {
        require(sample > 0) { "sample must be positive" }
        require(maxPixels > 0) { "maximum pixels must be positive" }
        require(points.size >= CORNER_VALUES) { "points must hold four corners" }

        points[0] = 0.0f
        points[1] = 0.0f
        points[2] = viewW.toFloat()
        points[3] = 0.0f
        points[4] = viewW.toFloat()
        points[5] = viewH.toFloat()
        points[6] = 0.0f
        points[7] = viewH.toFloat()
        inverseMatrix.mapPoints(points, 0, points, 0, CORNER_VALUES / 2)
        var minX = points[0]
        var maxX = points[0]
        var minY = points[1]
        var maxY = points[1]
        for (i in 2 until CORNER_VALUES step 2) {
            minX = min(minX, points[i])
            maxX = max(maxX, points[i])
            minY = min(minY, points[i + 1])
            maxY = max(maxY, points[i + 1])
        }

        val left = max(minX, 0.0f)
        val top = max(minY, 0.0f)
        val right = min(maxX, storedW.toFloat())
        val bottom = min(maxY, storedH.toFloat())
        if (left >= right || top >= bottom) {
            result.setEmpty()
            return sample
        }

        var used = sample
        if (compute(left, top, right, bottom, OVERSCAN, storedW, storedH, used, maxPixels, result)) {
            return used
        }
        while (!compute(left, top, right, bottom, 0.0f, storedW, storedH, used, maxPixels, result)) {
            used *= 2
        }
        return used
    }

    /**
     * Checks whether a decoded patch can be used to draw the visible region.
     *
     * @param patchRegion region covered by the patch in stored image coordinates.
     * @param patchSample sample size of the patch.
     * @param visibleRegion currently visible region in stored image coordinates.
     * @param requiredSample sample size needed to display the image sharply.
     * @return true when the patch is fine enough and fully covers the visible region.
     */
    fun isPatchValid(
        patchRegion: Rect,
        patchSample: Int,
        visibleRegion: Rect,
        requiredSample: Int
    ): Boolean {
        return patchSample <= requiredSample && patchRegion.contains(visibleRegion)
    }

    /**
     * Computes the snapped region for a given overscan and sample, storing it in [result].
     *
     * @param left left edge of the visible region in stored image coordinates, clamped to the image.
     * @param top top edge of the visible region in stored image coordinates, clamped to the image.
     * @param right right edge of the visible region in stored image coordinates, clamped to the image.
     * @param bottom bottom edge of the visible region in stored image coordinates, clamped to the image.
     * @param overscan fraction of the visible region size added on each side.
     * @param storedW width of the stored image in pixels.
     * @param storedH height of the stored image in pixels.
     * @param sample sample size used to snap the region outwards (power of two).
     * @param maxPixels maximum number of bitmap pixels of the patch.
     * @param result rectangle where the snapped region is stored in stored image coordinates.
     * @return true when the region fits within [maxPixels].
     */
    private fun compute(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        overscan: Float,
        storedW: Int,
        storedH: Int,
        sample: Int,
        maxPixels: Long,
        result: Rect
    ): Boolean {
        val dx = (right - left) * overscan
        val dy = (bottom - top) * overscan
        val l = snapDown(max(left - dx, 0.0f), sample)
        val t = snapDown(max(top - dy, 0.0f), sample)
        val r = min(snapUp(right + dx, sample).toLong(), storedW.toLong()).toInt()
        val b = min(snapUp(bottom + dy, sample).toLong(), storedH.toLong()).toInt()
        result.set(l, t, r, b)
        val pixels = ceilDiv(r - l, sample) * ceilDiv(b - t, sample)
        return pixels <= maxPixels
    }

    private fun snapDown(value: Float, sample: Int): Int =
        (floor(value / sample).toLong() * sample).toInt()

    private fun snapUp(value: Float, sample: Int): Int =
        min(ceil(value / sample).toLong() * sample, Int.MAX_VALUE.toLong()).toInt()

    private fun ceilDiv(value: Int, sample: Int): Long = (value.toLong() + sample - 1) / sample

    private fun fits(imageW: Int, imageH: Int, sample: Int, maxW: Int, maxH: Int): Boolean =
        ceilDiv(imageW, sample) <= maxW && ceilDiv(imageH, sample) <= maxH

    private fun bytes(imageW: Int, imageH: Int, sample: Int): Long =
        ceilDiv(imageW, sample) * ceilDiv(imageH, sample) * BYTES_PER_PIXEL
}
