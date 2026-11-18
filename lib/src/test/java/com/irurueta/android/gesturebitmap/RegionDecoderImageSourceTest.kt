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
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
class RegionDecoderImageSourceTest {

    private val decoders = mutableListOf<BitmapRegionDecoder>()
    private var openCount = 0

    @Before
    fun setUp() {
        mockkStatic(BitmapRegionDecoder::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(BitmapRegionDecoder::class)
    }

    @Test
    fun constructor_readsSize() {
        stubFactory(mockDecoder(100, 200))

        val source = RegionDecoderImageSource { opener() }

        assertEquals(100, source.storedWidth)
        assertEquals(200, source.storedHeight)
        assertEquals(1, source.orientation)
        // one stream for EXIF and one for the decoder
        assertEquals(2, openCount)
    }

    @Test
    fun constructor_whenFormatNotSupported_throwsIOException() {
        every { BitmapRegionDecoder.newInstance(any<InputStream>()) } throws IOException("bad")
        every { BitmapRegionDecoder.newInstance(any<InputStream>(), any()) } throws IOException("bad")

        val e = assertThrows(IOException::class.java) { RegionDecoderImageSource { opener() } }

        assertTrue(e.message?.contains("not supported") == true)
    }

    @Test
    fun constructor_whenDecoderIsNull_throwsIOException() {
        stubFactory(null)

        assertThrows(IOException::class.java) { RegionDecoderImageSource { opener() } }
    }

    @Test
    fun contentChanged_opensStreamsAgainAndClosesOldDecoder() {
        val first = mockDecoder(100, 200)
        val second = mockDecoder(100, 200)
        stubFactory(first, second)
        val source = RegionDecoderImageSource { opener() }

        source.contentChanged()

        assertEquals(4, openCount)
        verify(exactly = 1) { first.recycle() }
        val bitmap = mockk<Bitmap>()
        every { second.decodeRegion(any(), any()) } returns bitmap
        assertSame(bitmap, source.decodeRegion(Rect(0, 0, 10, 10), 1, Bitmap.Config.ARGB_8888, null))
    }

    @Test
    fun contentChanged_whenSizeDiffers_throwsIllegalStateException() {
        val first = mockDecoder(100, 200)
        val second = mockDecoder(101, 200)
        stubFactory(first, second)
        val source = RegionDecoderImageSource { opener() }

        assertThrows(IllegalStateException::class.java) { source.contentChanged() }

        // the previous decoder is kept, so the source is still usable
        verify(exactly = 0) { first.recycle() }
        verify { second.recycle() }
        val result = mockk<Bitmap>()
        every { first.decodeRegion(any(), any()) } returns result
        assertSame(result, source.decodeRegion(Rect(0, 0, 10, 10), 1, Bitmap.Config.ARGB_8888, null))
    }

    @Test
    fun contentChanged_whenImageCannotBeOpened_keepsPreviousDecoder() {
        val first = mockDecoder(100, 200)
        stubFactory(first, null)
        val source = RegionDecoderImageSource { opener() }

        assertThrows(IOException::class.java) { source.contentChanged() }

        verify(exactly = 0) { first.recycle() }
        val result = mockk<Bitmap>()
        every { first.decodeRegion(any(), any()) } returns result
        assertSame(result, source.decodeRegion(Rect(0, 0, 10, 10), 1, Bitmap.Config.ARGB_8888, null))
    }

    @Test
    fun contentChanged_whenClosed_doesNothing() {
        val decoder = mockDecoder(100, 200)
        stubFactory(decoder)
        val source = RegionDecoderImageSource { opener() }
        source.close()
        val opened = openCount

        source.contentChanged()

        assertEquals(opened, openCount)
    }

    @Test
    fun decodeRegion_withReuseOfDifferentSize_doesNotSetInBitmap() {
        val decoder = mockDecoder(100, 200)
        stubFactory(decoder)
        val source = RegionDecoderImageSource { opener() }
        val options = mutableListOf<BitmapFactory.Options>()
        every { decoder.decodeRegion(any(), capture(options)) } returns null
        val reuse = mockk<Bitmap>()
        every { reuse.isMutable } returns true
        every { reuse.isRecycled } returns false
        every { reuse.width } returns 10
        every { reuse.height } returns 12

        // decoded size is 12x12, which is not the size of the bitmap to reuse
        source.decodeRegion(Rect(0, 0, 48, 48), 4, Bitmap.Config.ARGB_8888, reuse)
        // decoded size cannot be predicted when the region is not a multiple of the sample size
        source.decodeRegion(Rect(0, 0, 41, 48), 4, Bitmap.Config.ARGB_8888, reuse)

        assertNull(options[0].inBitmap)
        assertNull(options[1].inBitmap)
    }

    @Test
    fun close_recyclesDecoderAndDecodeReturnsNull() {
        val decoder = mockDecoder(100, 200)
        stubFactory(decoder)
        val source = RegionDecoderImageSource { opener() }

        source.close()
        source.close()

        verify(exactly = 1) { decoder.recycle() }
        assertNull(source.decodeRegion(Rect(0, 0, 10, 10), 1, Bitmap.Config.ARGB_8888, null))
    }

    @Test
    fun decodeRegion_setsOptionsAndReusesMutableBitmap() {
        val decoder = mockDecoder(100, 200)
        stubFactory(decoder)
        val source = RegionDecoderImageSource { opener() }
        val options = slot<BitmapFactory.Options>()
        val result = mockk<Bitmap>()
        every { decoder.decodeRegion(any(), capture(options)) } returns result
        val reuse = mockk<Bitmap>()
        every { reuse.isMutable } returns true
        every { reuse.isRecycled } returns false
        every { reuse.width } returns 12
        every { reuse.height } returns 12
        val region = Rect(0, 0, 48, 48)

        assertSame(result, source.decodeRegion(region, 4, Bitmap.Config.RGB_565, reuse))

        assertEquals(4, options.captured.inSampleSize)
        assertEquals(Bitmap.Config.RGB_565, options.captured.inPreferredConfig)
        assertSame(reuse, options.captured.inBitmap)
    }

    @Test
    fun decodeRegion_withImmutableReuseOrHardware_doesNotSetInBitmap() {
        val decoder = mockDecoder(100, 200)
        stubFactory(decoder)
        val source = RegionDecoderImageSource { opener() }
        val options = slot<BitmapFactory.Options>()
        every { decoder.decodeRegion(any(), capture(options)) } returns null
        val immutable = mockk<Bitmap>()
        every { immutable.isMutable } returns false
        val mutable = mockk<Bitmap>()
        every { mutable.isMutable } returns true
        val region = Rect(0, 0, 50, 50)

        source.decodeRegion(region, 1, Bitmap.Config.ARGB_8888, immutable)
        assertNull(options.captured.inBitmap)

        source.decodeRegion(region, 1, Bitmap.Config.HARDWARE, mutable)
        assertNull(options.captured.inBitmap)
    }

    @Test
    fun decodeRegion_whenDecoderFails_returnsNull() {
        val decoder = mockDecoder(100, 200)
        stubFactory(decoder)
        val source = RegionDecoderImageSource { opener() }
        val region = Rect(0, 0, 50, 50)

        every { decoder.decodeRegion(any(), any()) } throws IllegalArgumentException()
        assertNull(source.decodeRegion(region, 1, Bitmap.Config.ARGB_8888, null))

        every { decoder.decodeRegion(any(), any()) } throws IOException()
        assertNull(source.decodeRegion(region, 1, Bitmap.Config.ARGB_8888, null))

        every { decoder.decodeRegion(any(), any()) } throws OutOfMemoryError()
        assertNull(source.decodeRegion(region, 1, Bitmap.Config.ARGB_8888, null))
    }

    private fun mockDecoder(width: Int, height: Int): BitmapRegionDecoder {
        val decoder = mockk<BitmapRegionDecoder>(relaxed = true)
        every { decoder.width } returns width
        every { decoder.height } returns height
        decoders.add(decoder)
        return decoder
    }

    private fun stubFactory(vararg results: BitmapRegionDecoder?) {
        var index = 0
        every { BitmapRegionDecoder.newInstance(any<InputStream>()) } answers {
            results[minOf(index++, results.size - 1)]
        }
        every { BitmapRegionDecoder.newInstance(any<InputStream>(), any()) } answers {
            results[minOf(index++, results.size - 1)]
        }
    }

    private fun opener(): InputStream {
        openCount++
        return ByteArrayInputStream(ByteArray(8))
    }
}
