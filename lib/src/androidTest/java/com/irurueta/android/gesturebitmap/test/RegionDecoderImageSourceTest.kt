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

package com.irurueta.android.gesturebitmap.test

import org.junit.Assert.assertNotSame
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.irurueta.android.gesturebitmap.GestureBitmapView
import com.irurueta.android.gesturebitmap.ImageOrientation
import com.irurueta.android.gesturebitmap.ImageSource
import com.irurueta.android.gesturebitmap.ImageSources
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.math.abs
import kotlin.math.min

/**
 * Tests [com.irurueta.android.gesturebitmap.RegionDecoderImageSource] with the real
 * [android.graphics.BitmapRegionDecoder], and the way [GestureBitmapView] displays EXIF oriented
 * images.
 *
 * Assets contain a 2000x1500 image whose quadrants are red (top-left), green (top-right), blue
 * (bottom-left) and yellow (bottom-right), once stored.
 */
@RunWith(AndroidJUnit4::class)
class RegionDecoderImageSourceTest {

    @get:Rule
    val activityScenarioRule = activityScenarioRule<GestureBitmapViewActivity>()

    private val sources = mutableListOf<ImageSource>()

    @After
    fun tearDown() {
        sources.forEach { it.close() }
        sources.clear()
    }

    @Test
    fun fromAsset_readsStoredSizeAndOrientation() {
        val normal = open(NORMAL_ASSET)
        assertEquals(WIDTH, normal.storedWidth)
        assertEquals(HEIGHT, normal.storedHeight)
        assertEquals(ImageOrientation.NORMAL, normal.orientation)

        val rotate180 = open(ROTATE_180_ASSET)
        assertEquals(WIDTH, rotate180.storedWidth)
        assertEquals(HEIGHT, rotate180.storedHeight)
        assertEquals(ImageOrientation.ROTATE_180, rotate180.orientation)

        assertEquals(ImageOrientation.ROTATE_90, open(ROTATE_90_ASSET).orientation)
        assertEquals(ImageOrientation.ROTATE_270, open(ROTATE_270_ASSET).orientation)
    }

    @Test
    fun decodeRegion_withPngWithAlpha_keepsTransparency() {
        val source = open(ALPHA_ASSET)
        assertEquals(200, source.storedWidth)
        assertEquals(100, source.storedHeight)

        val bitmap = source.decodeRegion(Rect(0, 0, 200, 100), 1, Bitmap.Config.ARGB_8888, null)

        assertNotNull(bitmap)
        bitmap!!
        assertTrue(bitmap.hasAlpha())
        assertColor("opaque", Color.RED, bitmap.getPixel(50, 50))
        assertEquals(0, Color.alpha(bitmap.getPixel(150, 50)))
    }

    @Test
    fun decodeRegion_withSampleSizes_returnsExpectedSizeAndColors() {
        val source = open(NORMAL_ASSET)
        // region that contains a part of each quadrant
        val region = Rect(WIDTH / 2 - 400, HEIGHT / 2 - 300, WIDTH / 2 + 400, HEIGHT / 2 + 300)

        for (sampleSize in intArrayOf(1, 2, 4)) {
            val bitmap = source.decodeRegion(region, sampleSize, Bitmap.Config.ARGB_8888, null)

            assertNotNull(bitmap)
            bitmap!!
            assertEquals(region.width() / sampleSize, bitmap.width)
            assertEquals(region.height() / sampleSize, bitmap.height)

            val quarterWidth = bitmap.width / 4
            val quarterHeight = bitmap.height / 4
            assertColor("red", Color.RED, bitmap.getPixel(quarterWidth, quarterHeight))
            assertColor("green", Color.GREEN, bitmap.getPixel(3 * quarterWidth, quarterHeight))
            assertColor("blue", Color.BLUE, bitmap.getPixel(quarterWidth, 3 * quarterHeight))
            assertColor(
                "yellow", Color.YELLOW, bitmap.getPixel(3 * quarterWidth, 3 * quarterHeight)
            )
        }
    }

    @Test
    fun decodeRegion_withReusableBitmap_returnsSameInstance() {
        val source = open(NORMAL_ASSET)
        val reuse = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)

        val result = source.decodeRegion(Rect(0, 0, 800, 600), 2, Bitmap.Config.ARGB_8888, reuse)

