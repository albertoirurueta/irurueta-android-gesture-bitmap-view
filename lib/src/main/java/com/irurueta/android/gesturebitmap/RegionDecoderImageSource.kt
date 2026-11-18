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
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import java.io.InputStream

/**
 * [ImageSource] that decodes regions of an encoded image (JPEG, PNG or WebP) using a
 * [BitmapRegionDecoder], so the whole image never needs to be held in memory.
 *
 * The encoded image is read through [opener], which is invoked each time the source needs to read the
 * image: once to read the EXIF orientation and once to open the region decoder, both on creation and on
 * every [contentChanged] call. Streams returned by [opener] are always closed by this class.
 *
 * @property opener provides a new stream positioned at the start of the encoded image on every call.
 * @throws IOException if the image cannot be read or its format is not supported by
 * [BitmapRegionDecoder].
 */
class RegionDecoderImageSource(private val opener: () -> InputStream) : ImageSource {

    private var decoder: BitmapRegionDecoder?

    /**
     * Width of the image in stored pixels, before applying [orientation].
     */
    override val storedWidth: Int

    /**
     * Height of the image in stored pixels, before applying [orientation].
     */
    override val storedHeight: Int

    /**
     * EXIF orientation of the image, or `ExifInterface.ORIENTATION_NORMAL` when it is unknown.
     */
    override var orientation: Int = ExifInterface.ORIENTATION_NORMAL
        private set

    init {
        orientation = readOrientation()
        val newDecoder = openDecoder()
        decoder = newDecoder
        storedWidth = newDecoder.width
        storedHeight = newDecoder.height
    }

    /**
     * Decodes a region of the image.
     *
     * `inBitmap` is only set when [reuse] is mutable and [config] is not `Bitmap.Config.HARDWARE`.
     *
     * @param region region to decode, expressed in stored pixels.
     * @param sampleSize subsampling factor, a power of two.
     * @param config bitmap configuration requested for the result.
     * @param reuse bitmap that may be reused to hold the result, or null.
     * @return decoded bitmap, or null if decoding failed, the region is invalid or the source was closed.
     */
    override fun decodeRegion(
        region: Rect,
        sampleSize: Int,
        config: Bitmap.Config,
        reuse: Bitmap?
    ): Bitmap? {
        val currentDecoder = decoder ?: return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = config
            if (reuse != null && reuse.isMutable && config != Bitmap.Config.HARDWARE) {
                inBitmap = reuse
            }
        }
        return try {
            currentDecoder.decodeRegion(region, options)
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: IOException) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    /**
     * Re-reads the image through the opener, closing the previous region decoder.
     *
     * @throws IllegalStateException if the size of the new image differs from the original one. The
     * source cannot decode anymore after this happens.
     * @throws IOException if the image cannot be read or its format is not supported.
     */
    override fun contentChanged() {
        decoder?.recycle()
        decoder = null
        orientation = readOrientation()
        val newDecoder = openDecoder()
        if (newDecoder.width != storedWidth || newDecoder.height != storedHeight) {
            val width = newDecoder.width
            val height = newDecoder.height
            newDecoder.recycle()
            throw IllegalStateException(
                "Image size changed from ${storedWidth}x$storedHeight to ${width}x$height"
            )
        }
        decoder = newDecoder
    }

    /**
     * Releases the region decoder. Further calls to [decodeRegion] return null.
     */
    override fun close() {
        decoder?.recycle()
        decoder = null
    }

    private fun readOrientation(): Int {
        return try {
            val value = opener().use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
            if (value in ExifInterface.ORIENTATION_NORMAL..ExifInterface.ORIENTATION_ROTATE_270) {
                value
            } else {
                ExifInterface.ORIENTATION_NORMAL
            }
        } catch (_: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    @Suppress("DEPRECATION")
    private fun openDecoder(): BitmapRegionDecoder {
        return opener().use { stream ->
            val result = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    BitmapRegionDecoder.newInstance(stream)
                } else {
                    BitmapRegionDecoder.newInstance(stream, false)
                }
            } catch (e: IOException) {
                throw IOException("Image format is not supported for region decoding", e)
            } catch (e: IllegalArgumentException) {
                throw IOException("Image format is not supported for region decoding", e)
            }
            result ?: throw IOException("Image format is not supported for region decoding")
        }
    }
}
