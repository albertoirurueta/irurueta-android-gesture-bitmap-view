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
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat
import com.irurueta.android.gesturebitmap.GestureBitmapView.DisplayType

/**
 * Saves and restores the state of a [GestureBitmapView]: its transformation matrices and its
 * configuration properties.
 */
internal object GestureBitmapViewState {

    /**
     * Saves the state of a view.
     *
     * @param view view whose configuration properties are saved.
     * @param superState state saved by the parent class of the view.
     * @param baseMatrix base matrix of the view.
     * @param paramsMatrix parameters matrix of the view.
     * @param displayMatrix display matrix of the view.
     * @return bundle containing stored view state.
     */
    fun save(
        view: GestureBitmapView,
        superState: Parcelable?,
        baseMatrix: Matrix,
        paramsMatrix: Matrix,
        displayMatrix: Matrix
    ): Bundle {
        val bundle = Bundle()
        bundle.putParcelable(SUPER_STATE_KEY, superState)

        bundle.putFloatArray(BASE_MATRIX_KEY, valuesOf(baseMatrix))
        bundle.putFloatArray(PARAMS_MATRIX_KEY, valuesOf(paramsMatrix))
        bundle.putFloatArray(DISPLAY_MATRIX_KEY, valuesOf(displayMatrix))

        bundle.putBoolean(ROTATION_ENABLED_KEY, view.rotationEnabled)
        bundle.putBoolean(SCALE_ENABLED_KEY, view.scaleEnabled)
        bundle.putBoolean(SCROLL_ENABLED_KEY, view.scrollEnabled)
        bundle.putBoolean(TWO_FINGER_SCROLL_ENABLED_KEY, view.twoFingerScrollEnabled)
        bundle.putBoolean(
            EXCLUSIVE_TWO_FINGER_SCROLL_ENABLED_KEY,
            view.exclusiveTwoFingerScrollEnabled
        )
        bundle.putBoolean(DOUBLE_TAP_ENABLED_KEY, view.doubleTapEnabled)

        bundle.putSerializable(DISPLAY_TYPE_KEY, view.displayType)

        bundle.putFloat(MIN_SCALE_KEY, view.minScale)
        bundle.putFloat(MAX_SCALE_KEY, view.maxScale)
        bundle.putFloat(SCALE_FACTOR_JUMP_KEY, view.scaleFactorJump)
        bundle.putFloat(MIN_SCALE_MARGIN_KEY, view.minScaleMargin)
        bundle.putFloat(MAX_SCALE_MARGIN_KEY, view.maxScaleMargin)
        bundle.putFloat(LEFT_SCROLL_MARGIN_KEY, view.leftScrollMargin)
        bundle.putFloat(TOP_SCROLL_MARGIN_KEY, view.topScrollMargin)
        bundle.putFloat(RIGHT_SCROLL_MARGIN_KEY, view.rightScrollMargin)
        bundle.putFloat(BOTTOM_SCROLL_MARGIN_KEY, view.bottomScrollMargin)
        bundle.putBoolean(SCROLL_TENSION_ENABLED_KEY, view.scrollTensionEnabled)
        bundle.putBoolean(ROTATION_FLING_ENABLED_KEY, view.rotationFlingEnabled)
        bundle.putFloat(SCROLL_TENSION_KEY, view.scrollTension)
        bundle.putBoolean(REGION_DECODING_ENABLED_KEY, view.regionDecodingEnabled)
        bundle.putBoolean(HARDWARE_BITMAPS_ENABLED_KEY, view.hardwareBitmapsEnabled)
        bundle.putLong(CACHED_LEVELS_MEMORY_BUDGET_KEY, view.cachedLevelsMemoryBudget)

        return bundle
    }

    /**
     * Gets the state of the parent class of a view out of a stored view state.
     *
     * @param state stored view state.
     * @return state to be restored by the parent class of the view.
     */
    fun superState(state: Parcelable?): Parcelable? {
        return if (state is Bundle) {
            BundleCompat.getParcelable(state, SUPER_STATE_KEY, Parcelable::class.java)
        } else {
            state
        }
    }

