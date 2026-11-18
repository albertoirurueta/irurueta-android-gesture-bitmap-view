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

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import androidx.core.graphics.withSave
import java.util.concurrent.Executor
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Draws the image of an [ImageSource] on behalf of a view, keeping its decoded resolution levels
 * in a [MultiResolutionImage].
 *
 * It owns the source and everything needed to decode and draw it: the decoded levels, the
 * maximum bitmap size supported by the canvas, the registration to receive low memory
 * notifications and the matrices mapping view coordinates to stored image pixels. The view using
 * it only has to forward its lifecycle, its size and its display transformation.
 *
 * All methods must be called on the UI thread.
 *
 * @property onChanged called when the bitmaps to draw changed, so the view must be drawn again.
 * @property onPreviewLoaded called when the preview level has been decoded.
 * @property onDetailLoaded called when a level finer than the preview, or a patch, has been
 * decoded.
 * @property onFailed called when a decode fails, with the cause when known.
 */
internal class SourceImageRenderer(
    private val onChanged: () -> Unit,
    private val onPreviewLoaded: () -> Unit,
    private val onDetailLoaded: () -> Unit,
    private val onFailed: (Throwable?) -> Unit
) {

    /**
     * Executor used to decode images in background, or null to use a thread owned by each image.
     * Replaceable for testing purposes.
     */
    var executor: Executor? = null

    /**
     * Source of the image being drawn, or null when there is none.
     */
    var source: ImageSource? = null
        private set

    /**
     * Holder of the decoded resolution levels of [source]. It is null when there is no source, or
     * while detached.
     */
    var image: MultiResolutionImage? = null
        private set

    /**
     * Indicates whether edges of the drawn bitmaps are antialiased.
     */
    var isAntiAlias: Boolean
        get() = paint.isAntiAlias
        set(value) {
            paint.isAntiAlias = value
        }

    /**
     * Paint used to draw levels and patches, which are always filtered because they are drawn at
     * a scale different from their own.
     */
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /**
     * Maximum width of a bitmap supported by the canvas, captured while drawing.
     */
    private var maxBitmapWidth = DEFAULT_MAX_BITMAP_SIZE

    /**
     * Maximum height of a bitmap supported by the canvas, captured while drawing.
     */
    private var maxBitmapHeight = DEFAULT_MAX_BITMAP_SIZE

    /**
     * Indicates whether the maximum bitmap size has been captured from a canvas.
     */
    private var maxBitmapSizeKnown = false

    /**
     * Handler used to post results of background decoding to the UI thread.
     */
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Context where [componentCallbacks] is registered, or null when not registered.
     */
    private var callbacksContext: Context? = null

    /**
     * Matrix mapping view coordinates into stored image coordinates. Reused to avoid allocations.
     */
    private val inverseStoredMatrix = Matrix()

    /**
     * Matrix mapping stored image coordinates into view coordinates. Reused to avoid allocations.
     */
    private val storedToViewMatrix = Matrix()

    /**
     * Matrix mapping upright image coordinates into stored image coordinates. Reused to avoid
     * allocations.
     */
    private val inverseOrientationMatrix = Matrix()

    /**
     * Array of matrix values. Reused to avoid allocations.
     */
    private val matrixValues = FloatArray(MATRIX_VALUES_LENGTH)

    /**
     * Rectangle expressed in upright image pixels. Reused to avoid allocations.
     */
    private val uprightRect = RectF()

    /**
     * Rectangle expressed in stored image pixels. Reused to avoid allocations.
     */
    private val storedRect = Rect()

    /**
     * Callbacks to free memory when the system is low on it.
     */
    private val componentCallbacks = object : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) {
            image?.trimLevels()
        }

        override fun onConfigurationChanged(newConfig: Configuration) {
            // not used
        }

        @Deprecated("Deprecated in Java")
        override fun onLowMemory() {
            image?.trimLevels()
        }
    }

    /**
     * Sets the source to draw, closing the previous one, and starts holding its decoded levels.
     *
     * @param newSource source to draw, which is owned by this instance from now on.
     */
    fun setSource(newSource: ImageSource) {
        closeSource()
        source = newSource
        createImage()
    }

    /**
     * Closes the current source, if any, along with its decoded levels.
     *
     * The source is closed in background, so that the UI thread never waits for a decode in
     * progress.
     */
    fun closeSource() {
        val currentImage = image
        val currentSource = source
        image = null
        source = null
        if (currentImage != null) {
            currentImage.close()
        } else if (currentSource != null) {
            closeInBackground(currentSource)
        }
    }

    /**
     * Starts listening to low memory notifications and decodes the levels of the source again
     * when they were released by [detach].
     *
     * @param context context of the view.
     * @return true when the decoded levels were created, so that the displayed region and the
     * configuration must be provided again.
     */
    fun attach(context: Context): Boolean {
        if (callbacksContext == null) {
            context.registerComponentCallbacks(componentCallbacks)
            callbacksContext = context
        }
        if (source != null && image == null) {
            createImage()
            return true
        }
        return false
    }

    /**
     * Stops listening to low memory notifications and releases the decoded levels. The source is
     * kept, so that levels are decoded again by [attach].
     */
    fun detach() {
        callbacksContext?.unregisterComponentCallbacks(componentCallbacks)
        callbacksContext = null
        image?.release()
        image = null
    }

    /**
     * Configures the decoded levels for the size of the view and its settings.
     *
     * Nothing is done without source or while the view has no size. When the image is larger than
     * the maximum bitmap size assumed before drawing, configuration waits until the real limit is
     * captured by [captureMaxBitmapSize], so that levels are not planned, or reported as failed,
     * against a limit that is not the real one.
     *
     * @param viewW width of the view in pixels.
     * @param viewH height of the view in pixels.
     * @param budgetBytes memory budget for all levels in bytes.
     * @param hardware true to decode hardware bitmaps.
     * @param regionDecoding true to decode by regions at several resolutions, false to decode
     * the whole image at full resolution.
     */
    fun configure(viewW: Int, viewH: Int, budgetBytes: Long, hardware: Boolean, regionDecoding: Boolean) {
        val currentImage = image ?: return
        val currentSource = source ?: return
        if (viewW <= 0 || viewH <= 0) {
            return
        }
        val exceedsAssumedLimit = currentSource.storedWidth > DEFAULT_MAX_BITMAP_SIZE ||
                currentSource.storedHeight > DEFAULT_MAX_BITMAP_SIZE
        if (!maxBitmapSizeKnown && exceedsAssumedLimit) {
            return
        }
        currentImage.configure(
            viewW, viewH, budgetBytes, maxBitmapWidth, maxBitmapHeight, hardware, regionDecoding
        )
    }

    /**
     * Notifies the displayed region, so that the level and detail patch to draw are chosen.
     *
     * @param displayMatrix matrix mapping upright image pixels into view coordinates.
     * @param orientationMatrix matrix mapping stored image pixels into upright image pixels.
     * @param viewW width of the view in pixels.
     * @param viewH height of the view in pixels.
     * @param scaling true while the user is scaling.
     */
    fun update(displayMatrix: Matrix, orientationMatrix: Matrix, viewW: Int, viewH: Int, scaling: Boolean) {
        val currentImage = image ?: return
        if (viewW <= 0 || viewH <= 0) {
            return
        }

        // stored pixels -> upright image pixels -> view coordinates
        storedToViewMatrix.set(displayMatrix)
        storedToViewMatrix.preConcat(orientationMatrix)
        if (!storedToViewMatrix.invert(inverseStoredMatrix)) {
            return
        }
        storedToViewMatrix.getValues(matrixValues)
        val sx = matrixValues[Matrix.MSCALE_X]
        val ky = matrixValues[Matrix.MSKEW_Y]
        val displayScale = sqrt(sx * sx + ky * ky)
        currentImage.update(inverseStoredMatrix, displayScale, viewW, viewH, scaling)
    }

    /**
     * Notifies that the contents of the image changed, so that its decoded levels are refreshed
     * in background.
     *
     * @param region changed region expressed in upright image pixels, or null when the whole
     * image changed.
     * @param orientationMatrix matrix mapping stored image pixels into upright image pixels.
     * @return true when there are decoded levels to refresh, false when there is no source or it
     * is detached.
     */
    fun invalidate(region: Rect?, orientationMatrix: Matrix): Boolean {
        val currentImage = image ?: return false
        if (region == null) {
            currentImage.invalidate(null)
        } else if (orientationMatrix.invert(inverseOrientationMatrix)) {
            uprightRect.set(region)
            inverseOrientationMatrix.mapRect(uprightRect)
            storedRect.set(
                floor(uprightRect.left).toInt(),
                floor(uprightRect.top).toInt(),
                ceil(uprightRect.right).toInt(),
                ceil(uprightRect.bottom).toInt()
            )
            currentImage.invalidate(storedRect)
        }
        return true
    }

    /**
     * Captures the maximum bitmap size supported by a canvas.
     *
     * @param canvas canvas being drawn.
     * @return true when the size was captured for the first time or it changed, so that the image
     * must be configured again.
     */
    fun captureMaxBitmapSize(canvas: Canvas): Boolean {
        val w = canvas.maximumBitmapWidth
        val h = canvas.maximumBitmapHeight
        if (w <= 0 || h <= 0) {
            return false
        }
        val changed = !maxBitmapSizeKnown || w != maxBitmapWidth || h != maxBitmapHeight
        maxBitmapWidth = w
        maxBitmapHeight = h
        maxBitmapSizeKnown = true
        return changed
    }

    /**
     * Draws the current level and detail patch of the image.
     * Canvas must be expressed in stored image pixels.
     *
     * @param canvas canvas where bitmaps are drawn.
     * @param destinationRect rectangle of the whole stored image, in stored image pixels.
     */
    fun draw(canvas: Canvas, destinationRect: RectF) {
        val currentImage = image ?: return
        val level = currentImage.levelBitmap
        val patch = currentImage.patchBitmap
        if (level != null && !level.isRecycled) {
            if (patch != null && currentImage.hasAlpha) {
                // avoid blending translucent pixels of the level with those of the patch
                canvas.withSave {
                    clipOutRect(currentImage.patchRegion)
                    drawLevel(this, level, null, destinationRect)
                }
            } else {
                drawLevel(canvas, level, null, destinationRect)
            }
        }
        if (patch != null && !patch.isRecycled) {
            drawLevel(canvas, patch, currentImage.patchRegion, destinationRect)
        }
    }

    /**
     * Draws a bitmap, using a software copy of it when it is a hardware bitmap and the canvas is
     * not hardware accelerated.
     *
     * @param canvas canvas where the bitmap is drawn.
     * @param bitmap bitmap to draw.
     * @param region region where the bitmap is drawn, or null to draw it over the whole stored
     * image.
     * @param destinationRect rectangle of the whole stored image, in stored image pixels.
     */
    private fun drawLevel(canvas: Canvas, bitmap: Bitmap, region: Rect?, destinationRect: RectF) {
        val needsCopy = !canvas.isHardwareAccelerated && bitmap.config == Bitmap.Config.HARDWARE
        val toDraw = if (needsCopy) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
        if (toDraw == null) {
            return
        }
        if (region != null) {
            canvas.drawBitmap(toDraw, null, region, paint)
        } else {
            canvas.drawBitmap(toDraw, null, destinationRect, paint)
        }
        if (toDraw !== bitmap) {
            toDraw.recycle()
        }
    }

    /**
     * Creates the holder of the decoded levels of the current source, releasing any previous one.
     */
    private fun createImage() {
        val currentSource = source ?: return
        image?.release()
        image = MultiResolutionImage(
            currentSource,
            executor,
            post = { runnable -> mainHandler.post(runnable) },
            onChanged = onChanged,
            onPreviewLoaded = onPreviewLoaded,
            onDetailLoaded = onDetailLoaded,
            onFailed = onFailed
        )
    }

    /**
     * Closes a source that has no decoded levels, away from the UI thread because a decode
     * started before detaching may still be in progress.
     *
     * @param closedSource source to close.
     */
    private fun closeInBackground(closedSource: ImageSource) {
        val closer = Runnable { MultiResolutionImage.closeQuietly(closedSource) }
        val currentExecutor = executor
        if (currentExecutor != null) {
            currentExecutor.execute(closer)
        } else {
            Thread(closer, CLOSER_THREAD_NAME).apply { isDaemon = true }.start()
        }
    }

    /**
     * Constants of [SourceImageRenderer].
     */
    private companion object {
        /**
         * Maximum bitmap size assumed until the canvas reports its own while drawing.
         */
        const val DEFAULT_MAX_BITMAP_SIZE = 4096

        /**
         * Length of arrays to store matrix values.
         */
        const val MATRIX_VALUES_LENGTH = 9

        /**
         * Name of the thread used to close sources that have no decoded levels.
         */
        const val CLOSER_THREAD_NAME = "SourceImageRendererCloser"
    }
}
