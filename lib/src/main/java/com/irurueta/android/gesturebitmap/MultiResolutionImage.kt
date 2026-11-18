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
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Keeps a set of decoded resolution levels of a large image, plus an optional detail patch, and
 * decodes them in the background.
 *
 * The image is decoded through an [ImageSource] on a worker thread, never on the calling thread.
 * A preview level is decoded first, followed by finer levels (coarse to fine) while they fit in
 * the memory budget. When the image is displayed with more detail than the finest cached level, a
 * patch covering only the visible region is decoded.
 *
 * All methods, callbacks and drawing state properties are meant to be used on the UI thread only:
 * results computed by the worker are handed over through [post]. Only one decode is in flight at a
 * time, and every request carries a generation number so that results of an older configuration
 * are recycled and dropped.
 *
 * @property source source of the image pixels.
 * @param executor executor used to run decoding jobs, or null to use a single daemon
 * thread owned by this instance.
 * @property post function posting a [Runnable] to the UI thread.
 * @property onChanged called on the UI thread when the drawing state (level or patch) changed.
 * @property onPreviewLoaded called on the UI thread when the preview level has been decoded.
 * @property onDetailLoaded called on the UI thread when a level finer than the preview, or a patch,
 * has been decoded.
 * @property onFailed called on the UI thread when a decode fails, with the cause when known.
 */
