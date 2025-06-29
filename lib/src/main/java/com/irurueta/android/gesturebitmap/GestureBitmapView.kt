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

import android.animation.Animator
import android.animation.FloatArrayEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.*
import androidx.core.content.withStyledAttributes
import androidx.core.os.BundleCompat

/**
 * View used to display a bitmap allowing gestures to change view zooming, panning and rotation.
 *
 * @param context Android context.
 * @param attrs XML layout attributes.
 * @param defStyleAttr style to be used.
 */
class GestureBitmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * Matrix initially computed for provided bitmap and current display type.
     */
    private val baseMatrix = Matrix()

    /**
     * Matrix containing changes in scale and translation that have been applied
     * respect the base matrix.
     */
    private val paramsMatrix = Matrix()

    /**
     * Accumulated matrix containing both the changes in [baseMatrix] and the changes
     * in [paramsMatrix].
     * This matrix is used to transform the original bitmaps into the image that is
     * displayed on the screen.
     */
    private val displayMatrix = Matrix()

    /**
     * Transformation parameters of a 2D metric transformation. This is reused for memory
     * efficiency.
     */
    private val parameters = MetricTransformationParameters()

    /**
     * Indicates the direction to increment or decrement scale when a double tap occurs.
     * If 1, scale will increment, if -1 it will decrement.
     */
    private var doubleTapDirection = 0

    /**
     * Handles scroll and double tap gesture detection.
     */
    private var gestureDetector: GestureDetector? = null

    /**
     * Detects scaling touch gestures.
     */
    private var scaleGestureDetector: ScaleGestureDetector? = null

    /**
     * Detects rotation touch gestures.
     */
    private var rotationGestureDetector: RotationGestureDetector? = null

    /**
     * Animator for simultaneous rotation and translation.
     */
    private var rotateAndTranslateAnimator: ValueAnimator? = null

    /**
     * Animator for scale.
     */
    private var scaleAnimator: ValueAnimator? = null

    /**
     * Animator for translation (scroll).
     */
    private var translateAnimator: ValueAnimator? = null

    /**
     * Point to be reused containing scroll variation.
     */
    private var scrollDiff = PointF()

    /**
     * Rectangle containing displayed bitmap to be reused during scroll
     * limit computation.
     */
    private var bitmapDisplayedRect = RectF()

    /**
     * Left coordinate of bitmap displayed rectangle when a touch gesture starts.
     */
    private var startRectLeft: Float = 0.0f

    /**
     * Top coordinate of bitmap displayed rectangle when a touch gesture starts.
     */
    private var startRectTop: Float = 0.0f

    /**
     * Right coordinate of bitmap displayed rectangle when a touch gesture starts.
     */
    private var startRectRight: Float = 0.0f

    /**
     * Bottom coordinate of bitmap displayed rectangle when a touch gesture starts.
     */
    private var startRectBottom: Float = 0.0f

    /**
     * Paint used for drawing bitmaps with antialiasing.
     */
    private var bitmapPaint = Paint()

    /**
     * Duration of animations to complete rotation, scale and translation expressed
     * in milliseconds.
     */
    var animationDurationMillis = DEFAULT_ANIMATION_DURATION_MILLIS

    /**
     * Listener to handle top bound reached events raised by this view.
     */
    var topBoundReachedListener: OnTopBoundReachedListener? = null

    /**
     * Listener to handle bottom bound reached events raised by this view.
     */
    var bottomBoundReachedListener: OnBottomBoundReachedListener? = null

    /**
     * Listener to handle left bound reached events raised by this view.
     */
    var leftBoundReachedListener: OnLeftBoundReachedListener? = null

    /**
     * Listener to handle right bound reached events raised by this view.
     */
    var rightBoundReachedListener: OnRightBoundReachedListener? = null

    /**
     * Listener to handle minimum scale reached events raised by this view.
     */
    var minScaleReachedListener: OnMinScaleReachedListener? = null

    /**
     * Listener to handle maximum scale reached events raised by this view.
     */
    var maxScaleReachedListener: OnMaxScaleReachedListener? = null

    /**
     * Listener to handle events raised by this view when rotate and translate animation completes.
     */
    var rotateAndTranslateAnimationCompletedListener: OnRotateAndTranslateAnimationCompletedListener? =
        null

    /**
     * Listener to handle events raised by this view when scale animation completes.
     */
    var scaleAnimationCompletedListener: OnScaleAnimationCompletedListener? = null

    /**
     * Listener to handle events raised by this view when scroll animation completes.
     */
    var scrollAnimationCompletedListener: OnScrollAnimationCompletedListener? = null

    /**
     * Listener to handle double tap events raised by this view.
     */
    var doubleTapListener: OnDoubleTapListener? = null

    /**
     * Indicates whether rotation gesture is enabled or not.
     */
    var rotationEnabled = true

    /**
     * Indicates whether scale gesture is enabled or not.
     */
    var scaleEnabled = true

    /**
     * Indicates whether scroll gesture is enabled or not.
     */
    var scrollEnabled = true

    /**
     * Indicates whether scroll with two fingers is enabled or not.
     * When both [scrollEnabled] and this property are enabled, user can scroll the
     * bitmap with two fingers.
     */
    var twoFingerScrollEnabled = true

    /**
     * Indicates whether scroll using two fingers is exclusive.
     * When this property is true, and both [scrollEnabled] and
     * [twoFingerScrollEnabled] are also true, then scroll is disabled with one
     * finger and can only be done with two fingers.
     * If false and both [scrollEnabled] and [twoFingerScrollEnabled], then scroll
     * can be made with both one or two fingers.
     */
    var exclusiveTwoFingerScrollEnabled = false

    /**
     * Indicates whether double tap is enabled or not to make a fast jump in scale.
     */
    var doubleTapEnabled = true

    /**
     * Internal bitmap.
     */
    private var _bitmap: Bitmap? = null

    /**
     * Gets or sets bitmap.
     * When setting background bitmap using this method, transformation is reset.
     */
    var bitmap: Bitmap?
        get() = _bitmap
        set(value) {
            if (value != _bitmap) {
                setBitmap(value, true)
            }
        }

    /**
     * Sets bitmap.
     * @param bitmap bitmap to be set.
     * @param resetMatrices true to reset transformation matrices, false otherwise.
     */
    fun setBitmap(bitmap: Bitmap?, resetMatrices: Boolean = true) {
        // recycle previously existing bitmap
        _bitmap?.recycle()

        if (bitmap == null || !bitmap.isRecycled) {
            _bitmap = bitmap
        }

        // set new bitmap
        if (bitmap != null && !bitmap.isRecycled) {
            if (resetMatrices) {
                computeBaseMatrix(bitmap, displayType, baseMatrix)

                // reset params matrix, update display matrix and invalidate
                resetTransformationParameters()
            } else {
                cancelAllAnimators()
                invalidate()
            }
        }
    }

    /**
     * Sets bitmap of this view using provided drawable intrinsic size.
     *
     * @param drawable drawable to be set.
     */
    fun setDrawable(drawable: Drawable?) {
        bitmap = drawable?.toBitmap()
    }

    /**
     * Gets or sets display type to use when image is initially displayed.
     * When display type is changed, display matrix is preserved (adjusting scale
     * and scroll as needed).
     */
    var displayType: DisplayType = DisplayType.FIT_X_CENTER
        set(value) {
            if (field != value) {
                field = value

                // recompute base matrix
                val bitmap = this.bitmap ?: return

                // displayMatrix follows the expression:
                // displayMatrix = paramsMatrix * baseMatrix

                // Hence, to keep displayMatrix constant
                // displayMatrix =  newParamsMatrix * newBaseMatrix
                // newParamsMatrix = displayMatrix * newBaseMatrix^-1

                // newParamsMatrix = paramsMatrix * baseMatrix * newBaseMatrix^-1

                cancelAllAnimators()

                val oldBaseMatrix = Matrix(baseMatrix)

                // recompute base matrix
                computeBaseMatrix(bitmap, value, baseMatrix)

                val invBaseMatrix = Matrix()
                if (!baseMatrix.invert(invBaseMatrix)) {
                    return
                }

                paramsMatrix.preConcat(oldBaseMatrix)
                paramsMatrix.preConcat(invBaseMatrix)

                updateDisplayMatrix()
                invalidate()
            }
        }

    /**
     * Gets or sets minimum zoom that can be applied to an image.
     * If the minimum zoom is not defined, it will be computed in terms of current
     * display type and the initial scaling.
     * @throws IllegalArgumentException if provided value is zero or negative.
     */
    var minScale = DEFAULT_MIN_SCALE
        set(value) {
            if (value <= 0.0) {
                throw IllegalArgumentException()
            }
            field = value
        }

    /**
     * Gets or sets maximum scale that can be applied ot an image.
     * If the maximum zoom is not defined, it will be computed.
     * @throws IllegalArgumentException if provided value is zero or negative.
     */
    var maxScale = DEFAULT_MAX_SCALE
        set(value) {
            if (value <= 0.0) {
                throw IllegalArgumentException()
            }
            field = value
        }

    /**
     * Jump in scale factor to be applied when doing a double tap gesture.
     * @throws IllegalArgumentException if provided value is less than 1.0f.
     */
    var scaleFactorJump = DEFAULT_SCALE_FACTOR_JUMP
        set(value) {
            if (value < 1.0f) {
                throw IllegalArgumentException()
            }
            field = value
        }

    /**
     * Margin on minimum scale so that a bounce effect happens when minimum image scale reaches the
     * limit.
     */
    var minScaleMargin = DEFAULT_MIN_SCALE_MARGIN

    /**
     * Margin on maximum scale so that a bounce effect happens when maximum image scale reaches the
     * limit.
     */
    var maxScaleMargin = DEFAULT_MAX_SCALE_MARGIN

    /**
     * Left scroll margin expressed in pixels.
     * Defines the amount of bounce when left scroll limit is reached.
     * @throws IllegalArgumentException if provided value is negative.
     */
    var leftScrollMargin = DEFAULT_SCROLL_MARGIN
        set(value) {
            if (value < 0.0) {
                throw IllegalArgumentException()
            }
            field = value
        }

    /**
     * Top scroll margin expressed in pixels.
     * Defines the amount of bounce when top scroll limit is reached.
     * @throws IllegalArgumentException if provided value is negative.
     */
    var topScrollMargin = DEFAULT_SCROLL_MARGIN
        set(value) {
            if (value < 0.0) {
                throw IllegalArgumentException()
            }
            field = value
        }

    /**
     * Right scroll margin expressed in pixels.
     * Defines the amount of bounce when right scroll limit is reached.
     * @throws IllegalArgumentException if provided value is negative.
     */
    var rightScrollMargin = DEFAULT_SCROLL_MARGIN
        set(value) {
            if (value < 0.0) {
                throw IllegalArgumentException()
            }
            field = value
        }

    /**
     * Bottom scroll margin expressed in pixels.
     * Defines the amount of bounce when bottom scroll limit is reached.
     * @throws IllegalArgumentException if provided value is negative.
     */
    var bottomScrollMargin = DEFAULT_SCROLL_MARGIN
        set(value) {
            if (value < 0.0) {
                throw IllegalArgumentException()
            }
            field = value
        }

    /**
     * Resets matrix containing changes on image (scale and translation) and
     * forces view redraw.
     */
    fun resetTransformationParameters() {
        cancelAllAnimators()
        paramsMatrix.reset()
        updateDisplayMatrix()
        invalidate()
    }

    /**
     * Gets or sets current transformation parameters used to display the image
     * respect the base transformation applied by current display type.
     * When setting transformation parameters, no limits are imposed on scale or
     * translation to ensure the bitmap remains visible within the view.
     */
    var transformationParameters: MetricTransformationParameters
        get() {
            val result = MetricTransformationParameters()
            getTransformationParameters(paramsMatrix, result)
            return result
        }
        set(value) {
            cancelAllAnimators()
            setTransformationParameters(value, paramsMatrix)
            updateDisplayMatrix()
            invalidate()
        }

    /**
     * Gets matrix containing current transformation parameters applied
     * to base matrix to display current image.
     */
    val transformationMatrix: Matrix
        get() = Matrix(paramsMatrix)

    /**
     * Gets base transformation parameters that were applied to the bitmap because of
     * current display type.
     */
    val baseTransformationParameters: MetricTransformationParameters
        get() {
            val result = MetricTransformationParameters()
            getTransformationParameters(baseMatrix, result)
            return result
        }

    /**
     * Gets matrix containing base transformation that was applied to the bitmap
     * because of current display type.
     */
    val baseTransformationMatrix: Matrix
        get() = Matrix(baseMatrix)

    /**
     * Gets overall transformation parameters that were applied to transform bitmap
     * coordinates into display coordinates. This takes into account base and current
     * transformation parameters.
     */
    val displayTransformationParameters: MetricTransformationParameters
        get() {
            val result = MetricTransformationParameters()
            getTransformationParameters(displayMatrix, result)
            return result
        }

    /**
     * Gets overall transformation matrix to transform bitmap coordinates into display
     * coordinates. This takes into account base and current transformation matrices.
     */
    val displayTransformationMatrix
        get() = Matrix(displayMatrix)

    /**
     * Gets rectangle where bitmap is displayed after applying the full display
     * transformation matrix.
     */
    val displayRect: RectF?
        get() {
            val result = RectF()
            return if (currentBitmapDisplayedRect(displayMatrix, result)) {
                result
            } else {
                null
            }
        }

    /**
     * Called when a touch event occurs on the view.
     *
     * @param event event data.
     * @return true if the event was properly handled, false otherwise.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }

        var action = event.action
        action = action and MotionEvent.ACTION_MASK
        if (action == MotionEvent.ACTION_DOWN) {
            cancelAllAnimators()
            updateStartDisplayedRectangleCoordinates()
        }

        val gestureHandled = if (twoFingerScrollEnabled || scrollEnabled || scaleEnabled) {
            gestureDetector?.onTouchEvent(event) == true
        } else {
            false
        }

        val scaleHandled = if (scaleEnabled) {
            scaleGestureDetector?.onTouchEvent(event) == true
        } else {
            false
        }

        val rotationHandled = if (rotationEnabled) {
            rotationGestureDetector?.onTouchEvent(event) == true
        } else {
            false
        }

        if (gestureHandled || scaleHandled || rotationHandled) {
            updateDisplayMatrix()
            invalidate()
        }

        if (action == MotionEvent.ACTION_UP) {
            limitScaleAndTranslation()
        }

        return true
    }

    /**
     * Called when the view must be drawn after requesting invalidation or relayout.
     *
     * @param canvas canvas where view will be drawn.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // draw bitmap
        val bitmap = this.bitmap
        if (bitmap != null && !bitmap.isRecycled) {
            val displayMatrix = displayTransformationMatrix
            canvas.drawBitmap(bitmap, displayMatrix, bitmapPaint)
        }
    }

    /**
     * Saves view state.
     *
     * @return parcelable containing stored view state.
     */
    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable(SUPER_STATE_KEY, super.onSaveInstanceState())

        val baseMatrixValues = FloatArray(MATRIX_VALUES_LENGTH)
        baseMatrix.getValues(baseMatrixValues)
        bundle.putFloatArray(BASE_MATRIX_KEY, baseMatrixValues)

        val paramsMatrixValues = FloatArray(MATRIX_VALUES_LENGTH)
        paramsMatrix.getValues(paramsMatrixValues)
        bundle.putFloatArray(PARAMS_MATRIX_KEY, paramsMatrixValues)

        val dispMatrixValues = FloatArray(MATRIX_VALUES_LENGTH)
        val displayMatrix = displayTransformationMatrix
        displayMatrix.getValues(dispMatrixValues)
        bundle.putFloatArray(DISPLAY_MATRIX_KEY, dispMatrixValues)

        bundle.putBoolean(ROTATION_ENABLED_KEY, rotationEnabled)
        bundle.putBoolean(SCALE_ENABLED_KEY, scaleEnabled)
        bundle.putBoolean(SCROLL_ENABLED_KEY, scrollEnabled)
        bundle.putBoolean(TWO_FINGER_SCROLL_ENABLED_KEY, twoFingerScrollEnabled)
        bundle.putBoolean(EXCLUSIVE_TWO_FINGER_SCROLL_ENABLED_KEY, exclusiveTwoFingerScrollEnabled)
        bundle.putBoolean(DOUBLE_TAP_ENABLED_KEY, doubleTapEnabled)

        bundle.putSerializable(DISPLAY_TYPE_KEY, displayType)

        bundle.putFloat(MIN_SCALE_KEY, minScale)
        bundle.putFloat(MAX_SCALE_KEY, maxScale)
        bundle.putFloat(SCALE_FACTOR_JUMP_KEY, scaleFactorJump)
        bundle.putFloat(MIN_SCALE_MARGIN_KEY, minScaleMargin)
        bundle.putFloat(MAX_SCALE_MARGIN_KEY, maxScaleMargin)
        bundle.putFloat(LEFT_SCROLL_MARGIN_KEY, leftScrollMargin)
        bundle.putFloat(TOP_SCROLL_MARGIN_KEY, topScrollMargin)
        bundle.putFloat(RIGHT_SCROLL_MARGIN_KEY, rightScrollMargin)
        bundle.putFloat(BOTTOM_SCROLL_MARGIN_KEY, bottomScrollMargin)

        return bundle
    }

    /**
     * Restores view state.
     *
     * @param state stored view state.
     */
    override fun onRestoreInstanceState(state: Parcelable?) {
        val viewState = if (state is Bundle) {
            BundleCompat.getParcelable<Parcelable>(state, SUPER_STATE_KEY, Parcelable::class.java)
        } else {
            state
        }
        super.onRestoreInstanceState(viewState)

        if (state is Bundle) {
            displayType = BundleCompat.getSerializable(state, DISPLAY_TYPE_KEY,
                DisplayType::class.java)!!

            val baseMatrixValues = state.getFloatArray(BASE_MATRIX_KEY)
            baseMatrix.setValues(baseMatrixValues)

            val paramsMatrixValues = state.getFloatArray(PARAMS_MATRIX_KEY)
            paramsMatrix.setValues(paramsMatrixValues)

            val displayMatrixValues = state.getFloatArray(DISPLAY_MATRIX_KEY)
            displayMatrix.setValues(displayMatrixValues)

            rotationEnabled = state.getBoolean(ROTATION_ENABLED_KEY)
            scaleEnabled = state.getBoolean(SCALE_ENABLED_KEY)
            scrollEnabled = state.getBoolean(SCROLL_ENABLED_KEY)
            twoFingerScrollEnabled = state.getBoolean(TWO_FINGER_SCROLL_ENABLED_KEY)
            exclusiveTwoFingerScrollEnabled = state.getBoolean(
                EXCLUSIVE_TWO_FINGER_SCROLL_ENABLED_KEY
            )
            doubleTapEnabled = state.getBoolean(DOUBLE_TAP_ENABLED_KEY)

            minScale = state.getFloat(MIN_SCALE_KEY)
            maxScale = state.getFloat(MAX_SCALE_KEY)
            scaleFactorJump = state.getFloat(SCALE_FACTOR_JUMP_KEY)
            minScaleMargin = state.getFloat(MIN_SCALE_MARGIN_KEY)
            maxScaleMargin = state.getFloat(MAX_SCALE_MARGIN_KEY)
            leftScrollMargin = state.getFloat(LEFT_SCROLL_MARGIN_KEY)
            topScrollMargin = state.getFloat(TOP_SCROLL_MARGIN_KEY)
            rightScrollMargin = state.getFloat(RIGHT_SCROLL_MARGIN_KEY)
            bottomScrollMargin = state.getFloat(BOTTOM_SCROLL_MARGIN_KEY)
        }

        // request redraw
        invalidate()
    }

    /**
     * Called when view size changes.
     *
     * @param w new width expressed in pixels.
     * @param h new height expressed in pixels.
     * @param oldw old width expressed in pixels.
     * @param oldh old height expressed in pixels.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && (w != oldw || h != oldh)) {
            // recompute base matrix (and display matrix) to adapt to new size
            val bitmap = this.bitmap ?: return
            cancelAllAnimators()
            computeBaseMatrix(bitmap, displayType, baseMatrix)
            updateDisplayMatrix()
            invalidate()
        }
    }

    /**
     * Updates bitmap displayed rectangle coordinates when touch gesture starts.
     */
    private fun updateStartDisplayedRectangleCoordinates() {
        currentBitmapDisplayedRect(displayMatrix, bitmapDisplayedRect)
        startRectLeft = bitmapDisplayedRect.left
        startRectTop = bitmapDisplayedRect.top
        startRectRight = bitmapDisplayedRect.right
        startRectBottom = bitmapDisplayedRect.bottom
    }

    private fun limitScaleAndTranslation() {
        getTransformationParameters(paramsMatrix, parameters)
        val currentScale = parameters.scale
        if (currentScale < minScale || currentScale > maxScale) {
            limitScale(currentScale)
        } else {
            val translateAnimator = this.translateAnimator
            val scaleAnimator = this.scaleAnimator
            if ((translateAnimator == null && scaleAnimator == null) ||
                (translateAnimator != null && !translateAnimator.isRunning &&
                        scaleAnimator != null && !scaleAnimator.isRunning)) {
                smoothScrollBy(0.0f, 0.0f)
            }
        }
    }

    /**
     * Called when a touch event finishes to ensure that scale is within proper
     * limit values.
     *
     * @param currentScale current scale used to display bitmap.
     */
    private fun limitScale(currentScale: Double) {
        if (currentScale < minScale || currentScale > maxScale) {
            val newScale = if (currentScale < minScale) minScale else maxScale
            val pivotX = (scaleGestureDetector?.focusX) ?: (width.toFloat() / 2.0f)
            val pivotY = (scaleGestureDetector?.focusY) ?: (height.toFloat() / 2.0f)
            smoothScaleTo(newScale, pivotX, pivotY)
        }
    }

    /**
     * Scrolls by provided amount.
     *
     * @param dx amount of horizontal scroll to make.
     * @param dy amount of vertical scroll to make.
     */
    private fun scrollBy(dx: Float, dy: Float) {
        cancelAllAnimators()
        scrollDiff.x = dx
        scrollDiff.y = dy
        limitScroll(
            dx,
            dy,
            leftScrollMargin,
            rightScrollMargin,
            topScrollMargin,
            bottomScrollMargin,
            scrollDiff
        )

        getTransformationParameters(paramsMatrix, parameters)
        val oldTx = parameters.horizontalTranslation
        val oldTy = parameters.verticalTranslation

        val tx = (oldTx + scrollDiff.x).toFloat()
        val ty = (oldTy + scrollDiff.y).toFloat()
        updateTranslation(tx, ty, false)
    }

    /**
     * Scrolls by provided amount using an animation.
     *
     * @param dx amount of horizontal scroll to make.
     * @param dy amount of vertical scroll to make.
     */
    private fun smoothScrollBy(
        dx: Float, dy: Float
    ) {
        scrollDiff.x = dx
        scrollDiff.y = dy
        limitScroll(
            dx,
            dy,
            0.0f,
            0.0f,
            0.0f,
            0.0f,
            scrollDiff
        )

        getTransformationParameters(paramsMatrix, parameters)
        val oldTx = parameters.horizontalTranslation
        val oldTy = parameters.verticalTranslation

        val tx = (oldTx + scrollDiff.x).toFloat()
        val ty = (oldTy + scrollDiff.y).toFloat()
        smoothTranslateTo(tx, ty)
    }

    /**
     * Limits amount of scroll to ensure that bitmap remains visible.
     *
     * @param dx amount of horizontal translation (scroll) variation.
     * @param dy amount of vertical translation (scroll) variation.
     * @param leftMargin left scroll margin expressed in pixels.
     * @param rightMargin right scroll margin expressed in pixels.
     * @param topMargin top scroll margin expressed in pixels.
     * @param bottomMargin bottom scroll margin expressed in pixels.
     * @param result point containing amount of allowed horizontal and vertical
     * scroll variation.
     */
    private fun limitScroll(
        dx: Float, dy: Float,
        leftMargin: Float, rightMargin: Float,
        topMargin: Float, bottomMargin: Float,
        result: PointF
    ) {
        currentBitmapDisplayedRect(displayMatrix, bitmapDisplayedRect)

        val oldRectLeft = bitmapDisplayedRect.left
        val oldRectTop = bitmapDisplayedRect.top
        val oldRectRight = bitmapDisplayedRect.right
        val oldRectBottom = bitmapDisplayedRect.bottom

        val newRectLeft = oldRectLeft + dx
        val newRectTop = oldRectTop + dy
        val newRectRight = oldRectRight + dx
        val newRectBottom = oldRectBottom + dy

        val paddedRight = width - rightMargin
        val paddedBottom = height - bottomMargin

        val viewCenterX = width / 2.0f
        val viewCenterY = height / 2.0f
        val oldRectCenterX = bitmapDisplayedRect.centerX()
        val oldRectCenterY = bitmapDisplayedRect.centerY()

        result.x = dx
        result.y = dy

        if (oldRectTop < topMargin && newRectTop >= topMargin) {
            // top limit has been reached
            result.y = topMargin - oldRectTop
            topBoundReachedListener?.onTopBoundReached(this)
        }
        if (oldRectBottom > paddedBottom && newRectBottom <= paddedBottom) {
            // bottom limit has been reached
            result.y = paddedBottom - oldRectBottom
            bottomBoundReachedListener?.onBottomBoundReached(this)
        }
        if (oldRectLeft < leftMargin && newRectLeft >= leftMargin) {
            // left limit has been reached
            result.x = leftMargin - oldRectLeft
            leftBoundReachedListener?.onLeftBoundReached(this)
        }
        if (oldRectRight > paddedRight && newRectRight <= paddedRight) {
            // right limit has been reached
            result.x = paddedRight - oldRectRight
            rightBoundReachedListener?.onRightBoundReached(this)
        }

        if (newRectTop >= topMargin || newRectBottom <= paddedBottom) {
            // if either top or bottom exceeds allowed margin
            if (startRectTop >= topMargin && startRectBottom <= paddedBottom) {
                // if bitmap fits vertically within the view, then snap to vertical center
                val dCenter = viewCenterY - oldRectCenterY
                val minDCenter = dCenter - bottomMargin
                val maxDCenter = dCenter + topMargin
                result.y = max(min(dy, maxDCenter), minDCenter)
            } else {
                // otherwise, snap to closest destination point to the margin (either top or bottom)
                val dTop = topMargin - newRectTop
                val dBottom = paddedBottom - newRectBottom
                result.y = if (abs(dTop) < abs(dBottom)) {
                    topMargin - oldRectTop
                } else {
                    paddedBottom - oldRectBottom
                }
            }
        }
        if (newRectLeft >= leftMargin || newRectRight <= paddedRight) {
            // if either left or right exceeds allowed margin
            if (startRectLeft >= leftMargin && startRectBottom <= paddedRight) {
                // if bitmap fits horizontally within the view, then snap to horizontal center
                val dCenter = viewCenterX - oldRectCenterX
                val minDCenter = dCenter - leftMargin
                val maxDCenter = dCenter + rightMargin
                result.x = max(min(dx, maxDCenter), minDCenter)
            } else {
                // otherwise, snap to closest destination point to the margin (either left or right)
                val dLeft = leftMargin - newRectLeft
                val dRight = paddedRight - newRectRight
                result.x = if (abs(dLeft) < abs(dRight)) {
                    leftMargin - oldRectLeft
                } else {
                    paddedRight - oldRectRight
                }
            }
        }
    }

    /**
     * Computes the coordinates of the region where the bitmap is displayed after applying full
     * display transformation (base + parameters).
     *
     * @param displayMatrix display matrix to be used.
     * @param result instance where computed rectangle data will be stored.
     * @return true if rectangle could be computed, false otherwise (if no bitmap has been set yet).
     */
    private fun currentBitmapDisplayedRect(displayMatrix: Matrix, result: RectF): Boolean {
        val bitmap = this.bitmap ?: return false

        result.set(0.0f, 0.0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        displayMatrix.mapRect(result)
        return true
    }

    /**
     * Computes display matrix for provided base and parameters matrices.
     *
     * @param baseMatrix base matrix containing transformation applied because of display type.
     * @param paramsMatrix matrix applying additional scale, rotation and translations.
     * @param result instance where result will be stored.
     */
    private fun getDisplayMatrix(baseMatrix: Matrix, paramsMatrix: Matrix, result: Matrix) {
        //We want to get displayMatrix = paramsMatrix * baseMatrix

        // first make result the identity
        result.reset()

        // displayMatrix = I * paramsMatrix
        result.preConcat(paramsMatrix)

        // displayMatrix = I * paramsMatrix * baseMatrix
        result.preConcat(baseMatrix)
    }

    /**
     * Recomputes display matrix
     */
    private fun updateDisplayMatrix() {
        getDisplayMatrix(baseMatrix, paramsMatrix, displayMatrix)
    }

    /**
     * Updates parameters matrix with new translation values.
     * This method does NOT check that new translation values are within allowed range of values
     * so that bitmap always remains visible on the view.
     *
     * @param tx x coordinate of translation to be set.
     * @param ty y coordinate of translation to be set..
     * @param invalidate if true, view is invalidated.
     */
    private fun updateTranslation(
        tx: Float,
        ty: Float,
        invalidate: Boolean
    ) {
        getTransformationParameters(paramsMatrix, parameters)

        parameters.horizontalTranslation = tx.toDouble()
        parameters.verticalTranslation = ty.toDouble()

        setTransformationParameters(parameters, paramsMatrix)
        if (invalidate) {
            updateDisplayMatrix()
            invalidate()
        }
    }

    /**
     * Updates parameters matrix with new scale at provided pivot point.
     * This method does NOT check that new scale is within allowed range of values [minScale] and
     * [maxScale].
     *
     * @param scale new scale to be set
     * @param pivotX horizontal coordinate of pivot point expressed in new scale
     * terms.
     * @param pivotY vertical coordinate of pivot point expressed in new scale terms
     * @param invalidate if true, view is invalidated.
     */
    private fun updateScale(
        scale: Double,
        pivotX: Float,
        pivotY: Float,
        invalidate: Boolean
    ) {
        /*

        We have an initial parameters metric transformation matrix

        M = [oldS * R       oldT]
            [0                 1]

        So that old point coordinates respect to base bitmap coordinates are:
        [oldX] = M * [baseX]
        [oldY]       [baseY]
        [1   ]       [baseZ]

        Where oldS is previous scale, R is current rotation, oldT = (oldTx, oldTy)^T are previous
        translation coordinates, 0 is a 1x2 zero vector and 1 is a scalar

        We move pivot position back to the origin by
        pre-multiplying by:

        [I      -p]
        [0       1]

        where p are the pivot coordinates (px, py) and I is the 2x2 identity

        apply new scale

        [s / oldS * I      0]
        [0                 1]

        // and reset back pivot position

        [I      p]
        [0      1]

        Hence the expression of new homogeneous coordinates (newX, newY, 1) expressed in terms
        of previous coordinates is (taking into account that each step involves a
        pre-multiplication)

        [newX] = [I     p][s / oldS * I     0][I    -p][oldX]
        [newY]   [0     1][0                1][0     1][oldY]
        [1   ]                                         [1   ]

        // multiplying former 2 matrices

        [newX] = [s / oldS * I     p][I    -p][oldX]
        [newY]   [0                1][0     1][oldY]
        [1   ]                                [1   ]

        // and finally

        [newX] = [s / oldS * I     -s / oldS * p + p][oldX]
        [newY]   [0                                1][oldY]
        [1   ]                                       [1   ]


        Notice that the expression above has no effect on pivot point. In other words:
        // oldX = px --> newX = px
        // oldY = py --> newY = py

        Because the expression above needs to be represented respect to base coordinates (when
        bitmap is initialized with base matrix to fit properly)

        Then:
        [newX] = [s / oldS * I     -s / oldS * p + p][oldS * R       oldT][baseX]
        [newY]   [0                                1][0                 1][baseY]
        [1   ]                                                            [1    ]

        [newX] = [s * R         s / oldS * oldT - s / oldS * p + p][baseX]
        [newY]   [0                                              1][baseY]
        [1   ]                                                     [1    ]


        Hence the new parameters matrix is

        newM = [s * R         s / oldS * oldT - s / oldS * p + p]
               [0                                              1]

        where rotation R is preserved, the new scale is s, and the only term that has changed is
        translation t which is equal to:
        t = s / oldS * (oldT - p) + p

        NOTE: rotation R follows expression
        R = [cos(theta)     -sin(theta)]
            [sin(theta)      cos(theta)]

        where theta is the rotation angle.
        */

        getTransformationParameters(paramsMatrix, parameters)
        val oldScale = parameters.scale
        val oldHorizontalTranslation = parameters.horizontalTranslation
        val oldVerticalTranslation = parameters.verticalTranslation

        // set new parameters
        val scaleDiff = scale / oldScale
        val horizontalTranslation = scaleDiff * (oldHorizontalTranslation - pivotX) + pivotX
        val verticalTranslation = scaleDiff * (oldVerticalTranslation - pivotY) + pivotY
        parameters.scale = scale
        parameters.horizontalTranslation = horizontalTranslation
        parameters.verticalTranslation = verticalTranslation

        setTransformationParameters(parameters, paramsMatrix)
        if (invalidate) {
            updateDisplayMatrix()
            invalidate()
        }
    }

    /**
     * Updates parameters matrix with new rotation angle at provided pivot point.
     *
     * @param rotationAngle new rotation angle to be set.
     * @param pivotX horizontal coordinate of pivot point.
     * @param pivotY vertical coordinate of pivot point.
     * @param invalidate if true, view is invalidated.
     */
    private fun updateRotation(
        rotationAngle: Float,
        pivotX: Float,
        pivotY: Float,
        invalidate: Boolean
    ) {
        /*

        We have an initial parameters metric transformation matrix

        M = [s * oldR       t]
            [0              1]

        So that old point coordinates respect to base bitmap coordinates are:
        [oldX] = M * [baseX]
        [oldY]       [baseY]
        [1   ]       [baseZ]

        Where s scale, oldR is previous rotation, t = (tx, ty)^T are translation
        coordinates, 0 is a 1x2 zero vector and 1 is a scalar

        We move pivot position back to the origin by
        pre-multiplying by:

        [I      -p]
        [0       1]

        where p are the pivot coordinates (px, py) and I is the 2x2 identity

        apply rotation change:
        diffR = R*oldR^-1 = R*oldR^T

        where oldR^-1 is the inverse of previous rotation, which is equivalent to its transpose.
        diffR is equivalent to applying a rotation with the difference of rotation angles, as
        follows:

        diffTheta = theta - oldTheta

        Hence,

        [diffR      0]
        [0          1]

        // and then reset back pivot position

        [I      p]
        [0      1]

        Hence the expression of new homogeneous coordinates (newX, newY, 1) expressed in terms
        of previous coordinates is (taking into account that each step involves a
        pre-multiplication)

        [newX] = [I     p][diffR     0][I    -p][oldX]
        [newY]   [0     1][0         1][0     1][oldY]
        [1   ]                                  [1   ]

        // multiplying former 2 matrices

        [newX] = [diffR     p][I    -p][oldX]
        [newY]   [0         1][0     1][oldY]
        [1   ]                         [1   ]

        // and finally

        [newX] = [diffR     -diffR * p + p][oldX]
        [newY]   [0                      1][oldY]
        [1   ]                             [1   ]


        Notice that the expression above has no effect on pivot point. In other words:
        // oldX = px --> newX = px
        // oldY = py --> newY = py

        Because the expression above needs to be represented respect to base coordinates (when
        bitmap is initialized with base matrix to fit properly)

        Then:
        [newX] = [diffR     -diffR * p + p][s * oldR       t][baseX]
        [newY]   [0                      1][0              1][baseY]
        [1   ]                                               [1    ]


        [newX] = [s * diffR * oldR     diffR * (t - p) + p][baseX]
        [newY]   [0                                      1][baseY]
        [1   ]                                             [1    ]

        taking into account that diffR = R*oldR^-1, then

        [newX] = [s * R * oldR^-1 * oldR     diffR * (t - p) + p][baseX]
        [newY]   [0                                            1][baseY]
        [1   ]                                                   [1    ]

        [newX] = [s * R     diffR * (t - p) + p][baseX]
        [newY]   [0                           1][baseY]
        [1   ]                                  [1    ]



        Hence the new transformation matrix is

        newM = [s * R     diffR * (t - p) + p]
               [0                           1]

        where new rotation R is set, scale s is preserved, and the only term that has changed is
        translation t which depends on amount of rotation change diffR and pivot point p as follows:

        t = diffR * (t - p) + p

        NOTE: rotation R follows expression
        R = [cos(theta)     -sin(theta)]
            [sin(theta)      cos(theta)]

        where theta is the new rotation angle.

        and rotation change diffR follows expression:
        R = [cos(theta - oldTheta)     -sin(theta - oldTheta)]
            [sin(theta - oldTheta)      cos(theta - oldTheta)]

        which depends on the amount of rotation change

        Hence, the new translation t is:
        newT = [newTx] = [cos(theta - oldTheta)     -sin(theta - oldTheta)][tx - px] + px
               [newTy]   [sin(theta - oldTheta)      cos(theta - oldTheta)][ty - py] + py

        newTx = cos(theta - oldTheta) * (tx - px) - sin(theta - oldTheta) * (ty - py) + px
        newTy = sin(theta - oldTheta) * (tx - px) + cos(theta - oldTheta) * (ty - py) + py
        */

        getTransformationParameters(paramsMatrix, parameters)
        val oldRotationAngle = parameters.rotationAngle
        val oldHorizontalTranslation = parameters.horizontalTranslation
        val oldVerticalTranslation = parameters.verticalTranslation

        // set new parameters
        val diffTheta = rotationAngle - oldRotationAngle
        val cosDiffTheta = cos(diffTheta)
        val sinDiffTheta = sin(diffTheta)
        val diffTx = oldHorizontalTranslation - pivotX
        val diffTy = oldVerticalTranslation - pivotY

        parameters.rotationAngle = rotationAngle.toDouble()
        parameters.horizontalTranslation = cosDiffTheta * diffTx - sinDiffTheta * diffTy + pivotX
        parameters.verticalTranslation = sinDiffTheta * diffTx + cosDiffTheta * diffTy + pivotY

        setTransformationParameters(parameters, paramsMatrix)
        if (invalidate) {
            updateDisplayMatrix()
            invalidate()
        }
    }

    /**
     * Updates parameters matrix with new rotation angle at provided pivot point and
     * provided new translation values.
     *
     * @param rotationAngle new rotation angle to be set.
     * @param pivotX horizontal coordinate of pivot point.
     * @param pivotY vertical coordinate of pivot point.
     * @param tx x coordinate of translation to be set.
     * @param ty y coordinate of translation to be set.
     */
    private fun updateRotationAndTranslation(
        rotationAngle: Float,
        pivotX: Float,
        pivotY: Float,
        tx: Float,
        ty: Float,
    ) {
        updateRotation(rotationAngle, pivotX, pivotY, invalidate = true)
        updateTranslation(tx, ty, invalidate = true)
    }

    /**
     * Cancels translate animator if it is running
     */
    private fun cancelTranslateAnimator() {
        val translateAnimator = this.translateAnimator
        if (translateAnimator != null && translateAnimator.isRunning) {
            translateAnimator.cancel()
        }
    }

    /**
     * Translates up to provided position using an animation.
     *
     * @param tx horizontal coordinate of translation to animate to.
     * @param ty vertical coordinate of translation to animate to.
     */
    private fun smoothTranslateTo(tx: Float, ty: Float) {
        cancelAllAnimators()

        getTransformationParameters(paramsMatrix, parameters)

        val initTx = parameters.horizontalTranslation.toFloat()
        val initTy = parameters.verticalTranslation.toFloat()
        val initT = floatArrayOf(initTx, initTy)
        val endT = floatArrayOf(tx, ty)

        val translateAnimator = ValueAnimator.ofObject(
            FloatArrayEvaluator(),
            initT, endT
        )
        this.translateAnimator = translateAnimator

        translateAnimator.setTarget(this)
        translateAnimator.addUpdateListener { animator ->
            val t = animator.animatedValue as FloatArray
            updateTranslation(t[0], t[1], invalidate = true)
        }
        translateAnimator.duration = animationDurationMillis
        translateAnimator.interpolator = DecelerateInterpolator()

        translateAnimator.addListener(object : Animator.AnimatorListener {
            var cancelled = false

            override fun onAnimationEnd(animation: Animator) {
                if (!cancelled) {
                    updateTranslation(tx, ty, invalidate = true)
                }

                scrollAnimationCompletedListener?.onScrollAnimationCompleted(
                    this@GestureBitmapView
                )
            }

            override fun onAnimationRepeat(animation: Animator) {
                // not used
            }

            override fun onAnimationCancel(animation: Animator) {
                cancelled = true
            }

            override fun onAnimationStart(animation: Animator) {
                // not used
            }
        })

        translateAnimator.start()
    }

    /**
     * Cancels scale animator if it is running.
     */
    private fun cancelScaleAnimator() {
        val scaleAnimator = this.scaleAnimator
        if (scaleAnimator != null && scaleAnimator.isRunning) {
            scaleAnimator.cancel()
        }
    }

    /**
     * Scales up to provided scale using an animation and provided pivot point.
     * If no pivot point coordinates are provided, then view center is used.
     *
     * @param scale scale to animate to.
     * @param pivotX horizontal coordinate of pivot point.
     * @param pivotY vertical coordinate of pivot point.
     */
    private fun smoothScaleTo(scale: Float, pivotX: Float, pivotY: Float) {
        cancelAllAnimators()

        getTransformationParameters(paramsMatrix, parameters)
        val initialScale = parameters.scale.toFloat()

        val scaleAnimator = ValueAnimator.ofFloat(initialScale, scale)
        this.scaleAnimator = scaleAnimator

        scaleAnimator.setTarget(this)
        scaleAnimator.addUpdateListener { animator ->
            val s = animator.animatedValue as Float
            updateScale(s.toDouble(), pivotX, pivotY, invalidate = true)
        }
        scaleAnimator.duration = animationDurationMillis
        scaleAnimator.interpolator = AccelerateInterpolator()

        scaleAnimator.addListener(object : Animator.AnimatorListener {
            var cancelled = false

            override fun onAnimationEnd(animation: Animator) {
                if (!cancelled) {
                    updateScale(
                        scale.toDouble(), pivotX, pivotY,
                        invalidate = true
                    )
                }

                getTransformationParameters(paramsMatrix, parameters)
                val currentScale = parameters.scale

                if (currentScale <= minScale) {
                    minScaleReachedListener?.onMinScaleReached(
                        this@GestureBitmapView
                    )
                }
                if (currentScale >= maxScale) {
                    maxScaleReachedListener?.onMaxScaleReached(
                        this@GestureBitmapView
                    )
                }
                scaleAnimationCompletedListener?.onScaleAnimationCompleted(
                    this@GestureBitmapView
                )

                // ensure that view returns to initial translation and rotation at
                // minimum scale
                if (currentScale <= minScale) {
                    smoothRotateAndTranslateTo(0.0f, width / 2.0f, height / 2.0f, 0.0f, 0.0f)
                }
            }

            override fun onAnimationRepeat(animation: Animator) {
                // not used
            }

            override fun onAnimationCancel(animation: Animator) {
                cancelled = true
            }

            override fun onAnimationStart(animation: Animator) {
                // not used
            }
        })

        scaleAnimator.start()
    }

    /**
     * Cancels rotate and translate animator if it is running.
     */
    private fun cancelRotateAndTranslateAnimator() {
        val rotateAndTranslateAnimator = this.rotateAndTranslateAnimator
        if (rotateAndTranslateAnimator != null && rotateAndTranslateAnimator.isRunning) {
            rotateAndTranslateAnimator.cancel()
        }
    }

    /**
     * Rotates and translates up to provided values using provided pivot point for
     * rotation.
     *
     * @param rotationAngle new rotation angle to be set.
     * @param pivotX horizontal coordinate of pivot point.
     * @param pivotY vertical coordinate of pivot point.
     * @param tx x coordinate of translation to be set.
     * @param ty y coordinate of translation to be set.
     */
    @Suppress("SameParameterValue")
    private fun smoothRotateAndTranslateTo(
        rotationAngle: Float,
        pivotX: Float,
        pivotY: Float,
        tx: Float,
        ty: Float
    ) {
        cancelAllAnimators()

        getTransformationParameters(paramsMatrix, parameters)

        val initTheta = parameters.rotationAngle.toFloat()
        val initTx = parameters.horizontalTranslation.toFloat()
        val initTy = parameters.verticalTranslation.toFloat()
        val initValues = floatArrayOf(initTheta, initTx, initTy)
        val endValues = floatArrayOf(rotationAngle, tx, ty)

        val rotateAndTranslateAnimator = ValueAnimator.ofObject(
            FloatArrayEvaluator(),
            initValues, endValues
        )
        this.rotateAndTranslateAnimator = rotateAndTranslateAnimator

        rotateAndTranslateAnimator.setTarget(this)
        rotateAndTranslateAnimator.addUpdateListener { animator ->
            val value = animator.animatedValue as FloatArray
            updateRotationAndTranslation(value[0], pivotX, pivotY, value[1], value[2])
        }
        rotateAndTranslateAnimator.duration = animationDurationMillis
        rotateAndTranslateAnimator.interpolator = DecelerateInterpolator()

        rotateAndTranslateAnimator.addListener(object : Animator.AnimatorListener {
            var cancelled = false

            override fun onAnimationEnd(animation: Animator) {
                if (!cancelled) {
                    updateRotationAndTranslation(rotationAngle, pivotX, pivotY, tx, ty)
                }

                rotateAndTranslateAnimationCompletedListener?.onRotateAndTranslateAnimationCompleted(
                    this@GestureBitmapView
                )
            }

            override fun onAnimationRepeat(animation: Animator) {
                // not used
            }


            override fun onAnimationCancel(animation: Animator) {
                cancelled = true
            }

            override fun onAnimationStart(animation: Animator) {
                // not used
            }

        })

        rotateAndTranslateAnimator.start()
    }

    /**
     * Cancels all animators if any of them is running.
     */
    private fun cancelAllAnimators() {
        cancelTranslateAnimator()
        cancelScaleAnimator()
        cancelRotateAndTranslateAnimator()
    }

    /**
     * Gets parameters for a 2D metric transformation contained in provided matrix.
     * Provided matrix is assumed to be a 3x3 matrix containing a
     * a metric transformation (equal horizontal and vertical scale,
     * translation and rotation).
     * Such kind of matrix has the form:
     * [s*R t]
     * [0   1]
     * Where:
     * - t  is the translation vector t = [tx, ty] containing
     *  horizontal and vertical translation coordinates.
     * - 0 is a 1x2 zero vector.
     * - 1 is a scalar value.
     * - s is the scale factor.
     * - and R is a 2x2 rotation matrix following the expression:
     * R =  [cos(theta)  -sin(theta)]
     *      [sin(theta) cos(theta)  ]
     * and theta is the rotation angle expressed in radians
     *
     * Notice that android.graphics.Matrix stores elements in row order.
     *
     * @param matrix matrix to extract parameters from.
     * @param result instance where extracted parameters will be stored.
     */
    private fun getTransformationParameters(
        matrix: Matrix,
        result: MetricTransformationParameters
    ) {
        result.fromMatrix(matrix)
    }

    /**
     * Sets provided parameters for a 2D metric transformation into provided matrix.
     * Provided matrix is assumed to be a 3x3 matrix that will contain a
     * a metric transformation (equal horizontal and vertical scale,
     * translation and rotation).
     * Such kind of matrix has the form:
     * [s*R t]
     * [0   1]
     * Where:
     * - t  is the translation vector t = [tx, ty] containing
     *  horizontal and vertical translation coordinates.
     * - 0 is a 1x2 zero vector.
     * - 1 is a scalar value.
     * - s is the scale factor.
     * - and R is a 2x2 rotation matrix following the expression:
     * R =  [cos(theta)  -sin(theta)]
     *      [sin(theta) cos(theta)  ]
     * and theta is the rotation angle expressed in radians
     *
     * Notice that android.graphics.Matrix stores elements in row order.
     *
     * @param params instance containing parameters to be set.
     * @param result matrix where parameters will be stored.
     */
    private fun setTransformationParameters(
        params: MetricTransformationParameters,
        result: Matrix
    ) {
        params.toMatrix(result)
    }

    /**
     * Computes base matrix for provided bitmap and display type.
     */
    private fun computeBaseMatrix(bitmap: Bitmap, displayType: DisplayType, result: Matrix) {
        val viewWidth = if (this.width > 0) this.width.toFloat() else this.measuredWidth.toFloat()
        val viewHeight =
            if (this.height > 0) this.height.toFloat() else this.measuredHeight.toFloat()

        val bitmapWidth = bitmap.width.toFloat()
        val bitmapHeight = bitmap.height.toFloat()

        val bitmapRect = RectF(0.0f, 0.0f, bitmapWidth, bitmapHeight)

        when (displayType) {
            DisplayType.NONE -> {
                // returns identity matrix
                result.reset()
            }
            DisplayType.FIT_IF_BIGGER -> {
                if (bitmapWidth > viewWidth || bitmapHeight > viewHeight) {
                    val scaleWidth = viewWidth / bitmapWidth
                    val scaleHeight = viewHeight / bitmapHeight

                    val scale = min(scaleWidth, scaleHeight)
                    val newWidth = scale * bitmapWidth
                    val newHeight = scale * bitmapHeight

                    val halfWidthDiff = (viewWidth - newWidth) / 2.0f
                    val halfHeightDiff = (viewHeight - newHeight) / 2.0f

                    val dstRect = RectF(
                        halfWidthDiff, halfHeightDiff,
                        newWidth + halfWidthDiff, newHeight + halfHeightDiff
                    )

                    result.setRectToRect(bitmapRect, dstRect, Matrix.ScaleToFit.CENTER)
                } else {
                    // image fits within the view
                    result.reset()
                }
            }
            DisplayType.FIT_X_TOP -> {
                val scale = viewWidth / bitmapWidth
                val newHeight = scale * bitmapHeight
                val dstRect = RectF(0.0f, 0.0f, viewWidth, newHeight)

                result.setRectToRect(bitmapRect, dstRect, Matrix.ScaleToFit.CENTER)
            }
            DisplayType.FIT_X_BOTTOM -> {
                val scale = viewWidth / bitmapWidth
                val newHeight = scale * bitmapHeight
                val heightDiff = viewHeight - newHeight
                val dstRect = RectF(0.0f, heightDiff, viewWidth, newHeight + heightDiff)

                result.setRectToRect(bitmapRect, dstRect, Matrix.ScaleToFit.CENTER)
            }
            DisplayType.FIT_X_CENTER -> {
                val scale = viewWidth / bitmapWidth
                val newHeight = scale * bitmapHeight
                val halfHeightDiff = (viewHeight - newHeight) / 2.0f
                val dstRect = RectF(0.0f, halfHeightDiff, viewWidth, newHeight + halfHeightDiff)

                result.setRectToRect(bitmapRect, dstRect, Matrix.ScaleToFit.CENTER)
            }
            DisplayType.FIT_Y_LEFT -> {
                val scale = viewHeight / bitmapHeight
                val newWidth = scale * bitmapWidth
                val dstRect = RectF(0.0f, 0.0f, newWidth, viewHeight)

                result.setRectToRect(bitmapRect, dstRect, Matrix.ScaleToFit.CENTER)
            }
            DisplayType.FIT_Y_RIGHT -> {
                val scale = viewHeight / bitmapHeight
                val newWidth = scale * bitmapWidth
                val widthDiff = (viewWidth - newWidth)
                val dstRect = RectF(widthDiff, 0.0f, newWidth + widthDiff, viewHeight)

                result.setRectToRect(bitmapRect, dstRect, Matrix.ScaleToFit.CENTER)
            }
            DisplayType.FIT_Y_CENTER -> {
                val scale = viewHeight / bitmapHeight
                val newWidth = scale * bitmapWidth
                val halfWidthDiff = (viewWidth - newWidth) / 2.0f
                val dstRect = RectF(halfWidthDiff, 0.0f, newWidth + halfWidthDiff, viewHeight)

                result.setRectToRect(bitmapRect, dstRect, Matrix.ScaleToFit.CENTER)
            }
            DisplayType.CENTER_CROP -> {
                val scaleWidth = viewWidth / bitmapWidth
                val scaleHeight = viewHeight / bitmapHeight

                val scale = max(scaleWidth, scaleHeight)
                val newWidth = scale * bitmapWidth
                val newHeight = scale * bitmapHeight

                val halfWidthDiff = (viewWidth - newWidth) / 2.0f
                val halfHeightDiff = (viewHeight - newHeight) / 2.0f

                val dstRect = RectF(
                    halfWidthDiff, halfHeightDiff,
                    newWidth + halfWidthDiff, newHeight + halfHeightDiff
                )

                result.setRectToRect(bitmapRect, dstRect, Matrix.ScaleToFit.CENTER)
            }
        }
    }

    /**
     * Computes new scale to display when a double tap is detected.
     *
     * @return new scale.
     */
    private fun newScaleOnDoubleTap(): Float {
        getTransformationParameters(paramsMatrix, parameters)
        val scale = parameters.scale

        return if (doubleTapDirection == 1) {
            // scale must be increased
            val result = min((scale + scaleFactorJump).toFloat(), maxScale)

            if (result == maxScale) {
                // maximum scale has been reached
                doubleTapDirection = -1
            }

            result
        } else {
            // return to original scale
            doubleTapDirection = 1
            1.0f
        }
    }

    /**
     * Initializes view.
     * @param context Android context.
     * @param attrs XML layout attributes.
     * @param defStyleAttr default style to use.
     */
    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        if (isInEditMode) {
            return
        }

        context.withStyledAttributes(attrs, R.styleable.GestureBitmapView, defStyleAttr, 0) {

            val drawable = getDrawable(R.styleable.GestureBitmapView_src)
            setDrawable(drawable)

            val displayTypeIndex = getInt(
                R.styleable.GestureBitmapView_displayType,
                displayType.ordinal
            )
            val displayType = DisplayType.entries[displayTypeIndex]
            this@GestureBitmapView.displayType = displayType

            animationDurationMillis = getInt(
                R.styleable.GestureBitmapView_animationDurationMillis,
                animationDurationMillis.toInt()
            ).toLong()

            rotationEnabled =
                getBoolean(R.styleable.GestureBitmapView_rotationEnabled, rotationEnabled)

            scaleEnabled = getBoolean(R.styleable.GestureBitmapView_scaleEnabled, scaleEnabled)

            scrollEnabled = getBoolean(R.styleable.GestureBitmapView_scrollEnabled, scrollEnabled)

            twoFingerScrollEnabled = getBoolean(
                R.styleable.GestureBitmapView_twoFingerScrollEnabled,
                twoFingerScrollEnabled
            )

            exclusiveTwoFingerScrollEnabled = getBoolean(
                R.styleable.GestureBitmapView_exclusiveTwoFingerScrollEnabled,
                exclusiveTwoFingerScrollEnabled
            )

            doubleTapEnabled =
                getBoolean(R.styleable.GestureBitmapView_doubleTapEnabled, doubleTapEnabled)

            minScale = getFloat(R.styleable.GestureBitmapView_minScale, minScale)

            maxScale = getFloat(R.styleable.GestureBitmapView_maxScale, maxScale)

            scaleFactorJump =
                getFloat(R.styleable.GestureBitmapView_scaleFactorJump, scaleFactorJump)

            minScaleMargin = getFloat(R.styleable.GestureBitmapView_minScaleMargin, minScaleMargin)
            maxScaleMargin = getFloat(R.styleable.GestureBitmapView_maxScaleMargin, maxScaleMargin)

            leftScrollMargin =
                getFloat(R.styleable.GestureBitmapView_leftScrollMargin, leftScrollMargin)
            topScrollMargin =
                getFloat(R.styleable.GestureBitmapView_topScrollMargin, topScrollMargin)
            rightScrollMargin =
                getFloat(R.styleable.GestureBitmapView_rightScrollMargin, rightScrollMargin)
            bottomScrollMargin =
                getFloat(R.styleable.GestureBitmapView_bottomScrollMargin, bottomScrollMargin)

        }
    }

    init {
        bitmapPaint.isAntiAlias = true

        doubleTapDirection = 1

        rotationGestureDetector = RotationGestureDetector(
            object : RotationGestureDetector.OnRotationGestureListener {
                override fun onRotation(
                    gestureDetector: RotationGestureDetector,
                    angle: Float,
                    pivotX: Float,
                    pivotY: Float
                ): Boolean {
                    cancelAllAnimators()
                    getTransformationParameters(paramsMatrix, parameters)
                    val oldRotation = parameters.rotationAngle
                    val newRotation = (oldRotation + angle).toFloat()

                    updateRotation(
                        newRotation, pivotX, pivotY,
                        invalidate = false
                    )

                    return true
                }
            })

        scaleGestureDetector =
            ScaleGestureDetector(
                context,
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        val span = detector.currentSpan - detector.previousSpan
                        if (span == 0.0f) {
                            return false
                        }

                        cancelAllAnimators()

                        val scale = parameters.scale

                        val scaleFactor = detector.scaleFactor
                        val focusX = detector.focusX
                        val focusY = detector.focusY
                        var targetScale = (scale * scaleFactor)

                        targetScale =
                            min(
                                maxScale.toDouble() + maxScaleMargin,
                                max(targetScale, minScale.toDouble() - minScaleMargin)
                            )
                        updateScale(
                            targetScale, focusX, focusY,
                            invalidate = false
                        )
                        doubleTapDirection = 1

                        return true
                    }

                })

        gestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    performClick()
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (doubleTapEnabled) {
                        var targetScale = newScaleOnDoubleTap()
                        targetScale = min(maxScale, max(targetScale, minScale))
                        val pivotX = e.x
                        val pivotY = e.y
                        smoothScaleTo(targetScale, pivotX, pivotY)
                    }

                    // notify double tap
                    doubleTapListener?.onDoubleTap(this@GestureBitmapView)

                    return super.onDoubleTap(e)
                }


                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (!scrollEnabled) {
                        return false
                    }

                    if (e1 == null) {
                        return false
                    }
                    if (exclusiveTwoFingerScrollEnabled) {
                        if (twoFingerScrollEnabled) {
                            if (e1.pointerCount < 2 || e2.pointerCount < 2) {
                                return false
                            }
                        } else {
                            if (e1.pointerCount > 1 || e2.pointerCount > 1) {
                                return false
                            }
                        }
                    }

                    getTransformationParameters(paramsMatrix, parameters)

                    // process fling
                    val time = animationDurationMillis.toFloat() / 1000.0f
                    val diffX = velocityX * time
                    val diffY = velocityY * time

                    smoothScrollBy(diffX, diffY)
                    return true
                }

                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    // check if scroll is allowed and check that only one finger
                    // has been used to make scroll
                    if (!scrollEnabled) {
                        return false
                    }
                    if (e1 == null) {
                        return false
                    }
                    if (exclusiveTwoFingerScrollEnabled) {
                        if (twoFingerScrollEnabled) {
                            if (e1.pointerCount < 2 || e2.pointerCount < 2) {
                                return false
                            }
                        } else {
                            if (e1.pointerCount > 1 || e2.pointerCount > 1) {
                                return false
                            }
                        }
                    }

                    // process scroll
                    cancelAllAnimators()

                    getTransformationParameters(paramsMatrix, parameters)
                    /*val scale = parameters.scale

                    if (scale <= 1.0f) {
                        return false
                    }*/

                    val distX = -distanceX
                    val distY = -distanceY

                    scrollBy(distX, distY)
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    if (isLongClickable) {
                        val scaleInProgress = (scaleGestureDetector?.isInProgress) == true
                        if (!scaleInProgress) {
                            isPressed = true
                            performLongClick()
                        }
                    }
                }
            })

        init(context, attrs, defStyleAttr)
    }

    companion object {
        /**
         * Duration of scale, translate or rotation animations expressed in milliseconds.
         */
        const val DEFAULT_ANIMATION_DURATION_MILLIS = 200L

        /**
         * Minimum scale factor to be used by default when displaying the image.
         */
        const val DEFAULT_MIN_SCALE = 1.0f

        /**
         * Maximum scale factor to be used by default when displaying the image.
         */
        const val DEFAULT_MAX_SCALE = 10.0f

        /**
         * Scale jump to make when doing a double tap.
         */
        const val DEFAULT_SCALE_FACTOR_JUMP = 3.0f

        /**
         * Margin on minimum scale so that a bounce effect happens when minimum image scale reaches
         * the limit.
         */
        const val DEFAULT_MIN_SCALE_MARGIN = 0.1f

        /**
         * Margin on maximum scale so that a bounce effect happens when maximum image scale reaches
         * the limit.
         */
        const val DEFAULT_MAX_SCALE_MARGIN = 1.0f

        /**
         * Default margin on allowed scroll expressed in pixels.
         * This value determines the amount of bounce when a scroll limit is
         * reached.
         */
        const val DEFAULT_SCROLL_MARGIN = 100.0f

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
         * Length of arrays to store matrix values.
         */
        private const val MATRIX_VALUES_LENGTH = MetricTransformationParameters.MATRIX_VALUES_LENGTH
    }

    /**
     * Indicates the modes the image can be initially scaled.
     */
    enum class DisplayType {
        /**
         * Image is not initially scaled.
         */
        NONE,

        /**
         * Image will be initially scaled only if bigger than the limits of this view.
         */
        FIT_IF_BIGGER,

        /**
         * Image will be initially scaled to fit horizontally while keeping aspect ratio,
         * hence, vertically there can be a blank space or image may not fit.
         * In this mode image is aligned on top border of the view.
         */
        FIT_X_TOP,

        /**
         * Image will be initially scaled to fit horizontally while keeping aspect ratio,
         * hence, vertically there can be a blank space or image may not fit.
         * In this mode image is aligned on bottom border of the view.
         */
        FIT_X_BOTTOM,

        /**
         * Image will be initially scaled to fit horizontally while keeping aspect ratio,
         * hence, vertically there can be a blank space or image may not fit.
         * In this mode image is vertically centered.
         */
        FIT_X_CENTER,

        /**
         * Image will be initially scaled to fit vertically while keeping aspect ratio,
         * hence, horizontally there can be a blank space or image may not fit.
         * In this mode, image is aligned to the left border of the view.
         */
        FIT_Y_LEFT,

        /**
         * Image will be initially scaled to fit vertically while keeping aspect ratio,
         * hence, horizontally there can be a blank space or image may not fit.
         * In this mode, image is aligned to the right border of the view.
         */
        FIT_Y_RIGHT,

        /**
         * Image will be initially scaled to fit vertically while keeping aspect ratio,
         * hence, horizontally there can be a blank space or image may not fit.
         * In this mode, image is horizontally centered.
         */
        FIT_Y_CENTER,

        /**
         * Initially scales image to fill the whole view and centers image on the view
         * while preserving aspect ratio.
         */
        CENTER_CROP
    }

    /**
     * Interface defining top bound reached event.
     */
    interface OnTopBoundReachedListener {
        /**
         * Called when top bound is reached.
         *
         * @param view view that raised the event.
         */
        fun onTopBoundReached(view: GestureBitmapView)
    }

    /**
     * Interface defining bottom bound reached event.
     */
    interface OnBottomBoundReachedListener {
        /**
         * Called when bottom bound is reached.
         *
         * @param view view that raised the event.
         */
        fun onBottomBoundReached(view: GestureBitmapView)
    }

    /**
     * Interface defining left bound reached event.
     */
    interface OnLeftBoundReachedListener {
        /**
         * Called when left bound is reached.
         *
         * @param view view that raised the event.
         */
        fun onLeftBoundReached(view: GestureBitmapView)
    }

    /**
     * Interface defining right bound reached event.
     */
    interface OnRightBoundReachedListener {
        /**
         * Called when right bound is reached.
         *
         * @param view view that raised the event.
         */
        fun onRightBoundReached(view: GestureBitmapView)
    }

    /**
     * Interface defining minimum scale reached event.
     */
    interface OnMinScaleReachedListener {
        /**
         * Called when minimum scale is reached.
         *
         * @param view view that raised the event.
         */
        fun onMinScaleReached(view: GestureBitmapView)
    }

    /**
     * Interface defining maximum scale reached event.
     */
    interface OnMaxScaleReachedListener {
        /**
         * Called when maximum scale is reached.
         *
         * @param view view that raised the event.
         */
        fun onMaxScaleReached(view: GestureBitmapView)
    }

    /**
     * Interface defining an event to notify completion of rotation and translation animation.
     */
    interface OnRotateAndTranslateAnimationCompletedListener {
        /**
         * Called when simultaneous rotation and translation animation completes.
         *
         * @param view view that raised the event.
         */
        fun onRotateAndTranslateAnimationCompleted(view: GestureBitmapView)
    }

    /**
     * Interface defining an event to notify completion of scale animation.
     */
    interface OnScaleAnimationCompletedListener {
        /**
         * Called when scale animation completes.
         *
         * @param view view that raised the event.
         */
        fun onScaleAnimationCompleted(view: GestureBitmapView)
    }

    /**
     * Interface defining an event to notify completion of scroll animation.
     */
    interface OnScrollAnimationCompletedListener {
        /**
         * Called when scroll animation completes.
         *
         * @param view view that raised the event.
         */
        fun onScrollAnimationCompleted(view: GestureBitmapView)
    }

    /**
     * Interface defining double tap event.
     */
    interface OnDoubleTapListener {
        /**
         * Called when a double tap touch event is detected.
         *
         * @param view view that raised the event.
         */
        fun onDoubleTap(view: GestureBitmapView)
    }
}