        assertSame(reuse, result)
        assertColor("red", Color.RED, reuse.getPixel(200, 150))
    }

    @Test
    fun decodeRegion_withReuseOfDifferentSize_returnsBitmapOfDecodedSize() {
        val source = open(NORMAL_ASSET)
        val reuse = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        val result = source.decodeRegion(Rect(0, 0, 800, 600), 2, Bitmap.Config.ARGB_8888, reuse)

        // a region decoder keeps the size of a reused bitmap, so one of another size is not reused
        assertNotNull(result)
        assertNotSame(reuse, result)
        assertEquals(400, result!!.width)
        assertEquals(300, result.height)
    }

    @Test
    fun decodeRegion_withHardwareConfig_returnsHardwareBitmapWhereSupported() {
        val source = open(NORMAL_ASSET)

        val bitmap = source.decodeRegion(Rect(0, 0, 800, 600), 2, Bitmap.Config.HARDWARE, null)

        assertNotNull(bitmap)
        bitmap!!
        assertEquals(400, bitmap.width)
        assertEquals(300, bitmap.height)
        if (Build.VERSION.SDK_INT >= VERIFIED_HARDWARE_API) {
            // region decoding into hardware bitmaps has been verified from this API level on
            assertEquals(Bitmap.Config.HARDWARE, bitmap.config)
        } else {
            // BitmapRegionDecoder may ignore the HARDWARE config on older API levels, in which
            // case a software bitmap that can still be drawn must be returned
            assertTrue(
                bitmap.config == Bitmap.Config.HARDWARE || bitmap.config == Bitmap.Config.ARGB_8888
            )
        }
    }

    @Test
    fun fromAsset_withUnsupportedFormat_throwsIOException() {
        try {
            open(TEXT_ASSET)
            fail("IOException expected")
        } catch (_: IOException) {
            // expected
        }
    }

    @Test
    fun setImageSource_forEachExifOrientation_displaysImageUpright() {
        for ((asset, expected) in UPRIGHT_QUADRANTS) {
            val recorder = ImageLoadRecorder()
            var view: GestureBitmapView? = null
            activityScenarioRule.scenario.onActivity { activity ->
                val v = activity.findViewById<GestureBitmapView>(R.id.gesture_bitmap_view_test)
                GestureBitmapViewTestHelper.resize(v)
                view = v
            }
            val v = view ?: return fail()
            assertTrue(GestureBitmapViewTestHelper.awaitSize(v, GestureBitmapViewTestHelper.SMALL_VIEW_SIZE))
            activityScenarioRule.scenario.onActivity {
                // keep the preview only, so that zooming in requires a detail patch
                v.cachedLevelsMemoryBudget = 0L
                v.imageLoadListener = recorder
                v.setImageSource(open(asset))
            }
            assertTrue(asset, recorder.awaitPreviews(1))

            // preview, with the whole image displayed
            val drawn = draw(v)
            val rect = v.displayRect ?: return fail()
            val dx = rect.width() / 4.0f
            val dy = rect.height() / 4.0f
            assertQuadrants(asset, drawn, rect.centerX(), rect.centerY(), dx, dy, expected)

            // detail, zooming in on the center of the image, where all quadrants meet
            val detailsBefore = recorder.detailCount
            activityScenarioRule.scenario.onActivity {
                GestureBitmapViewTestHelper.zoomAtCenter(v, ZOOM_SCALE)
            }
            recorder.awaitDetails(detailsBefore + 1)
            val zoomed = draw(v)
            val offset = min(v.width, v.height) * ZOOMED_OFFSET
            assertQuadrants(
                asset, zoomed, v.width / 2.0f, v.height / 2.0f, offset, offset, expected
            )
        }
    }

    private fun open(asset: String): ImageSource {
        val source = ImageSources.fromAsset(
            InstrumentationRegistry.getInstrumentation().context.assets, asset
        )
        sources.add(source)
        return source
    }

    private fun draw(view: GestureBitmapView): Bitmap {
        var bitmap: Bitmap? = null
        activityScenarioRule.scenario.onActivity {
            bitmap = GestureBitmapViewTestHelper.draw(view)
        }
        return bitmap ?: throw IllegalStateException("view could not be drawn")
    }

    private fun assertQuadrants(
        asset: String,
        bitmap: Bitmap,
        centerX: Float,
        centerY: Float,
        dx: Float,
        dy: Float,
        expected: IntArray
    ) {
        val left = (centerX - dx).toInt()
        val right = (centerX + dx).toInt()
        val top = (centerY - dy).toInt()
        val bottom = (centerY + dy).toInt()
        assertColor("$asset top-left", expected[0], bitmap.getPixel(left, top))
        assertColor("$asset top-right", expected[1], bitmap.getPixel(right, top))
        assertColor("$asset bottom-left", expected[2], bitmap.getPixel(left, bottom))
        assertColor("$asset bottom-right", expected[3], bitmap.getPixel(right, bottom))
    }

    private fun assertColor(message: String, expected: Int, actual: Int) {
        val ok = abs(Color.red(expected) - Color.red(actual)) <= TOLERANCE &&
                abs(Color.green(expected) - Color.green(actual)) <= TOLERANCE &&
                abs(Color.blue(expected) - Color.blue(actual)) <= TOLERANCE
        assertTrue(
            "$message expected #${Integer.toHexString(expected)} " +
                    "but was #${Integer.toHexString(actual)}",
            ok
        )
    }

    private companion object {
        /**
         * First API level where region decoding into hardware bitmaps has been verified.
         */
        const val VERIFIED_HARDWARE_API = 36
        private const val WIDTH = 2000
        private const val HEIGHT = 1500
        private const val TOLERANCE = 40
        private const val ZOOMED_OFFSET = 0.05f
        private const val ZOOM_SCALE = 10.0

        private const val NORMAL_ASSET = "quadrants.jpg"
        private const val ROTATE_180_ASSET = "quadrants_orientation_3.jpg"
        private const val ROTATE_90_ASSET = "quadrants_orientation_6.jpg"
        private const val ROTATE_270_ASSET = "quadrants_orientation_8.jpg"
        private const val ALPHA_ASSET = "alpha.png"
        private const val TEXT_ASSET = "not_an_image.jpg"

        /**
         * Expected colors of the top-left, top-right, bottom-left and bottom-right quadrants of the
         * upright image for each asset.
         */
        private val UPRIGHT_QUADRANTS = mapOf(
            NORMAL_ASSET to intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW),
            ROTATE_180_ASSET to intArrayOf(Color.YELLOW, Color.BLUE, Color.GREEN, Color.RED),
            ROTATE_90_ASSET to intArrayOf(Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN),
            ROTATE_270_ASSET to intArrayOf(Color.GREEN, Color.YELLOW, Color.RED, Color.BLUE)
        )
    }
}
