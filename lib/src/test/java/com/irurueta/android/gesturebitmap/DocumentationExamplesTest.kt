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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.Executor

/**
 * Compiled versions of the code examples shown in the documentation pages `usage.adoc` and
 * `editing.adoc`. Each snippet is delimited by `tag::name[]` and `end::name[]` comments, and the
 * documentation shows exactly the lines between them.
 *
 * Variables used by a snippet but not declared in it (`view`, `context`, `file`, ...) are the ones
 * a real application would already have, and are declared before its tag.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DocumentationExamplesTest {

    /**
     * Stands for the `R` class of the application used by the snippets (`R.drawable.image`).
     */
    private object R {
        object drawable {
            var image = 0
        }
    }

    private val direct = Executor { it.run() }

    private lateinit var context: Context
    private lateinit var view: GestureBitmapView
    private val tempFiles = mutableListOf<File>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        R.drawable.image =
            context.resources.getIdentifier("test_raster", "drawable", context.packageName)
        stubDecoder()
        view = GestureBitmapView(context)
        view.imageExecutor = direct
        view.layout(0, 0, VIEW_SIZE, VIEW_SIZE)
    }

    @After
    fun tearDown() {
        view.imageSource = null
        shadowOf(Looper.getMainLooper()).idle()
        tempFiles.forEach { it.delete() }
        unmockkStatic(BitmapRegionDecoder::class)
    }

    // Robolectric cannot region decode, so a decoder reporting the size of the test images is used
    private fun stubDecoder() {
        val decoder = mockk<BitmapRegionDecoder>(relaxed = true)
        every { decoder.width } returns IMAGE_SIZE
        every { decoder.height } returns IMAGE_SIZE
        mockkStatic(BitmapRegionDecoder::class)
        every { BitmapRegionDecoder.newInstance(any<InputStream>()) } returns decoder
        every { BitmapRegionDecoder.newInstance(any<InputStream>(), any()) } returns decoder
    }

    private fun encodedPng(color: Int, size: Int = IMAGE_SIZE): ByteArray {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    private fun tempFile(color: Int): File {
        val file = File.createTempFile("docs", ".png")
        file.writeBytes(encodedPng(color))
        tempFiles.add(file)
        return file
    }

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun drawView(): Bitmap {
        val result = Bitmap.createBitmap(VIEW_SIZE, VIEW_SIZE, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(result))
        return result
    }

    @Test
    fun setImageResource() {
        // tag::setImageResource[]
        // R.drawable.image is a JPEG, PNG or WebP resource
        view.setImageResource(R.drawable.image)
        // end::setImageResource[]

        assertNotNull(view.imageSource)
        assertNull(view.bitmap)
    }

    @Test
    fun setImageUri() {
        val file = tempFile(Color.RED)
        // tag::setImageUri[]
        // a content:// or file:// URI, for instance returned by the system photo picker
        val uri: Uri = Uri.fromFile(file)
        view.setImageUri(uri)
        // end::setImageUri[]

        assertNotNull(view.imageSource)
        assertEquals(IMAGE_SIZE, view.imageSource!!.storedWidth)
    }

    @Test
    fun setImageFile() {
        val file = tempFile(Color.GREEN)
        // tag::setImageFile[]
        view.setImageFile(file)

        // keep the current zoom and position when replacing the image by another one
        view.setImageFile(file, resetMatrices = false)
        // end::setImageFile[]

        assertNotNull(view.imageSource)
    }

    @Test
    fun setImageSourceWithFactory() {
        val bytes = encodedPng(Color.BLUE)
        // tag::setImageSource[]
        // other factories: fromResource, fromAsset, fromFile and fromUri
        view.setImageSource(ImageSources.fromBytes(bytes))
        // end::setImageSource[]

        assertNotNull(view.imageSource)
        assertEquals(IMAGE_SIZE, view.imageSource!!.storedHeight)
    }

    @Test
    fun customImageSource() {
        // tag::customImageSource[]
        // Generates a 20000 x 20000 pixels image procedurally. It is never held whole in memory.
        class CheckerboardImageSource : ImageSource {
            override val storedWidth = 20000
            override val storedHeight = 20000
            override val orientation = ExifInterface.ORIENTATION_NORMAL

            // called on a worker thread, never concurrently with itself
            override fun decodeRegion(
                region: Rect,
                sampleSize: Int,
                config: Bitmap.Config,
                reuse: Bitmap?
            ): Bitmap? {
                val width = (region.width() + sampleSize - 1) / sampleSize
                val height = (region.height() + sampleSize - 1) / sampleSize
                val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val column = (region.left + x * sampleSize) / 1000
                        val row = (region.top + y * sampleSize) / 1000
                        val color = if ((column + row) % 2 == 0) Color.DKGRAY else Color.LTGRAY
                        result.setPixel(x, y, color)
                    }
                }
                return result
            }

            override fun close() {
                // release anything held by the source
            }
        }

        view.setImageSource(CheckerboardImageSource())
        // end::customImageSource[]

        val source = view.imageSource!!
        assertEquals(20000, source.storedWidth)
        val decoded = source.decodeRegion(Rect(0, 0, 2000, 2000), 1000, Bitmap.Config.ARGB_8888, null)!!
        assertEquals(2, decoded.width)
        assertEquals(Color.DKGRAY, decoded.getPixel(0, 0))
        assertEquals(Color.LTGRAY, decoded.getPixel(1, 0))
    }

    @Test
    fun regionDecodingDisabled() {
        val file = tempFile(Color.RED)
        // tag::regionDecodingDisabled[]
        // images set through an ImageSource are decoded whole, at their full resolution
        view.regionDecodingEnabled = false
        view.setImageFile(file)
        // end::regionDecodingDisabled[]

        assertFalse(view.regionDecodingEnabled)
        assertNotNull(view.imageSource)
    }

    @Test
    fun bitmapPath() {
        val bitmap = Bitmap.createBitmap(IMAGE_SIZE, IMAGE_SIZE, Bitmap.Config.ARGB_8888)
        // tag::bitmapPath[]
        // the bitmap is held whole in memory; the view recycles it when it is replaced
        view.bitmap = bitmap
        // end::bitmapPath[]

        assertSame(bitmap, view.bitmap)
        assertNull(view.imageSource)
    }

    @Test
    fun cachedLevelsMemoryBudget() {
        // tag::cachedLevelsMemoryBudget[]
        // 32 MiB for the decoded levels
        view.cachedLevelsMemoryBudget = 32L * 1024L * 1024L

        // only the preview is kept: lowest memory use, details are decoded as patches
        view.cachedLevelsMemoryBudget = 0L

        // back to the default: 4 times the pixels of the view, 4 bytes each
        view.cachedLevelsMemoryBudget = GestureBitmapView.DEFAULT_CACHED_LEVELS_MEMORY_BUDGET
        // end::cachedLevelsMemoryBudget[]

        assertEquals(
            GestureBitmapView.DEFAULT_CACHED_LEVELS_MEMORY_BUDGET,
            view.cachedLevelsMemoryBudget
        )
    }

    @Test
    fun hardwareBitmapsDisabled() {
        // tag::hardwareBitmapsDisabled[]
        // decode into software bitmaps (for instance to read their pixels)
        view.hardwareBitmapsEnabled = false
        // end::hardwareBitmapsDisabled[]

        assertFalse(view.hardwareBitmapsEnabled)
    }

    @Test
    fun imageLoadListener() {
        val file = tempFile(Color.RED)
        // tag::imageLoadListener[]
        view.imageLoadListener = object : GestureBitmapView.OnImageLoadListener {
            override fun onPreviewLoaded(view: GestureBitmapView) {
                // the whole image is visible, at low resolution
            }

            override fun onDetailLoaded(view: GestureBitmapView) {
                // a sharper level or a patch has been decoded
            }

            override fun onImageLoadFailed(view: GestureBitmapView, error: Throwable?) {
                Log.e(TAG, "Image could not be decoded", error)
            }
        }
        // end::imageLoadListener[]

        val documented = view.imageLoadListener!!
        var previews = 0
        var failures = 0
        view.imageLoadListener = object : GestureBitmapView.OnImageLoadListener {
            override fun onPreviewLoaded(view: GestureBitmapView) {
                previews++
                documented.onPreviewLoaded(view)
            }

            override fun onDetailLoaded(view: GestureBitmapView) {
                documented.onDetailLoaded(view)
            }

            override fun onImageLoadFailed(view: GestureBitmapView, error: Throwable?) {
                failures++
                documented.onImageLoadFailed(view, error)
            }
        }
        view.setImageFile(file)
        idle()

        assertTrue(previews > 0)
        assertEquals(0, failures)
    }

    @Test
    fun mutableBitmapWithInvalidateImage() {
        // tag::mutableBitmap[]
        val bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
        view.bitmap = bitmap

        // edit the bitmap, then tell the view that its contents changed
        Canvas(bitmap).drawColor(Color.RED)
        view.invalidateImage()
        // end::mutableBitmap[]

        assertSame(bitmap, view.bitmap)
        assertEquals(Color.RED, drawView().getPixel(VIEW_SIZE / 2, VIEW_SIZE / 2))
    }

    @Test
    fun bitmapImageSourceWithRegion() {
        val bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        // tag::bitmapImageSource[]
        // the bitmap stays owned by the caller: the view never recycles it
        view.setImageSource(BitmapImageSource(bitmap))

        // draw on the bitmap, synchronizing on it, and refresh only the edited region
        val edited = Rect(100, 100, 300, 300) // upright image pixels
        synchronized(bitmap) {
            val paint = Paint().apply { color = Color.RED }
            Canvas(bitmap).drawRect(edited, paint)
        }
        view.invalidateImage(edited)
        // end::bitmapImageSource[]
        idle()

        assertFalse(bitmap.isRecycled)
        assertNotNull(view.imageSource)
    }

    @Test
    fun rewrittenFileWithInvalidateImage() {
        val file = tempFile(Color.RED)
        val bytes = encodedPng(Color.GREEN)
        view.setImageFile(file)
        idle()
        // tag::rewrittenFile[]
        view.setImageFile(file)

        // later, another component rewrites the file with an image of the same size
        file.writeBytes(bytes)

        // decode it again
        view.invalidateImage()
        // end::rewrittenFile[]
        idle()

        assertNotNull(view.imageSource)
    }

    @Test
    fun completeDrawMode() {
        val bitmap = Bitmap.createBitmap(IMAGE_SIZE, IMAGE_SIZE, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        // tag::drawMode[]
        // the image to draw on: the view displays it and the caller keeps ownership of the bitmap
        view.setImageSource(BitmapImageSource(bitmap))

        // gestures must not compete with drawing
        view.scrollEnabled = false
        view.scaleEnabled = false
        view.rotationEnabled = false
        view.doubleTapEnabled = false

        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 8.0f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        var stroke: Path? = null // stroke being drawn, in upright image pixels
        val point = PointF()

        // the stroke in progress is drawn above the image, in image pixels
        view.imageOverlayDrawer = object : GestureBitmapView.ImageOverlayDrawer {
            override fun onDrawOverImage(view: GestureBitmapView, canvas: Canvas) {
                stroke?.let { canvas.drawPath(it, paint) }
            }
        }

        view.setOnTouchListener { v, event ->
            // convert the touch position from view pixels to image pixels
            if (!view.mapViewToImage(event.x, event.y, point)) {
                return@setOnTouchListener false
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    stroke = Path().apply { moveTo(point.x, point.y) }
                }
                MotionEvent.ACTION_MOVE -> {
                    stroke?.lineTo(point.x, point.y)
                    v.invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    stroke?.let { finished ->
                        finished.lineTo(point.x, point.y)

                        // commit the stroke to the bitmap
                        synchronized(bitmap) {
                            Canvas(bitmap).drawPath(finished, paint)
                        }

                        // refresh only the region touched by the stroke
                        val bounds = RectF()
                        finished.computeBounds(bounds, true)
                        bounds.inset(-paint.strokeWidth, -paint.strokeWidth)
                        val region = Rect()
                        bounds.roundOut(region)
                        view.invalidateImage(region)
                    }
                    stroke = null
                    v.invalidate()
                }
            }
            true
        }
        // end::drawMode[]

        val down = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 60.0f, 100.0f, 0)
        val move = MotionEvent.obtain(0L, 10L, MotionEvent.ACTION_MOVE, 100.0f, 100.0f, 0)
        val up = MotionEvent.obtain(0L, 20L, MotionEvent.ACTION_UP, 140.0f, 100.0f, 0)
        assertTrue(view.dispatchTouchEvent(down))
        assertTrue(view.dispatchTouchEvent(move))
        // the stroke in progress is not yet in the bitmap
        assertEquals(Color.WHITE, bitmap.getPixel(IMAGE_SIZE / 2, IMAGE_SIZE / 2))
        assertTrue(view.dispatchTouchEvent(up))
        down.recycle()
        move.recycle()
        up.recycle()
        idle()

        assertEquals(Color.RED, bitmap.getPixel(IMAGE_SIZE / 2, IMAGE_SIZE / 2))
        assertFalse(bitmap.isRecycled)
    }

    private companion object {
        const val TAG = "DocumentationExamples"
        const val VIEW_SIZE = 200
        const val IMAGE_SIZE = 100
    }
}
