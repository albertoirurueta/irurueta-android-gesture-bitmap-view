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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.graphics.createBitmap

/**
 * [ImageSource] backed by a bitmap that is held whole in memory.
 *
 * The bitmap is owned by the caller: this source never recycles it, not even on [close]. The caller can
 * keep drawing on the bitmap when it is mutable, and the edits are visible in the next decoded region.
 * Reads of the wrapped bitmap are synchronised on it, so callers must synchronise on the same bitmap
 * while drawing on it.
 *
 * @property bitmap caller-owned bitmap to wrap.
 * @constructor Creates a source wrapping [bitmap].
 * @param bitmap caller-owned bitmap to wrap.
 */
class BitmapImageSource(private val bitmap: Bitmap) : ImageSource {

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    /**
     * Width of the wrapped bitmap, in pixels.
     */
    override val storedWidth: Int = bitmap.width

    /**
     * Height of the wrapped bitmap, in pixels.
     */
    override val storedHeight: Int = bitmap.height

    /**
     * EXIF orientation of the wrapped bitmap, which is always [ImageOrientation.NORMAL].
     */
    override val orientation: Int = ImageOrientation.NORMAL

    /**
     * Draws a region of the wrapped bitmap scaled by `1 / sampleSize`.
     *
     * @param region region to draw, expressed in pixels of the wrapped bitmap.
     * @param sampleSize subsampling factor. Values lower than 1 are treated as 1.
     * @param config bitmap configuration requested for the result. When it is
     * [Bitmap.Config.HARDWARE] a hardware copy of the drawn bitmap is returned.
     * @param reuse bitmap to draw into when it is mutable, not recycled, not hardware and has exactly
     * the size of the result. Otherwise a new [Bitmap.Config.ARGB_8888] bitmap is used.
     * @return decoded bitmap, or null if the region is empty or does not intersect the bitmap, or if the
     * wrapped bitmap has been recycled.
     */
    override fun decodeRegion(
        region: Rect,
        sampleSize: Int,
        config: Bitmap.Config,
        reuse: Bitmap?
    ): Bitmap? {
        val sample = maxOf(sampleSize, 1)
        val source = Rect(region)
        if (!source.intersect(0, 0, storedWidth, storedHeight)) {
            return null
        }
        val width = maxOf((source.width() + sample - 1) / sample, 1)
        val height = maxOf((source.height() + sample - 1) / sample, 1)

        val usableReuse = reuse?.takeIf {
            it.isMutable && !it.isRecycled && it.config != Bitmap.Config.HARDWARE &&
                it.width == width && it.height == height
        }
        val target = usableReuse ?: createBitmap(width, height)
        target.eraseColor(Color.TRANSPARENT)

        val drawn = synchronized(bitmap) {
            if (bitmap.isRecycled) {
                false
            } else {
                Canvas(target).drawBitmap(bitmap, source, Rect(0, 0, width, height), paint)
                true
            }
        }
        if (!drawn) {
            if (usableReuse == null) {
                target.recycle()
            }
            return null
        }

        if (config != Bitmap.Config.HARDWARE) {
            return target
        }
        val hardware = target.copy(Bitmap.Config.HARDWARE, false)
        if (usableReuse == null) {
            target.recycle()
        }
        return hardware
    }

    /**
     * Releases nothing: the wrapped bitmap belongs to the caller and is not recycled.
     */
    override fun close() {
        // the wrapped bitmap is caller-owned
    }
}
