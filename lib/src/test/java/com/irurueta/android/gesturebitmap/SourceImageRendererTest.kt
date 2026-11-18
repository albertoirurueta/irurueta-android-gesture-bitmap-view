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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SourceImageRendererTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val direct = Executor { it.run() }

    private var changed = 0

    private var previewLoaded = 0

    private var detailLoaded = 0

    private val failures = mutableListOf<Throwable?>()

    private lateinit var renderer: SourceImageRenderer

    @Before
    fun setUp() {
        renderer = SourceImageRenderer(
            onChanged = { changed++ },
            onPreviewLoaded = { previewLoaded++ },
            onDetailLoaded = { detailLoaded++ },
            onFailed = { failures.add(it) }
        )
        renderer.executor = direct
    }

    @Test
    fun constructor_setsDefaultValues() {
        assertNull(renderer.source)
        assertNull(renderer.image)
        assertTrue(renderer.isAntiAlias)
    }

    @Test
    fun isAntiAlias_whenSet_returnsExpectedValue() {
        renderer.isAntiAlias = false

        assertFalse(renderer.isAntiAlias)
    }

    @Test
    fun setSource_whenReplacingSource_closesPreviousAndCreatesImage() {
        val first = FakeSource()
        val second = FakeSource()
        renderer.setSource(first)
        val firstImage = renderer.image

        renderer.setSource(second)

        assertTrue(first.closed)
        assertFalse(second.closed)
        assertSame(second, renderer.source)
        assertNotNull(renderer.image)
        assertNotSame(firstImage, renderer.image)
    }

    @Test
    fun closeSource_whenDetached_closesSourceOnExecutor() {
        val pending = mutableListOf<Runnable>()
        renderer.executor = Executor { pending.add(it) }
        val source = FakeSource()
        renderer.setSource(source)
        renderer.detach()

        renderer.closeSource()

        // the source is not closed on the calling thread
        assertFalse(source.closed)
        assertNull(renderer.source)

        pending.forEach { it.run() }

        assertTrue(source.closed)
    }

    @Test
    fun closeSource_withoutSource_doesNothing() {
        renderer.closeSource()

        assertNull(renderer.source)
        assertNull(renderer.image)
    }

    @Test
    fun attach_afterDetach_createsImageAgainAndListensToLowMemory() {
        val source = FakeSource()
        renderer.setSource(source)

        assertFalse(renderer.attach(context))

        renderer.detach()

        assertNull(renderer.image)
        assertSame(source, renderer.source)
        assertFalse(source.closed)

        assertTrue(renderer.attach(context))

        assertNotNull(renderer.image)

        renderer.detach()
    }

    @Test
    fun configure_withImageWithinAssumedLimit_decodesBeforeDrawing() {
        val source = FakeSource()
        renderer.setSource(source)

        renderer.configure(VIEW_SIZE, VIEW_SIZE, BUDGET, false, true)
        idle()

        assertTrue(source.decodes.isNotEmpty())
        assertEquals(1, previewLoaded)
    }

    @Test
    fun configure_withImageLargerThanAssumedLimit_waitsForCanvasLimit() {
        val source = FakeSource(LARGE_SIZE, LARGE_SIZE)
        renderer.setSource(source)

        renderer.configure(VIEW_SIZE, VIEW_SIZE, BUDGET, false, false)
        idle()

        assertTrue(source.decodes.isEmpty())
        assertTrue(failures.isEmpty())

        val canvas = Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
        assertTrue(renderer.captureMaxBitmapSize(canvas))
        renderer.configure(VIEW_SIZE, VIEW_SIZE, BUDGET, false, false)
        idle()

        assertEquals(1, source.decodes.size)
        assertTrue(failures.isEmpty())
    }

    @Test
    fun configure_withoutSourceOrViewSize_doesNothing() {
        renderer.configure(VIEW_SIZE, VIEW_SIZE, BUDGET, false, true)

        val source = FakeSource()
        renderer.setSource(source)
        renderer.configure(0, VIEW_SIZE, BUDGET, false, true)
        idle()

        assertTrue(source.decodes.isEmpty())
    }

    @Test
    fun captureMaxBitmapSize_whenCapturedAgainWithSameCanvas_returnsFalse() {
        val canvas = Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))

        assertTrue(renderer.captureMaxBitmapSize(canvas))
        assertFalse(renderer.captureMaxBitmapSize(canvas))
    }

    @Test
    fun update_whenZoomedIn_requestsPatchAndDrawsLevelAndPatch() {
        val source = FakeSource()
        renderer.setSource(source)
        renderer.configure(VIEW_SIZE, VIEW_SIZE, 0L, false, true)
        idle()

        renderer.update(Matrix(), Matrix(), VIEW_SIZE, VIEW_SIZE, false)
        idle()

        val image = requireNotNull(renderer.image)
        assertNotNull(image.patchBitmap)
        assertEquals(1, detailLoaded)

        val target = Bitmap.createBitmap(VIEW_SIZE, VIEW_SIZE, Bitmap.Config.ARGB_8888)
        renderer.draw(Canvas(target), RectF(0.0f, 0.0f, IMAGE_SIZE.toFloat(), IMAGE_SIZE.toFloat()))

        assertEquals(Color.BLUE, target.getPixel(VIEW_SIZE / 2, VIEW_SIZE / 2))
    }

    @Test
    fun update_withoutImageOrViewSize_doesNothing() {
        renderer.update(Matrix(), Matrix(), VIEW_SIZE, VIEW_SIZE, false)

        val source = FakeSource()
        renderer.setSource(source)
        renderer.update(Matrix(), Matrix(), 0, VIEW_SIZE, false)
        idle()

        assertTrue(source.decodes.isEmpty())
    }

    @Test
    fun invalidate_withSource_notifiesContentChange() {
        val source = FakeSource()
        renderer.setSource(source)
        renderer.configure(VIEW_SIZE, VIEW_SIZE, BUDGET, false, true)
        idle()

        assertTrue(renderer.invalidate(Rect(0, 0, 10, 10), Matrix()))
        assertTrue(renderer.invalidate(null, Matrix()))
        idle()

        assertTrue(source.contentChangedCalls > 0)
    }

    @Test
    fun invalidate_withoutSource_returnsFalse() {
        assertFalse(renderer.invalidate(null, Matrix()))
    }

    @Test
    fun onTrimMemory_whenAttached_dropsPatch() {
        val source = FakeSource()
        renderer.setSource(source)
        renderer.attach(context)
        renderer.configure(VIEW_SIZE, VIEW_SIZE, 0L, false, true)
        renderer.update(Matrix(), Matrix(), VIEW_SIZE, VIEW_SIZE, false)
        idle()
        val image = requireNotNull(renderer.image)
        assertNotNull(image.patchBitmap)

        context.applicationContext.let { app ->
            (app as android.app.Application).onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        }

        assertNull(image.patchBitmap)

        renderer.detach()
    }

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private class FakeSource(
        override val storedWidth: Int = IMAGE_SIZE,
        override val storedHeight: Int = IMAGE_SIZE
    ) : ImageSource {
        override val orientation = ImageOrientation.NORMAL
        val decodes = mutableListOf<Rect>()
        var contentChangedCalls = 0
        var closed = false

        override fun decodeRegion(region: Rect, sampleSize: Int, config: Bitmap.Config, reuse: Bitmap?): Bitmap? {
            decodes.add(Rect(region))
            val w = (region.width() + sampleSize - 1) / sampleSize
            val h = (region.height() + sampleSize - 1) / sampleSize
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.BLUE)
            return bitmap
        }

        override fun contentChanged() {
            contentChangedCalls++
        }

        override fun close() {
            closed = true
        }
    }

    private companion object {
        const val IMAGE_SIZE = 1024
        const val LARGE_SIZE = 5000
        const val VIEW_SIZE = 128
        const val BUDGET = 64L * 1024 * 1024
    }
}
