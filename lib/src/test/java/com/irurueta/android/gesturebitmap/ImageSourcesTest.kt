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

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapRegionDecoder
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
class ImageSourcesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun drawableId(name: String): Int =
        context.resources.getIdentifier(name, "drawable", context.packageName)

    @Test
    fun isRegionDecodable_withXmlDrawable_returnsFalse() {
        val id = drawableId("test_vector")
        assertNotEquals(0, id)

        assertFalse(ImageSources.isRegionDecodable(context.resources, id))
    }

    @Test
    fun isRegionDecodable_withRasterResource_returnsTrueWhenMimeTypeIsSupported() {
        val id = drawableId("test_raster")
        assertNotEquals(0, id)

        assertTrue(ImageSources.isRegionDecodable(context.resources, id))
    }

    @Test
    fun isRegionDecodable_withUnknownResource_returnsFalse() {
        assertFalse(ImageSources.isRegionDecodable(context.resources, 0x7f7f7f7f))
    }

    @Test
    fun fromFile_whenFileDoesNotExist_throwsIOException() {
        val file = File("does-not-exist-${System.nanoTime()}.jpg")

        assertThrows(IOException::class.java) { ImageSources.fromFile(file) }
    }

    @Test
    fun fromBytes_whenNotAnImage_throwsIOException() {
        assertThrows(IOException::class.java) { ImageSources.fromBytes(ByteArray(16)) }
    }

    @Test
    fun fromUri_whenResolverReturnsNull_throwsFileNotFoundException() {
        val resolver = mockk<ContentResolver>()
        val uri = Uri.parse("content://test/image")
        every { resolver.openInputStream(uri) } returns null

        assertThrows(FileNotFoundException::class.java) { ImageSources.fromUri(resolver, uri) }
    }

    @Test
    fun fromAsset_whenAssetDoesNotExist_throwsIOException() {
        assertThrows(IOException::class.java) {
            ImageSources.fromAsset(context.assets, "missing.jpg")
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(BitmapRegionDecoder::class)
    }

    private fun stubDecoder() {
        val decoder = mockk<BitmapRegionDecoder>(relaxed = true)
        every { decoder.width } returns 10
        every { decoder.height } returns 20
        mockkStatic(BitmapRegionDecoder::class)
        every { BitmapRegionDecoder.newInstance(any<InputStream>()) } returns decoder
        every { BitmapRegionDecoder.newInstance(any<InputStream>(), any()) } returns decoder
    }

    @Test
    fun fromBytes_withSupportedImage_createsSource() {
        stubDecoder()

        ImageSources.fromBytes(ByteArray(8)).use {
            assertEquals(10, it.storedWidth)
            assertEquals(20, it.storedHeight)
        }
    }

    @Test
    fun fromResource_withSupportedImage_createsSource() {
        stubDecoder()

        ImageSources.fromResource(context.resources, drawableId("test_raster")).use {
            assertEquals(10, it.storedWidth)
        }
    }

    @Test
    fun fromAsset_withSupportedImage_createsSource() {
        stubDecoder()
        val assets = mockk<android.content.res.AssetManager>()
        every { assets.open("image.jpg") } answers { ByteArrayInputStream(ByteArray(8)) }

        ImageSources.fromAsset(assets, "image.jpg").use {
            assertEquals(20, it.storedHeight)
        }
    }

    @Test
    fun fromFile_withSupportedImage_createsSource() {
        stubDecoder()
        val file = File.createTempFile("image", ".jpg")
        try {
            ImageSources.fromFile(file).use {
                assertEquals(10, it.storedWidth)
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun fromUri_withSupportedImage_createsSource() {
        stubDecoder()
        val resolver = mockk<ContentResolver>()
        val uri = Uri.parse("content://test/image")
        every { resolver.openInputStream(uri) } answers { ByteArrayInputStream(ByteArray(8)) }

        ImageSources.fromUri(resolver, uri).use {
            assertEquals(10, it.storedWidth)
        }
    }
}