    /**
     * Restores the state of a view.
     *
     * @param view view whose configuration properties are restored.
     * @param state bundle containing stored view state.
     * @param baseMatrix base matrix of the view, which is restored.
     * @param paramsMatrix parameters matrix of the view, which is restored.
     * @param displayMatrix display matrix of the view, which is restored.
     */
    fun restore(
        view: GestureBitmapView,
        state: Bundle,
        baseMatrix: Matrix,
        paramsMatrix: Matrix,
        displayMatrix: Matrix
    ) {
        view.displayType = BundleCompat.getSerializable(
            state, DISPLAY_TYPE_KEY,
            DisplayType::class.java
        )!!

        baseMatrix.setValues(state.getFloatArray(BASE_MATRIX_KEY))
        paramsMatrix.setValues(state.getFloatArray(PARAMS_MATRIX_KEY))
        displayMatrix.setValues(state.getFloatArray(DISPLAY_MATRIX_KEY))

        view.rotationEnabled = state.getBoolean(ROTATION_ENABLED_KEY)
        view.scaleEnabled = state.getBoolean(SCALE_ENABLED_KEY)
        view.scrollEnabled = state.getBoolean(SCROLL_ENABLED_KEY)
        view.twoFingerScrollEnabled = state.getBoolean(TWO_FINGER_SCROLL_ENABLED_KEY)
        view.exclusiveTwoFingerScrollEnabled = state.getBoolean(
            EXCLUSIVE_TWO_FINGER_SCROLL_ENABLED_KEY
        )
        view.doubleTapEnabled = state.getBoolean(DOUBLE_TAP_ENABLED_KEY)

        view.minScale = state.getFloat(MIN_SCALE_KEY)
        view.maxScale = state.getFloat(MAX_SCALE_KEY)
        view.scaleFactorJump = state.getFloat(SCALE_FACTOR_JUMP_KEY)
        view.minScaleMargin = state.getFloat(MIN_SCALE_MARGIN_KEY)
        view.maxScaleMargin = state.getFloat(MAX_SCALE_MARGIN_KEY)
        view.leftScrollMargin = state.getFloat(LEFT_SCROLL_MARGIN_KEY)
        view.topScrollMargin = state.getFloat(TOP_SCROLL_MARGIN_KEY)
        view.rightScrollMargin = state.getFloat(RIGHT_SCROLL_MARGIN_KEY)
        view.bottomScrollMargin = state.getFloat(BOTTOM_SCROLL_MARGIN_KEY)
        view.scrollTensionEnabled = state.getBoolean(SCROLL_TENSION_ENABLED_KEY, true)
        view.rotationFlingEnabled = state.getBoolean(ROTATION_FLING_ENABLED_KEY, true)
        view.scrollTension = state.getFloat(
            SCROLL_TENSION_KEY,
            GestureBitmapView.DEFAULT_SCROLL_TENSION
        )
        view.regionDecodingEnabled = state.getBoolean(REGION_DECODING_ENABLED_KEY, true)
        view.hardwareBitmapsEnabled = state.getBoolean(HARDWARE_BITMAPS_ENABLED_KEY, true)
        view.cachedLevelsMemoryBudget = state.getLong(
            CACHED_LEVELS_MEMORY_BUDGET_KEY,
            GestureBitmapView.DEFAULT_CACHED_LEVELS_MEMORY_BUDGET
        )
    }

    /**
     * Gets the values of a matrix.
     *
     * @param matrix matrix to get values from.
     * @return new array containing the values of the matrix in row order.
     */
    private fun valuesOf(matrix: Matrix): FloatArray {
        val values = FloatArray(MetricTransformationParameters.MATRIX_VALUES_LENGTH)
        matrix.getValues(values)
        return values
    }

