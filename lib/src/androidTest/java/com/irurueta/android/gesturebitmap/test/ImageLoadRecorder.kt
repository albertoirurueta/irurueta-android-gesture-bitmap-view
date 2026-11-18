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

import com.irurueta.android.gesturebitmap.GestureBitmapView
import java.util.concurrent.TimeUnit

/**
 * Image load listener that counts the events raised by a [GestureBitmapView], so that tests can wait
 * for them from a thread other than the UI thread.
 */
class ImageLoadRecorder : GestureBitmapView.OnImageLoadListener {

    private val lock = Object()

    private var previews = 0
    private var details = 0
    private var failures = 0

    /**
     * Last failure reported, if any.
     */
    @Volatile
    var lastError: Throwable? = null
        private set

    /**
     * Number of previews loaded so far.
     */
    val previewCount: Int
        get() = synchronized(lock) { previews }

    /**
     * Number of detail loads so far.
     */
    val detailCount: Int
        get() = synchronized(lock) { details }

    /**
     * Number of failures reported so far.
     */
    val failureCount: Int
        get() = synchronized(lock) { failures }

    /**
     * Called when the preview has been decoded.
     *
     * @param view view that raised the event.
     */
    override fun onPreviewLoaded(view: GestureBitmapView) {
        synchronized(lock) {
            previews++
            lock.notifyAll()
        }
    }

    /**
     * Called when a more detailed level or patch has been decoded.
     *
     * @param view view that raised the event.
     */
    override fun onDetailLoaded(view: GestureBitmapView) {
        synchronized(lock) {
            details++
            lock.notifyAll()
        }
    }

    /**
     * Called when decoding failed.
     *
     * @param view view that raised the event.
     * @param error cause of the failure, when known.
     */
    override fun onImageLoadFailed(view: GestureBitmapView, error: Throwable?) {
        synchronized(lock) {
            failures++
            lastError = error
            lock.notifyAll()
        }
    }

    /**
     * Waits until at least [count] previews have been loaded.
     *
     * @param count expected number of previews.
     * @param timeoutMillis maximum time to wait.
     * @return true if the previews were loaded in time, false otherwise.
     */
    fun awaitPreviews(count: Int, timeoutMillis: Long = TIMEOUT_MILLIS): Boolean =
        await(timeoutMillis) { previews >= count }

    /**
     * Waits until at least [count] detail loads have been notified.
     *
     * @param count expected number of detail loads.
     * @param timeoutMillis maximum time to wait.
     * @return true if the details were loaded in time, false otherwise.
     */
    fun awaitDetails(count: Int, timeoutMillis: Long = TIMEOUT_MILLIS): Boolean =
        await(timeoutMillis) { details >= count }

    private fun await(timeoutMillis: Long, condition: () -> Boolean): Boolean {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        synchronized(lock) {
            while (!condition()) {
                val remaining = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())
                if (remaining <= 0L) {
                    return false
                }
                lock.wait(remaining)
            }
            return true
        }
    }

    companion object {
        /**
         * Default maximum time to wait for an event, in milliseconds.
         */
        const val TIMEOUT_MILLIS = 20000L
    }
}
