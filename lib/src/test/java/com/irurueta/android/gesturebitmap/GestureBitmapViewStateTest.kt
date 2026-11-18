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
import android.graphics.Matrix
import android.os.Bundle
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.irurueta.android.gesturebitmap.GestureBitmapView.DisplayType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GestureBitmapViewStateTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun save_storesSuperStateMatricesAndProperties() {
        val view = GestureBitmapView(context)
        val superState = View.BaseSavedState.EMPTY_STATE

        val bundle = GestureBitmapViewState.save(
            view, superState, matrix(2.0f), matrix(3.0f), matrix(4.0f)
        )

        assertSame(superState, GestureBitmapViewState.superState(bundle))
        assertEquals(2.0f, bundle.getFloatArray("baseMatrix")!![Matrix.MSCALE_X], 0.0f)
        assertEquals(3.0f, bundle.getFloatArray("paramsMatrix")!![Matrix.MSCALE_X], 0.0f)
        assertEquals(4.0f, bundle.getFloatArray("displayMatrix")!![Matrix.MSCALE_X], 0.0f)
        assertEquals(view.minScale, bundle.getFloat("minScale"), 0.0f)
    }

    @Test
    fun superState_whenStateIsNotBundle_returnsSameState() {
        val state = View.BaseSavedState.EMPTY_STATE

        assertSame(state, GestureBitmapViewState.superState(state))
    }

    @Test
    fun restore_afterSave_restoresMatricesAndProperties() {
        val saved = GestureBitmapView(context)
        saved.displayType = DisplayType.CENTER_CROP
        saved.rotationEnabled = false
        saved.scaleEnabled = false
        saved.scrollEnabled = false
        saved.twoFingerScrollEnabled = false
        saved.exclusiveTwoFingerScrollEnabled = true
        saved.doubleTapEnabled = false
        saved.minScale = 0.5f
        saved.maxScale = 20.0f
        saved.scaleFactorJump = 2.5f
        saved.leftScrollMargin = 1.0f
        saved.topScrollMargin = 2.0f
        saved.rightScrollMargin = 3.0f
        saved.bottomScrollMargin = 4.0f
        saved.scrollTensionEnabled = false
        saved.rotationFlingEnabled = false
        saved.scrollTension = 2.0f
        saved.regionDecodingEnabled = false
        saved.hardwareBitmapsEnabled = false
        saved.cachedLevelsMemoryBudget = 1024L
        val bundle = GestureBitmapViewState.save(
            saved, null, matrix(2.0f), matrix(3.0f), matrix(4.0f)
        )

        val restored = GestureBitmapView(context)
        val base = Matrix()
        val params = Matrix()
        val display = Matrix()
        GestureBitmapViewState.restore(restored, bundle, base, params, display)

        assertEquals(matrix(2.0f), base)
        assertEquals(matrix(3.0f), params)
        assertEquals(matrix(4.0f), display)
        assertEquals(DisplayType.CENTER_CROP, restored.displayType)
        assertFalse(restored.rotationEnabled)
        assertFalse(restored.scaleEnabled)
        assertFalse(restored.scrollEnabled)
        assertFalse(restored.twoFingerScrollEnabled)
        assertTrue(restored.exclusiveTwoFingerScrollEnabled)
        assertFalse(restored.doubleTapEnabled)
        assertEquals(0.5f, restored.minScale, 0.0f)
        assertEquals(20.0f, restored.maxScale, 0.0f)
        assertEquals(2.5f, restored.scaleFactorJump, 0.0f)
        assertEquals(saved.minScaleMargin, restored.minScaleMargin, 0.0f)
        assertEquals(saved.maxScaleMargin, restored.maxScaleMargin, 0.0f)
        assertEquals(1.0f, restored.leftScrollMargin, 0.0f)
        assertEquals(2.0f, restored.topScrollMargin, 0.0f)
        assertEquals(3.0f, restored.rightScrollMargin, 0.0f)
        assertEquals(4.0f, restored.bottomScrollMargin, 0.0f)
        assertFalse(restored.scrollTensionEnabled)
        assertFalse(restored.rotationFlingEnabled)
        assertEquals(2.0f, restored.scrollTension, 0.0f)
        assertFalse(restored.regionDecodingEnabled)
        assertFalse(restored.hardwareBitmapsEnabled)
        assertEquals(1024L, restored.cachedLevelsMemoryBudget)
    }

    @Test
    fun restore_whenNewerKeysAreMissing_restoresDefaultValues() {
        val saved = GestureBitmapView(context)
        val bundle = GestureBitmapViewState.save(saved, null, Matrix(), Matrix(), Matrix())
        for (key in NEWER_KEYS) {
            bundle.remove(key)
        }
        val restored = GestureBitmapView(context)
        restored.scrollTensionEnabled = false
        restored.rotationFlingEnabled = false
        restored.scrollTension = 3.0f
        restored.regionDecodingEnabled = false
        restored.hardwareBitmapsEnabled = false
        restored.cachedLevelsMemoryBudget = 1L

        GestureBitmapViewState.restore(restored, bundle, Matrix(), Matrix(), Matrix())

        assertTrue(restored.scrollTensionEnabled)
        assertTrue(restored.rotationFlingEnabled)
        assertEquals(GestureBitmapView.DEFAULT_SCROLL_TENSION, restored.scrollTension, 0.0f)
        assertTrue(restored.regionDecodingEnabled)
        assertTrue(restored.hardwareBitmapsEnabled)
        assertEquals(
            GestureBitmapView.DEFAULT_CACHED_LEVELS_MEMORY_BUDGET,
            restored.cachedLevelsMemoryBudget
        )
        assertNotNull(Bundle(bundle))
    }

    private fun matrix(scale: Float) = Matrix().apply { setScale(scale, scale) }

    private companion object {
        val NEWER_KEYS = listOf(
            "scrollTensionEnabled",
            "rotationFlingEnabled",
            "scrollTension",
            "regionDecodingEnabled",
            "hardwareBitmapsEnabled",
            "cachedLevelsMemoryBudget"
        )
    }
}
