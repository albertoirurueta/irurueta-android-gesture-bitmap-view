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
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.withSave
import androidx.core.os.BundleCompat
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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
     * Animator for scale. When returning to minimum scale it also animates rotation and
     * translation back to their initial values.
     */
    private var scaleAnimator: ValueAnimator? = null

    /**
     * Animator for translation (scroll).
     */
    private var translateAnimator: ValueAnimator? = null

    /**
     * Animator for rotation (rotation fling).
     */
    private var rotateAnimator: ValueAnimator? = null

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
     * Width of the upright image being displayed expressed in image pixels (0 when there is no
     * image). Base, params and display matrices are expressed in image pixels.
     */
    private var imageWidth = 0

    /**
     * Height of the upright image being displayed expressed in image pixels (0 when there is no
     * image).
     */
    private var imageHeight = 0

    /**
     * Matrix mapping drawn bitmap pixels into upright image pixels. Identity for plain bitmaps.
     */
    private val orientationMatrix = Matrix()

    /**
     * Destination rectangle, expressed in upright image pixels, where the bitmap is drawn.
     * Reused to avoid allocations while drawing.
     */
    private val destinationRect = RectF()

    /**
     * Source of the image being displayed, or null when the image is a plain bitmap or there is
     * no image.
     */
    private var source: ImageSource? = null

    /**
     * Holder of the decoded resolution levels of [source]. It is null when there is no source, or
     * while the view is detached from its window.
     */
    private var multiResolutionImage: MultiResolutionImage? = null

    /**
     * Maximum width of a bitmap supported by the canvas, captured while drawing.
     */
    private var maxBitmapWidth = DEFAULT_MAX_BITMAP_SIZE

    /**
     * Maximum height of a bitmap supported by the canvas, captured while drawing.
     */
    private var maxBitmapHeight = DEFAULT_MAX_BITMAP_SIZE

    /**
     * Handler used to post results of background decoding to the UI thread.
     */
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Context where [componentCallbacks] is registered, or null when not registered.
     */
    private var callbacksContext: Context? = null

    /**
     * Callbacks to free memory when the system is low on it.
     */
    private val componentCallbacks = object : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) {
            multiResolutionImage?.trimLevels()
        }

        override fun onConfigurationChanged(newConfig: Configuration) {
            // not used
        }

        @Deprecated("Deprecated in Java")
        override fun onLowMemory() {
            multiResolutionImage?.trimLevels()
        }
    }

    /**
     * Matrix mapping view coordinates into stored image coordinates. Reused to avoid
     * allocations.
     */
    private val inverseStoredMatrix = Matrix()

    /**
     * Matrix mapping stored image coordinates into view coordinates. Reused to avoid
     * allocations.
     */
    private val storedToViewMatrix = Matrix()

    /**
     * Matrix mapping upright image coordinates into stored image coordinates. Reused to avoid
     * allocations.
     */
    private val inverseOrientationMatrix = Matrix()

    /**
     * Matrix mapping view coordinates into upright image coordinates. Reused to avoid
     * allocations.
     */
    private val inverseDisplayMatrix = Matrix()

    /**
     * Array of matrix values. Reused to avoid allocations.
     */
    private val matrixValues = FloatArray(MATRIX_VALUES_LENGTH)

    /**
     * Array containing a point. Reused to avoid allocations.
     */
    private val pointValues = FloatArray(2)

    /**
     * Rectangle expressed in upright image pixels. Reused to avoid allocations.
     */
    private val uprightRect = RectF()

    /**
     * Rectangle expressed in stored image pixels. Reused to avoid allocations.
     */
    private val storedRect = Rect()

    /**
     * Executor used to decode images from a source in background, or null to use a thread owned
     * by each image. Replaceable for testing purposes.
     */
    internal var imageExecutor: Executor? = null

    /**
     * Indicates whether any gesture has changed the transformation since current touch
     * sequence started.
     */
    private var transformationChangedByGesture = false

    /**
     * Touch event currently being processed, if any. Used to obtain the time when a rotation
     * ends, only when that happens.
     */
    private var currentTouchEvent: MotionEvent? = null

    /**
     * Indicates whether a rotation fling is waiting for the last finger to be lifted.
     * A rotation ends when one of its two fingers is lifted, but the finger still touching the
     * screen keeps generating scroll events that would cancel an animation started at that point.
     * Hence, the fling only starts once every finger has been lifted.
     */
    private var rotationFlingPending = false

    /**
     * Angle to animate by the pending rotation fling, expressed in radians.
     */
    private var pendingRotationFlingAngle = 0.0f

    /**
     * Horizontal coordinate of the pivot point of the pending rotation fling.
     */
    private var pendingRotationFlingPivotX = 0.0f

    /**
     * Vertical coordinate of the pivot point of the pending rotation fling.
     */
    private var pendingRotationFlingPivotY = 0.0f

    /**
     * Time when the rotation of the pending rotation fling ended, expressed in milliseconds.
     */
    private var pendingRotationFlingTime = 0L

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
     * This is only notified when rotation and translation are animated back to their initial
     * values together with the scale, which happens when double tap returns the view to its
     * minimum scale. It is not notified when a gesture ends below the minimum scale, because in
     * that case only scale is animated back to the minimum (rotation is kept and translation is
     * brought back within its scroll limits).
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
     * Indicates whether images set through an [ImageSource] are decoded by regions at several
     * resolutions (true), or decoded whole at their full resolution (false).
     * Changing it reconfigures the current image.
     */
    var regionDecodingEnabled = true
        set(value) {
            if (field != value) {
                field = value
                configureImage()
            }
        }

    /**
     * Indicates whether images set through an [ImageSource] are decoded as hardware bitmaps.
     * Hardware bitmaps are only used when this property is true and the view is hardware
     * accelerated. Changing it reconfigures the current image.
     */
    var hardwareBitmapsEnabled = true
        set(value) {
            if (field != value) {
                field = value
                configureImage()
            }
        }

    /**
     * Memory budget expressed in bytes for the decoded resolution levels of an image set through
     * an [ImageSource]. [DEFAULT_CACHED_LEVELS_MEMORY_BUDGET] (default value) means 4 times the
     * number of pixels of the view, 4 bytes each. Changing it reconfigures the current image.
     *
     * @throws IllegalArgumentException if a negative value is set.
     */
    var cachedLevelsMemoryBudget = DEFAULT_CACHED_LEVELS_MEMORY_BUDGET
        set(value) {
            require(value >= 0L) { "budget must not be negative" }
            if (field != value) {
                field = value
                configureImage()
            }
        }

    /**
     * Listener to handle events raised while an image set through an [ImageSource] is loaded.
     */
    var imageLoadListener: OnImageLoadListener? = null

    /**
     * Drawer invoked after the image has been drawn, to draw on top of it.
     */
    var imageOverlayDrawer: ImageOverlayDrawer? = null

    /**
     * Internal bitmap.
     */
    private var _bitmap: Bitmap? = null

    /**
     * Gets or sets bitmap.
     * When setting background bitmap using this method, transformation is reset, and the current
     * [imageSource], if any, is closed. While an [imageSource] is set this property is null.
     */
    var bitmap: Bitmap?
        get() = _bitmap
        set(value) {
            if (value != _bitmap || source != null) {
                setBitmap(value, true)
            }
        }

    /**
     * Indicates whether there is an image (either a bitmap or a source) to be displayed.
     */
    private val hasImage: Boolean
        get() = _bitmap != null || source != null

    /**
     * Gets or sets the source of the image to display.
     * Setting it replaces any previous bitmap or source and resets transformation. The previous
     * source is closed, and so is the one set here when it is replaced later or the view is given
     * another image, so it must not be used by the caller afterwards.
     */
    var imageSource: ImageSource?
        get() = source
        set(value) {
            if (value !== source) {
                setImageSource(value, true)
            }
        }

    /**
     * Sets the source of the image to display, which is decoded by regions at the resolution
     * required by the display transformation.
     * Any previous bitmap is recycled and any previous source is closed. The provided source is
     * owned by this view from now on: it is closed when it is replaced by another image or source,
     * and it must not be closed by the caller. Setting the source that is already set does nothing.
     *
     * @param source source to be set, or null to clear the image.
     * @param resetMatrices true to reset transformation matrices, false otherwise.
     */
    fun setImageSource(source: ImageSource?, resetMatrices: Boolean = true) {
        if (source == null) {
            setBitmap(null, resetMatrices)
            return
        }
        if (source === this.source) {
            return
        }

        _bitmap?.recycle()
        _bitmap = null
        closeSource()

        this.source = source
        val storedWidth = source.storedWidth
        val storedHeight = source.storedHeight
        val orientation = source.orientation
        imageWidth = ImageOrientation.uprightWidth(orientation, storedWidth, storedHeight)
        imageHeight = ImageOrientation.uprightHeight(orientation, storedWidth, storedHeight)
        ImageOrientation.toMatrix(orientation, storedWidth, storedHeight, orientationMatrix)
        destinationRect.set(0.0f, 0.0f, storedWidth.toFloat(), storedHeight.toFloat())

        createMultiResolutionImage()

        if (resetMatrices) {
            if (imageWidth > 0 && imageHeight > 0) {
                computeBaseMatrix(imageWidth, imageHeight, displayType, baseMatrix)
            }

            // reset params matrix, update display matrix and invalidate
            resetTransformationParameters()
        } else {
            cancelAllAnimators()
            // this method can be called from any thread
            postInvalidate()
        }
        configureImage()
    }

    /**
     * Sets the image to display from a raster drawable resource, decoded by regions.
     *
     * @param resId identifier of the drawable resource.
     * @param resetMatrices true to reset transformation matrices, false otherwise.
     * @throws IOException if the resource cannot be read or its format is not supported.
     */
    fun setImageResource(resId: Int, resetMatrices: Boolean = true) {
        setImageSource(ImageSources.fromResource(resources, resId), resetMatrices)
    }

    /**
     * Sets the image to display from a content URI, decoded by regions.
     *
     * @param uri URI of the encoded image.
     * @param resetMatrices true to reset transformation matrices, false otherwise.
     * @throws IOException if the URI cannot be read or its format is not supported.
     */
    fun setImageUri(uri: Uri, resetMatrices: Boolean = true) {
        setImageSource(ImageSources.fromUri(context.contentResolver, uri), resetMatrices)
    }

    /**
     * Sets the image to display from a file, decoded by regions.
     *
     * @param file file containing the encoded image.
     * @param resetMatrices true to reset transformation matrices, false otherwise.
     * @throws IOException if the file cannot be read or its format is not supported.
     */
    fun setImageFile(file: File, resetMatrices: Boolean = true) {
        setImageSource(ImageSources.fromFile(file), resetMatrices)
    }

    /**
     * Notifies that the contents of the displayed image have changed, so that it is drawn again.
     * When the image comes from an [ImageSource] its decoded levels are refreshed in background.
     * When the image is a bitmap, the view is just invalidated. Nothing is done without image.
     *
     * @param region changed region expressed in upright image pixels, or null when the whole image
     * changed.
     */
    fun invalidateImage(region: Rect? = null) {
        val image = multiResolutionImage
        if (image != null) {
            if (region == null) {
                image.invalidate(null)
            } else if (orientationMatrix.invert(inverseOrientationMatrix)) {
                uprightRect.set(region)
                inverseOrientationMatrix.mapRect(uprightRect)
                storedRect.set(
                    floor(uprightRect.left).toInt(),
                    floor(uprightRect.top).toInt(),
                    ceil(uprightRect.right).toInt(),
                    ceil(uprightRect.bottom).toInt()
                )
                image.invalidate(storedRect)
            }
        } else if (_bitmap != null) {
            invalidate()
        }
    }

    /**
     * Maps a point expressed in view coordinates into upright image pixels.
     *
     * @param x horizontal view coordinate.
     * @param y vertical view coordinate.
     * @param result instance where the image coordinates are stored.
     * @return true if the point could be mapped, false when there is no image or the display
     * matrix cannot be inverted.
     */
    fun mapViewToImage(x: Float, y: Float, result: PointF): Boolean {
        if (!hasImage || !displayMatrix.invert(inverseDisplayMatrix)) {
            return false
        }
        pointValues[0] = x
        pointValues[1] = y
        inverseDisplayMatrix.mapPoints(pointValues)
        result.set(pointValues[0], pointValues[1])
        return true
    }

    /**
     * Sets bitmap.
     * @param bitmap bitmap to be set.
     * @param resetMatrices true to reset transformation matrices, false otherwise.
     */
    fun setBitmap(bitmap: Bitmap?, resetMatrices: Boolean = true) {
        // a bitmap replaces any previous source
        closeSource()

        // recycle previously existing bitmap
        _bitmap?.recycle()

        if (bitmap == null || !bitmap.isRecycled) {
            _bitmap = bitmap
            imageWidth = bitmap?.width ?: 0
            imageHeight = bitmap?.height ?: 0
            orientationMatrix.reset()
            destinationRect.set(0.0f, 0.0f, imageWidth.toFloat(), imageHeight.toFloat())
        }

        // set new bitmap
        if (bitmap != null && !bitmap.isRecycled) {
            // bitmap is frequently drawn at a much smaller size than its own, mipmaps make
            // minified drawing cheaper and smoother on hardware accelerated canvases
            bitmap.setHasMipMap(true)

            if (resetMatrices) {
                computeBaseMatrix(imageWidth, imageHeight, displayType, baseMatrix)

                // reset params matrix, update display matrix and invalidate
                resetTransformationParameters()
            } else {
                cancelAllAnimators()
                // this method can be called from any thread
                postInvalidate()
            }
        }
    }

    /**
     * Sets bitmap of this view using provided drawable.
     *
     * The drawable is converted to a bitmap of its intrinsic size, except for bitmap drawables
     * whose intrinsic size is larger than their actual bitmap (e.g. a resource placed in a
     * `drawable` folder of lower density than the screen). Those are never scaled up: their
     * bitmap is used at its own size, because a scaled up copy needs much more memory, can
     * exceed the maximum size the GPU is able to keep as a texture and makes drawing every
     * frame very slow, without adding any detail.
     *
     * @param drawable drawable to be set.
     */
    fun setDrawable(drawable: Drawable?) {
        bitmap = toBitmapWithoutUpscaling(drawable)
    }

    /**
     * Converts provided drawable into a bitmap, without scaling up bitmap drawables beyond the
     * size of their actual bitmap.
     *
     * @param drawable drawable to be converted.
     * @return converted bitmap or null if no drawable is provided.
     */
    private fun toBitmapWithoutUpscaling(drawable: Drawable?): Bitmap? {
        if (drawable == null) {
            return null
        }

        val source = (drawable as? BitmapDrawable)?.bitmap
        return if (source != null
            && (drawable.intrinsicWidth > source.width || drawable.intrinsicHeight > source.height)
        ) {
            // intrinsic size is scaled by screen density, use actual bitmap size instead
            drawable.toBitmap(source.width, source.height)
        } else {
            drawable.toBitmap()
        }
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
                if (!hasImage) return

                // displayMatrix follows the expression:
                // displayMatrix = paramsMatrix * baseMatrix

                // Hence, to keep displayMatrix constant
                // displayMatrix =  newParamsMatrix * newBaseMatrix
                // newParamsMatrix = displayMatrix * newBaseMatrix^-1

                // newParamsMatrix = paramsMatrix * baseMatrix * newBaseMatrix^-1

                cancelAllAnimators()

                val oldBaseMatrix = Matrix(baseMatrix)

                // recompute base matrix
                computeBaseMatrix(imageWidth, imageHeight, value, baseMatrix)

                val invBaseMatrix = Matrix()
                if (!baseMatrix.invert(invBaseMatrix)) {
                    return
                }

                paramsMatrix.preConcat(oldBaseMatrix)
                paramsMatrix.preConcat(invBaseMatrix)

                updateDisplayMatrix()
                // this setter can be called from any thread
                postInvalidate()
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
     * Indicates whether tension is applied when the bitmap is dragged beyond its scroll limits.
     * When true, dragging past the un-margined bound is progressively resisted up to the
     * corresponding scroll margin (rubber-band effect). When false, the bitmap follows the finger
     * until the hard margin is reached.
     */
    var scrollTensionEnabled = true

    /**
     * Indicates whether a rotation gesture keeps rotating the bitmap with decaying speed after
     * the fingers are released (rotation fling), when they were rotating fast enough.
     * The fling starts when the last finger is lifted, provided it is lifted shortly after the
     * rotation ends (within 150 ms), and it replaces any scroll fling made by that same release.
     * This only applies when [rotationEnabled] is true. If the scale is out of its limits when the
     * gesture ends, the scale correction animation takes precedence and no rotation fling is
     * kept.
     */
    var rotationFlingEnabled = true

    /**
     * Amount of tension (resistance) applied when the bitmap is dragged beyond its scroll limits
     * and [scrollTensionEnabled] is true.
     * Each outward finger movement is multiplied by `(1 - overscroll / margin)^scrollTension`.
     * A value of 1 (default) makes resistance grow linearly with the overscroll. Larger values
     * make resistance grow faster (stiffer feel), values between 0 and 1 make it softer, and 0
     * removes tension so that the bitmap follows the finger until the hard margin.
     * @throws IllegalArgumentException if provided value is negative.
     */
    var scrollTension = DEFAULT_SCROLL_TENSION
        set(value) {
            if (value < 0.0f) {
                throw IllegalArgumentException()
            }
            field = value
        }

    /**
     * Indicates whether antialiasing is enabled or not.
     */
    var isAntialiasingEnabled: Boolean
        get() {
            return bitmapPaint.isAntiAlias
        }
        set(value) {
            bitmapPaint.isAntiAlias = value
        }

    /**
     * Resets matrix containing changes on image (scale and translation) and
     * forces view redraw.
     */
    fun resetTransformationParameters() {
        cancelAllAnimators()
        paramsMatrix.reset()
        updateDisplayMatrix()
        // this method can be called from any thread
        postInvalidate()
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
            // this setter can be called from any thread
            postInvalidate()
        }

    /**
     * Gets matrix containing current transformation parameters applied
     * to base matrix to display current image. Parameters are expressed in image pixels.
     */
    val transformationMatrix: Matrix
        get() = Matrix(paramsMatrix)

    /**
     * Gets base transformation parameters that were applied to the image (expressed in image pixels) because of
     * current display type.
     */
    val baseTransformationParameters: MetricTransformationParameters
        get() {
            val result = MetricTransformationParameters()
            getTransformationParameters(baseMatrix, result)
            return result
        }

    /**
     * Gets matrix containing base transformation that was applied to the image
     * (expressed in image pixels) because of current display type.
     */
    val baseTransformationMatrix: Matrix
        get() = Matrix(baseMatrix)

    /**
     * Gets overall transformation parameters that were applied to transform image
     * pixels into display coordinates. This takes into account base and current
     * transformation parameters.
     */
    val displayTransformationParameters: MetricTransformationParameters
        get() {
            val result = MetricTransformationParameters()
            getTransformationParameters(displayMatrix, result)
            return result
        }

    /**
     * Gets overall transformation matrix to transform image pixels into display
     * coordinates. This takes into account base and current transformation matrices.
     */
    val displayTransformationMatrix
        get() = Matrix(displayMatrix)

    /**
     * Gets rectangle, expressed in display coordinates, where the image is displayed after applying the full display
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
        currentTouchEvent = event
        if (action == MotionEvent.ACTION_DOWN) {
            cancelAllAnimators()
            transformationChangedByGesture = false
            rotationFlingPending = false
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            // a new finger starts a new multi-touch gesture, discarding any pending fling
            rotationFlingPending = false
            if (isAnyMultiTouchGestureEnabled()) {
                // a multi-touch gesture handled by this view has started, prevent scrolling
                // parents from stealing it
                parent?.requestDisallowInterceptTouchEvent(true)
            }
        } else if (action == MotionEvent.ACTION_CANCEL) {
            rotationFlingPending = false
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
            // display matrix is already updated by whichever gesture changed the transformation
            transformationChangedByGesture = true
            invalidate()
        }

        if (action == MotionEvent.ACTION_UP && rotationFlingPending) {
            startPendingRotationFling(event.eventTime)
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            updateImageLevels(false)
        }

        if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)
            && transformationChangedByGesture
        ) {
            // limits are only restored when a gesture has changed the transformation, so that
            // transformations set programmatically are preserved by plain taps
            transformationChangedByGesture = false
            limitScaleAndTranslation()
        }

        currentTouchEvent = null
        return true
    }

    /**
     * Called when the view must be drawn after requesting invalidation or relayout.
     *
     * @param canvas canvas where view will be drawn.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val image = multiResolutionImage
        val bitmap = _bitmap
        val hasBitmap = bitmap != null && !bitmap.isRecycled
        if (image == null && !hasBitmap) {
            return
        }

        captureMaxBitmapSize(canvas)

        // matrices are used directly to avoid allocations while drawing
        canvas.withSave {
            concat(displayMatrix)
            withSave {
                concat(orientationMatrix)
                if (image != null) {
                    drawLevels(this, image)
                } else if (bitmap != null) {
                    drawBitmap(bitmap, null, destinationRect, bitmapPaint)
                }
            }

            val drawer = imageOverlayDrawer
            if (drawer != null) {
                // canvas is now expressed in upright image pixels
                clipRect(0, 0, imageWidth, imageHeight)
                drawer.onDrawOverImage(this@GestureBitmapView, this)
            }
        }
    }

    /**
     * Draws the current level and detail patch of the image.
     * Canvas must be expressed in stored image pixels.
     *
     * @param canvas canvas where bitmaps are drawn.
     * @param image image containing the bitmaps to draw.
     */
    private fun drawLevels(canvas: Canvas, image: MultiResolutionImage) {
        val level = image.levelBitmap
        val patch = image.patchBitmap
        if (level != null && !level.isRecycled) {
            if (patch != null && image.hasAlpha) {
                // avoid blending translucent pixels of the level with those of the patch
                canvas.withSave {
                    clipOutRect(image.patchRegion)
                    drawLevel(this, level, null)
                }
            } else {
                drawLevel(canvas, level, null)
            }
        }
        if (patch != null && !patch.isRecycled) {
            drawLevel(canvas, patch, image.patchRegion)
        }
    }

    /**
     * Draws a bitmap, using a software copy of it when it is a hardware bitmap and the canvas is
     * not hardware accelerated.
     *
     * @param canvas canvas where the bitmap is drawn.
     * @param bitmap bitmap to draw.
     * @param region region where the bitmap is drawn, or null to draw it over the whole stored
     * image.
     */
    private fun drawLevel(canvas: Canvas, bitmap: Bitmap, region: Rect?) {
        val needsCopy = !canvas.isHardwareAccelerated && bitmap.config == Bitmap.Config.HARDWARE
        val toDraw = if (needsCopy) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
        if (toDraw == null) {
            return
        }
        if (region != null) {
            canvas.drawBitmap(toDraw, null, region, bitmapPaint)
        } else {
            canvas.drawBitmap(toDraw, null, destinationRect, bitmapPaint)
        }
        if (toDraw !== bitmap) {
            toDraw.recycle()
        }
    }

    /**
     * Captures the maximum bitmap size supported by the canvas, reconfiguring the image when it
     * changes.
     *
     * @param canvas canvas being drawn.
     */
    private fun captureMaxBitmapSize(canvas: Canvas) {
        val w = canvas.maximumBitmapWidth
        val h = canvas.maximumBitmapHeight
        if (w > 0 && h > 0 && (w != maxBitmapWidth || h != maxBitmapHeight)) {
            maxBitmapWidth = w
            maxBitmapHeight = h
            configureImage()
        }
    }

    /**
     * Called when the view is attached to a window.
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (callbacksContext == null) {
            context.registerComponentCallbacks(componentCallbacks)
            callbacksContext = context
        }
        if (source != null && multiResolutionImage == null) {
            createMultiResolutionImage()
            updateImageLevels(false)
        }
        configureImage()
    }

    /**
     * Called when the view is detached from its window. Memory used by decoded levels is
     * released, and decoded again when the view is attached.
     */
    override fun onDetachedFromWindow() {
        callbacksContext?.unregisterComponentCallbacks(componentCallbacks)
        callbacksContext = null
        multiResolutionImage?.release()
        multiResolutionImage = null
        super.onDetachedFromWindow()
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
        bundle.putBoolean(SCROLL_TENSION_ENABLED_KEY, scrollTensionEnabled)
        bundle.putBoolean(ROTATION_FLING_ENABLED_KEY, rotationFlingEnabled)
        bundle.putFloat(SCROLL_TENSION_KEY, scrollTension)
        bundle.putBoolean(REGION_DECODING_ENABLED_KEY, regionDecodingEnabled)
        bundle.putBoolean(HARDWARE_BITMAPS_ENABLED_KEY, hardwareBitmapsEnabled)
        bundle.putLong(CACHED_LEVELS_MEMORY_BUDGET_KEY, cachedLevelsMemoryBudget)

        return bundle
    }

    /**
     * Restores view state.
     *
     * @param state stored view state.
     */
    override fun onRestoreInstanceState(state: Parcelable?) {
        val viewState = if (state is Bundle) {
            BundleCompat.getParcelable(state, SUPER_STATE_KEY, Parcelable::class.java)
        } else {
            state
        }
        super.onRestoreInstanceState(viewState)

        if (state is Bundle) {
            displayType = BundleCompat.getSerializable(
                state, DISPLAY_TYPE_KEY,
                DisplayType::class.java
            )!!

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
            scrollTensionEnabled = state.getBoolean(SCROLL_TENSION_ENABLED_KEY, true)
            rotationFlingEnabled = state.getBoolean(ROTATION_FLING_ENABLED_KEY, true)
            scrollTension = state.getFloat(SCROLL_TENSION_KEY, DEFAULT_SCROLL_TENSION)
            regionDecodingEnabled = state.getBoolean(REGION_DECODING_ENABLED_KEY, true)
            hardwareBitmapsEnabled = state.getBoolean(HARDWARE_BITMAPS_ENABLED_KEY, true)
            cachedLevelsMemoryBudget = state.getLong(
                CACHED_LEVELS_MEMORY_BUDGET_KEY,
                DEFAULT_CACHED_LEVELS_MEMORY_BUDGET
            )
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
            if (!hasImage) return
            cancelAllAnimators()
            computeBaseMatrix(imageWidth, imageHeight, displayType, baseMatrix)
            updateDisplayMatrix()
            configureImage()
            invalidate()
        }
    }

    /**
     * Indicates whether any gesture requiring more than one finger is enabled.
     *
     * @return true if scale, rotation or two finger scroll is enabled, false otherwise.
     */
    private fun isAnyMultiTouchGestureEnabled(): Boolean {
        return scaleEnabled || rotationEnabled || (scrollEnabled && twoFingerScrollEnabled)
    }

    /**
     * Called when a touch gesture finishes to smoothly return scale and translation to their
     * allowed limits.
     * Rotation is never modified by this correction.
     * A scale that differs from the limits by less than [SCALE_TOLERANCE] is considered to be
     * within them, because transformation parameters are obtained from a single precision matrix
     * whose accumulated rounding errors (e.g. after rotating) can make the scale slightly differ
     * from its actual value.
     */
    private fun limitScaleAndTranslation() {
        getTransformationParameters(paramsMatrix, parameters)
        val currentScale = parameters.scale
        if (currentScale < minScale - SCALE_TOLERANCE || currentScale > maxScale + SCALE_TOLERANCE) {
            limitScale(currentScale)
        } else if (!isAnyAnimatorRunning()) {
            smoothScrollBy(0.0f, 0.0f, skipIfNotNeeded = true)
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
            smoothScaleTo(newScale, pivotX, pivotY, resetRotation = false)
        }
    }

    /**
     * Scrolls by provided amount while a touch gesture is in progress.
     * Scroll amount is limited so that bitmap is not moved beyond allowed limits, without
     * snapping it back to them.
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
            snap = false,
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
     * @param skipIfNotNeeded true to avoid starting an animation when no translation change is
     * required, false to always start the animation.
     */
    private fun smoothScrollBy(
        dx: Float, dy: Float, skipIfNotNeeded: Boolean = false
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
            snap = true,
            scrollDiff
        )

        if (skipIfNotNeeded && abs(scrollDiff.x) < MIN_SCROLL_CORRECTION
            && abs(scrollDiff.y) < MIN_SCROLL_CORRECTION
        ) {
            // required correction is not noticeable
            return
        }

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
     * Limits are computed from the rectangle where the bitmap is currently displayed, so that
     * they remain valid when scale or rotation change in the middle of a gesture.
     *
     * @param dx amount of horizontal translation (scroll) variation.
     * @param dy amount of vertical translation (scroll) variation.
     * @param leftMargin left scroll margin expressed in pixels.
     * @param rightMargin right scroll margin expressed in pixels.
     * @param topMargin top scroll margin expressed in pixels.
     * @param bottomMargin bottom scroll margin expressed in pixels.
     * @param snap true if a bitmap lying outside of allowed limits must be fully returned to
     * them (used when result is animated), false to never move the bitmap more than requested
     * (used while a finger drags the bitmap, to avoid sudden jumps).
     * @param result point containing amount of allowed horizontal and vertical
     * scroll variation.
     */
    private fun limitScroll(
        dx: Float, dy: Float,
        leftMargin: Float, rightMargin: Float,
        topMargin: Float, bottomMargin: Float,
        snap: Boolean,
        result: PointF
    ) {
        if (!currentBitmapDisplayedRect(displayMatrix, bitmapDisplayedRect)) {
            // there is nothing to scroll when no bitmap is available
            result.x = 0.0f
            result.y = 0.0f
            return
        }

        val rectLeft = bitmapDisplayedRect.left
        val rectTop = bitmapDisplayedRect.top
        val rectWidth = bitmapDisplayedRect.width()
        val rectHeight = bitmapDisplayedRect.height()
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        // vertical limits
        val minTop = minAllowedStart(rectHeight, viewHeight, bottomMargin)
        val maxTop = maxAllowedStart(rectHeight, viewHeight, topMargin)
        val newTop = rectTop + dy
        if (rectTop < maxTop && newTop >= maxTop) {
            topBoundReachedListener?.onTopBoundReached(this)
        }
        if (rectTop > minTop && newTop <= minTop) {
            bottomBoundReachedListener?.onBottomBoundReached(this)
        }
        result.y = limitDelta(
            rectTop, dy, minTop, maxTop, snap,
            minAllowedStart(rectHeight, viewHeight, 0.0f),
            maxAllowedStart(rectHeight, viewHeight, 0.0f)
        )

        // horizontal limits
        val minLeft = minAllowedStart(rectWidth, viewWidth, rightMargin)
        val maxLeft = maxAllowedStart(rectWidth, viewWidth, leftMargin)
        val newLeft = rectLeft + dx
        if (rectLeft < maxLeft && newLeft >= maxLeft) {
            leftBoundReachedListener?.onLeftBoundReached(this)
        }
        if (rectLeft > minLeft && newLeft <= minLeft) {
            rightBoundReachedListener?.onRightBoundReached(this)
        }
        result.x = limitDelta(
            rectLeft, dx, minLeft, maxLeft, snap,
            minAllowedStart(rectWidth, viewWidth, 0.0f),
            maxAllowedStart(rectWidth, viewWidth, 0.0f)
        )
    }

    /**
     * Computes minimum allowed start (left or top) coordinate of displayed bitmap on one axis.
     *
     * @param rectSize size of displayed bitmap on the axis.
     * @param viewSize size of the view on the axis.
     * @param endMargin scroll margin at the end (right or bottom) of the axis.
     * @return minimum allowed start coordinate.
     */
    private fun minAllowedStart(rectSize: Float, viewSize: Float, endMargin: Float): Float {
        return if (rectSize <= viewSize) {
            // bitmap fits within the view, it must remain centered (up to margin)
            (viewSize - rectSize) / 2.0f - endMargin
        } else {
            // bitmap end can not leave an empty space larger than margin
            viewSize - endMargin - rectSize
        }
    }

    /**
     * Computes maximum allowed start (left or top) coordinate of displayed bitmap on one axis.
     *
     * @param rectSize size of displayed bitmap on the axis.
     * @param viewSize size of the view on the axis.
     * @param startMargin scroll margin at the start (left or top) of the axis.
     * @return maximum allowed start coordinate.
     */
    private fun maxAllowedStart(rectSize: Float, viewSize: Float, startMargin: Float): Float {
        return if (rectSize <= viewSize) {
            // bitmap fits within the view, it must remain centered (up to margin)
            (viewSize - rectSize) / 2.0f + startMargin
        } else {
            // bitmap start can not leave an empty space larger than margin
            startMargin
        }
    }

    /**
     * Limits requested variation of a coordinate so that it remains within provided range.
     *
     * @param current current value of coordinate.
     * @param delta requested variation.
     * @param minValue minimum allowed value.
     * @param maxValue maximum allowed value.
     * @param snap true to always end within the allowed range. If false and current value is
     * already out of range (e.g. because scale or rotation have changed), only variations
     * approaching the allowed range are accepted, and never beyond requested amount.
     * @param softMinValue minimum value allowed without scroll margin (tension starts below it).
     * @param softMaxValue maximum value allowed without scroll margin (tension starts above it).
     * @return allowed variation.
     */
    private fun limitDelta(
        current: Float,
        delta: Float,
        minValue: Float,
        maxValue: Float,
        snap: Boolean,
        softMinValue: Float,
        softMaxValue: Float
    ): Float {
        val target = current + delta
        return when {
            snap -> max(minValue, min(target, maxValue)) - current

            current in minValue..maxValue -> {
                val allowed = tensionDelta(current, delta, softMinValue, softMaxValue, minValue, maxValue)
                max(minValue, min(current + allowed, maxValue)) - current
            }

            current > maxValue -> if (delta < 0.0f) max(target, minValue) - current else 0.0f
            else -> if (delta > 0.0f) min(target, maxValue) - current else 0.0f
        }
    }

    /**
     * Applies tension (resistance) to a requested variation when it moves the coordinate further
     * beyond the un-margined bounds, so that the resistance grows with the overscroll (as
     * configured by [scrollTension]) until it reaches the margin.
     *
     * @param current current value of coordinate, within the margin-expanded range.
     * @param delta requested variation.
     * @param softMinValue minimum value without margin.
     * @param softMaxValue maximum value without margin.
     * @param minValue minimum value including margin.
     * @param maxValue maximum value including margin.
     * @return variation after applying tension, or the requested one if tension does not apply.
     */
    private fun tensionDelta(
        current: Float,
        delta: Float,
        softMinValue: Float,
        softMaxValue: Float,
        minValue: Float,
        maxValue: Float
    ): Float {
        if (!scrollTensionEnabled) {
            return delta
        }

        val overscroll: Float
        val margin: Float
        if (delta > 0.0f) {
            overscroll = max(0.0f, current - softMaxValue)
            margin = maxValue - softMaxValue
        } else {
            overscroll = max(0.0f, softMinValue - current)
            margin = softMinValue - minValue
        }
        if (overscroll <= 0.0f || margin <= 0.0f) {
            return delta
        }

        val resistance = (1.0f - overscroll / margin).coerceIn(0.0f, 1.0f).pow(scrollTension)
        return delta * resistance
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
        if (imageWidth <= 0 || imageHeight <= 0) return false

        result.set(0.0f, 0.0f, imageWidth.toFloat(), imageHeight.toFloat())
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
        if (multiResolutionImage != null) {
            updateImageLevels(
                scaleGestureDetector?.isInProgress == true || scaleAnimator?.isRunning == true
            )
        }
    }

    /**
     * Notifies the displayed region to the multi resolution image, so that the level and detail
     * patch to draw are chosen.
     *
     * @param scaling true while the user is scaling.
     */
    private fun updateImageLevels(scaling: Boolean) {
        val image = multiResolutionImage ?: return
        if (width <= 0 || height <= 0) {
            return
        }

        // stored pixels -> upright image pixels -> view coordinates
        storedToViewMatrix.set(displayMatrix)
        storedToViewMatrix.preConcat(orientationMatrix)
        if (!storedToViewMatrix.invert(inverseStoredMatrix)) {
            return
        }
        storedToViewMatrix.getValues(matrixValues)
        val sx = matrixValues[Matrix.MSCALE_X]
        val ky = matrixValues[Matrix.MSKEW_Y]
        val displayScale = sqrt(sx * sx + ky * ky)
        image.update(inverseStoredMatrix, displayScale, width, height, scaling)
    }

    /**
     * Creates the multi resolution image of the current source, releasing any previous one.
     */
    private fun createMultiResolutionImage() {
        val currentSource = source ?: return
        multiResolutionImage?.release()
        multiResolutionImage = MultiResolutionImage(
            currentSource,
            imageExecutor,
            post = { runnable -> mainHandler.post(runnable) },
            onChanged = { invalidate() },
            onPreviewLoaded = { imageLoadListener?.onPreviewLoaded(this) },
            onDetailLoaded = { imageLoadListener?.onDetailLoaded(this) },
            onFailed = { error -> imageLoadListener?.onImageLoadFailed(this, error) }
        )
    }

    /**
     * Configures the multi resolution image for current view size and settings.
     * Nothing is done when there is no source or the view has no size yet.
     */
    private fun configureImage() {
        val image = multiResolutionImage ?: return
        val w = width
        val h = height
        if (w <= 0 || h <= 0) {
            return
        }
        val budget = if (cachedLevelsMemoryBudget == DEFAULT_CACHED_LEVELS_MEMORY_BUDGET) {
            DEFAULT_BUDGET_VIEW_COUNT * w * h * BYTES_PER_PIXEL
        } else {
            cachedLevelsMemoryBudget
        }
        image.configure(
            w,
            h,
            budget,
            maxBitmapWidth,
            maxBitmapHeight,
            hardwareBitmapsEnabled && isHardwareAccelerated,
            regionDecodingEnabled
        )
    }

    /**
     * Closes current source, if any, along with its multi resolution image.
     */
    private fun closeSource() {
        val image = multiResolutionImage
        val currentSource = source
        multiResolutionImage = null
        source = null
        if (image != null) {
            image.close()
        } else {
            currentSource?.close()
        }
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
        // display matrix is always kept up to date, since scroll limits are computed from it
        updateDisplayMatrix()
        if (invalidate) {
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
        // display matrix is always kept up to date, since scroll limits are computed from it
        updateDisplayMatrix()
        if (invalidate) {
            invalidate()
        }
    }

    /**
     * Updates parameters matrix with new rotation angle at provided pivot point.
     *
     * @param rotationAngle new rotation angle to be set.
     * @param pivotX horizontal coordinate of pivot point.
     * @param pivotY vertical coordinate of pivot point.
     */
    private fun updateRotation(
        rotationAngle: Float,
        pivotX: Float,
        pivotY: Float
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
        updateDisplayMatrix()
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
                updateImageLevels(false)

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
     * Starts the pending rotation fling (if any), provided that the last finger has been lifted
     * soon enough after the rotation ended. Otherwise (e.g. the remaining finger was kept on the
     * screen), the pending fling is discarded.
     * The rotation fling replaces any scroll fling started by the same release.
     *
     * @param eventTime time when the last finger was lifted, expressed in milliseconds.
     */
    private fun startPendingRotationFling(eventTime: Long) {
        if (!rotationFlingPending) {
            return
        }
        rotationFlingPending = false

        if (eventTime - pendingRotationFlingTime <= ROTATION_FLING_MAX_RELEASE_DELAY_MILLIS) {
            smoothRotateBy(
                pendingRotationFlingAngle,
                pendingRotationFlingPivotX,
                pendingRotationFlingPivotY
            )
        }
    }

    /**
     * Cancels rotate animator if it is running.
     */
    private fun cancelRotateAnimator() {
        val rotateAnimator = this.rotateAnimator
        if (rotateAnimator != null && rotateAnimator.isRunning) {
            rotateAnimator.cancel()
        }
    }

    /**
     * Rotates by provided angle using a decelerating animation.
     *
     * @param angle angle variation to animate, in radians.
     * @param pivotX horizontal coordinate of pivot point.
     * @param pivotY vertical coordinate of pivot point.
     */
    private fun smoothRotateBy(angle: Float, pivotX: Float, pivotY: Float) {
        cancelAllAnimators()

        getTransformationParameters(paramsMatrix, parameters)
        val initRotation = parameters.rotationAngle.toFloat()
        val endRotation = initRotation + angle

        val rotateAnimator = ValueAnimator.ofFloat(initRotation, endRotation)
        this.rotateAnimator = rotateAnimator

        rotateAnimator.setTarget(this)
        rotateAnimator.addUpdateListener { animator ->
            updateRotation(animator.animatedValue as Float, pivotX, pivotY)
            invalidate()
        }
        rotateAnimator.duration = animationDurationMillis
        rotateAnimator.interpolator = DecelerateInterpolator()

        rotateAnimator.addListener(object : Animator.AnimatorListener {
            var cancelled = false

            override fun onAnimationEnd(animation: Animator) {
                if (cancelled) {
                    return
                }

                // animator must not be reported as running when limits are restored
                if (this@GestureBitmapView.rotateAnimator === rotateAnimator) {
                    this@GestureBitmapView.rotateAnimator = null
                }
                updateRotation(endRotation, pivotX, pivotY)
                invalidate()
                updateImageLevels(false)
                limitScaleAndTranslation()
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

        rotateAnimator.start()
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
     * When provided scale is the minimum scale (or lower) and [resetRotation] is true, rotation
     * and translation are animated back to their initial values within the same animation (see
     * [smoothResetTo]), and [rotateAndTranslateAnimationCompletedListener] is notified.
     * Otherwise, only scale is animated (keeping current rotation) and, when animation finishes,
     * bitmap is smoothly returned within its scroll limits if needed.
     *
     * @param scale scale to animate to.
     * @param pivotX horizontal coordinate of pivot point.
     * @param pivotY vertical coordinate of pivot point.
     * @param resetRotation true to reset rotation and translation when scale is the minimum
     * scale (or lower), false to keep current rotation.
     */
    private fun smoothScaleTo(
        scale: Float,
        pivotX: Float,
        pivotY: Float,
        resetRotation: Boolean
    ) {
        if (resetRotation && scale <= minScale) {
            smoothResetTo(scale)
            return
        }

        cancelAllAnimators()

        getTransformationParameters(paramsMatrix, parameters)
        val initialScale = parameters.scale.toFloat()

        val scaleAnimator = ValueAnimator.ofFloat(initialScale, scale)
        scaleAnimator.addUpdateListener { animator ->
            val s = animator.animatedValue as Float
            updateScale(s.toDouble(), pivotX, pivotY, invalidate = true)
        }

        startScaleAnimator(scaleAnimator) { cancelled ->
            if (!cancelled) {
                updateScale(scale.toDouble(), pivotX, pivotY, invalidate = true)
            }
            notifyScaleAnimationCompleted()

            if (!cancelled) {
                // scaling around a pivot can leave the bitmap beyond its scroll limits
                smoothScrollBy(0.0f, 0.0f, skipIfNotNeeded = true)
            }
        }
    }

    /**
     * Animates scale up to provided value while rotation and translation are animated back to
     * their initial values, so that a single continuous movement is obtained.
     * This is used when view returns to its minimum scale because of a double tap, and it is the
     * only path that notifies [rotateAndTranslateAnimationCompletedListener].
     *
     * @param scale scale to animate to.
     */
    private fun smoothResetTo(scale: Float) {
        cancelAllAnimators()

        getTransformationParameters(paramsMatrix, parameters)
        val initValues = floatArrayOf(
            parameters.scale.toFloat(),
            parameters.rotationAngle.toFloat(),
            parameters.horizontalTranslation.toFloat(),
            parameters.verticalTranslation.toFloat()
        )
        val endValues = floatArrayOf(scale, 0.0f, 0.0f, 0.0f)

        val scaleAnimator = ValueAnimator.ofObject(FloatArrayEvaluator(), initValues, endValues)
        scaleAnimator.addUpdateListener { animator ->
            val value = animator.animatedValue as FloatArray
            updateAllParameters(value[0], value[1], value[2], value[3])
        }

        startScaleAnimator(scaleAnimator) { cancelled ->
            if (!cancelled) {
                updateAllParameters(scale, 0.0f, 0.0f, 0.0f)
            }
            notifyScaleAnimationCompleted()

            if (!cancelled) {
                // rotation and translation have also been animated
                rotateAndTranslateAnimationCompletedListener
                    ?.onRotateAndTranslateAnimationCompleted(this)
            }
        }
    }

    /**
     * Configures and starts provided animator as the scale animator of this view.
     *
     * @param animator animator to be started.
     * @param onEnd called when animation ends, receiving true if animation was cancelled or
     * false if it reached its end.
     */
    private fun startScaleAnimator(animator: ValueAnimator, onEnd: (cancelled: Boolean) -> Unit) {
        this.scaleAnimator = animator

        animator.setTarget(this)
        animator.duration = animationDurationMillis
        animator.interpolator = DecelerateInterpolator()

        animator.addListener(object : Animator.AnimatorListener {
            var cancelled = false

            override fun onAnimationEnd(animation: Animator) {
                updateImageLevels(false)
                onEnd(cancelled)
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

        animator.start()
    }

    /**
     * Notifies listeners when a scale animation finishes.
     */
    private fun notifyScaleAnimationCompleted() {
        getTransformationParameters(paramsMatrix, parameters)
        val currentScale = parameters.scale

        if (currentScale <= minScale) {
            minScaleReachedListener?.onMinScaleReached(this)
        }
        if (currentScale >= maxScale) {
            maxScaleReachedListener?.onMaxScaleReached(this)
        }
        scaleAnimationCompletedListener?.onScaleAnimationCompleted(this)
    }

    /**
     * Sets all parameters of the transformation applied respect the base matrix and requests
     * a redraw.
     *
     * @param scale scale to be set.
     * @param rotationAngle rotation angle to be set expressed in radians.
     * @param tx x coordinate of translation to be set.
     * @param ty y coordinate of translation to be set.
     */
    private fun updateAllParameters(scale: Float, rotationAngle: Float, tx: Float, ty: Float) {
        parameters.scale = scale.toDouble()
        parameters.rotationAngle = rotationAngle.toDouble()
        parameters.horizontalTranslation = tx.toDouble()
        parameters.verticalTranslation = ty.toDouble()

        setTransformationParameters(parameters, paramsMatrix)
        updateDisplayMatrix()
        invalidate()
    }

    /**
     * Indicates whether any animator is currently running.
     *
     * @return true if at least one animator is running, false otherwise.
     */
    private fun isAnyAnimatorRunning(): Boolean {
        return translateAnimator?.isRunning == true || scaleAnimator?.isRunning == true
                || rotateAnimator?.isRunning == true
    }

    /**
     * Cancels all animators if any of them is running.
     */
    private fun cancelAllAnimators() {
        cancelTranslateAnimator()
        cancelScaleAnimator()
        cancelRotateAnimator()
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
     * Computes base matrix for provided image size and display type.
     *
     * @param imageWidth upright image width expressed in image pixels.
     * @param imageHeight upright image height expressed in image pixels.
     * @param displayType display type determining how the bitmap is initially scaled and aligned.
     * @param result instance where computed base matrix will be stored.
     */
    private fun computeBaseMatrix(
        imageWidth: Int,
        imageHeight: Int,
        displayType: DisplayType,
        result: Matrix
    ) {
        val viewWidth = if (this.width > 0) this.width.toFloat() else this.measuredWidth.toFloat()
        val viewHeight =
            if (this.height > 0) this.height.toFloat() else this.measuredHeight.toFloat()

        val bitmapWidth = imageWidth.toFloat()
        val bitmapHeight = imageHeight.toFloat()

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

            regionDecodingEnabled = getBoolean(
                R.styleable.GestureBitmapView_regionDecodingEnabled,
                regionDecodingEnabled
            )
            hardwareBitmapsEnabled = getBoolean(
                R.styleable.GestureBitmapView_hardwareBitmapsEnabled,
                hardwareBitmapsEnabled
            )
            if (hasValue(R.styleable.GestureBitmapView_cachedLevelsMemoryBudget)) {
                cachedLevelsMemoryBudget = getInt(
                    R.styleable.GestureBitmapView_cachedLevelsMemoryBudget,
                    0
                ).toLong()
            }

            val srcResId = getResourceId(R.styleable.GestureBitmapView_src, 0)
            val regionDecoded = srcResId != 0 && regionDecodingEnabled
                    && ImageSources.isRegionDecodable(this@GestureBitmapView.resources, srcResId)
            var sourceSet = false
            if (regionDecoded) {
                try {
                    setImageResource(srcResId)
                    sourceSet = true
                } catch (_: IOException) {
                    // falls back to decoding the whole drawable
                }
            }
            if (!sourceSet) {
                val drawable = getDrawable(R.styleable.GestureBitmapView_src)
                setDrawable(drawable)
            }

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

            scrollTensionEnabled = getBoolean(
                R.styleable.GestureBitmapView_scrollTensionEnabled,
                scrollTensionEnabled
            )
            scrollTension = getFloat(R.styleable.GestureBitmapView_scrollTension, scrollTension)

            rotationFlingEnabled = getBoolean(
                R.styleable.GestureBitmapView_rotationFlingEnabled,
                rotationFlingEnabled
            )

        }
    }

    init {
        bitmapPaint.isAntiAlias = true
        bitmapPaint.isFilterBitmap = true

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

                    updateRotation(newRotation, pivotX, pivotY)

                    // rotation has resumed, a previous fling no longer applies
                    rotationFlingPending = false

                    return true
                }

                override fun onRotationEnd(
                    gestureDetector: RotationGestureDetector,
                    angularVelocity: Float,
                    pivotX: Float,
                    pivotY: Float
                ) {
                    if (rotationEnabled && rotationFlingEnabled
                        && abs(angularVelocity) >= MIN_ROTATION_FLING_VELOCITY
                    ) {
                        // fling starts when the last finger is lifted (see onTouchEvent), because
                        // the finger that is still touching the screen would cancel it
                        val time = animationDurationMillis / 1000.0f
                        pendingRotationFlingAngle =
                            angularVelocity * time / FLING_INITIAL_SPEED_FACTOR
                        pendingRotationFlingPivotX = pivotX
                        pendingRotationFlingPivotY = pivotY
                        pendingRotationFlingTime = currentTouchEvent?.eventTime ?: 0L
                        rotationFlingPending = true
                    } else {
                        rotationFlingPending = false
                    }
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

                        getTransformationParameters(paramsMatrix, parameters)
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
                        smoothScaleTo(targetScale, pivotX, pivotY, resetRotation = true)
                    }

                    // notify double tap
                    doubleTapListener?.onDoubleTap(this@GestureBitmapView)

                    return super.onDoubleTap(e)
                }


                /**
                 * Starts an animated scroll that continues the movement of a fling gesture.
                 *
                 * @param e1 first down motion event that started the fling.
                 * @param e2 move motion event that triggered the current fling.
                 * @param velocityX horizontal velocity of the fling expressed in pixels per second.
                 * @param velocityY vertical velocity of the fling expressed in pixels per second.
                 * @return true if the fling has been handled, false otherwise.
                 */
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
                    // scroll is animated with a decelerate interpolator, whose initial speed
                    // is twice the average one. Hence, the distance below makes the animation
                    // start at the velocity of the finger
                    val time = animationDurationMillis.toFloat() / 1000.0f
                    val diffX = velocityX * time / FLING_INITIAL_SPEED_FACTOR
                    val diffY = velocityY * time / FLING_INITIAL_SPEED_FACTOR

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

    /**
     * Default values and constants shared by [GestureBitmapView] instances.
     */
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
         * Tolerance used when checking whether current scale is within minimum and maximum scale
         * limits, to avoid correcting scales that only differ from the limits because of
         * single precision rounding errors accumulated by the transformation matrix.
         */
        private const val SCALE_TOLERANCE = 1.0e-4

        /**
         * Value of [cachedLevelsMemoryBudget] meaning that the budget is 4 times the number of
         * pixels of the view, 4 bytes each.
         */
        const val DEFAULT_CACHED_LEVELS_MEMORY_BUDGET = Long.MAX_VALUE

        /**
         * Maximum bitmap size assumed until the canvas reports its own while drawing.
         */
        private const val DEFAULT_MAX_BITMAP_SIZE = 4096

        /**
         * Number of bytes of each pixel used by the default memory budget.
         */
        private const val BYTES_PER_PIXEL = 4L

        /**
         * Number of view sized images used by the default memory budget.
         */
        private const val DEFAULT_BUDGET_VIEW_COUNT = 4L

        /**
         * Default amount of tension applied when overscrolling (linear resistance).
         */
        const val DEFAULT_SCROLL_TENSION = 1.0f

        /**
         * Ratio between initial and average speed of the interpolator used to animate scroll.
         */
        private const val FLING_INITIAL_SPEED_FACTOR = 2.0f

        /**
         * Minimum angular velocity (in radians per second) required to start a rotation fling.
         */
        private const val MIN_ROTATION_FLING_VELOCITY = 0.5f

        /**
         * Maximum time in milliseconds between the end of a rotation (one of its fingers being
         * lifted) and the last finger being lifted for the rotation fling to start. If the
         * remaining finger stays longer on the screen, no rotation fling is made.
         */
        private const val ROTATION_FLING_MAX_RELEASE_DELAY_MILLIS = 150L

        /**
         * Minimum scroll correction expressed in pixels that is worth animating when a gesture
         * finishes.
         */
        private const val MIN_SCROLL_CORRECTION = 0.5f

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

    /**
     * Interface defining events raised while an image set through an [ImageSource] is loaded.
     */
    interface OnImageLoadListener {
        /**
         * Called when the preview (lowest resolution) of the image has been decoded.
         *
         * @param view view that raised the event.
         */
        fun onPreviewLoaded(view: GestureBitmapView)

        /**
         * Called when a more detailed resolution level, or a detail patch, has been decoded.
         *
         * @param view view that raised the event.
         */
        fun onDetailLoaded(view: GestureBitmapView)

        /**
         * Called when decoding the image failed.
         *
         * @param view view that raised the event.
         * @param error cause of the failure, when known.
         */
        fun onImageLoadFailed(view: GestureBitmapView, error: Throwable?)
    }

    /**
     * Interface to draw on top of the displayed image.
     */
    interface ImageOverlayDrawer {
        /**
         * Called after the image has been drawn. The canvas is expressed in upright image pixels
         * and clipped to the image rectangle.
         *
         * @param view view being drawn.
         * @param canvas canvas to draw on.
         */
        fun onDrawOverImage(view: GestureBitmapView, canvas: Canvas)
    }
}
