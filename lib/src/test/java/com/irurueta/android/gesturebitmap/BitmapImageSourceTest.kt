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

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BitmapImageSourceTest {

    private lateinit var bitmap: Bitmap
    private lateinit var source: BitmapImageSource

    @Before
    fun setUp() {
        bitmap = Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        source = BitmapImageSource(bitmap)
    }

    @After
    fun tearDown() {
        source.close()
    }

    @Test
    fun properties_returnBitmapSizeAndNormalOrientation() {
        assertEquals(40, source.storedWidth)
        assertEquals(20, source.storedHeight)
        assertEquals(ImageOrientation.NORMAL, source.orientation)
    }

    @Test
    fun decodeRegion_fullRegionSampleOne_returnsSameSizeBitmap() {
        val result = source.decodeRegion(Rect(0, 0, 40, 20), 1, Bitmap.Config.ARGB_8888, null)

        assertNotNull(result)
        assertEquals(40, result?.width)
        assertEquals(20, result?.height)
        assertEquals(Color.RED, result?.getPixel(5, 5))
    }

    @Test
    fun decodeRegion_regionAndSample_returnsScaledRegion() {
        val result = source.decodeRegion(Rect(10, 4, 30, 16), 4, Bitmap.Config.ARGB_8888, null)

        assertEquals(5, result?.width)
        assertEquals(3, result?.height)
    }

    @Test
    fun decodeRegion_regionOutsideBitmap_returnsNull() {
        assertNull(source.decodeRegion(Rect(100, 100, 120, 120), 1, Bitmap.Config.ARGB_8888, null))
    }

    @Test
    fun decodeRegion_regionPartiallyOutside_isClipped() {
        val result = source.decodeRegion(Rect(30, 10, 60, 40), 1, Bitmap.Config.ARGB_8888, null)

        assertEquals(10, result?.width)
        assertEquals(10, result?.height)
    }

    @Test
    fun decodeRegion_usableReuse_returnsReusedBitmap() {
        val reuse = Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888)

        val result = source.decodeRegion(Rect(0, 0, 40, 20), 2, Bitmap.Config.ARGB_8888, reuse)

        assertSame(reuse, result)
        assertEquals(Color.RED, reuse.getPixel(3, 3))
    }

    @Test
    fun decodeRegion_reuseOfDifferentSize_returnsNewBitmap() {
        val reuse = Bitmap.createBitmap(7, 7, Bitmap.Config.ARGB_8888)

        val result = source.decodeRegion(Rect(0, 0, 40, 20), 2, Bitmap.Config.ARGB_8888, reuse)

        assertNotSame(reuse, result)
        assertEquals(20, result?.width)
    }

    @Test
    fun decodeRegion_recycledReuse_returnsNewBitmap() {
        val reuse = Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888)
        reuse.recycle()

        val result = source.decodeRegion(Rect(0, 0, 40, 20), 2, Bitmap.Config.ARGB_8888, reuse)

        assertNotSame(reuse, result)
        assertNotNull(result)
    }

    @Test
    fun decodeRegion_afterEditingWrappedBitmap_showsEdit() {
        val first = source.decodeRegion(Rect(0, 0, 40, 20), 1, Bitmap.Config.ARGB_8888, null)
        assertEquals(Color.RED, first?.getPixel(1, 1))

        synchronized(bitmap) {
            bitmap.eraseColor(Color.BLUE)
        }
        source.contentChanged()
        val second = source.decodeRegion(Rect(0, 0, 40, 20), 1, Bitmap.Config.ARGB_8888, null)

        assertEquals(Color.BLUE, second?.getPixel(1, 1))
    }

    @Test
    fun decodeRegion_recycledWrappedBitmap_returnsNull() {
        bitmap.recycle()

        assertNull(source.decodeRegion(Rect(0, 0, 40, 20), 1, Bitmap.Config.ARGB_8888, null))
    }

    @Test
    fun close_doesNotRecycleWrappedBitmap() {
        source.close()

        assertFalse(bitmap.isRecycled)
    }
}
