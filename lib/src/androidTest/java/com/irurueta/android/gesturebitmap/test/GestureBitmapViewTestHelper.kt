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

import android.graphics.Bitmap
import android.graphics.Canvas
import com.irurueta.android.gesturebitmap.GestureBitmapView
import com.irurueta.android.gesturebitmap.MetricTransformationParameters

/**
 * Helpers shared by instrumented tests that inspect what a [GestureBitmapView] draws.
 * Functions flagged as UI thread functions must be invoked from the UI thread.
 */
object GestureBitmapViewTestHelper {

    /**
     * Side, in pixels, of the small square view used by tests, so that the preview of test images
     * is subsampled and zooming in requires a detail patch.
     */
    const val SMALL_VIEW_SIZE = 200

    /**
     * Changes the size of the view (UI thread).
     *
     * @param view view to resize.
     * @param size new width and height in pixels.
     */
    fun resize(view: GestureBitmapView, size: Int = SMALL_VIEW_SIZE) {
        val params = view.layoutParams
        params.width = size
        params.height = size
        view.layoutParams = params
        view.requestLayout()
    }

    /**
     * Draws the view on a bitmap backed canvas, which is not hardware accelerated (UI thread).
     *
     * @param view view to draw.
     * @return bitmap containing the drawn view.
     */
    fun draw(view: GestureBitmapView): Bitmap {
        return Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also {
            view.draw(Canvas(it))
        }
    }

    /**
     * Sets a scale keeping the center of the displayed image at the center of the view (UI thread).
     *
     * @param view view to zoom.
     * @param scale scale to set.
     */
    fun zoomAtCenter(view: GestureBitmapView, scale: Double) {
        view.transformationParameters = MetricTransformationParameters(scale = scale)
        val rect = view.displayRect ?: return
        // params matrix is applied after the base matrix, hence translation is in view pixels
        view.transformationParameters = MetricTransformationParameters(
            scale = scale,
            horizontalTranslation = view.width / 2.0 - rect.centerX(),
            verticalTranslation = view.height / 2.0 - rect.centerY()
        )
    }

    /**
     * Waits until the view has the given size.
     *
     * @param view view to wait for.
     * @param size expected width and height in pixels.
     * @param timeoutMillis maximum time to wait.
     * @return true if the view got the size in time.
     */
    fun awaitSize(view: GestureBitmapView, size: Int, timeoutMillis: Long = 5000L): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (view.width == size && view.height == size) {
                return true
            }
            Thread.sleep(50L)
        }
        return false
    }
}
