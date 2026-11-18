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
import android.graphics.Rect
import java.io.Closeable

/**
 * Provider of the pixels of a large image, which can be decoded one region at a time.
 *
 * An implementation exposes the size and EXIF orientation of the image as it is stored, and decodes
 * arbitrary rectangular regions of it at a reduced resolution. All methods other than the properties
 * are called on a worker thread, and never concurrently: implementations do not need to be thread safe
 * for decoding, but must not depend on being called from the main thread.
 *
 * @property storedWidth width of the image in stored pixels, before applying [orientation].
 * @property storedHeight height of the image in stored pixels, before applying [orientation].
 * @property orientation one of the 8 EXIF orientation values (`ExifInterface.ORIENTATION_*`), or
 * `ExifInterface.ORIENTATION_NORMAL` (1) when it is unknown.
 */
interface ImageSource : Closeable {

    /**
     * Width of the image in stored pixels, before applying [orientation].
     */
    val storedWidth: Int

    /**
     * Height of the image in stored pixels, before applying [orientation].
     */
    val storedHeight: Int

    /**
     * EXIF orientation of the image: one of the 8 `ExifInterface.ORIENTATION_*` values, or
     * `ExifInterface.ORIENTATION_NORMAL` when it is unknown.
     */
    val orientation: Int

    /**
     * Decodes a region of the image.
     *
     * This method is called on a worker thread and is never called concurrently with itself or with
     * [contentChanged].
     *
     * @param region region to decode, expressed in stored pixels.
     * @param sampleSize subsampling factor, a power of two. A value of N yields a bitmap N times smaller
     * on each side.
     * @param config bitmap configuration requested for the result.
     * @param reuse bitmap that may be reused to hold the result, or null. Implementations are free to
     * ignore it.
     * @return decoded bitmap, which is owned by the caller from then on, or null if decoding failed.
     */
    fun decodeRegion(region: Rect, sampleSize: Int, config: Bitmap.Config, reuse: Bitmap?): Bitmap?

    /**
     * Notifies that the contents of the image changed, keeping its size.
     *
     * Called on a worker thread before decoding again after a change. The default implementation does
     * nothing.
     */
    fun contentChanged() {}

    /**
     * Releases the resources held by this source.
     *
     * A view owning this source may call this method on a worker thread, after any decode in
     * progress, so that the UI thread never waits for a decode to finish.
     */
    override fun close()
}