internal class MultiResolutionImage(
    private val source: ImageSource,
    executor: Executor? = null,
    private val post: (Runnable) -> Unit,
    private val onChanged: () -> Unit,
    private val onPreviewLoaded: () -> Unit,
    private val onDetailLoaded: () -> Unit,
    private val onFailed: (Throwable?) -> Unit
) {

    /**
     * Executor created by this instance when none was provided. It is shut down on [release].
     */
    private val ownedExecutor: ExecutorService? =
        if (executor == null) Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, THREAD_NAME).apply { isDaemon = true }
        } else {
            null
        }

    /**
     * Executor used to run decoding jobs, either the provided one or the owned one.
     */
    private val executor: Executor = executor ?: requireNotNull(ownedExecutor)

    /**
     * Decoded levels indexed by sample size.
     */
    private val levels = LinkedHashMap<Int, Bitmap>()
    /**
     * Sample sizes of the levels to keep, from the preview (coarsest) to the finest one.
     */
    private var levelSamples: List<Int> = emptyList()
    /**
     * Sample sizes of the levels waiting to be decoded.
     */
    private val pendingLevels = ArrayList<Int>()
    /**
     * Sample sizes whose decoding failed, not retried until the configuration changes or the content is invalidated.
     */
    private val failedLevels = HashSet<Int>()
    /**
     * Levels waiting to be decoded again because the content changed.
     */
    private val refreshes = ArrayList<RefreshTask>()

    /**
     * Spare bitmap that can be reused to decode the next patch, or null when there is none.
     */
    private var backBitmap: Bitmap? = null
    /**
     * Indicates whether the displayed patch no longer reflects the contents of the image.
     */
    private var patchStale = false
    /**
     * Sample size of the patch that must be decoded next, or [NO_SAMPLE] when no patch is needed.
     */
    private var wantedSample = NO_SAMPLE
    /**
     * Region in stored pixels of the patch that must be decoded next. Only meaningful while
     * [wantedSample] is not [NO_SAMPLE].
     */
    private val wantedRegion = Rect()
    /**
     * Sample size of the last patch whose decoding failed, or [NO_SAMPLE] when there is none. It is
     * not retried until it changes or the content is invalidated.
     */
    private var failedSample = NO_SAMPLE
    /**
     * Region in stored pixels of the last patch whose decoding failed. Only meaningful while
     * [failedSample] is not [NO_SAMPLE].
     */
    private val failedRegion = Rect()
    /**
     * Sample size a patch is planned for in the last known state, or [NO_SAMPLE] when no patch is
     * planned. Used to check decoded patches are still useful.
     */
    private var planSample = NO_SAMPLE
    /**
     * Visible region in stored pixels of the last known state. Only meaningful while [planSample]
     * is not [NO_SAMPLE].
     */
    private val planVisible = Rect()
    /**
     * Sample sizes of the decoded levels, kept in sync with [levels] to avoid allocations while
     * choosing the level to draw.
     */
    private val available = ArrayList<Int>()
    /**
     * Corners of the view. Reused to avoid allocations.
     */
    private val corners = FloatArray(CORNER_VALUES)

    /**
     * Indicates whether a content change must be notified to the source.
     */
    private var pendingInvalidate = false
    /**
     * Region of the pending content change in stored pixels, or null when the whole image changed.
     */
    private var pendingInvalidateRegion: Rect? = null

    /**
     * Job currently running on the worker thread, or null when idle.
     */
    private var inFlightJob: Job? = null
    /**
     * Generation of the current configuration. Jobs created for an older generation are stale and their results are dropped.
     */
    private var generation = 0
    /**
     * Indicates whether [configure] has accepted a valid configuration.
     */
    private var configured = false
    /**
     * Indicates whether [release] has been called.
     */
    private var released = false
    /**
     * Indicates whether levels and patches are decoded as hardware bitmaps.
     */
    private var hardware = false
    /**
     * Indicates whether the source is decoded by regions at several sample sizes.
     */
    private var regionDecoding = true

    /**
     * Indicates whether [update] has provided a displayed region yet.
     */
    private var hasState = false
    /**
     * Copy of the last matrix mapping view coordinates to stored image coordinates.
     */
    private val lastInverse = Matrix()
    /**
     * Sample size required by the last known display scale.
     */
    private var lastRequired = Int.MAX_VALUE
    /**
     * Width of the view in pixels given by the last call to [update].
     */
    private var lastViewW = 0
    /**
     * Height of the view in pixels given by the last call to [update].
     */
    private var lastViewH = 0
    /**
     * Indicates whether the last known state was reported while the user is scaling.
     */
    private var scaling = false

    /**
     * Region of the stored image wanted for a detail patch, in stored pixels.
     */
    private val planned = Rect()
    /**
     * Region of the stored image visible in the view, in stored pixels.
     */
    private val visible = Rect()
    /**
     * Paint used to copy refreshed regions over levels, replacing the previous pixels.
     */
    private val copyPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC) }

    /**
     * Level bitmap to draw, or null when nothing has been decoded yet. Only for the UI thread.
     */
    var levelBitmap: Bitmap? = null
        private set

    /**
     * Sample size of [levelBitmap], or 0 when there is none. Only for the UI thread.
     */
    var levelSample: Int = NO_SAMPLE
        private set

    /**
     * Detail patch bitmap, or null when there is none. Only for the UI thread.
     */
    var patchBitmap: Bitmap? = null
        private set

    /**
     * Region of the stored image covered by [patchBitmap], in stored pixels. Only meaningful while
     * [patchBitmap] is not null. Only for the UI thread.
     */
    val patchRegion = Rect()

    /**
     * Sample size of [patchBitmap], or 0 when there is none. Only for the UI thread.
     */
    var patchSample: Int = NO_SAMPLE
        private set

    /**
     * Indicates whether any of the bitmaps to draw has an alpha channel.
     */
    val hasAlpha: Boolean
        get() = levelBitmap?.hasAlpha() == true || patchBitmap?.hasAlpha() == true

    /**
     * Computes the resolution levels to keep and starts decoding them, the preview first.
     *
     * Calling it again with the same resulting levels and modes does nothing. Otherwise bitmaps
     * already decoded are kept until replaced. Nothing is done when the view size is not positive.
     * Failures are reported through [onFailed].
     *
     * @param viewW width of the view in pixels.
     * @param viewH height of the view in pixels.
     * @param budgetBytes memory budget for all levels in bytes. 0 keeps the preview only.
     * @param maxBitmapW maximum width of a bitmap in pixels.
     * @param maxBitmapH maximum height of a bitmap in pixels.
     * @param hardware true to decode levels and patches as [Bitmap.Config.HARDWARE] bitmaps, false
     * to use mutable [Bitmap.Config.ARGB_8888] ones.
     * @param regionDecoding true when the source can decode regions at several sample sizes. When
     * false a single level at sample 1 is decoded, and failure is reported if it exceeds the
     * maximum bitmap size.
     */
    fun configure(
        viewW: Int,
        viewH: Int,
        budgetBytes: Long,
        maxBitmapW: Int,
        maxBitmapH: Int,
        hardware: Boolean,
        regionDecoding: Boolean
    ) {
        if (released) {
            return
        }
        val w = source.storedWidth
        val h = source.storedHeight
        if (w <= 0 || h <= 0 || maxBitmapW <= 0 || maxBitmapH <= 0 || budgetBytes < 0) {
            onFailed(IllegalArgumentException("invalid image or bitmap size"))
            return
        }
        if (viewW <= 0 || viewH <= 0) {
            return
        }

        val samples = if (regionDecoding) {
            val preview = ResolutionPlanner.previewSampleSize(w, h, viewW, viewH)
            ResolutionPlanner.cachedLevelSampleSizes(w, h, preview, budgetBytes, maxBitmapW, maxBitmapH)
        } else {
            if (w > maxBitmapW || h > maxBitmapH) {
                onFailed(IllegalStateException("image exceeds the maximum bitmap size"))
                return
            }
            listOf(1)
        }
        if (configured && samples == levelSamples && hardware == this.hardware &&
            regionDecoding == this.regionDecoding
        ) {
            return
        }

        val modeChanged = configured && hardware != this.hardware
        generation++
        configured = true
        levelSamples = samples
        this.hardware = hardware
        this.regionDecoding = regionDecoding
        failedLevels.clear()
        failedSample = NO_SAMPLE
        pendingLevels.clear()
        if (modeChanged) {
            // every level is decoded again, so pending refreshes are no longer needed
            refreshes.clear()
        }
        if (modeChanged && patchBitmap != null) {
            patchStale = true
        }
        for (sample in samples) {
            if (modeChanged || !levels.containsKey(sample)) {
                pendingLevels.add(sample)
            }
        }
        pruneLevels()
        notifyIfChanged(evaluate())
        pump()
    }

    /**
     * Updates the displayed region, choosing the level to draw and requesting a detail patch when
     * the cached levels are not fine enough.
     *
     * Invalid scales are ignored.
     *
     * @param inverseDisplayTimesOrientation matrix mapping view coordinates to stored image
     * coordinates.
     * @param displayScale number of view pixels per stored image pixel.
     * @param viewW width of the view in pixels.
     * @param viewH height of the view in pixels.
     * @param scaling true while the user is scaling, which defers patches with a different sample
     * size until scaling ends.
     */
    fun update(
        inverseDisplayTimesOrientation: Matrix,
        displayScale: Float,
        viewW: Int,
        viewH: Int,
        scaling: Boolean
    ) {
        if (released || viewW <= 0 || viewH <= 0 || !(displayScale > 0.0f && displayScale.isFinite())) {
            return
        }
        lastInverse.set(inverseDisplayTimesOrientation)
        lastRequired = ResolutionPlanner.requiredSampleSize(displayScale)
        lastViewW = viewW
        lastViewH = viewH
        this.scaling = scaling
        hasState = true
        notifyIfChanged(evaluate())
        pump()
    }

    /**
     * Notifies that the contents of the image changed, so decoded bitmaps must be refreshed.
     *
     * Pending regions are merged. The source is notified on the worker thread, and then every
     * level and the patch intersecting the region are decoded again. Old bitmaps stay in use until
     * replaced. In software mode only the region of each level is decoded and copied into it; in
     * hardware mode levels are decoded whole.
     *
     * @param region changed region in stored pixels, or null when the whole image changed.
     */
    fun invalidate(region: Rect?) {
        if (released || !configured) {
            return
        }
        if (region == null) {
            pendingInvalidateRegion = null
            pendingInvalidate = true
        } else if (!pendingInvalidate || pendingInvalidateRegion != null) {
            val clipped = Rect(region)
            if (!clipped.intersect(0, 0, source.storedWidth, source.storedHeight)) {
                return
            }
            pendingInvalidateRegion?.let { clipped.union(it) }
            pendingInvalidateRegion = clipped
            pendingInvalidate = true
        }
        failedLevels.clear()
        failedSample = NO_SAMPLE
        pump()
    }

    /**
     * Drops every level finer than the preview and the patch, to free memory. Nothing is decoded
     * again until the next call to [update] demands it.
     */
    fun trimLevels() {
        if (released) {
            return
        }
        generation++
        pendingLevels.clear()
        wantedSample = NO_SAMPLE
        releasePatch()
        val preview = levelSamples.firstOrNull() ?: return
        val iterator = levels.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key < preview) {
                entry.value.recycle()
                iterator.remove()
            }
        }
        syncAvailable()
        if (!levels.containsKey(preview)) {
            pendingLevels.add(preview)
        }
        hasState = false
        lastRequired = Int.MAX_VALUE
        notifyIfChanged(evaluate())
        pump()
    }

    /**
     * Cancels pending work, recycles every bitmap it decoded and shuts down the executor when it
     * is owned by this instance. The source is not closed. Calling it again does nothing.
     */
    fun release() {
        if (released) {
            return
        }
        releaseBitmaps()
        ownedExecutor?.shutdown()
    }

    /**
     * Releases this instance (see [release]) and closes the source.
     *
     * The source is closed on the worker, after any decode in flight, so that the calling thread
     * never waits for a decode to finish. It is closed on the calling thread only when the
     * executor no longer accepts work.
     */
    fun close() {
        val wasReleased = released
        if (!wasReleased) {
            releaseBitmaps()
        }
        try {
            executor.execute { closeQuietly(source) }
        } catch (_: RejectedExecutionException) {
            closeQuietly(source)
        }
        if (!wasReleased) {
            ownedExecutor?.shutdown()
        }
    }

    /**
     * Cancels pending work and recycles every bitmap decoded by this instance.
     */
    private fun releaseBitmaps() {
        released = true
        generation++
        pendingLevels.clear()
        refreshes.clear()
        wantedSample = NO_SAMPLE
        pendingInvalidate = false
        pendingInvalidateRegion = null
        for (bitmap in levels.values) {
            bitmap.recycle()
        }
        levels.clear()
        available.clear()
        levelBitmap = null
        levelSample = NO_SAMPLE
        releasePatch()
    }

    /**
     * Copies the sample sizes of the decoded levels into [available].
     */
    private fun syncAvailable() {
        available.clear()
        available.addAll(levels.keys)
    }

    /**
     * Notifies the UI thread that the drawing state changed, when it did.
     *
     * @param changed true when the drawing state changed.
     */
    private fun notifyIfChanged(changed: Boolean) {
        if (changed) {
            onChanged()
        }
    }

    /**
     * Selects the decoded level with the given sample size to draw, or no level when it is not decoded yet.
     *
     * @param sample sample size of the level to select, or [NO_SAMPLE].
     * @return true when the level to draw changed.
     */
    private fun selectLevel(sample: Int): Boolean {
        val bitmap = levels[sample]
        val changed = bitmap !== levelBitmap || (bitmap != null && sample != levelSample)
        levelBitmap = bitmap
        levelSample = if (bitmap != null) sample else NO_SAMPLE
        return changed
    }

    /**
     * Drops cached levels that are not planned, once the preview is available.
     */
    private fun pruneLevels() {
        val preview = levelSamples.firstOrNull() ?: return
        if (!levels.containsKey(preview)) {
            return
        }
        val iterator = levels.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!levelSamples.contains(entry.key)) {
                entry.value.recycle()
                iterator.remove()
            }
        }
        syncAvailable()
    }

    /**
     * Releases the patch and its spare bitmap.
     *
     * @return true when there was a patch displayed.
     */
    private fun releasePatch(): Boolean {
        val had = patchBitmap != null
        patchBitmap?.recycle()
        patchBitmap = null
        backBitmap?.recycle()
        backBitmap = null
        patchRegion.setEmpty()
        patchSample = NO_SAMPLE
        patchStale = false
        return had
    }

    /**
     * Recomputes which level is drawn and which patch is wanted for the last known state.
     *
     * @return true when the drawing state changed.
     */
    private fun evaluate(): Boolean {
        wantedSample = NO_SAMPLE
        planSample = NO_SAMPLE
        if (available.isEmpty()) {
            return selectLevel(NO_SAMPLE)
        }
        val choice = ResolutionPlanner.levelSampleToDraw(available, lastRequired)
        val chosen = abs(choice)
        val needsPatch = choice < 0
        val changed = selectLevel(chosen)
        demandLevels()
        if (!regionDecoding) {
            // a patch decoded before region decoding was disabled is dropped as soon as the
            // full resolution level is displayed, since it is not refreshed in this mode
            val had = chosen == 1 && releasePatch()
            return had || changed
        }
        if (!hasState) {
            return changed
        }
        if (!needsPatch) {
            val had = releasePatch()
            return had || changed
        }

        val w = source.storedWidth
        val h = source.storedHeight
        val used = ResolutionPlanner.patchRegion(
            lastInverse, lastViewW, lastViewH, w, h, lastRequired,
            PATCH_PIXELS_FACTOR * lastViewW.toLong() * lastViewH, planned, corners
        )
        if (planned.isEmpty) {
            return changed
        }
        if (used >= chosen) {
            val had = releasePatch()
            return had || changed
        }
        computeVisible(w, h)
        planSample = used
        planVisible.set(visible)
        val valid = patchBitmap != null && !patchStale &&
                ResolutionPlanner.isPatchValid(patchRegion, patchSample, visible, used)
        if (valid || (scaling && (patchBitmap == null || patchSample != used))) {
            return changed
        }
        if (failedSample != used || failedRegion != planned) {
            wantedSample = used
            wantedRegion.set(planned)
        }
        return changed
    }

    /**
     * Queues the planned levels that are missing and needed to display the required sample.
     */
    private fun demandLevels() {
        if (levelSamples.isEmpty()) {
            return
        }
        val target = abs(ResolutionPlanner.levelSampleToDraw(levelSamples, lastRequired))
        val running = inFlightJob
        for (sample in levelSamples) {
            val inFlight = running != null && running.kind == Kind.LEVEL && running.sample == sample &&
                    running.generation == generation
            if (sample >= target && !levels.containsKey(sample) && !pendingLevels.contains(sample) &&
                !failedLevels.contains(sample) && !inFlight
            ) {
                pendingLevels.add(sample)
            }
        }
    }

    /**
     * Computes the visible region of the image in stored pixels into [visible].
      *
      * @param storedW width of the stored image in pixels.
      * @param storedH height of the stored image in pixels.
     */
    private fun computeVisible(storedW: Int, storedH: Int) {
        val points = corners
        points[0] = 0.0f
        points[1] = 0.0f
        points[2] = lastViewW.toFloat()
        points[3] = 0.0f
        points[4] = lastViewW.toFloat()
        points[5] = lastViewH.toFloat()
        points[6] = 0.0f
        points[7] = lastViewH.toFloat()
        lastInverse.mapPoints(points)
        var minX = points[0]
        var maxX = points[0]
        var minY = points[1]
        var maxY = points[1]
        for (i in 2 until points.size step 2) {
            minX = min(minX, points[i])
            maxX = max(maxX, points[i])
            minY = min(minY, points[i + 1])
            maxY = max(maxY, points[i + 1])
        }
        visible.set(floor(minX).toInt(), floor(minY).toInt(), ceil(maxX).toInt(), ceil(maxY).toInt())
        if (!visible.intersect(0, 0, storedW, storedH)) {
            visible.setEmpty()
        }
    }

    /**
     * Starts the next job when none is in flight.
     */
    private fun pump() {
        if (released || !configured || inFlightJob != null) {
            return
        }
        val job = nextJob() ?: return
        inFlightJob = job
        try {
            executor.execute { run(job) }
        } catch (e: RejectedExecutionException) {
            inFlightJob = null
            job.reuse?.let { giveBack(it, null) }
            onFailed(e)
        }
    }

    /**
     * Creates the next job to run, in order of priority: content invalidation, detail patch, level
     * refreshes and pending levels.
     *
     * @return job to run, or null when there is nothing to do.
     */
    private fun nextJob(): Job? {
        val w = source.storedWidth
        val h = source.storedHeight
        val levelConfig = if (hardware) Bitmap.Config.HARDWARE else Bitmap.Config.ARGB_8888

        if (pendingInvalidate) {
            val region = pendingInvalidateRegion
            pendingInvalidate = false
            pendingInvalidateRegion = null
            return Job(Kind.INVALIDATE, generation, NO_SAMPLE, region?.let { Rect(it) }, levelConfig, null, hardware)
        }
        if (wantedSample != NO_SAMPLE) {
            val sample = wantedSample
            val region = Rect(wantedRegion)
            wantedSample = NO_SAMPLE
            return if (hardware) {
                Job(Kind.PATCH, generation, sample, region, Bitmap.Config.HARDWARE, null, true)
            } else {
                Job(Kind.PATCH, generation, sample, region, Bitmap.Config.ARGB_8888, takeReuse(), false)
            }
        }
        while (refreshes.isNotEmpty()) {
            val task = refreshes.removeAt(0)
            if (!levels.containsKey(task.sample)) {
                continue
            }
            val region = task.region
            return if (region == null) {
                Job(Kind.LEVEL, generation, task.sample, Rect(0, 0, w, h), levelConfig, null, hardware, true)
            } else {
                Job(
                    Kind.REFRESH, generation, task.sample, alignRegion(region, task.sample, w, h),
                    Bitmap.Config.ARGB_8888, null, false
                )
            }
        }
        if (pendingLevels.isNotEmpty()) {
            val sample = pendingLevels.removeAt(0)
            return Job(Kind.LEVEL, generation, sample, Rect(0, 0, w, h), levelConfig, null, hardware)
        }
        return null
    }

    /**
     * Aligns a region to the sample grid of a level so that its decoded pixels map exactly onto the
     * level, clamped to the stored image size.
     *
     * @param region region in stored pixels.
     * @param sample sample size of the level.
     * @param w width of the stored image in pixels.
     * @param h height of the stored image in pixels.
     * @return aligned region in stored pixels.
     */
    private fun alignRegion(region: Rect, sample: Int, w: Int, h: Int): Rect {
        val left = region.left / sample * sample
        val top = region.top / sample * sample
        val right = min((region.right + sample - 1) / sample * sample, w)
        val bottom = min((region.bottom + sample - 1) / sample * sample, h)
        return Rect(left, top, right, bottom)
    }

    /**
     * Takes the spare patch bitmap to be reused by the decoder, if it can be reused.
      *
      * @return spare bitmap to be reused by the decoder, or null when none can be reused.
     */
    private fun takeReuse(): Bitmap? {
        val bitmap = backBitmap
        backBitmap = null
        return if (bitmap != null && !bitmap.isRecycled && bitmap.isMutable &&
            bitmap.config != Bitmap.Config.HARDWARE
        ) {
            bitmap
        } else {
            bitmap?.recycle()
            null
        }
    }

    /**
     * Keeps [lent] as the spare patch bitmap unless it is the decoded [result] or a spare already exists.
      *
      * @param lent bitmap lent to the decoder.
      * @param result bitmap decoded with [lent], or null when decoding failed.
     */
    private fun giveBack(lent: Bitmap, result: Bitmap?) {
        if (lent === result || lent.isRecycled) {
            return
        }
        if (backBitmap == null && !released) {
            backBitmap = lent
        } else {
            lent.recycle()
        }
    }

    /**
     * Runs a job on the worker thread and posts its result to the UI thread.
      *
      * @param job job to run.
     */
    private fun run(job: Job) {
        var bitmap: Bitmap? = null
        var error: Throwable? = null
        try {
            when (job.kind) {
                Kind.INVALIDATE -> source.contentChanged()
                Kind.LEVEL, Kind.REFRESH, Kind.PATCH ->
                    bitmap = source.decodeRegion(job.region ?: Rect(), job.sample, job.config, job.reuse)
            }
        } catch (e: Exception) {
            error = e
        } catch (e: OutOfMemoryError) {
            error = e
        }
        val result = bitmap
        val cause = error
        post(Runnable { onResult(job, result, cause) })
    }

    /**
     * Handles the result of a job on the UI thread and then starts the next job.
     *
     * @param job job that finished.
     * @param bitmap decoded bitmap, or null when decoding failed.
     * @param error failure cause, or null when there is none.
     */
    private fun onResult(job: Job, bitmap: Bitmap?, error: Throwable?) {
        inFlightJob = null
        if (released) {
            bitmap?.recycle()
            job.reuse?.recycle()
            return
        }
        val stale = job.generation != generation
        when (job.kind) {
            Kind.INVALIDATE -> onInvalidated(job, error)
            Kind.LEVEL -> onLevel(job, bitmap, error, stale)
            Kind.REFRESH -> onRefresh(job, bitmap, error, stale)
            Kind.PATCH -> onPatch(job, bitmap, error, stale)
        }
        pump()
    }

    /**
     * Handles the result of a content change notification, scheduling refreshes of the levels and of
     * the patch affected by the change.
     *
     * @param job invalidation job that finished.
     * @param error failure cause, or null when there is none.
     */
    private fun onInvalidated(job: Job, error: Throwable?) {
        if (error != null) {
            onFailed(error)
            return
        }
        val region = job.region
        for (sample in levels.keys) {
            val bitmap = levels[sample]
            val partial = region != null && !hardware && regionDecoding && bitmap != null &&
                    bitmap.isMutable && bitmap.config != Bitmap.Config.HARDWARE
            addRefresh(sample, if (partial) region else null)
        }
        if (patchBitmap != null && (region == null || Rect.intersects(patchRegion, region))) {
            patchStale = true
        }
        notifyIfChanged(evaluate())
    }

    /**
     * Schedules a refresh of a level, merging it with any refresh already pending for that level.
     *
     * @param sample sample size of the level to refresh.
     * @param region changed region in stored pixels, or null when the whole level changed.
     */
    private fun addRefresh(sample: Int, region: Rect?) {
        val index = refreshes.indexOfFirst { it.sample == sample }
        if (index < 0) {
            refreshes.add(RefreshTask(sample, region?.let { Rect(it) }))
            return
        }
        val existing = refreshes[index].region
        if (existing == null || region == null) {
            refreshes[index] = RefreshTask(sample, null)
        } else {
            refreshes[index] = RefreshTask(sample, Rect(existing).apply { union(region) })
        }
    }

    /**
     * Handles the decoding result of a level on the UI thread.
     *
     * @param job level job that finished.
     * @param bitmap decoded level, or null when decoding failed.
     * @param error failure cause, or null when there is none.
     * @param stale true when the job was created for an older generation.
     */
    private fun onLevel(job: Job, bitmap: Bitmap?, error: Throwable?, stale: Boolean) {
        if (stale) {
            bitmap?.recycle()
            if (job.refresh && job.hardware == hardware) {
                // the content change is still pending for this level
                addRefresh(job.sample, null)
            }
            return
        }
        if (bitmap == null) {
            if (!job.refresh) {
                failedLevels.add(job.sample)
            }
            onFailed(error)
            return
        }
        val old = levels.put(job.sample, bitmap)
        syncAvailable()
        pruneLevels()
        val changed = evaluate()
        if (old != null && old !== bitmap) {
            old.recycle()
        }
        notifyIfChanged(changed)
        if (!job.refresh) {
            if (job.sample == levelSamples.firstOrNull()) {
                onPreviewLoaded()
            } else {
                onDetailLoaded()
            }
        }
    }

    /**
     * Handles the decoding result of a refreshed region, copying it over the level it belongs to.
     *
     * @param job refresh job that finished.
     * @param bitmap decoded region, or null when decoding failed.
     * @param error failure cause, or null when there is none.
     * @param stale true when the job was created for an older generation.
     */
    private fun onRefresh(job: Job, bitmap: Bitmap?, error: Throwable?, stale: Boolean) {
        val region = job.region
        val target = levels[job.sample]
        if (stale && region != null && job.hardware == hardware) {
            // the content change is still pending for this level
            addRefresh(job.sample, region)
        }
        if (stale || bitmap == null || region == null || target == null || !target.isMutable) {
            bitmap?.recycle()
            if (!stale && bitmap == null) {
                onFailed(error)
            }
            return
        }
        Canvas(target).drawBitmap(
            bitmap, (region.left / job.sample).toFloat(), (region.top / job.sample).toFloat(), copyPaint
        )
        bitmap.recycle()
        notifyIfChanged(target === levelBitmap)
    }

    /**
     * Handles the decoding result of a detail patch, replacing the displayed patch when it is still
     * useful.
     *
     * @param job patch job that finished.
     * @param bitmap decoded patch, or null when decoding failed.
     * @param error failure cause, or null when there is none.
     * @param stale true when the job was created for an older generation.
     */
    private fun onPatch(job: Job, bitmap: Bitmap?, error: Throwable?, stale: Boolean) {
        val region = job.region ?: return
        val lent = job.reuse
        if (bitmap == null) {
            if (!stale) {
                failedSample = job.sample
                failedRegion.set(region)
                onFailed(error)
            }
            lent?.let { giveBack(it, null) }
            return
        }
        val usable = !stale && planSample != NO_SAMPLE &&
                ResolutionPlanner.isPatchValid(region, job.sample, planVisible, planSample)
        if (!usable) {
            if (lent != null && lent === bitmap) {
                // the decoder reused the spare bitmap, which is kept for the next patch
                giveBack(lent, null)
            } else {
                bitmap.recycle()
                lent?.let { giveBack(it, bitmap) }
            }
            notifyIfChanged(evaluate())
            return
        }
        val old = patchBitmap
        patchBitmap = bitmap
        patchRegion.set(region)
        patchSample = job.sample
        patchStale = false
        if (job.hardware) {
            old?.recycle()
        } else {
            lent?.let { giveBack(it, bitmap) }
            if (old != null && old !== bitmap) {
                if (backBitmap == null && old.isMutable && !old.isRecycled &&
                    old.config != Bitmap.Config.HARDWARE
                ) {
                    backBitmap = old
                } else {
                    old.recycle()
                }
            }
        }
        evaluate()
        onChanged()
        onDetailLoaded()
    }

    /**
     * Kind of work done on the worker thread.
     */
    private enum class Kind { INVALIDATE, LEVEL, REFRESH, PATCH }

    /**
     * Unit of work executed on the worker thread.
     *
     * @property kind kind of work.
     * @property generation generation the job was created for.
     * @property sample sample size to decode, or [NO_SAMPLE].
     * @property region region to decode in stored pixels, or the invalidated region.
     * @property config bitmap configuration to decode.
     * @property reuse bitmap lent to the decoder, if any.
     * @property hardware true when the job was created in hardware mode.
     * @property refresh true when a level is decoded again because the content changed.
     */
    private class Job(
        val kind: Kind,
        val generation: Int,
        val sample: Int,
        val region: Rect?,
        val config: Bitmap.Config,
        val reuse: Bitmap?,
        val hardware: Boolean,
        val refresh: Boolean = false
    )

    /**
     * Level to decode again.
     *
     * @property sample sample size of the level.
     * @property region changed region in stored pixels, or null for the whole level.
     */
    private class RefreshTask(val sample: Int, val region: Rect?)

    /**
     * Constants shared by [MultiResolutionImage] instances.
     */
    companion object {
        /**
         * Patch budget as a multiple of the number of view pixels.
         */
        private const val PATCH_PIXELS_FACTOR = 2L

        /**
         * Sample size used when there is no bitmap.
         */
        private const val NO_SAMPLE = 0

        /**
         * Name of the thread owned by this class.
         */
        private const val THREAD_NAME = "MultiResolutionImage"

        /**
         * Number of values needed to store the four corners of the view.
         */
        private const val CORNER_VALUES = 8

        /**
         * Closes a source ignoring failures, since nothing can be done about them.
         *
         * @param source source to close.
         */
        fun closeQuietly(source: ImageSource) {
            try {
                source.close()
            } catch (_: Exception) {
                // nothing can be done when closing fails
            }
        }
    }
}
