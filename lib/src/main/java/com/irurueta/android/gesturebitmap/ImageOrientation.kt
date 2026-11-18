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

/**
 * Helper to handle EXIF image orientations.
 *
 * Orientations use the standard EXIF values (1 to 8). Any other value is treated as [NORMAL].
 */
internal object ImageOrientation {

    /**
     * EXIF value for an image that is stored upright.
     */
    const val NORMAL = 1

    /**
     * EXIF value for an image that is stored mirrored horizontally.
     */
    const val FLIP_HORIZONTAL = 2

    /**
     * EXIF value for an image that is stored rotated 180 degrees.
     */
    const val ROTATE_180 = 3

    /**
     * EXIF value for an image that is stored mirrored vertically.
     */
    const val FLIP_VERTICAL = 4

    /**
     * EXIF value for an image that is stored transposed (mirrored along the main diagonal).
     */
    const val TRANSPOSE = 5

    /**
     * EXIF value for an image that must be rotated 90 degrees clockwise to be upright.
     */
    const val ROTATE_90 = 6

    /**
     * EXIF value for an image that is stored transversed (mirrored along the secondary diagonal).
     */
    const val TRANSVERSE = 7

    /**
     * EXIF value for an image that must be rotated 270 degrees clockwise to be upright.
     */
    const val ROTATE_270 = 8

    /**
     * Gets the width of the upright image.
     *
     * @param orientation EXIF orientation value.
     * @param storedW width of the stored (non-rotated) image in pixels.
     * @param storedH height of the stored (non-rotated) image in pixels.
     * @return width of the upright image in pixels.
     */
    fun uprightWidth(orientation: Int, storedW: Int, storedH: Int): Int {
        return if (swapsAxes(orientation)) storedH else storedW
    }

    /**
     * Gets the height of the upright image.
     *
     * @param orientation EXIF orientation value.
     * @param storedW width of the stored (non-rotated) image in pixels.
     * @param storedH height of the stored (non-rotated) image in pixels.
     * @return height of the upright image in pixels.
     */
    fun uprightHeight(orientation: Int, storedW: Int, storedH: Int): Int {
        return if (swapsAxes(orientation)) storedW else storedH
    }

    /**
     * Sets [result] to the matrix that maps stored pixel coordinates to upright image pixel coordinates.
     *
     * @param orientation EXIF orientation value. Unknown values behave as [NORMAL].
     * @param storedW width of the stored (non-rotated) image in pixels.
     * @param storedH height of the stored (non-rotated) image in pixels.
     * @param result matrix where the transformation is stored.
     */
    fun toMatrix(orientation: Int, storedW: Int, storedH: Int, result: Matrix) {
        val w = storedW.toFloat()
        val h = storedH.toFloat()
        val values = when (orientation) {
            FLIP_HORIZONTAL -> floatArrayOf(-1.0f, 0.0f, w, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f)
            ROTATE_180 -> floatArrayOf(-1.0f, 0.0f, w, 0.0f, -1.0f, h, 0.0f, 0.0f, 1.0f)
            FLIP_VERTICAL -> floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, -1.0f, h, 0.0f, 0.0f, 1.0f)
            TRANSPOSE -> floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f)
            ROTATE_90 -> floatArrayOf(0.0f, -1.0f, h, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f)
            TRANSVERSE -> floatArrayOf(0.0f, -1.0f, h, -1.0f, 0.0f, w, 0.0f, 0.0f, 1.0f)
            ROTATE_270 -> floatArrayOf(0.0f, 1.0f, 0.0f, -1.0f, 0.0f, w, 0.0f, 0.0f, 1.0f)
            else -> floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f)
        }
        result.setValues(values)
    }

    private fun swapsAxes(orientation: Int): Boolean {
        return orientation == TRANSPOSE || orientation == ROTATE_90 ||
                orientation == TRANSVERSE || orientation == ROTATE_270
    }
}
