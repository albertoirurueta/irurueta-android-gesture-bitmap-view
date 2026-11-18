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
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
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
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MultiResolutionImageTest {

    private val direct = Executor { it.run() }

    private lateinit var source: FakeSource
    private var changed = 0
    private var previewLoaded = 0
    private var detailLoaded = 0
    private val failures = mutableListOf<Throwable?>()

    @Before
    fun setUp() {
        source = FakeSource()
        changed = 0
        previewLoaded = 0
        detailLoaded = 0
        failures.clear()
    }

    @Test
    fun configure_decodesPreviewThenLevelsInOrder() {
        val image = create()
        image.configure()

        assertEquals(listOf(4, 2, 1), source.decodes.map { it.sample })
        for (decode in source.decodes) {
            assertEquals(Rect(0, 0, IMAGE_W, IMAGE_H), decode.region)
            assertEquals(Bitmap.Config.ARGB_8888, decode.config)
            assertNull(decode.reuse)
        }
        assertEquals(1, previewLoaded)
        assertEquals(2, detailLoaded)
        assertEquals(4, image.levelSample)
        assertSame(source.results[0], image.levelBitmap)
        assertNull(image.patchBitmap)
        assertTrue(image.hasAlpha)
        assertTrue(changed > 0)
    }

    @Test
    fun configure_withZeroBudget_decodesOnlyPreview() {
        val image = create()
        image.configure(budget = 0L)

        assertEquals(listOf(4), source.decodes.map { it.sample })
        assertEquals(1, previewLoaded)
        assertEquals(0, detailLoaded)
    }

    @Test
    fun configure_withHardware_usesHardwareConfig() {
        val image = create()
        image.configure(hardware = true)

        assertTrue(source.decodes.all { it.config == Bitmap.Config.HARDWARE })
    }

    @Test
    fun configure_withSameParameters_doesNotDecodeAgain() {
        val image = create()
        image.configure()
        image.configure()

        assertEquals(3, source.decodes.size)
    }

    @Test
    fun configure_withInvalidSizes_reportsFailureOrIgnores() {
        val image = create()
        image.configure(viewW = 0)
        assertTrue(source.decodes.isEmpty())
        assertTrue(failures.isEmpty())

        image.configure(viewW = VIEW_W, viewH = VIEW_H, budget = -1L)
        assertEquals(1, failures.size)

        source = FakeSource(0, 0)
        val empty = create()
        empty.configure()
        assertEquals(2, failures.size)
    }

    @Test
    fun update_withCachedLevelEnough_swapsLevelWithoutDecoding() {
        val image = create()
        image.configure()
        val count = source.decodes.size

        image.show(scaled(2.0f), 0.5f)

        assertEquals(2, image.levelSample)
        assertSame(source.results[1], image.levelBitmap)
        assertEquals(count, source.decodes.size)
        assertNull(image.patchBitmap)
    }

    @Test
    fun update_withFinestLevelEnough_doesNotRequestPatch() {
        val image = create()
        image.configure()
        val count = source.decodes.size

        image.show(Matrix(), 1.0f)
        image.show(scaled(0.5f), 2.0f)

        assertEquals(1, image.levelSample)
        assertEquals(count, source.decodes.size)
        assertNull(image.patchBitmap)
    }

    @Test
    fun update_beyondFinestLevel_requestsPatch() {
        val image = create()
        image.configure(budget = LIMITED_BUDGET)
        assertEquals(listOf(4, 2), source.decodes.map { it.sample })
        val details = detailLoaded

        image.show()

        val patch = source.decodes.last()
        assertEquals(1, patch.sample)
        assertEquals(Rect(0, 0, 1250, 938), patch.region)
        assertEquals(Bitmap.Config.ARGB_8888, patch.config)
        assertSame(source.results.last(), image.patchBitmap)
        assertEquals(Rect(0, 0, 1250, 938), image.patchRegion)
        assertEquals(1, image.patchSample)
        assertEquals(2, image.levelSample)
        assertEquals(details + 1, detailLoaded)

        val count = source.decodes.size
        image.show()
        assertEquals(count, source.decodes.size)
    }

    @Test
    fun update_whileScalingWithDifferentSample_defersPatch() {
        val image = create()
        image.configure(budget = 0L)

        image.show(scaled(2.0f), 0.5f)
        assertEquals(2, source.decodes.last().sample)
        assertEquals(2, source.decodes.size)

        image.show(Matrix(), 1.0f, scaling = true)
        assertEquals(2, source.decodes.size)

        image.show(Matrix(), 1.0f, scaling = false)
        assertEquals(3, source.decodes.size)
        assertEquals(1, source.decodes.last().sample)
        assertEquals(1, image.patchSample)
    }

    @Test
    fun update_withCoverageMissWhileScaling_requestsPatchAtOnce() {
        val image = create()
        image.configure(budget = 0L)
        image.show()
        assertEquals(2, source.decodes.size)

        image.show(Matrix().apply { setTranslate(1500.0f, 0.0f) }, 1.0f, scaling = true)

        assertEquals(3, source.decodes.size)
        assertTrue(image.patchRegion.contains(Rect(1500, 0, 2500, 750)))
    }

    @Test
    fun update_zoomingOut_releasesPatch() {
        val image = create()
        image.configure(budget = LIMITED_BUDGET)
        image.show()
        val patch = image.patchBitmap
        assertNotNull(patch)
        val count = source.decodes.size

        image.show(scaled(2.0f), 0.5f)

        assertNull(image.patchBitmap)
        assertTrue(patch?.isRecycled == true)
        assertEquals(2, image.levelSample)
        assertEquals(count, source.decodes.size)
    }

    @Test
    fun update_withInvalidScale_isIgnored() {
        val image = create()
        image.configure()
        val count = changed

        image.update(Matrix(), 0.0f, VIEW_W, VIEW_H, false)
        image.update(Matrix(), Float.NaN, VIEW_W, VIEW_H, false)
        image.update(Matrix(), 1.0f, 0, VIEW_H, false)

        assertEquals(count, changed)
    }

    @Test
    fun configure_whenCalledAgainBeforeDecoding_recyclesAndDropsOlderResults() {
        val executor = ManualExecutor()
        val image = create(executor)
        image.configure(budget = 0L)
        image.configure(budget = 0L, viewW = 500, viewH = 375)
        assertTrue(source.decodes.isEmpty())

        executor.runAll()

        assertEquals(listOf(4, 8), source.decodes.map { it.sample })
        assertTrue(source.results[0].isRecycled)
        assertSame(source.results[1], image.levelBitmap)
        assertEquals(8, image.levelSample)
        assertEquals(1, previewLoaded)
    }

    @Test
    fun configure_withFinerPreview_keepsOldBitmapUntilReplaced() {
        val executor = ManualExecutor()
        val image = create(executor)
        image.configure(budget = 0L, viewW = 500, viewH = 375)
        executor.runAll()
        val coarse = image.levelBitmap
        assertEquals(8, image.levelSample)
        val notifications = changed

        image.configure(budget = 0L)
        assertSame(coarse, image.levelBitmap)
        assertFalse(coarse?.isRecycled == true)

        executor.runAll()

        assertEquals(4, image.levelSample)
        assertNotSame(coarse, image.levelBitmap)
        assertTrue(coarse?.isRecycled == true)
        assertTrue(changed > notifications)
        assertEquals(2, previewLoaded)
    }

    @Test
    fun trimLevels_dropsLevelsAndPatchAndDecodesAgainOnDemand() {
        val image = create()
        image.configure()
        image.show()
        assertEquals(1, image.levelSample)

        image.trimLevels()

        assertEquals(4, image.levelSample)
        assertNull(image.patchBitmap)
        assertTrue(source.results[1].isRecycled)
        assertTrue(source.results[2].isRecycled)
        assertFalse(source.results[0].isRecycled)
        assertEquals(3, source.decodes.size)

        image.show()

        assertEquals(listOf(1, 2, 1), source.decodes.drop(3).map { it.sample })
        assertEquals(1, image.levelSample)
        assertNull(image.patchBitmap)
    }

    @Test
    fun invalidate_inSoftwareMode_decodesMergedRegionIntoLevels() {
        val executor = ManualExecutor()
        val image = create(executor)
        image.configure()
        executor.runAll()
        val level = image.levelBitmap
        source.decodes.clear()
        source.color = Color.RED
        val notifications = changed

        image.invalidate(Rect(0, 0, 100, 100))
        image.invalidate(Rect(50, 50, 200, 200))
        image.invalidate(Rect(150, 150, 300, 300))
        assertEquals(0, source.contentChangedCalls)
        executor.runAll()

        assertEquals(2, source.contentChangedCalls)
        assertEquals(listOf(4, 2, 1), source.decodes.map { it.sample })
        for (decode in source.decodes) {
            assertEquals(Rect(0, 0, 300, 300), decode.region)
        }
        assertSame(level, image.levelBitmap)
        assertEquals(Color.RED, level?.getPixel(10, 10))
        assertEquals(Color.BLUE, level?.getPixel(100, 100))
        assertTrue(changed > notifications)
    }

    @Test
    fun invalidate_withWholeImage_decodesWholeLevels() {
        val image = create()
        image.configure()
        val level = image.levelBitmap
        source.decodes.clear()

        image.invalidate(null)

        assertEquals(1, source.contentChangedCalls)
        assertEquals(3, source.decodes.size)
        for (decode in source.decodes) {
            assertEquals(Rect(0, 0, IMAGE_W, IMAGE_H), decode.region)
        }
        assertNotSame(level, image.levelBitmap)
        assertTrue(level?.isRecycled == true)
    }

    @Test
    fun invalidate_inHardwareMode_decodesWholeLevelsAndMergesCalls() {
        val executor = ManualExecutor()
        val image = create(executor)
        image.configure(hardware = true)
        executor.runAll()
        val level = image.levelBitmap
        source.decodes.clear()

        image.invalidate(Rect(0, 0, 100, 100))
        image.invalidate(Rect(500, 500, 600, 600))
        image.invalidate(Rect(700, 700, 800, 800))
        executor.runAll()

        assertEquals(2, source.contentChangedCalls)
        assertEquals(3, source.decodes.size)
        for (decode in source.decodes) {
            assertEquals(Rect(0, 0, IMAGE_W, IMAGE_H), decode.region)
            assertEquals(Bitmap.Config.HARDWARE, decode.config)
        }
        assertNotSame(level, image.levelBitmap)
        assertTrue(level?.isRecycled == true)
    }

    @Test
    fun invalidate_withRegionOutsideImage_isIgnored() {
        val image = create()
        image.configure()

        image.invalidate(Rect(IMAGE_W + 10, 0, IMAGE_W + 20, 10))

        assertEquals(0, source.contentChangedCalls)
    }

    @Test
    fun invalidate_decodesPatchAgainReusingSoftwareBitmaps() {
        val image = create()
        image.configure(budget = LIMITED_BUDGET)
        image.show()
        val first = image.patchBitmap
        source.decodes.clear()

        image.invalidate(Rect(0, 0, 100, 100))
        val second = image.patchBitmap
        assertNotSame(first, second)
        image.invalidate(Rect(0, 0, 100, 100))

        val patches = source.decodes.filter { it.sample == 1 }
        assertEquals(2, patches.size)
        assertNull(patches[0].reuse)
        assertSame(first, patches[1].reuse)
        assertTrue(second?.isRecycled == true)
        assertFalse(first?.isRecycled == true)
        assertEquals(2, source.contentChangedCalls)
    }

    @Test
    fun invalidate_withPatchInHardwareMode_recyclesPreviousPatch() {
        val image = create()
        image.configure(budget = LIMITED_BUDGET, hardware = true)
        image.show()
        val first = image.patchBitmap
        assertEquals(Bitmap.Config.HARDWARE, source.decodes.last().config)

        image.invalidate(null)

        assertTrue(first?.isRecycled == true)
        assertNotSame(first, image.patchBitmap)
        assertNull(source.decodes.last().reuse)
    }

    @Test
    fun invalidate_whenContentChangedThrows_reportsFailure() {
        val image = create()
        image.configure()
        val error = IllegalStateException("boom")
        source.contentChangedError = error
        source.decodes.clear()

        image.invalidate(null)

        assertEquals(listOf<Throwable?>(error), failures)
        assertTrue(source.decodes.isEmpty())
    }

    @Test
    fun configure_whenDecodeReturnsNull_reportsFailure() {
        source.returnNull = true
        val image = create()
        image.configure()

        assertEquals(3, failures.size)
        assertTrue(failures.all { it == null })
        assertNull(image.levelBitmap)
    }

    @Test
    fun configure_withoutRegionDecoding_decodesSingleLevel() {
        source = FakeSource(1000, 800)
        val image = create()
        image.configure(regionDecoding = false)

        assertEquals(1, source.decodes.size)
        assertEquals(1, source.decodes[0].sample)
        assertEquals(Rect(0, 0, 1000, 800), source.decodes[0].region)
        assertEquals(1, previewLoaded)

        image.show(Matrix(), 4.0f)
        assertEquals(1, source.decodes.size)
        assertNull(image.patchBitmap)
        assertEquals(1, image.levelSample)
    }

    @Test
    fun configure_withoutRegionDecodingAndTooLargeImage_reportsFailure() {
        source = FakeSource(5000, 4000)
        val image = create()
        image.configure(VIEW_W, VIEW_H, FULL_BUDGET, 4096, 4096, false, false)

        assertTrue(source.decodes.isEmpty())
        assertEquals(1, failures.size)
        assertTrue(failures[0] is IllegalStateException)
    }

    @Test
    fun configure_withExecutorThatDoesNotRun_decodesNothing() {
        val image = create(Executor { })
        image.configure()
        image.show()

        assertTrue(source.decodes.isEmpty())
        assertNull(image.levelBitmap)
        assertEquals(0, changed)
    }

    @Test
    fun configure_neverDecodesOnCallingThread() {
        val executor = ManualExecutor()
        val image = create(executor)
        image.configure()

        assertTrue(source.decodes.isEmpty())
        assertEquals(1, executor.pending)
    }

    @Test
    fun configure_withDefaultExecutor_decodesOnWorkerThread() {
        val latch = CountDownLatch(1)
        val image = MultiResolutionImage(
            source,
            post = { it.run() },
            onChanged = { },
            onPreviewLoaded = { latch.countDown() },
            onDetailLoaded = { },
            onFailed = { }
        )
        image.configure(budget = 0L)

        assertTrue(latch.await(10, TimeUnit.SECONDS))
        assertEquals(1, source.threads.size)
        assertNotSame(Thread.currentThread(), source.threads[0])
        image.release()
    }

    @Test
    fun release_recyclesEverythingAndKeepsSourceOpen() {
        val image = create()
        image.configure(budget = LIMITED_BUDGET)
        image.show()
        val bitmaps = source.results.toList()
        assertEquals(3, bitmaps.size)

        image.release()

        assertTrue(bitmaps.all { it.isRecycled })
        assertNull(image.levelBitmap)
        assertNull(image.patchBitmap)
        assertFalse(source.closed)

        val count = source.decodes.size
        image.configure()
        image.show()
        image.invalidate(null)
        image.trimLevels()
        assertEquals(count, source.decodes.size)
    }

    @Test
    fun release_withDecodeInFlight_recyclesResultAndDoesNotNotify() {
        val executor = ManualExecutor()
        val image = create(executor)
        image.configure()
        image.release()

        executor.runAll()

        assertEquals(1, source.results.size)
        assertTrue(source.results[0].isRecycled)
        assertEquals(0, changed)
        assertEquals(0, previewLoaded)
    }

    @Test
    fun close_releasesAndClosesSource() {
        val image = create()
        image.configure()
        val bitmaps = source.results.toList()

        image.close()

        assertTrue(source.closed)
        assertTrue(bitmaps.all { it.isRecycled })
    }

    @Test
    fun close_withDecodeInFlight_closesSourceOnExecutorAfterIt() {
        val executor = ManualExecutor()
        val image = create(executor)
        image.configure()

        image.close()

        // the calling thread does not close the source while a decode may be running
        assertFalse(source.closed)

        executor.runAll()

        assertTrue(source.closed)
    }

    @Test
    fun configure_withoutRegionDecodingAfterPatch_releasesPatch() {
        val image = create()
        image.configure(budget = LIMITED_BUDGET)
        image.show()
        val patch = image.patchBitmap
        assertNotNull(patch)

        image.configure(budget = LIMITED_BUDGET, regionDecoding = false)

        assertNull(image.patchBitmap)
        assertTrue(patch?.isRecycled == true)
        assertEquals(1, image.levelSample)
    }

    @Test
    fun configure_withPendingRefreshes_stillRefreshesKeptLevels() {
        val executor = ManualExecutor()
        val image = create(executor)
        image.configure(hardware = true)
        executor.runAll()
        source.decodes.clear()

        // content changes and, before levels are decoded again, the view is resized
        image.invalidate(null)
        image.configure(hardware = true, viewW = VIEW_W / 2, viewH = VIEW_H / 2)
        executor.runAll()

        val kept = image.levelSample
        assertTrue(source.decodes.any { it.sample == kept })
    }

    @Test
    fun update_whenDecodedPatchIsNoLongerUseful_keepsReusedSpareBitmap() {
        val executor = ManualExecutor()
        val image = create(executor)
        image.configure(budget = LIMITED_BUDGET)
        executor.runAll()
        image.show()
        executor.runAll()
        image.invalidate(null)
        executor.runAll()
        // a spare bitmap now exists; the next patch is decoded into it
        source.reuseGiven = true
        image.invalidate(null)
        // run the invalidation, which queues the patch, then pan away so it is no longer useful
        executor.runNext()
        image.show(Matrix().apply { setTranslate(IMAGE_W / 2.0f, IMAGE_H / 2.0f) })
        executor.runAll()

        // the spare bitmap survives the discarded patch and is lent again to the next one
        val lent = source.decodes.mapNotNull { it.reuse }
        assertEquals(2, lent.size)
        assertSame(lent[0], lent[1])
        assertFalse(lent[0].isRecycled)
    }

    private fun create(executor: Executor? = direct) = MultiResolutionImage(
        source,
        executor,
        post = { it.run() },
        onChanged = { changed++ },
        onPreviewLoaded = { previewLoaded++ },
        onDetailLoaded = { detailLoaded++ },
        onFailed = { failures.add(it) }
    )

    private fun MultiResolutionImage.configure(
        budget: Long = FULL_BUDGET,
        hardware: Boolean = false,
        regionDecoding: Boolean = true,
        viewW: Int = VIEW_W,
        viewH: Int = VIEW_H
    ) = configure(viewW, viewH, budget, MAX_BITMAP, MAX_BITMAP, hardware, regionDecoding)

    private fun MultiResolutionImage.show(
        inverse: Matrix = Matrix(),
        scale: Float = 1.0f,
        scaling: Boolean = false
    ) = update(inverse, scale, VIEW_W, VIEW_H, scaling)

    private fun scaled(factor: Float) = Matrix().apply { setScale(factor, factor) }

    private class Decode(val region: Rect, val sample: Int, val config: Bitmap.Config, val reuse: Bitmap?)

    private class FakeSource(
        override val storedWidth: Int = IMAGE_W,
        override val storedHeight: Int = IMAGE_H
    ) : ImageSource {
        override val orientation = ImageOrientation.NORMAL
        val decodes = mutableListOf<Decode>()
        val results = mutableListOf<Bitmap>()
        val threads = mutableListOf<Thread>()
        var contentChangedCalls = 0
        var contentChangedError: Exception? = null
        var closed = false
        var returnNull = false
        var reuseGiven = false
        var color = Color.BLUE

        override fun decodeRegion(region: Rect, sampleSize: Int, config: Bitmap.Config, reuse: Bitmap?): Bitmap? {
            decodes.add(Decode(Rect(region), sampleSize, config, reuse))
            threads.add(Thread.currentThread())
            if (returnNull) {
                return null
            }
            if (reuseGiven && reuse != null) {
                return reuse
            }
            val w = (region.width() + sampleSize - 1) / sampleSize
            val h = (region.height() + sampleSize - 1) / sampleSize
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(color)
            results.add(bitmap)
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

    private class ManualExecutor : Executor {
        private val queue = ArrayDeque<Runnable>()

        val pending: Int
            get() = queue.size

        override fun execute(command: Runnable) {
            queue.addLast(command)
        }

        fun runNext() {
            queue.removeFirst().run()
        }

        fun runAll() {
            while (queue.isNotEmpty()) {
                queue.removeFirst().run()
            }
        }
    }

    private companion object {
        const val IMAGE_W = 4000
        const val IMAGE_H = 3000
        const val VIEW_W = 1000
        const val VIEW_H = 750
        const val MAX_BITMAP = 8192
        const val FULL_BUDGET = 200L * 1024 * 1024
        const val LIMITED_BUDGET = 16L * 1024 * 1024
    }
}