    /**
     * Key to store state of parent class.
     */
    private const val SUPER_STATE_KEY = "superState"

    /**
     * Key to store base matrix.
     */
    private const val BASE_MATRIX_KEY = "baseMatrix"

    /**
     * Key to store params matrix.
     */
    private const val PARAMS_MATRIX_KEY = "paramsMatrix"

    /**
     * Key to store display matrix.
     */
    private const val DISPLAY_MATRIX_KEY = "displayMatrix"

    /**
     * Key to store flag indicating whether rotation gesture is enabled.
     */
    private const val ROTATION_ENABLED_KEY = "rotationEnabled"

    /**
     * Key to store flag indicating whether scaling gesture is enabled.
     */
    private const val SCALE_ENABLED_KEY = "scaleEnabled"

    /**
     * Key to store flag indicating whether scroll gesture is enabled.
     */
    private const val SCROLL_ENABLED_KEY = "scrollEnabled"

    /**
     * Key to store flag indicating whether scroll gesture is allowed with two fingers
     * only.
     */
    private const val TWO_FINGER_SCROLL_ENABLED_KEY = "twoFingerScrollEnabled"

    /**
     * Key to store flag indicating whether two finger scroll is exclusively enabled.
     */
    private const val EXCLUSIVE_TWO_FINGER_SCROLL_ENABLED_KEY =
        "exclusiveTwoFingerScrollEnabled"

    /**
     * Key to store flag indicating whether tap gesture is enabled.
     */
    private const val DOUBLE_TAP_ENABLED_KEY = "doubleTapEnabled"

    /**
     * Key to store display type.
     */
    private const val DISPLAY_TYPE_KEY = "displayType"

    /**
     * Key to store minimum allowed scale.
     */
    private const val MIN_SCALE_KEY = "minScale"

    /**
     * Key to store maximum allowed scale.
     */
    private const val MAX_SCALE_KEY = "maxScale"

    /**
     * Key to store scale factor jump.
     */
    private const val SCALE_FACTOR_JUMP_KEY = "scaleFactorJump"

    /**
     * Key to store minimum scale margin.
     */
    private const val MIN_SCALE_MARGIN_KEY = "minScaleMargin"

    /**
     * Key to store maximum scale margin.
     */
    private const val MAX_SCALE_MARGIN_KEY = "maxScaleMargin"

    /**
     * Key to store left scroll margin.
     */
    private const val LEFT_SCROLL_MARGIN_KEY = "leftScrollMargin"

    /**
     * Key to store top scroll margin.
     */
    private const val TOP_SCROLL_MARGIN_KEY = "topScrollMargin"

    /**
     * Key to store right scroll margin.
     */
    private const val RIGHT_SCROLL_MARGIN_KEY = "rightScrollMargin"

    /**
     * Key to store bottom scroll margin.
     */
    private const val BOTTOM_SCROLL_MARGIN_KEY = "bottomScrollMargin"

    /**
     * Key to store whether scroll tension is enabled.
     */
    private const val SCROLL_TENSION_ENABLED_KEY = "scrollTensionEnabled"

    /**
     * Key to store whether rotation fling is enabled.
     */
    private const val ROTATION_FLING_ENABLED_KEY = "rotationFlingEnabled"

    /**
     * Key to store amount of scroll tension.
     */
    private const val SCROLL_TENSION_KEY = "scrollTension"

    /**
     * Key to store whether region decoding is enabled.
     */
    private const val REGION_DECODING_ENABLED_KEY = "regionDecodingEnabled"

    /**
     * Key to store whether hardware bitmaps are enabled.
     */
    private const val HARDWARE_BITMAPS_ENABLED_KEY = "hardwareBitmapsEnabled"

    /**
     * Key to store the memory budget of cached levels.
     */
    private const val CACHED_LEVELS_MEMORY_BUDGET_KEY = "cachedLevelsMemoryBudget"
}
