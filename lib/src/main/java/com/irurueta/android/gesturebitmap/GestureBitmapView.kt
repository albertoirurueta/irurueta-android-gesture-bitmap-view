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
     * Inverse matrix of [displayMatrix].
     * This matrix is used to transform coordinates on the display to coordinates on
     * the original bitmap.
     */
    private val inverseDisplayMatrix = Matrix()

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
     * Indicates if left image border has been reached when doing a scroll
     * operation.
     */
    private var leftBoundReached = false

    /**
     * Indicates if right image border has been reached when doing a scroll
     * operation.
     */
    private var rightBoundReached = false

    /**
     * Indicates if top image border has been reahced when doing a scroll
     * operation.
     */
    private var topBoundReached = false

    /**
     * Indicates if bottom image border has been reached when doing a scroll
     * operation.
     */
    private var bottomBoundReached = false

    /**
     * Point to be reused containing scroll variation.
     */
    private var scrollDiff = PointF()

    /**
     * Rectangle to containing displayed bitmap to be reused during scroll
     * limit computation.
     */
    private var bitmapDisplayedRect = RectF()

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
     * Margin on min/max scale so that a bounce effect happens when image scale reaches the limit.
     */
    var scaleMargin = DEFAULT_SCALE_MARGIN

    /**
     * Resets matrix containing changes on image (scale and translation) and
     * forces view redraw.
     */
    fun resetTransformationParameters() {
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
    val displayTransformationMatrix: Matrix
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

        if (rotationEnabled) {
            rotationGestureDetector?.onTouchEvent(event)
        }

        if (scaleEnabled) {
            scaleGestureDetector?.onTouchEvent(event)
        }

        val scaleInProgress = (scaleGestureDetector?.isInProgress) ?: false
        if ((twoFingerScrollEnabled || !scaleInProgress) && (scaleEnabled || scrollEnabled)) {
            gestureDetector?.onTouchEvent(event)
        }

        var action = event.action
        action = action and MotionEvent.ACTION_MASK
        if (action == MotionEvent.ACTION_UP) {
            limitScale()
        }

        return true
    }

    /**
     * Called when the view must be drawn after requesting invalidation or relayout.
     *
     * @param canvas canvas where view will be drawn.
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        // draw bitmap
        val bitmap = this.bitmap
        if (bitmap != null && !bitmap.isRecycled) {
            canvas?.drawBitmap(bitmap, displayMatrix, bitmapPaint)
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
        displayMatrix.getValues(dispMatrixValues)
        bundle.putFloatArray(DISPLAY_MATRIX_KEY, dispMatrixValues)

        val invDisplayMatrixValues = FloatArray(MATRIX_VALUES_LENGTH)
        inverseDisplayMatrix.getValues(invDisplayMatrixValues)
        bundle.putFloatArray(INVERSE_DISPLAY_MATRIX_KEY, invDisplayMatrixValues)

        bundle.putBoolean(ROTATION_ENABLED_KEY, rotationEnabled)
        bundle.putBoolean(SCALE_ENABLED_KEY, scaleEnabled)
        bundle.putBoolean(SCROLL_ENABLED_KEY, scrollEnabled)
        bundle.putBoolean(TWO_FINGER_SCROLL_ENABLED_KEY, twoFingerScrollEnabled)
        bundle.putBoolean(DOUBLE_TAP_ENABLED_KEY, doubleTapEnabled)

        bundle.putSerializable(DISPLAY_TYPE_KEY, displayType)

        bundle.putFloat(MIN_SCALE_KEY, minScale)
        bundle.putFloat(MAX_SCALE_KEY, maxScale)
        bundle.putFloat(SCALE_FACTOR_JUMP_KEY, scaleFactorJump)

        return bundle
    }

    /**
     * Restores view state.
     *
     * @param state stored view state.
     */
    override fun onRestoreInstanceState(state: Parcelable?) {
        val viewState = if (state is Bundle) state.getParcelable(SUPER_STATE_KEY) else state
        super.onRestoreInstanceState(viewState)

        if (state is Bundle) {
            displayType = state.getSerializable(DISPLAY_TYPE_KEY) as DisplayType

            val baseMatrixValues = state.getFloatArray(BASE_MATRIX_KEY)
            baseMatrix.setValues(baseMatrixValues)

            val paramsMatrixValues = state.getFloatArray(PARAMS_MATRIX_KEY)
            paramsMatrix.setValues(paramsMatrixValues)

            val displayMatrixValues = state.getFloatArray(DISPLAY_MATRIX_KEY)
            displayMatrix.setValues(displayMatrixValues)

            val inverseDisplayMatrixValues = state.getFloatArray(INVERSE_DISPLAY_MATRIX_KEY)
            inverseDisplayMatrix.setValues(inverseDisplayMatrixValues)

            rotationEnabled = state.getBoolean(ROTATION_ENABLED_KEY)
            scaleEnabled = state.getBoolean(SCALE_ENABLED_KEY)
            scrollEnabled = state.getBoolean(SCROLL_ENABLED_KEY)
            twoFingerScrollEnabled = state.getBoolean(TWO_FINGER_SCROLL_ENABLED_KEY)
            doubleTapEnabled = state.getBoolean(DOUBLE_TAP_ENABLED_KEY)

            minScale = state.getFloat(MIN_SCALE_KEY)
            maxScale = state.getFloat(MAX_SCALE_KEY)
            scaleFactorJump = state.getFloat(SCALE_FACTOR_JUMP_KEY)
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
            computeBaseMatrix(bitmap, displayType, baseMatrix)
            updateDisplayMatrix()
            invalidate()
        }
    }

    /**
     * Called when a touch event finishes to ensure that scale is within proper
     * limit values.
     */
    private fun limitScale() {
        getTransformationParameters(paramsMatrix, transformationParameters)
        if (transformationParameters.scale < minScale || transformationParameters.scale > maxScale) {
            val scale = if (transformationParameters.scale < minScale) minScale else maxScale
            val pivotX = (scaleGestureDetector?.focusX) ?: (width.toFloat() / 2.0f)
            val pivotY = (scaleGestureDetector?.focusY) ?: (height.toFloat() / 2.0f)
            smoothScaleTo(scale, pivotX, pivotY)
        }
    }

    /**
     * Scrolls by provided amount.
     *
     * @param dx amount of horizontal scroll to make.
     * @param dy amount of vertical scroll to make.
     */
    private fun scrollBy(dx: Float, dy: Float) {
        limitScroll(dx, dy, scrollDiff)

        getTransformationParameters(paramsMatrix, transformationParameters)
        val oldTx = transformationParameters.horizontalTranslation
        val oldTy = transformationParameters.verticalTranslation

        val tx = (oldTx + scrollDiff.x).toFloat()
        val ty = (oldTy + scrollDiff.y).toFloat()
        updateTranslation(tx, ty)
    }

    /**
     * Scrolls by provided amount using an animation.
     *
     * @param dx amount of horizontal scroll to make.
     * @param dy amount of vertical scroll to make.
     */
    private fun smoothScrollBy(dx: Float, dy: Float) {
        limitScroll(dx, dy, scrollDiff)

        getTransformationParameters(paramsMatrix, transformationParameters)
        val oldTx = transformationParameters.horizontalTranslation
        val oldTy = transformationParameters.verticalTranslation

        val tx = (oldTx + scrollDiff.x).toFloat()
        val ty = (oldTy + scrollDiff.y).toFloat()
        smoothTranslateTo(tx, ty)
    }

    /**
     * Limits amount of scroll to ensure that bitmap remains visible.
     *
     * @param dx amount of horizontal translation (scroll) variation.
     * @param dy amount of vertical translation (scroll) variation.
     * @param result point containing amount of allowed horizontal and vertical
     * scroll variation.
     */
    private fun limitScroll(dx: Float, dy: Float, result: PointF) {
        getTransformationParameters(paramsMatrix, transformationParameters)
        currentBitmapDisplayedRect(displayMatrix, bitmapDisplayedRect)
        val scale = transformationParameters.scale

        leftBoundReached = false
        rightBoundReached = false
        topBoundReached = false
        bottomBoundReached = false

        result.x = dx
        result.y = dy

        if (bitmapDisplayedRect.top >= 0.0f && bitmapDisplayedRect.bottom <= height) {
            // whole image fits vertically within the view at current scale,
            // so no vertical scroll is allowed
            result.y = 0.0f
            topBoundReached = true
            bottomBoundReached = true
        }
        if (bitmapDisplayedRect.left >= 0.0f && bitmapDisplayedRect.right <= width) {
            // whole image fits horizontally within the view at current scale,
            // so no horizontal scroll is allowed
            result.x = 0.0f
            leftBoundReached = true
            rightBoundReached = true
        }
        if ((bitmapDisplayedRect.top + dy) >= 0.0f && bitmapDisplayedRect.bottom > height) {
            // top limit has been reached
            result.y = -bitmapDisplayedRect.top

            if (scale > 1.0f) {
                topBoundReached = true
                // notify top bound reached
                topBoundReachedListener?.onTopBoundReached(this)
            }
        }
        if ((bitmapDisplayedRect.bottom + dy) <= height && bitmapDisplayedRect.top < 0.0f) {
            // bottom limit has been reached
            result.y = height - bitmapDisplayedRect.bottom

            if (scale > 1.0f) {
                bottomBoundReached = true

                // notify bottom bound reached
                bottomBoundReachedListener?.onBottomBoundReached(this)
            }
        }
        if ((bitmapDisplayedRect.left + dx) >= 0.0f && bitmapDisplayedRect.right > width) {
            // left limit has been reached
            result.x = -bitmapDisplayedRect.left

            if (scale > 1.0f) {
                leftBoundReached = true

                // notify left bound reached
                leftBoundReachedListener?.onLeftBoundReached(this)
            }
        }
        if ((bitmapDisplayedRect.right + dx) <= width && bitmapDisplayedRect.left < 0.0f) {
            // right limit has been reached
            result.x = width - bitmapDisplayedRect.right

            if (scale > 1.0f) {
                rightBoundReached = true

                // notify right bound reached
                rightBoundReachedListener?.onRightBoundReached(this)
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
        //We want to get displayMatrix = suppMatrix * baseMatrix

        // firs make result the identity
        result.reset()

        // dispplayMatrix = I * paramsMatrix
        result.preConcat(paramsMatrix)

        // displayMatrix = I * suppMatrix * baseMatrix
        result.preConcat(baseMatrix)
    }

    /**
     * Recomputes display matrix
     */
    private fun updateDisplayMatrix() {
        getDisplayMatrix(baseMatrix, paramsMatrix, displayMatrix)
        displayMatrix.invert(inverseDisplayMatrix)
    }

    /**
     * Updates parameters matrix with new translation values.
     * This method does NOT check that new translation values are within allowed range of values
     * so that bitmap always remains visible on the view.
     *
     * @param tx x coordinate of translation to be set.
     * @param ty y coordinate of translation to be set.
     */
    private fun updateTranslation(tx: Float, ty: Float) {
        getTransformationParameters(paramsMatrix, parameters)

        parameters.horizontalTranslation = tx.toDouble()
        parameters.verticalTranslation = ty.toDouble()

        setTransformationParameters(parameters, paramsMatrix)
        updateDisplayMatrix()
        invalidate()
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
     */
    private fun updateScale(scale: Float, pivotX: Float, pivotY: Float) {
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
        val scaleDiff = scale.toDouble() / oldScale
        parameters.scale = scale.toDouble()
        parameters.horizontalTranslation =
            scaleDiff * (oldHorizontalTranslation - pivotX) + pivotX
        parameters.verticalTranslation =
            scaleDiff * (oldVerticalTranslation - pivotY) + pivotY


        setTransformationParameters(parameters, paramsMatrix)
        updateDisplayMatrix()
        invalidate()
    }

    /**
     * Updates parameters matrix with new rotation angle at provided pivot point.
     *
     * @param rotationAngle new rotation angle to be set.
     * @param pivotX horizontal coordinate of pivot point.
     * @param pivotY vertical coordinate of pivot point.
     */
    private fun updateRotation(rotationAngle: Float, pivotX: Float, pivotY: Float) {
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
        updateDisplayMatrix()
        invalidate()
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
        ty: Float
    ) {
        updateRotation(rotationAngle, pivotX, pivotY)
        updateTranslation(tx, ty)
    }

    /**
     * Translates up to provided position using an animation.
     *
     * @param tx horizontal coordinate of translation to animate to.
     * @param ty vertical coordinate of translation to animate to.
     */
    private fun smoothTranslateTo(tx: Float, ty: Float) {
        var translateAnimator = this.translateAnimator
        if (translateAnimator != null && translateAnimator.isRunning) {
            translateAnimator.cancel()
        }

        getTransformationParameters(paramsMatrix, transformationParameters)

        val initTx = transformationParameters.horizontalTranslation.toFloat()
        val initTy = transformationParameters.verticalTranslation.toFloat()
        val initT = floatArrayOf(initTx, initTy)
        val endT = floatArrayOf(tx, ty)

        translateAnimator = ValueAnimator.ofObject(
            FloatArrayEvaluator(),
            initT, endT
        )
        this.translateAnimator = translateAnimator

        translateAnimator.setTarget(this)
        translateAnimator.addUpdateListener { animator ->
            val t = animator.animatedValue as FloatArray
            updateTranslation(t[0], t[1])
        }
        translateAnimator.duration = animationDurationMillis
        translateAnimator.interpolator = DecelerateInterpolator()

        translateAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator?) {
                updateTranslation(tx, ty)

                scrollAnimationCompletedListener?.onScrollAnimationCompleted(
                    this@GestureBitmapView
                )
            }

            override fun onAnimationRepeat(animation: Animator?) {
                // not used
            }

            override fun onAnimationCancel(animation: Animator?) {
                // not used
            }

            override fun onAnimationStart(animation: Animator?) {
                // not used
            }
        })

        translateAnimator.start()
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

        var scaleAnimator = this.scaleAnimator
        if (scaleAnimator != null && scaleAnimator.isRunning) {
            scaleAnimator.cancel()
        }

        getTransformationParameters(paramsMatrix, transformationParameters)
        val initialScale = transformationParameters.scale.toFloat()

        scaleAnimator = ValueAnimator.ofFloat(initialScale, scale)
        this.scaleAnimator = scaleAnimator

        scaleAnimator.setTarget(this)
        scaleAnimator.addUpdateListener { animator ->
            val s = animator.animatedValue as Float
            updateScale(s, pivotX, pivotY)
        }
        scaleAnimator.duration = animationDurationMillis
        scaleAnimator.interpolator = AccelerateInterpolator()

        scaleAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator?) {
                updateScale(scale, pivotX, pivotY)

                if (scale <= minScale) {
                    minScaleReachedListener?.onMinScaleReached(
                        this@GestureBitmapView
                    )
                }
                if (scale >= maxScale) {
                    maxScaleReachedListener?.onMaxScaleReached(
                        this@GestureBitmapView
                    )
                }
                scaleAnimationCompletedListener?.onScaleAnimationCompleted(
                    this@GestureBitmapView
                )


                // ensure that view returns to initial translation and rotation at
                // minimum scale
                if (scale <= minScale) {
                    smoothRotateAndTranslateTo(0.0f, width / 2.0f, height / 2.0f, 0.0f, 0.0f)
                }
            }

            override fun onAnimationRepeat(animation: Animator?) {
                // not used
            }

            override fun onAnimationCancel(animation: Animator?) {
                // not used
            }

            override fun onAnimationStart(animation: Animator?) {
                // not used
            }
        })

        scaleAnimator.start()
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

        var rotateAndTranslateAnimator = this.rotateAndTranslateAnimator
        if (rotateAndTranslateAnimator != null && rotateAndTranslateAnimator.isRunning) {
            rotateAndTranslateAnimator.cancel()
        }

        getTransformationParameters(paramsMatrix, transformationParameters)

        val initTheta = transformationParameters.rotationAngle.toFloat()
        val initTx = transformationParameters.horizontalTranslation.toFloat()
        val initTy = transformationParameters.verticalTranslation.toFloat()
        val initValues = floatArrayOf(initTheta, initTx, initTy)
        val endValues = floatArrayOf(rotationAngle, tx, ty)

        rotateAndTranslateAnimator = ValueAnimator.ofObject(
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
            override fun onAnimationEnd(animation: Animator?) {
                updateRotationAndTranslation(rotationAngle, pivotX, pivotY, tx, ty)

                rotateAndTranslateAnimationCompletedListener?.onRotateAndTranslateAnimationCompleted(
                    this@GestureBitmapView
                )
            }

            override fun onAnimationRepeat(animation: Animator?) {
                // not used
            }


            override fun onAnimationCancel(animation: Animator?) {
                // not used
            }

            override fun onAnimationStart(animation: Animator?) {
                // not used
            }

        })

        rotateAndTranslateAnimator.start()
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
        getTransformationParameters(paramsMatrix, transformationParameters)
        val scale = transformationParameters.scale

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

        val a =
            context.obtainStyledAttributes(attrs, R.styleable.GestureBitmapView, defStyleAttr, 0)

        val drawable = a.getDrawable(R.styleable.GestureBitmapView_src)
        setDrawable(drawable)

        val displayTypeIndex = a.getInt(
            R.styleable.GestureBitmapView_displayType,
            displayType.ordinal
        )
        val displayType = DisplayType.values()[displayTypeIndex]
        this.displayType = displayType

        animationDurationMillis = a.getInt(
            R.styleable.GestureBitmapView_animationDurationMillis,
            animationDurationMillis.toInt()
        ).toLong()

        rotationEnabled =
            a.getBoolean(R.styleable.GestureBitmapView_rotationEnabled, rotationEnabled)

        scaleEnabled = a.getBoolean(R.styleable.GestureBitmapView_scaleEnabled, scaleEnabled)

        scrollEnabled = a.getBoolean(R.styleable.GestureBitmapView_scrollEnabled, scrollEnabled)

        twoFingerScrollEnabled = a.getBoolean(
            R.styleable.GestureBitmapView_twoFingerScrollEnabled,
            twoFingerScrollEnabled
        )

        doubleTapEnabled =
            a.getBoolean(R.styleable.GestureBitmapView_doubleTapEnabled, doubleTapEnabled)

        minScale = a.getFloat(R.styleable.GestureBitmapView_minScale, minScale)

        maxScale = a.getFloat(R.styleable.GestureBitmapView_maxScale, maxScale)

        scaleFactorJump =
            a.getFloat(R.styleable.GestureBitmapView_scaleFactorJump, scaleFactorJump)

        scaleMargin = a.getFloat(R.styleable.GestureBitmapView_scaleMargin, scaleMargin)

        a.recycle()
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
                    getTransformationParameters(paramsMatrix, transformationParameters)
                    val oldRotation = transformationParameters.rotationAngle
                    val newRotation = (oldRotation + angle).toFloat()

                    updateRotation(newRotation, pivotX, pivotY)

                    return true
                }
            })

        scaleGestureDetector =
            ScaleGestureDetector(
                context,
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

                    override fun onScale(detector: ScaleGestureDetector?): Boolean {
                        if (detector == null) return false

                        val span = detector.currentSpan - detector.previousSpan
                        if (span == 0.0f) return false

                        getTransformationParameters(paramsMatrix, transformationParameters)
                        val scale = transformationParameters.scale
                        var targetScale = (scale * detector.scaleFactor).toFloat()

                        targetScale =
                            min(maxScale + scaleMargin, max(targetScale, minScale - scaleMargin))
                        updateScale(targetScale, detector.focusX, detector.focusY)
                        doubleTapDirection = 1

                        return true
                    }

                })

        gestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

                override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                    performClick()
                    return true
                }

                override fun onDoubleTap(e: MotionEvent?): Boolean {
                    if (doubleTapEnabled) {
                        var targetScale = newScaleOnDoubleTap()
                        targetScale = min(maxScale, max(targetScale, minScale))
                        val pivotX = (e?.x) ?: (width.toFloat() / 2.0f)
                        val pivotY = (e?.y) ?: (height.toFloat() / 2.0f)
                        smoothScaleTo(targetScale, pivotX, pivotY)
                    }

                    // notify double tap
                    doubleTapListener?.onDoubleTap(this@GestureBitmapView)

                    return super.onDoubleTap(e)
                }


                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent?,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (!scrollEnabled) return false

                    if (e1 == null || e2 == null) return false
                    if (twoFingerScrollEnabled) {
                        if (e1.pointerCount < 2 || e2.pointerCount < 2) return false
                    } else {
                        if (e1.pointerCount > 1 || e2.pointerCount > 1) return false
                    }

                    val scaleInProgress = (scaleGestureDetector?.isInProgress) ?: false
                    if (scaleInProgress) return false

                    getTransformationParameters(paramsMatrix, transformationParameters)
                    val scale = transformationParameters.scale
                    if (scale <= 1.0f) return false

                    // process fling

                    val time = animationDurationMillis.toFloat() / 1000.0f
                    val diffX = velocityX * time
                    val diffY = velocityY * time

                    smoothScrollBy(diffX, diffY)
                    return true
                }

                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent?,
                    distanceX: Float,
                    distanceY: Float
                ): Boolean {
                    // check if scroll is allowed and check that only one finger
                    // has been used to make scroll
                    if (!scrollEnabled) return false
                    if (e1 == null || e2 == null) return false
                    if (twoFingerScrollEnabled) {
                        if (e1.pointerCount < 2 && e2.pointerCount < 2) return false
                    } else {
                        if (e1.pointerCount > 1 || e2.pointerCount > 1) return false
                    }

                    val scaleInProgress = (scaleGestureDetector?.isInProgress) ?: false
                    if (!twoFingerScrollEnabled && scaleInProgress) {
                        return false
                    } else {
                        // process scroll
                        getTransformationParameters(paramsMatrix, transformationParameters)
                        val scale = transformationParameters.scale

                        if (scale <= 1.0f) return false
                        scrollBy(-distanceX, -distanceY)
                        return true
                    }
                }

                override fun onLongPress(e: MotionEvent?) {
                    if (isLongClickable) {
                        val scaleInProgress = (scaleGestureDetector?.isInProgress) ?: false
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
         * Margin on min/max scale so that a bounce effect happens when image scale reaches the
         * limit.
         */
        const val DEFAULT_SCALE_MARGIN = 0.1f

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
         * Key to store inverse of display matrix.
         */
        private const val INVERSE_DISPLAY_MATRIX_KEY = "inverseDisplayMatrix"

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
         * while presrving aspect ratio.
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