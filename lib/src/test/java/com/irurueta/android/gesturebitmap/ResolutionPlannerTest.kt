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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ResolutionPlannerTest {

    @Test
    fun requiredSampleSize_atBoundaries_returnsPowersOfTwo() {
        assertEquals(1, ResolutionPlanner.requiredSampleSize(1.0f))
        assertEquals(1, ResolutionPlanner.requiredSampleSize(0.6f))
        assertEquals(2, ResolutionPlanner.requiredSampleSize(0.5f))
        assertEquals(2, ResolutionPlanner.requiredSampleSize(0.26f))
        assertEquals(4, ResolutionPlanner.requiredSampleSize(0.25f))
        assertEquals(8, ResolutionPlanner.requiredSampleSize(0.125f))
    }

    @Test
    fun requiredSampleSize_whenScaleAboveOne_returnsOne() {
        assertEquals(1, ResolutionPlanner.requiredSampleSize(1.5f))
        assertEquals(1, ResolutionPlanner.requiredSampleSize(100.0f))
    }

    @Test
    fun requiredSampleSize_whenTinyScale_doesNotOverflow() {
        assertTrue(ResolutionPlanner.requiredSampleSize(Float.MIN_VALUE) > 0)
    }

    @Test
    fun requiredSampleSize_whenInvalidScale_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            ResolutionPlanner.requiredSampleSize(0.0f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ResolutionPlanner.requiredSampleSize(-1.0f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ResolutionPlanner.requiredSampleSize(Float.POSITIVE_INFINITY)
        }
    }

    @Test
    fun previewSampleSize_usesFitScale() {
        // fit scale = min(1000/4000, 500/1000) = 0.25
        assertEquals(4, ResolutionPlanner.previewSampleSize(4000, 1000, 1000, 500))
        // fit scale = 0.5 -> 2
        assertEquals(2, ResolutionPlanner.previewSampleSize(2000, 1000, 1000, 1000))
        // image smaller than the view
        assertEquals(1, ResolutionPlanner.previewSampleSize(100, 100, 1000, 1000))
        // just below a boundary (scale slightly above 0.25 -> 3.9 -> 2)
        assertEquals(2, ResolutionPlanner.previewSampleSize(3900, 3900, 1000, 1000))
    }

    @Test
    fun previewSampleSize_whenInvalidSizes_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            ResolutionPlanner.previewSampleSize(0, 10, 10, 10)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ResolutionPlanner.previewSampleSize(10, 10, 10, 0)
        }
    }

    @Test
    fun cachedLevelSampleSizes_whenBudgetZero_returnsPreviewOnly() {
        val result = ResolutionPlanner.cachedLevelSampleSizes(4000, 4000, 8, 0L, 4096, 4096)

        assertEquals(listOf(8), result)
    }

    @Test
    fun cachedLevelSampleSizes_whenDefaultBudget_returnsLevelsWithinBudget() {
        // 4000x4000: sample 8 -> 500x500 = 1MB, 4 -> 1000x1000 = 4MB, 2 -> 2000x2000 = 16MB
        val budget = 6L * 1024 * 1024
        val result = ResolutionPlanner.cachedLevelSampleSizes(4000, 4000, 8, budget, 8192, 8192)

        assertEquals(listOf(8, 4), result)
    }

    @Test
    fun cachedLevelSampleSizes_whenLargeBudget_returnsAllLevelsDownToOne() {
        val result =
            ResolutionPlanner.cachedLevelSampleSizes(1000, 1000, 4, Long.MAX_VALUE, 8192, 8192)

        assertEquals(listOf(4, 2, 1), result)
    }

    @Test
    fun cachedLevelSampleSizes_whenPreviewIsOne_returnsOnlyIt() {
        val result =
            ResolutionPlanner.cachedLevelSampleSizes(100, 100, 1, Long.MAX_VALUE, 8192, 8192)

        assertEquals(listOf(1), result)
    }

    @Test
    fun cachedLevelSampleSizes_whenMaxBitmapSizeExceeded_increasesPreviewAndStopsLevels() {
        // sample 2 -> 5000 > 4096 so preview becomes 4 (2500)
        val result =
            ResolutionPlanner.cachedLevelSampleSizes(10000, 10000, 2, Long.MAX_VALUE, 4096, 4096)

        assertEquals(listOf(4), result.take(1))
        assertEquals(listOf(4), result)
    }

    @Test
    fun cachedLevelSampleSizes_whenMaxBitmapSizeAllowsFinerLevels_stopsAtFirstExceeding() {
        // sample 1 -> 3000 > 2500, sample 2 -> 1500 fits
        val result =
            ResolutionPlanner.cachedLevelSampleSizes(3000, 3000, 4, Long.MAX_VALUE, 2500, 2500)

        assertEquals(listOf(4, 2), result)
    }

    @Test
    fun cachedLevelSampleSizes_whenInvalidArguments_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            ResolutionPlanner.cachedLevelSampleSizes(0, 10, 1, 0L, 10, 10)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ResolutionPlanner.cachedLevelSampleSizes(10, 10, 1, 0L, 0, 10)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ResolutionPlanner.cachedLevelSampleSizes(10, 10, 3, 0L, 10, 10)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ResolutionPlanner.cachedLevelSampleSizes(10, 10, 1, -1L, 10, 10)
        }
    }

    @Test
    fun levelToDraw_whenExactMatch_returnsItWithoutPatch() {
        val choice = ResolutionPlanner.levelToDraw(listOf(8, 4, 2), 4)

        assertEquals(ResolutionPlanner.LevelChoice(4, false), choice)
    }

    @Test
    fun levelToDraw_whenNoExactMatch_returnsCoarsestFineEnough() {
        val choice = ResolutionPlanner.levelToDraw(listOf(8, 2), 4)

        assertEquals(2, choice.sample)
        assertFalse(choice.needsPatch)
    }

    @Test
    fun levelToDraw_whenAvailableUnordered_returnsCoarsestFineEnough() {
        val choice = ResolutionPlanner.levelToDraw(listOf(1, 8, 2, 4), 4)

        assertEquals(4, choice.sample)
    }

    @Test
    fun levelToDraw_whenNoneFineEnough_returnsFinestWithPatch() {
        val choice = ResolutionPlanner.levelToDraw(listOf(8, 4), 1)

        assertEquals(ResolutionPlanner.LevelChoice(4, true), choice)
    }

    @Test
    fun levelToDraw_whenEmpty_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            ResolutionPlanner.levelToDraw(emptyList(), 1)
        }
    }

    @Test
    fun patchRegion_whenIdentity_returnsViewSnappedWithOverscan() {
        val result = Rect()
        val sample = ResolutionPlanner.patchRegion(
            Matrix(), 100, 100, 1000, 1000, 8, 1_000_000L, result
        )

        assertEquals(8, sample)
        // 0..100 expanded by 25 -> -25..125, clamped to 0 and snapped up to 128
        assertEquals(Rect(0, 0, 128, 128), result)
    }

    @Test
    fun patchRegion_whenViewInsideImage_expandsOverscanAndSnapsOutwards() {
        val inverse = Matrix().apply { setTranslate(500.0f, 500.0f) }
        val result = Rect()

        val sample = ResolutionPlanner.patchRegion(
            inverse, 100, 100, 2000, 2000, 16, 1_000_000L, result
        )

        assertEquals(16, sample)
        // 500..600 +- 25 -> 475..625 -> snapped 464..640
        assertEquals(Rect(464, 464, 640, 640), result)
        assertEquals(0, result.left % 16)
        assertEquals(0, result.right % 16)
    }

    @Test
    fun patchRegion_whenExpandedBeyondImage_clampsToImage() {
        val inverse = Matrix().apply { setTranslate(950.0f, 950.0f) }
        val result = Rect()

        ResolutionPlanner.patchRegion(inverse, 100, 100, 1000, 1000, 64, 1_000_000L, result)

        assertEquals(1000, result.right)
        assertEquals(1000, result.bottom)
    }

    @Test
    fun patchRegion_whenImageOffScreen_returnsEmpty() {
        val inverse = Matrix().apply { setTranslate(5000.0f, 0.0f) }
        val result = Rect(1, 2, 3, 4)

        val sample = ResolutionPlanner.patchRegion(
            inverse, 100, 100, 1000, 1000, 2, 1_000_000L, result
        )

        assertEquals(2, sample)
        assertTrue(result.isEmpty)
    }

    @Test
    fun patchRegion_whenOverBudgetWithOverscan_dropsOverscanFirstKeepingSample() {
        val inverse = Matrix().apply { setTranslate(500.0f, 500.0f) }
        val result = Rect()

        // with overscan: 175 px -> 175x175 = 30625 (> 15000); without: 100x100 = 10000
        val sample = ResolutionPlanner.patchRegion(
            inverse, 100, 100, 2000, 2000, 1, 15_000L, result
        )

        assertEquals(1, sample)
        assertEquals(Rect(500, 500, 600, 600), result)
    }

    @Test
    fun patchRegion_whenOverBudgetWithoutOverscan_doublesSample() {
        val inverse = Matrix().apply { setTranslate(500.0f, 500.0f) }
        val result = Rect()

        // 100x100 = 10000 > 3000 at sample 1; sample 2 -> 2500 fits
        val sample = ResolutionPlanner.patchRegion(
            inverse, 100, 100, 2000, 2000, 1, 3_000L, result
        )

        assertEquals(2, sample)
        assertEquals(Rect(500, 500, 600, 600), result)
    }

    @Test
    fun patchRegion_whenViewRotated90_returnsRotatedBounds() {
        val display = Matrix().apply {
            setRotate(90.0f, 500.0f, 500.0f)
            postTranslate(-450.0f, -450.0f)
        }
        val inverse = Matrix()
        assertTrue(display.invert(inverse))
        val result = Rect()

        ResolutionPlanner.patchRegion(inverse, 200, 100, 1000, 1000, 1, 10_000_000L, result)

        assertFalse(result.isEmpty)
        // a 200x100 view rotated by 90 degrees covers 100x200 stored pixels (plus overscan)
        assertTrue(result.height() > result.width())
        assertEquals(300, result.height())
        assertEquals(150, result.width())
    }

    @Test
    fun patchRegion_whenViewRotated45_returnsBoundingBox() {
        val display = Matrix().apply {
            setRotate(45.0f, 500.0f, 500.0f)
            postTranslate(-450.0f, -450.0f)
        }
        val inverse = Matrix()
        assertTrue(display.invert(inverse))
        val result = Rect()

        ResolutionPlanner.patchRegion(inverse, 100, 100, 1000, 1000, 1, 10_000_000L, result)

        assertFalse(result.isEmpty)
        // bounding box of the rotated square is wider than the view (~141 px) without overscan
        assertTrue(result.width() > 141)
        assertTrue(result.height() > 141)
    }

    @Test
    fun patchRegion_forEachExifOrientation_coversWholeImageWhenUprightFitsView() {
        val w = 400
        val h = 200
        for (orientation in 1..8) {
            val oriented = orientationMatrix(orientation, w, h)
            val swapped = orientation >= 5
            val uprightW = if (swapped) h else w
            val uprightH = if (swapped) w else h
            val inverse = Matrix()
            assertTrue(oriented.invert(inverse))
            val result = Rect()

            val sample = ResolutionPlanner.patchRegion(
                inverse, uprightW, uprightH, w, h, 1, 10_000_000L, result
            )

            assertEquals("orientation $orientation", 1, sample)
            assertEquals("orientation $orientation", Rect(0, 0, w, h), result)
        }
    }

    @Test
    fun patchRegion_whenInvalidArguments_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            ResolutionPlanner.patchRegion(Matrix(), 10, 10, 10, 10, 0, 100L, Rect())
        }
        assertThrows(IllegalArgumentException::class.java) {
            ResolutionPlanner.patchRegion(Matrix(), 10, 10, 10, 10, 1, 0L, Rect())
        }
    }

    @Test
    fun isPatchValid_whenCoveredAndFineEnough_returnsTrue() {
        assertTrue(
            ResolutionPlanner.isPatchValid(
                Rect(0, 0, 100, 100), 2, Rect(10, 10, 90, 90), 2
            )
        )
        assertTrue(
            ResolutionPlanner.isPatchValid(
                Rect(0, 0, 100, 100), 1, Rect(0, 0, 100, 100), 4
            )
        )
    }

    @Test
    fun isPatchValid_whenTooCoarse_returnsFalse() {
        assertFalse(
            ResolutionPlanner.isPatchValid(
                Rect(0, 0, 100, 100), 4, Rect(10, 10, 90, 90), 2
            )
        )
    }

    @Test
    fun isPatchValid_whenNotCovering_returnsFalse() {
        assertFalse(
            ResolutionPlanner.isPatchValid(
                Rect(0, 0, 100, 100), 1, Rect(50, 50, 150, 90), 2
            )
        )
        assertFalse(
            ResolutionPlanner.isPatchValid(Rect(), 1, Rect(0, 0, 10, 10), 2)
        )
    }

    /** Builds a matrix mapping stored pixels to upright pixels for an EXIF orientation. */
    private fun orientationMatrix(orientation: Int, w: Int, h: Int): Matrix {
        val wf = w.toFloat()
        val hf = h.toFloat()
        val values = when (orientation) {
            1 -> floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
            2 -> floatArrayOf(-1f, 0f, wf, 0f, 1f, 0f, 0f, 0f, 1f)
            3 -> floatArrayOf(-1f, 0f, wf, 0f, -1f, hf, 0f, 0f, 1f)
            4 -> floatArrayOf(1f, 0f, 0f, 0f, -1f, hf, 0f, 0f, 1f)
            5 -> floatArrayOf(0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
            6 -> floatArrayOf(0f, -1f, hf, 1f, 0f, 0f, 0f, 0f, 1f)
            7 -> floatArrayOf(0f, -1f, hf, -1f, 0f, wf, 0f, 0f, 1f)
            else -> floatArrayOf(0f, 1f, 0f, -1f, 0f, wf, 0f, 0f, 1f)
        }
        return Matrix().apply { setValues(values) }
    }
}
