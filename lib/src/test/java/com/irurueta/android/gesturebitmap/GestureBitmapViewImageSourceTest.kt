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

import android.app.Activity
import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.widget.FrameLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.test.core.app.ApplicationProvider
import com.irurueta.android.testutils.callPrivateFunc
import com.irurueta.android.testutils.callPrivateFuncWithResult
import com.irurueta.android.testutils.getPrivateProperty
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.File
import java.io.InputStream
import java.util.concurrent.Executor
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GestureBitmapViewImageSourceTest {

    private val direct = Executor { it.run() }

    private lateinit var context: Context
    private lateinit var source: FakeSource

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        source = FakeSource()
    }

    @After
    fun tearDown() {
        idle()
        unmockkStatic(BitmapRegionDecoder::class)
    }

    @Test
    fun constructor_setsDefaultValues() {
        val view = GestureBitmapView(context)

        assertNull(view.imageSource)
        assertNull(view.bitmap)
        assertTrue(view.regionDecodingEnabled)
        assertTrue(view.hardwareBitmapsEnabled)
        assertEquals(
            GestureBitmapView.DEFAULT_CACHED_LEVELS_MEMORY_BUDGET,
            view.cachedLevelsMemoryBudget
        )
        assertNull(view.imageLoadListener)
        assertNull(view.imageOverlayDrawer)
    }

    @Test
    fun cachedLevelsMemoryBudget_withNegativeValue_throws() {
        val view = GestureBitmapView(context)

        assertThrows(IllegalArgumentException::class.java) { view.cachedLevelsMemoryBudget = -1L }
        view.cachedLevelsMemoryBudget = 0L
        assertEquals(0L, view.cachedLevelsMemoryBudget)
    }

    @Test
    fun setImageSource_setsSourceAndComputesDisplayRect() {
        val view = createView()

        view.setImageSource(source)

        assertSame(source, view.imageSource)
        assertNull(view.bitmap)
        assertNotNull(image(view))
        val rect = view.displayRect
        assertNotNull(rect)
        assertEquals(RectF(0.0f, 0.0f, VIEW_W.toFloat(), VIEW_H.toFloat()), rect)
    }

    @Test
    fun setImageSource_withRotatedOrientation_usesUprightSize() {
        val view = createView()

        view.setImageSource(FakeSource(orientation = ImageOrientation.ROTATE_90))

        val rect = view.displayRect
        assertNotNull(rect)
        // upright image is 3000x4000 and is fitted horizontally
        assertEquals(VIEW_W.toFloat(), rect!!.width(), 0.01f)
        assertEquals(VIEW_W * 4.0f / 3.0f, rect.height(), 0.01f)
    }

    @Test
    fun setImageSource_decodesPreviewAndNotifiesListener() {
        val view = createView()
        val listener = Listener()
        view.imageLoadListener = listener

        view.setImageSource(source)
        idle()

        assertTrue(source.decodes.isNotEmpty())
        assertEquals(1, listener.preview)
        assertTrue(listener.failures.isEmpty())
    }

    @Test
    fun setImageSource_withSameSource_doesNothing() {
        val view = createView()
        view.setImageSource(source)
        val image = image(view)

        view.setImageSource(source)
        view.imageSource = source

        assertFalse(source.closed)
        assertSame(image, image(view))
    }

    @Test
    fun setImageSource_withoutResettingMatrices_keepsTransformation() {
        val view = createView()
        view.setImageSource(source)
        view.transformationParameters = MetricTransformationParameters(
            2.0, 0.0, 5.0, 6.0
        )

        view.setImageSource(FakeSource(), resetMatrices = false)

        assertEquals(2.0, view.transformationParameters.scale, 1e-6)
    }

    @Test
    fun setImageSource_whenReplacingSource_closesPrevious() {
        val view = createView()
        val other = FakeSource()
        view.setImageSource(source)

        view.imageSource = other

        assertTrue(source.closed)
        assertFalse(other.closed)
        assertSame(other, view.imageSource)
    }

    @Test
    fun setImageSource_withNull_closesSourceAndClearsImage() {
        val view = createView()
        view.setImageSource(source)

        view.setImageSource(null)

        assertTrue(source.closed)
        assertNull(view.imageSource)
        assertNull(image(view))
        assertNull(view.displayRect)
    }

    @Test
    fun setBitmap_whenSourceIsSet_closesSource() {
        val view = createView()
        view.setImageSource(source)
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        view.bitmap = bitmap

        assertTrue(source.closed)
        assertNull(view.imageSource)
        assertSame(bitmap, view.bitmap)
    }

    @Test
    fun setBitmap_withNullWhenSourceIsSet_closesSource() {
        val view = createView()
        view.setImageSource(source)

        view.bitmap = null

        assertTrue(source.closed)
        assertNull(view.imageSource)
    }

    @Test
    fun setImageSource_whenBitmapIsSet_recyclesBitmap() {
        val view = createView()
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        view.bitmap = bitmap

        view.setImageSource(source)

        assertTrue(bitmap.isRecycled)
        assertNull(view.bitmap)
        assertSame(source, view.imageSource)
    }

    @Test
    fun setImageResource_usesRegionDecodedSource() {
        stubDecoder()
        val view = GestureBitmapView(context)

        view.setImageResource(drawableId("test_raster"))

        assertNotNull(view.imageSource)
        assertEquals(10, view.imageSource!!.storedWidth)
        assertNull(view.bitmap)
    }

    @Test
    fun setImageFile_usesRegionDecodedSource() {
        stubDecoder()
        val view = GestureBitmapView(context)

        view.setImageFile(uniqueFile())

        assertEquals(10, view.imageSource!!.storedWidth)
        assertEquals(20, view.imageSource!!.storedHeight)
    }

    @Test
    fun setImageUri_usesRegionDecodedSource() {
        stubDecoder()
        val view = GestureBitmapView(context)

        view.setImageUri(Uri.fromFile(uniqueFile()))

        assertEquals(10, view.imageSource!!.storedWidth)
    }

    @Test
    fun transformationParameters_whenZoomingInAndOut_requestsAndReleasesPatchWithoutChangingBaseMatrix() {
        val view = createView()
        view.cachedLevelsMemoryBudget = 0L
        view.setImageSource(source)
        idle()
        val image = image(view)!!
        assertNull(image.patchBitmap)

        val before = view.displayTransformationMatrix
        val baseBefore = view.baseTransformationMatrix
        view.transformationParameters = MetricTransformationParameters(8.0, 0.0, 0.0, 0.0)
        idle()
        val zoomed = view.displayTransformationMatrix

        assertNotNull(image.patchBitmap)
        assertEquals(baseBefore, view.baseTransformationMatrix)

        // patch appearing does not change matrices
        idle()
        assertEquals(zoomed, view.displayTransformationMatrix)

        view.transformationParameters = MetricTransformationParameters(1.0, 0.0, 0.0, 0.0)
        idle()

        assertNull(image.patchBitmap)
        assertEquals(before, view.displayTransformationMatrix)
        assertEquals(baseBefore, view.baseTransformationMatrix)
    }

    @Test
    fun transformationParameters_whenZoomingInAndFinestLevelIsCached_requestsNoPatch() {
        val view = createView()
        view.cachedLevelsMemoryBudget = 200L * 1024 * 1024
        view.setImageSource(source)
        idle()

        view.transformationParameters = MetricTransformationParameters(8.0, 0.0, 0.0, 0.0)
        idle()

        assertNull(image(view)!!.patchBitmap)
        assertTrue(source.decodes.all { it.width() == IMAGE_W && it.height() == IMAGE_H })
    }

    @Test
    fun regionDecodingEnabled_whenChangedWithSourceSet_decodesImageAgain() {
        val view = createView()
        view.cachedLevelsMemoryBudget = 0L
        view.setImageSource(source)
        idle()
        val preview = source.decodes.size

        view.regionDecodingEnabled = false
        idle()
        assertFalse(view.regionDecodingEnabled)
        assertTrue(source.decodes.size > preview)
        assertEquals(Rect(0, 0, IMAGE_W, IMAGE_H), source.decodes.last())

        val count = source.decodes.size
        view.regionDecodingEnabled = true
        view.cachedLevelsMemoryBudget = 200L * 1024 * 1024
        idle()
        assertTrue(source.decodes.size > count)

        view.hardwareBitmapsEnabled = false
        assertFalse(view.hardwareBitmapsEnabled)
    }

    @Test
    fun onSaveAndRestoreInstanceState_restoresImageDecodingProperties() {
        val view = createView()
        view.regionDecodingEnabled = false
        view.hardwareBitmapsEnabled = false
        view.cachedLevelsMemoryBudget = 777L
        val state: Bundle = view.callPrivateFuncWithResult("onSaveInstanceState")!!

        val restored = GestureBitmapView(context)
        restored.callPrivateFunc("onRestoreInstanceState", state)

        assertFalse(restored.regionDecodingEnabled)
        assertFalse(restored.hardwareBitmapsEnabled)
        assertEquals(777L, restored.cachedLevelsMemoryBudget)
    }

    @Test
    fun onRestoreInstanceState_whenDecodingKeysAreMissing_restoresDefaultValues() {
        val view = createView()
        val state: Bundle = view.callPrivateFuncWithResult("onSaveInstanceState")!!
        state.remove("regionDecodingEnabled")
        state.remove("hardwareBitmapsEnabled")
        state.remove("cachedLevelsMemoryBudget")

        val restored = GestureBitmapView(context)
        restored.regionDecodingEnabled = false
        restored.hardwareBitmapsEnabled = false
        restored.cachedLevelsMemoryBudget = 5L
        restored.callPrivateFunc("onRestoreInstanceState", state)

        assertTrue(restored.regionDecodingEnabled)
        assertTrue(restored.hardwareBitmapsEnabled)
        assertEquals(
            GestureBitmapView.DEFAULT_CACHED_LEVELS_MEMORY_BUDGET,
            restored.cachedLevelsMemoryBudget
        )
        assertNull(restored.imageSource)
    }

    @Test
    fun onDetachedFromWindow_whenAttachedAgain_releasesAndRecreatesImage() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val container = FrameLayout(activity)
        activity.setContentView(container)
        val view = GestureBitmapView(activity)
        view.imageExecutor = direct
        container.addView(view, FrameLayout.LayoutParams(VIEW_W, VIEW_H))
        view.layout(0, 0, VIEW_W, VIEW_H)
        view.setImageSource(source)
        idle()
        assertNotNull(image(view))
        val decodes = source.decodes.size

        container.removeView(view)
        idle()

        assertNull(image(view))
        assertFalse(source.closed)
        assertSame(source, view.imageSource)
        draw(view)

        container.addView(view, FrameLayout.LayoutParams(VIEW_W, VIEW_H))
        view.layout(0, 0, VIEW_W, VIEW_H)
        idle()

        assertNotNull(image(view))
        assertTrue(source.decodes.size > decodes)
        assertNotNull(image(view)!!.levelBitmap)
    }

    @Test
    fun onTrimMemory_whenAttached_dropsPatch() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val container = FrameLayout(activity)
        activity.setContentView(container)
        val view = GestureBitmapView(activity)
        view.imageExecutor = direct
        view.cachedLevelsMemoryBudget = 0L
        container.addView(view, FrameLayout.LayoutParams(VIEW_W, VIEW_H))
        view.layout(0, 0, VIEW_W, VIEW_H)
        view.setImageSource(source)
        view.transformationParameters = MetricTransformationParameters(8.0, 0.0, 0.0, 0.0)
        idle()
        val image = image(view)!!
        assertNotNull(image.patchBitmap)

        activity.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)

        assertNull(image.patchBitmap)

        // detail is requested again when the view becomes visible
        view.onVisibilityAggregated(true)
        idle()

        assertNotNull(image.patchBitmap)

        // no longer registered after detaching
        container.removeView(view)
        activity.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }

    @Test
    fun onDraw_withImageLargerThanAssumedMaximumBitmapSize_decodesOnlyOnceDrawn() {
        val large = FakeSource(LARGE_IMAGE_SIZE, LARGE_IMAGE_SIZE)
        val view = createView()
        val listener = Listener()
        view.imageLoadListener = listener
        view.regionDecodingEnabled = false

        view.setImageSource(large)
        idle()

        // nothing is decoded, nor reported as failed, until the canvas reports its limit
        assertTrue(large.decodes.isEmpty())
        assertTrue(listener.failures.isEmpty())

        draw(view)
        idle()

        assertEquals(1, large.decodes.size)
        assertTrue(listener.failures.isEmpty())
    }

    @Test
    fun constructor_keepsPlatformDefaultFilteringForPlainBitmaps() {
        val view = createView()

        val paint: Paint? = view.getPrivateProperty("bitmapPaint")

        // plain bitmaps are drawn as before: filtering is whatever the platform defaults to
        assertEquals(Paint().isFilterBitmap, requireNotNull(paint).isFilterBitmap)
    }

    @Test
    fun imageLoadListener_whenDetailLoadsAndDecodingFails_isNotified() {
        val view = createView()
        val listener = Listener()
        view.imageLoadListener = listener
        view.cachedLevelsMemoryBudget = 200L * 1024 * 1024
        view.setImageSource(source)
        idle()

        assertEquals(1, listener.preview)
        assertTrue(listener.detail > 0)

        val failing = FakeSource().apply { returnNull = true }
        view.setImageSource(failing)
        idle()

        assertTrue(listener.failures.isNotEmpty())
    }

    @Test
    fun onDraw_withAlphaAndPatch_clipsOutPatchRegion() {
        val view = createView()
        view.cachedLevelsMemoryBudget = 0L
        view.setImageSource(source)
        view.transformationParameters = MetricTransformationParameters(8.0, 0.0, 0.0, 0.0)
        idle()
        assertNotNull(image(view)!!.patchBitmap)

        val canvas = draw(view)

        assertEquals(1, canvas.clipOutCalls)
        // level and patch
        assertEquals(2, canvas.drawnConfigs.size)
    }

    @Test
    fun onDraw_withOpaqueImageAndPatch_doesNotClipOut() {
        val view = createView()
        source.opaque = true
        view.cachedLevelsMemoryBudget = 0L
        view.setImageSource(source)
        view.transformationParameters = MetricTransformationParameters(8.0, 0.0, 0.0, 0.0)
        idle()

        val canvas = draw(view)

        assertEquals(0, canvas.clipOutCalls)
        assertEquals(2, canvas.drawnConfigs.size)
    }

    @Test
    fun drawLevel_withSoftwareBitmapOnSoftwareCanvas_drawsBitmapItself() {
        // hardware bitmaps are not available on the JVM, so only the software path is checked
        val view = createView()
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val canvas = RecordingCanvas(Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888))

        renderer(view).callPrivateFunc(
            "drawLevel", canvas, bitmap, Rect(0, 0, 5, 5), RectF(0.0f, 0.0f, 10.0f, 10.0f)
        )

        assertEquals(listOf<Bitmap.Config?>(Bitmap.Config.ARGB_8888), canvas.drawnConfigs)
        assertFalse(bitmap.isRecycled)
    }

    @Test
    fun onDraw_withoutImage_doesNotCallOverlayDrawer() {
        val view = createView()
        var calls = 0
        view.imageOverlayDrawer = object : GestureBitmapView.ImageOverlayDrawer {
            override fun onDrawOverImage(view: GestureBitmapView, canvas: Canvas) {
                calls++
            }
        }

        draw(view)

        assertEquals(0, calls)
    }

    @Test
    fun imageOverlayDrawer_withSource_receivesUprightImageCanvas() {
        for (orientation in intArrayOf(
            ImageOrientation.NORMAL,
            ImageOrientation.ROTATE_90,
            ImageOrientation.ROTATE_180,
            ImageOrientation.FLIP_HORIZONTAL
        )) {
            for (scale in doubleArrayOf(1.0, 2.0)) {
                for (angle in doubleArrayOf(0.0, Math.PI / 2.0, Math.PI)) {
                    val view = createView(SMALL_W, SMALL_H)
                    val src = FakeSource(orientation = orientation)
                    view.setImageSource(src)
                    idle()
                    view.transformationParameters =
                        MetricTransformationParameters(scale, angle, 1000.0, 1000.0)
                    assertOverlay(
                        view, src.uprightWidth(), src.uprightHeight(), scale == 1.0 && angle == 0.0
                    )
                }
            }
        }
    }

    @Test
    fun imageOverlayDrawer_withBitmap_receivesImageCanvas() {
        for (scale in doubleArrayOf(1.0, 2.0)) {
            for (angle in doubleArrayOf(0.0, Math.PI / 2.0, Math.PI)) {
                val view = createView(SMALL_W, SMALL_H)
                view.bitmap = Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888)
                view.transformationParameters =
                    MetricTransformationParameters(scale, angle, 1000.0, 1000.0)
                assertOverlay(view, 40, 20, scale == 1.0 && angle == 0.0)
            }
        }
    }

    @Test
    fun mapViewToImage_roundTripsWithDisplayTransformationMatrix() {
        val view = createView()
        view.setImageSource(FakeSource(orientation = ImageOrientation.ROTATE_90))
        view.transformationParameters = MetricTransformationParameters(2.0, 0.3, 10.0, -20.0)
        val matrix = view.displayTransformationMatrix
        val result = PointF()

        for (point in arrayOf(floatArrayOf(0.0f, 0.0f), floatArrayOf(100.0f, 250.0f))) {
            val imagePoint = point.copyOf()
            val viewPoint = FloatArray(2)
            matrix.mapPoints(viewPoint, imagePoint)

            assertTrue(view.mapViewToImage(viewPoint[0], viewPoint[1], result))

            assertEquals(imagePoint[0], result.x, 1e-2f)
            assertEquals(imagePoint[1], result.y, 1e-2f)
        }
    }

    @Test
    fun mapViewToImage_withoutImage_returnsFalse() {
        val view = createView()

        assertFalse(view.mapViewToImage(1.0f, 2.0f, PointF()))
    }

    @Test
    fun mapViewToImage_withNonInvertibleMatrix_returnsFalse() {
        val view = createView()
        view.bitmap = Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888)
        view.transformationParameters = MetricTransformationParameters(0.0, 0.0, 0.0, 0.0)

        assertFalse(view.mapViewToImage(1.0f, 2.0f, PointF()))
    }

    @Test
    fun invalidateImage_withoutImage_doesNothing() {
        val view = createView()

        view.invalidateImage()
        view.invalidateImage(Rect(0, 0, 10, 10))
    }

    @Test
    fun invalidateImage_withWholeImage_notifiesSourceAndDecodesAgain() {
        val view = createView()
        view.setImageSource(source)
        idle()
        val decodes = source.decodes.size

        view.invalidateImage()
        view.invalidateImage()
        idle()

        assertTrue(source.contentChangedCalls >= 1)
        assertTrue(source.decodes.size > decodes)
    }

    @Test
    fun invalidateImage_withRegion_mapsUprightRegionToStoredPixels() {
        val upright = Rect(10, 20, 110, 70)
        val cases = mapOf(
            ImageOrientation.NORMAL to Rect(10, 20, 110, 70),
            ImageOrientation.ROTATE_180 to Rect(IMAGE_W - 110, IMAGE_H - 70, IMAGE_W - 10, IMAGE_H - 20),
            ImageOrientation.ROTATE_90 to Rect(20, IMAGE_H - 110, 70, IMAGE_H - 10),
            ImageOrientation.FLIP_HORIZONTAL to Rect(IMAGE_W - 110, 20, IMAGE_W - 10, 70)
        )
        for ((orientation, stored) in cases) {
            val view = createView()
            val src = FakeSource(orientation = orientation)
            view.cachedLevelsMemoryBudget = 200L * 1024 * 1024
            view.setImageSource(src)
            idle()
            val before = src.decodes.size

            view.invalidateImage(upright)
            idle()

            val refreshed = src.decodes.drop(before)
            assertTrue("orientation $orientation", refreshed.isNotEmpty())
            assertTrue(
                "orientation $orientation: $refreshed",
                refreshed.any { it.contains(stored) && it.width() < IMAGE_W }
            )
        }
    }

    @Test
    fun invalidateImage_withBitmap_nextDrawUsesEditedBitmap() {
        val view = createView(40, 20)
        view.displayType = GestureBitmapView.DisplayType.NONE
        val bitmap = Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        view.bitmap = bitmap
        val first = draw(view, 40)
        assertTrue(first.drawnConfigs.isNotEmpty())

        bitmap.eraseColor(Color.GREEN)
        view.invalidateImage()
        val canvasBitmap = Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(canvasBitmap))

        assertEquals(Color.GREEN, canvasBitmap.getPixel(20, 10))
    }

    @Test
    fun invalidateImage_whenSizeChanged_reportsFailure() {
        val view = createView()
        val listener = Listener()
        view.imageLoadListener = listener
        view.setImageSource(source)
        idle()
        source.contentChangedError = IllegalStateException("size changed")

        view.invalidateImage()
        idle()

        assertEquals(1, listener.failures.size)
        assertTrue(listener.failures[0] is IllegalStateException)
    }

    @Test
    fun constructor_withRasterSrc_usesSource() {
        stubDecoder()
        val view = GestureBitmapView(context, attributes("src" to "@drawable/test_raster"))

        assertNotNull(view.imageSource)
        assertNull(view.bitmap)
    }

    @Test
    fun constructor_withXmlDrawableSrc_usesDrawablePath() {
        val view = GestureBitmapView(context, attributes("src" to "@drawable/test_vector"))

        assertNull(view.imageSource)
        val expected = context.getDrawable(drawableId("test_vector"))!!.toBitmap()
        assertEquals(expected.width, view.bitmap!!.width)
        assertEquals(expected.height, view.bitmap!!.height)
    }

    @Test
    fun constructor_withRegionDecodingDisabled_usesDrawablePath() {
        val view = GestureBitmapView(
            context,
            attributes(
                "src" to "@drawable/test_raster",
                "regionDecodingEnabled" to "false"
            )
        )

        assertNull(view.imageSource)
        assertFalse(view.regionDecodingEnabled)
        val expected = context.getDrawable(drawableId("test_raster"))!!.toBitmap()
        assertEquals(expected.width, view.bitmap!!.width)
        assertEquals(expected.height, view.bitmap!!.height)
    }

    @Test
    fun constructor_withAttributes_setsProperties() {
        val view = GestureBitmapView(
            context,
            attributes(
                "regionDecodingEnabled" to "false",
                "hardwareBitmapsEnabled" to "false",
                "cachedLevelsMemoryBudget" to "12345"
            )
        )

        assertFalse(view.regionDecodingEnabled)
        assertFalse(view.hardwareBitmapsEnabled)
        assertEquals(12345L, view.cachedLevelsMemoryBudget)
    }

    private fun stubDecoder() {
        val decoder = mockk<BitmapRegionDecoder>(relaxed = true)
        every { decoder.width } returns 10
        every { decoder.height } returns 20
        mockkStatic(BitmapRegionDecoder::class)
        every { BitmapRegionDecoder.newInstance(any<InputStream>()) } returns decoder
        every { BitmapRegionDecoder.newInstance(any<InputStream>(), any()) } returns decoder
    }

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun createView(w: Int = VIEW_W, h: Int = VIEW_H): GestureBitmapView {
        val view = GestureBitmapView(context)
        view.imageExecutor = direct
        view.layout(0, 0, w, h)
        return view
    }

    private fun renderer(view: GestureBitmapView): SourceImageRenderer {
        val renderer: SourceImageRenderer? = view.getPrivateProperty("sourceImageRenderer")
        return requireNotNull(renderer)
    }

    private fun image(view: GestureBitmapView): MultiResolutionImage? = renderer(view).image

    private fun uniqueFile(): File {
        val file = File.createTempFile("gbv", ".png")
        file.deleteOnExit()
        val bitmap = Bitmap.createBitmap(16, 8, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    private fun drawableId(name: String): Int =
        context.resources.getIdentifier(name, "drawable", context.packageName)

    private fun draw(view: GestureBitmapView, size: Int = CANVAS_SIZE): RecordingCanvas {
        val canvas = RecordingCanvas(Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888))
        view.draw(canvas)
        return canvas
    }

    private fun FakeSource.uprightWidth() =
        ImageOrientation.uprightWidth(orientation, storedWidth, storedHeight)

    private fun FakeSource.uprightHeight() =
        ImageOrientation.uprightHeight(orientation, storedWidth, storedHeight)

    @Suppress("DEPRECATION")
    private fun assertOverlay(view: GestureBitmapView, w: Int, h: Int, exact: Boolean) {
        var calls = 0
        var matrix: Matrix? = null
        var clip: Rect? = null
        view.imageOverlayDrawer = object : GestureBitmapView.ImageOverlayDrawer {
            override fun onDrawOverImage(view: GestureBitmapView, canvas: Canvas) {
                calls++
                matrix = Matrix(canvas.matrix)
                clip = canvas.clipBounds
            }
        }

        draw(view)

        assertEquals(1, calls)
        val expected = FloatArray(9)
        view.displayTransformationMatrix.getValues(expected)
        val actual = FloatArray(9)
        matrix!!.getValues(actual)
        for (i in 0 until 9) {
            assertEquals(expected[i], actual[i], 1e-3f)
        }
        val bounds = clip!!
        // clip is never larger than the image rectangle
        assertTrue("clip $bounds", bounds.left >= -CLIP_TOLERANCE && bounds.top >= -CLIP_TOLERANCE)
        assertTrue("clip $bounds", bounds.right <= w + CLIP_TOLERANCE)
        assertTrue("clip $bounds", bounds.bottom <= h + CLIP_TOLERANCE)
        if (exact) {
            // image is fully visible
            assertTrue("clip $bounds", abs(bounds.right - w) <= CLIP_TOLERANCE)
            assertTrue("clip $bounds", abs(bounds.bottom - h) <= CLIP_TOLERANCE)
            assertTrue("clip $bounds", abs(bounds.left) <= CLIP_TOLERANCE)
            assertTrue("clip $bounds", abs(bounds.top) <= CLIP_TOLERANCE)
        }
    }

    private fun attributes(vararg values: Pair<String, String>): android.util.AttributeSet {
        val builder = Robolectric.buildAttributeSet()
        for ((name, value) in values) {
            val id = context.resources.getIdentifier(name, "attr", context.packageName)
            builder.addAttribute(id, value)
        }
        return builder.build()
    }

    private class FakeSource(
        override val storedWidth: Int = IMAGE_W,
        override val storedHeight: Int = IMAGE_H,
        override val orientation: Int = ImageOrientation.NORMAL
    ) : ImageSource {
        val decodes = mutableListOf<Rect>()
        var contentChangedCalls = 0
        var contentChangedError: Exception? = null
        var closed = false
        var returnNull = false
        var opaque = false

        override fun decodeRegion(
            region: Rect,
            sampleSize: Int,
            config: Bitmap.Config,
            reuse: Bitmap?
        ): Bitmap? {
            decodes.add(Rect(region))
            if (returnNull) {
                return null
            }
            val w = (region.width() + sampleSize - 1) / sampleSize
            val h = (region.height() + sampleSize - 1) / sampleSize
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.BLUE)
            if (opaque) {
                bitmap.setHasAlpha(false)
            }
            return bitmap
        }

        override fun contentChanged() {
            contentChangedCalls++
            contentChangedError?.let { throw it }
        }

        override fun close() {
            closed = true
        }
    }

    private class RecordingCanvas(bitmap: Bitmap) : Canvas(bitmap) {
        var clipOutCalls = 0
        val drawnConfigs = mutableListOf<Bitmap.Config?>()

        override fun clipOutRect(rect: Rect): Boolean {
            clipOutCalls++
            return super.clipOutRect(rect)
        }

        override fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: Rect, paint: Paint?) {
            drawnConfigs.add(bitmap.config)
            super.drawBitmap(bitmap, src, dst, paint)
        }

        override fun drawBitmap(bitmap: Bitmap, src: Rect?, dst: RectF, paint: Paint?) {
            drawnConfigs.add(bitmap.config)
            super.drawBitmap(bitmap, src, dst, paint)
        }
    }

    private class Listener : GestureBitmapView.OnImageLoadListener {
        var preview = 0
        var detail = 0
        val failures = mutableListOf<Throwable?>()

        override fun onPreviewLoaded(view: GestureBitmapView) {
            preview++
        }

        override fun onDetailLoaded(view: GestureBitmapView) {
            detail++
        }

        override fun onImageLoadFailed(view: GestureBitmapView, error: Throwable?) {
            failures.add(error)
        }
    }

    private companion object {
        const val LARGE_IMAGE_SIZE = 5000
        const val IMAGE_W = 4000
        const val IMAGE_H = 3000
        const val VIEW_W = 1000
        const val VIEW_H = 750
        const val SMALL_W = 400
        const val SMALL_H = 300
        const val CANVAS_SIZE = 2000
        const val CLIP_TOLERANCE = 12
    }
}
