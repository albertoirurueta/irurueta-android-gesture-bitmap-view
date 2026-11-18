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
import android.graphics.RectF
import com.irurueta.android.gesturebitmap.GestureBitmapView.DisplayType
import kotlin.math.max
import kotlin.math.min

/**
 * Computes the base matrix of an image, which is the transformation initially scaling and
 * aligning it within a view according to a display type.
 */
internal object BaseMatrixCalculator {

    /**
     * Computes base matrix for provided image size, view size and display type.
     *
     * @param imageWidth upright image width expressed in image pixels.
     * @param imageHeight upright image height expressed in image pixels.
     * @param viewWidth width of the view expressed in pixels.
     * @param viewHeight height of the view expressed in pixels.
     * @param displayType display type determining how the image is initially scaled and aligned.
     * @param result instance where computed base matrix will be stored.
     */
    fun compute(
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Float,
        viewHeight: Float,
        displayType: DisplayType,
        result: Matrix
    ) {
        val bitmapWidth = imageWidth.toFloat()
        val bitmapHeight = imageHeight.toFloat()

        val bitmapRect = RectF(0.0f, 0.0f, bitmapWidth, bitmapHeight)

        when (displayType) {
            DisplayType.NONE -> {
                // returns identity matrix
                result.reset()
            }

            DisplayType.FIT_IF_BIGGER -> {
                if (bitmapWidth > viewWidth || bitmapHeight > viewHeight) {
                    val scaleWidth = viewWidth / bitmapWidth
                    val scaleHeight = viewHeight / bitmapHeight

                    val scale = min(scaleWidth, scaleHeight)
                    val newWidth = scale * bitmapWidth
                    val newHeight = scale * bitmapHeight

                    val halfWidthDiff = (viewWidth - newWidth) / 2.0f
                    val halfHeightDiff = (viewHeight - newHeight) / 2.0f

                    val dstRect = RectF(
                        halfWidthDiff, halfHeightDiff,
                        newWidth + halfWidthDiff, newHeight + halfHeightDiff
                    )

                    result.setRectToRect(bitmapRect, dstRect, Matrix.ScaleToFit.CENTER)
                } else {
                    // image fits within the view
                    result.reset()
                }
            }

            DisplayType.FIT_X_TOP -> {
                val scale = viewWidth / bitmapWidth
                val newHeight = scale * bitmapHeight
                val dstRect = RectF(0.0f, 0.0f, viewWidth, newHeight)

                result.setRectToRect(bitmapRect, dstRect, Matrix.ScaleToFit.CENTER)
            }

            DisplayType.FIT_X_BOTTOM -> {
                val scale = viewWidth / bitmapWidth
                val newHeight = scale * bitmapHeight
                val heightDiff = viewHeight - newHeight
                val dstRect = RectF(0.0f, heightDiff, viewWidth, newHeight + heightDiff)

                result.setRectToRect(bitmapRect, dstRect, Matrix.ScaleToFit.CENTER)
            }

            DisplayType.FIT_X_CENTER -> {
                val scale = viewWidth / bitmapWidth
                val newHeight = scale * bitmapHeight
                val halfHeightDiff = (viewHeight - newHeight) / 2.0f
                val dstRect = RectF(0.0f, halfHeightDiff, viewWidth, newHeight + halfHeightDiff)

                result.setRectToRect(bitmapRect, dstRect, Matrix.ScaleToFit.CENTER)
            }

            DisplayType.FIT_Y_LEFT -> {
                val scale = viewHeight / bitmapHeight
                val newWidth = scale * bitmapWidth
                val dstRect = RectF(0.0f, 0.0f, newWidth, viewHeight)

                result.setRectToRect(bitmapRect, dstRect, Matrix.ScaleToFit.CENTER)
            }

            DisplayType.FIT_Y_RIGHT -> {
                val scale = viewHeight / bitmapHeight
                val newWidth = scale * bitmapWidth
                val widthDiff = (viewWidth - newWidth)
                val dstRect = RectF(widthDiff, 0.0f, newWidth + widthDiff, viewHeight)

                result.setRectToRect(bitmapRect, dstRect, Matrix.ScaleToFit.CENTER)
            }

            DisplayType.FIT_Y_CENTER -> {
                val scale = viewHeight / bitmapHeight
                val newWidth = scale * bitmapWidth
                val halfWidthDiff = (viewWidth - newWidth) / 2.0f
                val dstRect = RectF(halfWidthDiff, 0.0f, newWidth + halfWidthDiff, viewHeight)

                result.setRectToRect(bitmapRect, dstRect, Matrix.ScaleToFit.CENTER)
            }

            DisplayType.CENTER_CROP -> {
                val scaleWidth = viewWidth / bitmapWidth
                val scaleHeight = viewHeight / bitmapHeight

                val scale = max(scaleWidth, scaleHeight)
                val newWidth = scale * bitmapWidth
                val newHeight = scale * bitmapHeight

                val halfWidthDiff = (viewWidth - newWidth) / 2.0f
                val halfHeightDiff = (viewHeight - newHeight) / 2.0f

                val dstRect = RectF(
                    halfWidthDiff, halfHeightDiff,
                    newWidth + halfWidthDiff, newHeight + halfHeightDiff
                )

                result.setRectToRect(bitmapRect, dstRect, Matrix.ScaleToFit.CENTER)
            }
        }
    }
}
