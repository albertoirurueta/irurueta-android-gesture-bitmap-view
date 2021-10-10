package com.irurueta.android.gesturebitmap

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.util.AttributeSet
import android.view.*
import androidx.core.graphics.drawable.toBitmap
import androidx.test.core.app.ApplicationProvider
import com.irurueta.statistics.UniformRandomizer
import io.mockk.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
class GestureBitmapViewTest {

    @Test
    fun constants_haveExpectedDefaultValues() {
        assertEquals(200L, GestureBitmapView.DEFAULT_ANIMATION_DURATION_MILLIS)
        assertEquals(1.0f, GestureBitmapView.DEFAULT_MIN_SCALE, 0.0f)
        assertEquals(10.0f, GestureBitmapView.DEFAULT_MAX_SCALE, 0.0f)
        assertEquals(3.0f, GestureBitmapView.DEFAULT_SCALE_FACTOR_JUMP, 0.0f)
        assertEquals(0.1f, GestureBitmapView.DEFAULT_MIN_SCALE_MARGIN, 0.0f)
        assertEquals(1.0f, GestureBitmapView.DEFAULT_MAX_SCALE_MARGIN, 0.0f)
        assertEquals(100.0f, GestureBitmapView.DEFAULT_SCROLL_MARGIN, 0.0f)
    }

    @Test
    fun constructor_initializesDefaultValues() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        assertEquals(
            GestureBitmapView.DEFAULT_ANIMATION_DURATION_MILLIS,
            view.animationDurationMillis
        )
        assertNull(view.topBoundReachedListener)
        assertNull(view.bottomBoundReachedListener)
        assertNull(view.leftBoundReachedListener)
        assertNull(view.rightBoundReachedListener)
        assertNull(view.minScaleReachedListener)
        assertNull(view.maxScaleReachedListener)
        assertNull(view.rotateAndTranslateAnimationCompletedListener)
        assertNull(view.scaleAnimationCompletedListener)
        assertNull(view.scrollAnimationCompletedListener)
        assertNull(view.doubleTapListener)
        assertTrue(view.rotationEnabled)
        assertTrue(view.scaleEnabled)
        assertTrue(view.scrollEnabled)
        assertTrue(view.twoFingerScrollEnabled)
        assertFalse(view.exclusiveTwoFingerScrollEnabled)
        assertTrue(view.doubleTapEnabled)
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        assertEquals(GestureBitmapView.DEFAULT_MIN_SCALE, view.minScale, 0.0f)
        assertEquals(GestureBitmapView.DEFAULT_MAX_SCALE, view.maxScale, 0.0f)
        assertEquals(GestureBitmapView.DEFAULT_SCALE_FACTOR_JUMP, view.scaleFactorJump, 0.0f)
        assertEquals(GestureBitmapView.DEFAULT_MIN_SCALE_MARGIN, view.minScaleMargin, 0.0f)
        assertEquals(GestureBitmapView.DEFAULT_MAX_SCALE_MARGIN, view.maxScaleMargin, 0.0f)
        assertEquals(GestureBitmapView.DEFAULT_SCROLL_MARGIN, view.leftScrollMargin, 0.0f)
        assertEquals(GestureBitmapView.DEFAULT_SCROLL_MARGIN, view.topScrollMargin, 0.0f)
        assertEquals(GestureBitmapView.DEFAULT_SCROLL_MARGIN, view.rightScrollMargin, 0.0f)
        assertEquals(GestureBitmapView.DEFAULT_SCROLL_MARGIN, view.bottomScrollMargin, 0.0f)

        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        val identity = Matrix()
        assertEquals(identity, view.transformationMatrix)

        val baseTransformationParameters = view.baseTransformationParameters
        assertEquals(1.0, baseTransformationParameters.scale, 0.0)
        assertEquals(0.0, baseTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, baseTransformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, baseTransformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.baseTransformationMatrix)

        val displayTransformationParameters = view.displayTransformationParameters
        assertEquals(1.0, displayTransformationParameters.scale, 0.0)
        assertEquals(0.0, displayTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, displayTransformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, displayTransformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.displayTransformationMatrix)

        assertNull(view.displayRect)
    }

    @Test
    fun constructor_withAttributeSet_initializesWithDifferentParameters() {
        val typedArray = mockk<TypedArray>()
        every { typedArray.indexCount }.returns(0)

        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)
        justRun { bitmap.recycle() }

        val drawable = mockk<Drawable>()
        every { drawable.intrinsicWidth }.returns(320)
        every { drawable.intrinsicHeight }.returns(240)

        // mock extension function toBitmap
        mockkStatic("androidx.core.graphics.drawable.DrawableKt")
        every { drawable.toBitmap(any(), any(), any()) }.returns(bitmap)

        every { typedArray.getDrawable(R.styleable.GestureBitmapView_src) }.returns(drawable)
        every { typedArray.getInt(R.styleable.GestureBitmapView_displayType, any()) }.returns(
            GestureBitmapView.DisplayType.NONE.ordinal
        )
        every {
            typedArray.getInt(
                R.styleable.GestureBitmapView_animationDurationMillis,
                any()
            )
        }.returns(3000)
        every {
            typedArray.getBoolean(
                R.styleable.GestureBitmapView_rotationEnabled,
                any()
            )
        }.returns(false)
        every {
            typedArray.getBoolean(
                R.styleable.GestureBitmapView_scaleEnabled,
                any()
            )
        }.returns(false)
        every {
            typedArray.getBoolean(
                R.styleable.GestureBitmapView_scrollEnabled,
                any()
            )
        }.returns(false)
        every {
            typedArray.getBoolean(
                R.styleable.GestureBitmapView_twoFingerScrollEnabled,
                any()
            )
        }.returns(false)
        every {
            typedArray.getBoolean(
                R.styleable.GestureBitmapView_exclusiveTwoFingerScrollEnabled,
                any()
            )
        }.returns(true)
        every {
            typedArray.getBoolean(
                R.styleable.GestureBitmapView_doubleTapEnabled,
                any()
            )
        }.returns(false)
        every {
            typedArray.getFloat(
                R.styleable.GestureBitmapView_minScale,
                any()
            )
        }.returns(0.5f)
        every {
            typedArray.getFloat(
                R.styleable.GestureBitmapView_maxScale,
                any()
            )
        }.returns(5.0f)
        every {
            typedArray.getFloat(
                R.styleable.GestureBitmapView_scaleFactorJump,
                any()
            )
        }.returns(2.0f)
        every {
            typedArray.getFloat(
                R.styleable.GestureBitmapView_minScaleMargin,
                any()
            )
        }.returns(0.2f)
        every {
            typedArray.getFloat(
                R.styleable.GestureBitmapView_maxScaleMargin,
                any()
            )
        }.returns(2.0f)
        every {
            typedArray.getFloat(
                R.styleable.GestureBitmapView_leftScrollMargin,
                any()
            )
        }.returns(125.0f)
        every {
            typedArray.getFloat(
                R.styleable.GestureBitmapView_topScrollMargin,
                any()
            )
        }.returns(150.0f)
        every {
            typedArray.getFloat(
                R.styleable.GestureBitmapView_rightScrollMargin,
                any()
            )
        }.returns(175.0f)
        every {
            typedArray.getFloat(
                R.styleable.GestureBitmapView_bottomScrollMargin,
                any()
            )
        }.returns(200.0f)

        justRun { typedArray.recycle() }

        val context = spyk(ApplicationProvider.getApplicationContext())
        every { context.obtainStyledAttributes(any(), any(), any(), any()) }.returns(typedArray)

        val attrs = mockk<AttributeSet>()
        val view = GestureBitmapView(context, attrs)

        // check
        assertSame(bitmap, view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.NONE, view.displayType)
        assertEquals(3000, view.animationDurationMillis)
        assertFalse(view.rotationEnabled)
        assertFalse(view.scrollEnabled)
        assertFalse(view.twoFingerScrollEnabled)
        assertTrue(view.exclusiveTwoFingerScrollEnabled)
        assertEquals(0.5f, view.minScale, 0.0f)
        assertEquals(5.0f, view.maxScale, 0.0f)
        assertEquals(2.0f, view.scaleFactorJump, 0.0f)
        assertEquals(0.2f, view.minScaleMargin, 0.0f)
        assertEquals(2.0f, view.maxScaleMargin, 0.0f)
        assertEquals(125.0f, view.leftScrollMargin, 0.0f)
        assertEquals(150.0f, view.topScrollMargin, 0.0f)
        assertEquals(175.0f, view.rightScrollMargin, 0.0f)
        assertEquals(200.0f, view.bottomScrollMargin, 0.0f)
        verify(exactly = 2) { typedArray.recycle() }
    }

    @Test
    fun animationDurationMillis_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertEquals(
            GestureBitmapView.DEFAULT_ANIMATION_DURATION_MILLIS,
            view.animationDurationMillis
        )

        // set new value
        view.animationDurationMillis = 2 * GestureBitmapView.DEFAULT_ANIMATION_DURATION_MILLIS

        // check
        assertEquals(
            2 * GestureBitmapView.DEFAULT_ANIMATION_DURATION_MILLIS,
            view.animationDurationMillis
        )
    }

    @Test
    fun topBoundReachedListener_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertNull(view.topBoundReachedListener)

        // set new value
        val listener = mockk<GestureBitmapView.OnTopBoundReachedListener>()
        view.topBoundReachedListener = listener

        // check
        assertSame(listener, view.topBoundReachedListener)
    }

    @Test
    fun bottomBoundReachedListener_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertNull(view.bottomBoundReachedListener)

        // set new value
        val listener = mockk<GestureBitmapView.OnBottomBoundReachedListener>()
        view.bottomBoundReachedListener = listener

        // check
        assertSame(listener, view.bottomBoundReachedListener)
    }

    @Test
    fun leftBoundReachedListener_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertNull(view.leftBoundReachedListener)

        // set new value
        val listener = mockk<GestureBitmapView.OnLeftBoundReachedListener>()
        view.leftBoundReachedListener = listener

        // check
        assertSame(listener, view.leftBoundReachedListener)
    }

    @Test
    fun rightBoundReachedListener_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertNull(view.rightBoundReachedListener)

        // set new value
        val listener = mockk<GestureBitmapView.OnRightBoundReachedListener>()
        view.rightBoundReachedListener = listener

        // check
        assertSame(listener, view.rightBoundReachedListener)
    }

    @Test
    fun minScaleReachedListener_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertNull(view.minScaleReachedListener)

        // set new value
        val listener = mockk<GestureBitmapView.OnMinScaleReachedListener>()
        view.minScaleReachedListener = listener

        // check
        assertSame(listener, view.minScaleReachedListener)
    }

    @Test
    fun maxScaleReachedListener_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertNull(view.maxScaleReachedListener)

        // set new value
        val listener = mockk<GestureBitmapView.OnMaxScaleReachedListener>()
        view.maxScaleReachedListener = listener

        // check
        assertSame(listener, view.maxScaleReachedListener)
    }

    @Test
    fun rotateAndTranslateAnimationCompletedListener_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertNull(view.rotateAndTranslateAnimationCompletedListener)

        // set new value
        val listener = mockk<GestureBitmapView.OnRotateAndTranslateAnimationCompletedListener>()
        view.rotateAndTranslateAnimationCompletedListener = listener

        // check
        assertSame(listener, view.rotateAndTranslateAnimationCompletedListener)
    }

    @Test
    fun scaleAnimationCompletedListener_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertNull(view.scaleAnimationCompletedListener)

        // set new value
        val listener = mockk<GestureBitmapView.OnScaleAnimationCompletedListener>()
        view.scaleAnimationCompletedListener = listener

        // check
        assertSame(listener, view.scaleAnimationCompletedListener)
    }

    @Test
    fun scrollAnimationCompletedListener_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertNull(view.scrollAnimationCompletedListener)

        // set new value
        val listener = mockk<GestureBitmapView.OnScrollAnimationCompletedListener>()
        view.scrollAnimationCompletedListener = listener

        // check
        assertSame(listener, view.scrollAnimationCompletedListener)
    }

    @Test
    fun doubleTapListener_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertNull(view.doubleTapListener)

        // set new value
        val listener = mockk<GestureBitmapView.OnDoubleTapListener>()
        view.doubleTapListener = listener

        // check
        assertSame(listener, view.doubleTapListener)
    }

    @Test
    fun rotationEnabled_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertTrue(view.rotationEnabled)

        // set new value
        view.rotationEnabled = false

        // check
        assertFalse(view.rotationEnabled)
    }

    @Test
    fun scaleEnabled_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertTrue(view.scaleEnabled)

        // set new value
        view.scaleEnabled = false

        // check
        assertFalse(view.scaleEnabled)
    }

    @Test
    fun scrollEnabled_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertTrue(view.scrollEnabled)

        // set new value
        view.scrollEnabled = false

        // check
        assertFalse(view.scrollEnabled)
    }

    @Test
    fun twoFingerScrollEnabled_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertTrue(view.twoFingerScrollEnabled)

        // set new value
        view.twoFingerScrollEnabled = false

        // check
        assertFalse(view.twoFingerScrollEnabled)
    }

    @Test
    fun exclusiveTwoFingerScrollEnabled_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertFalse(view.exclusiveTwoFingerScrollEnabled)

        // set new value
        view.exclusiveTwoFingerScrollEnabled = true

        // check
        assertTrue(view.exclusiveTwoFingerScrollEnabled)
    }

    @Test
    fun doubleTapEnabled_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertTrue(view.doubleTapEnabled)

        // set new value
        view.doubleTapEnabled = false

        // check
        assertFalse(view.doubleTapEnabled)
    }

    @Test
    fun bitmap_whenNoPreviousBitmap_returnsAndSetsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set new value
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)

        view.bitmap = bitmap

        // check
        assertSame(bitmap, view.bitmap)

        // base transformation matrix has been computed
        val baseTransformationParameters = view.baseTransformationParameters
        assertNotEquals(1.0, baseTransformationParameters.scale, 0.0)
        assertEquals(0.0, baseTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, baseTransformationParameters.horizontalTranslation, 0.0)
        assertNotEquals(0.0, baseTransformationParameters.verticalTranslation, 0.0)

        assertNotEquals(identity, view.baseTransformationMatrix)

        // transformation parameters are reset
        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.transformationMatrix)

        // display transformation parameters have changed because base transformation matrix is
        // no longer the identity
        val displayTransformationParameters = view.displayTransformationParameters
        assertNotEquals(1.0, displayTransformationParameters.scale, 0.0)
        assertEquals(0.0, displayTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, displayTransformationParameters.horizontalTranslation, 0.0)
        assertNotEquals(0.0, displayTransformationParameters.verticalTranslation, 0.0)

        assertNotEquals(identity, view.displayTransformationMatrix)

        // now display rect is defined
        assertNotNull(view.displayRect)
    }

    @Test
    fun bitmap_whenDisplayTypeNone_returnsAndSetsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set display type none
        view.displayType = GestureBitmapView.DisplayType.NONE

        // check
        assertEquals(GestureBitmapView.DisplayType.NONE, view.displayType)

        // set new value
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)

        view.bitmap = bitmap

        // check
        assertSame(bitmap, view.bitmap)

        // base transformation matrix has been computed as the identity
        val baseTransformationParameters = view.baseTransformationParameters
        assertEquals(1.0, baseTransformationParameters.scale, 0.0)
        assertEquals(0.0, baseTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, baseTransformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, baseTransformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.baseTransformationMatrix)

        // transformation parameters are reset
        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.transformationMatrix)

        // display transformation parameters have not changed because base transformation matrix is
        // set to the identity
        val displayTransformationParameters = view.displayTransformationParameters
        assertEquals(1.0, displayTransformationParameters.scale, 0.0)
        assertEquals(0.0, displayTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, displayTransformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, displayTransformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.displayTransformationMatrix)

        // now display rect is defined
        assertNotNull(view.displayRect)
    }

    @Test
    fun bitmap_whenDisplayTypeFitIfBiggerAndImageDoesNotFit_returnsAndSetsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set new display type
        view.displayType = GestureBitmapView.DisplayType.FIT_IF_BIGGER

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_IF_BIGGER, view.displayType)

        // set new value
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(1280)
        every { bitmap.height }.returns(960)

        view.bitmap = bitmap

        // check
        assertSame(bitmap, view.bitmap)

        // base transformation matrix has been computed
        val baseTransformationParameters = view.baseTransformationParameters
        assertTrue(baseTransformationParameters.scale < 1.0)
        assertEquals(0.0, baseTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, baseTransformationParameters.horizontalTranslation, 0.0)
        assertTrue(baseTransformationParameters.verticalTranslation > 0.0)

        assertNotEquals(identity, view.baseTransformationMatrix)

        // transformation parameters are reset
        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.transformationMatrix)

        // display transformation parameters have not changed because base transformation matrix is
        // set to the identity
        val displayTransformationParameters = view.displayTransformationParameters
        assertTrue(displayTransformationParameters.scale < 1.0)
        assertEquals(0.0, displayTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, displayTransformationParameters.horizontalTranslation, 0.0)
        assertTrue(displayTransformationParameters.verticalTranslation > 0.0)

        assertNotEquals(identity, view.displayTransformationMatrix)

        // now display rect is defined
        assertNotNull(view.displayRect)
    }

    @Test
    fun bitmap_whenDisplayTypeFitIfBiggerAndImageFits_returnsAndSetsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set new display type
        view.displayType = GestureBitmapView.DisplayType.FIT_IF_BIGGER

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_IF_BIGGER, view.displayType)

        // set new value
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)

        view.bitmap = bitmap

        // check
        assertSame(bitmap, view.bitmap)

        // base transformation matrix has been computed as the identity
        val baseTransformationParameters = view.baseTransformationParameters
        assertEquals(1.0, baseTransformationParameters.scale, 0.0)
        assertEquals(0.0, baseTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, baseTransformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, baseTransformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.baseTransformationMatrix)

        // transformation parameters are reset
        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.transformationMatrix)

        // display transformation parameters have not changed because base transformation matrix is
        // set to the identity
        val displayTransformationParameters = view.displayTransformationParameters
        assertEquals(1.0, displayTransformationParameters.scale, 0.0)
        assertEquals(0.0, displayTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, displayTransformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, displayTransformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.displayTransformationMatrix)

        // now display rect is defined
        assertNotNull(view.displayRect)
    }

    @Test
    fun bitmap_whenDisplayTypeFitXTop_returnsAndSetsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set new display type
        view.displayType = GestureBitmapView.DisplayType.FIT_X_TOP

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_X_TOP, view.displayType)

        // set new value
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)

        view.bitmap = bitmap

        // check
        assertSame(bitmap, view.bitmap)

        // base transformation matrix has been computed
        val baseTransformationParameters = view.baseTransformationParameters
        assertTrue(baseTransformationParameters.scale > 1.0)
        assertEquals(0.0, baseTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, baseTransformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, baseTransformationParameters.verticalTranslation, 0.0)

        assertNotEquals(identity, view.baseTransformationMatrix)

        // transformation parameters are reset
        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.transformationMatrix)

        // display transformation parameters have not changed because base transformation matrix is
        // set to the identity
        val displayTransformationParameters = view.displayTransformationParameters
        assertTrue(displayTransformationParameters.scale > 1.0)
        assertEquals(0.0, displayTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, displayTransformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, displayTransformationParameters.verticalTranslation, 0.0)

        assertNotEquals(identity, view.displayTransformationMatrix)

        // now display rect is defined
        assertNotNull(view.displayRect)
    }

    @Test
    fun bitmap_whenDisplayTypeFitXBottom_returnsAndSetsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set new display type
        view.displayType = GestureBitmapView.DisplayType.FIT_X_BOTTOM

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_X_BOTTOM, view.displayType)

        // set new value
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)

        view.bitmap = bitmap

        // check
        assertSame(bitmap, view.bitmap)

        // base transformation matrix has been computed
        val baseTransformationParameters = view.baseTransformationParameters
        assertTrue(baseTransformationParameters.scale > 1.0)
        assertEquals(0.0, baseTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, baseTransformationParameters.horizontalTranslation, 0.0)
        assertTrue(baseTransformationParameters.verticalTranslation > 0.0)

        assertNotEquals(identity, view.baseTransformationMatrix)

        // transformation parameters are reset
        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.transformationMatrix)

        // display transformation parameters have not changed because base transformation matrix is
        // set to the identity
        val displayTransformationParameters = view.displayTransformationParameters
        assertTrue(displayTransformationParameters.scale > 1.0)
        assertEquals(0.0, displayTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, displayTransformationParameters.horizontalTranslation, 0.0)
        assertTrue(displayTransformationParameters.verticalTranslation > 0.0)

        assertNotEquals(identity, view.displayTransformationMatrix)

        // now display rect is defined
        assertNotNull(view.displayRect)
    }

    @Test
    fun bitmap_whenDisplayTypeFitXCenter_returnsAndSetsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set new value
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)

        view.bitmap = bitmap

        // check
        assertSame(bitmap, view.bitmap)

        // base transformation matrix has been computed
        val baseTransformationParameters = view.baseTransformationParameters
        assertTrue(baseTransformationParameters.scale > 1.0)
        assertEquals(0.0, baseTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, baseTransformationParameters.horizontalTranslation, 0.0)
        assertTrue(baseTransformationParameters.verticalTranslation > 0.0)

        assertNotEquals(identity, view.baseTransformationMatrix)

        // transformation parameters are reset
        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.transformationMatrix)

        // display transformation parameters have not changed because base transformation matrix is
        // set to the identity
        val displayTransformationParameters = view.displayTransformationParameters
        assertTrue(displayTransformationParameters.scale > 1.0)
        assertEquals(0.0, displayTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, displayTransformationParameters.horizontalTranslation, 0.0)
        assertTrue(displayTransformationParameters.verticalTranslation > 0.0)

        assertNotEquals(identity, view.displayTransformationMatrix)

        // now display rect is defined
        assertNotNull(view.displayRect)
    }

    @Test
    fun bitmap_whenDisplayTypeFitYLeft_returnsAndSetsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set new display type
        view.displayType = GestureBitmapView.DisplayType.FIT_Y_LEFT

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_Y_LEFT, view.displayType)

        // set new value
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)

        view.bitmap = bitmap

        // check
        assertSame(bitmap, view.bitmap)

        // base transformation matrix has been computed
        val baseTransformationParameters = view.baseTransformationParameters
        assertTrue(baseTransformationParameters.scale > 1.0)
        assertEquals(0.0, baseTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, baseTransformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, baseTransformationParameters.verticalTranslation, 0.0)

        assertNotEquals(identity, view.baseTransformationMatrix)

        // transformation parameters are reset
        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.transformationMatrix)

        // display transformation parameters have not changed because base transformation matrix is
        // set to the identity
        val displayTransformationParameters = view.displayTransformationParameters
        assertTrue(displayTransformationParameters.scale > 1.0)
        assertEquals(0.0, displayTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, displayTransformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, displayTransformationParameters.verticalTranslation, 0.0)

        assertNotEquals(identity, view.displayTransformationMatrix)

        // now display rect is defined
        assertNotNull(view.displayRect)
    }

    @Test
    fun bitmap_whenDisplayTypeFitYRight_returnsAndSetsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set new display type
        view.displayType = GestureBitmapView.DisplayType.FIT_Y_RIGHT

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_Y_RIGHT, view.displayType)

        // set new value
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)

        view.bitmap = bitmap

        // check
        assertSame(bitmap, view.bitmap)

        // base transformation matrix has been computed
        val baseTransformationParameters = view.baseTransformationParameters
        assertTrue(baseTransformationParameters.scale > 1.0)
        assertEquals(0.0, baseTransformationParameters.rotationAngle, 0.0)
        assertTrue(baseTransformationParameters.horizontalTranslation < 0.0)
        assertEquals(0.0, baseTransformationParameters.verticalTranslation, 0.0)

        assertNotEquals(identity, view.baseTransformationMatrix)

        // transformation parameters are reset
        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.transformationMatrix)

        // display transformation parameters have not changed because base transformation matrix is
        // set to the identity
        val displayTransformationParameters = view.displayTransformationParameters
        assertTrue(displayTransformationParameters.scale > 1.0)
        assertEquals(0.0, displayTransformationParameters.rotationAngle, 0.0)
        assertTrue(displayTransformationParameters.horizontalTranslation < 0.0)
        assertEquals(0.0, displayTransformationParameters.verticalTranslation, 0.0)

        assertNotEquals(identity, view.displayTransformationMatrix)

        // now display rect is defined
        assertNotNull(view.displayRect)
    }

    @Test
    fun bitmap_whenDisplayTypeFitYCenter_returnsAndSetsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set new display type
        view.displayType = GestureBitmapView.DisplayType.FIT_Y_CENTER

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_Y_CENTER, view.displayType)

        // set new value
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)

        view.bitmap = bitmap

        // check
        assertSame(bitmap, view.bitmap)

        // base transformation matrix has been computed
        val baseTransformationParameters = view.baseTransformationParameters
        assertTrue(baseTransformationParameters.scale > 1.0)
        assertEquals(0.0, baseTransformationParameters.rotationAngle, 0.0)
        assertTrue(baseTransformationParameters.horizontalTranslation < 0.0)
        assertEquals(0.0, baseTransformationParameters.verticalTranslation, 0.0)

        assertNotEquals(identity, view.baseTransformationMatrix)

        // transformation parameters are reset
        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.transformationMatrix)

        // display transformation parameters have not changed because base transformation matrix is
        // set to the identity
        val displayTransformationParameters = view.displayTransformationParameters
        assertTrue(displayTransformationParameters.scale > 1.0)
        assertEquals(0.0, displayTransformationParameters.rotationAngle, 0.0)
        assertTrue(displayTransformationParameters.horizontalTranslation < 0.0)
        assertEquals(0.0, displayTransformationParameters.verticalTranslation, 0.0)

        assertNotEquals(identity, view.displayTransformationMatrix)

        // now display rect is defined
        assertNotNull(view.displayRect)
    }

    @Test
    fun bitmap_whenDisplayTypeCenterCrop_returnsAndSetsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set new display type
        view.displayType = GestureBitmapView.DisplayType.CENTER_CROP

        // check
        assertEquals(GestureBitmapView.DisplayType.CENTER_CROP, view.displayType)

        // set new value
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)

        view.bitmap = bitmap

        // check
        assertSame(bitmap, view.bitmap)

        // base transformation matrix has been computed
        val baseTransformationParameters = view.baseTransformationParameters
        assertTrue(baseTransformationParameters.scale > 1.0)
        assertEquals(0.0, baseTransformationParameters.rotationAngle, 0.0)
        assertTrue(baseTransformationParameters.horizontalTranslation < 0.0)
        assertEquals(0.0, baseTransformationParameters.verticalTranslation, 0.0)

        assertNotEquals(identity, view.baseTransformationMatrix)

        // transformation parameters are reset
        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.transformationMatrix)

        // display transformation parameters have not changed because base transformation matrix is
        // set to the identity
        val displayTransformationParameters = view.displayTransformationParameters
        assertTrue(displayTransformationParameters.scale > 1.0)
        assertEquals(0.0, displayTransformationParameters.rotationAngle, 0.0)
        assertTrue(displayTransformationParameters.horizontalTranslation < 0.0)
        assertEquals(0.0, displayTransformationParameters.verticalTranslation, 0.0)

        assertNotEquals(identity, view.displayTransformationMatrix)

        // now display rect is defined
        assertNotNull(view.displayRect)
    }

    @Test
    fun bitmap_whenRecycled_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set new value
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(true)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)

        view.bitmap = bitmap

        // check
        assertNull(view.bitmap)

        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.transformationMatrix)
        assertEquals(identity, view.baseTransformationMatrix)

        val displayTransformationParameters = view.displayTransformationParameters
        assertEquals(1.0, displayTransformationParameters.scale, 0.0)
        assertEquals(0.0, displayTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, displayTransformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, displayTransformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.displayTransformationMatrix)

        assertNull(view.displayRect)
    }

    @Test
    fun bitmap_whenPreviousBitmap_returnsAndSetsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set initial bitmap
        val bitmap1 = mockk<Bitmap>()
        every { bitmap1.isRecycled }.returns(false)
        every { bitmap1.width }.returns(640)
        every { bitmap1.height }.returns(480)
        justRun { bitmap1.recycle() }

        view.bitmap = bitmap1

        // check
        assertSame(bitmap1, view.bitmap)
        verify(exactly = 0) { bitmap1.recycle() }

        // set second bitmap so that 1st one is recycled
        val bitmap2 = mockk<Bitmap>()

        every { bitmap2.isRecycled }.returns(false)
        every { bitmap2.width }.returns(640)
        every { bitmap2.height }.returns(480)

        view.bitmap = bitmap2

        // check
        assertSame(bitmap2, view.bitmap)
        verify(exactly = 1) { bitmap1.recycle() }
    }

    @Test
    fun bitmap_whenNullAfterPreviousBitmap_returnsAndSetsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set initial bitmap
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)
        justRun { bitmap.recycle() }

        view.bitmap = bitmap

        // check
        assertSame(bitmap, view.bitmap)

        // base transformation matrix has been computed
        val baseTransformationMatrix = view.baseTransformationMatrix
        assertNotEquals(identity, baseTransformationMatrix)

        // transformation parameters are reset
        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        val transformationMatrix = view.transformationMatrix
        assertEquals(identity, transformationMatrix)

        // display transformation parameters have changed because base transformation matrix is
        // no longer the identity
        val displayTransformationParameters = view.displayTransformationParameters
        assertNotEquals(1.0, displayTransformationParameters.scale, 0.0)
        assertEquals(0.0, displayTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, displayTransformationParameters.horizontalTranslation, 0.0)
        assertNotEquals(0.0, displayTransformationParameters.verticalTranslation, 0.0)

        val displayTransformationMatrix = view.displayTransformationMatrix
        assertNotEquals(identity, displayTransformationMatrix)

        // now display rect is defined
        val displayRect = view.displayRect
        assertNotNull(displayRect)

        // set second bitmap as null
        view.bitmap = null

        // check
        assertNull(view.bitmap)
        verify(exactly = 1) { bitmap.recycle() }

        // transformation matrices are not modified
        assertEquals(baseTransformationMatrix, view.baseTransformationMatrix)
        assertEquals(transformationParameters, view.transformationParameters)
        assertEquals(transformationMatrix, view.transformationMatrix)
        assertEquals(displayTransformationParameters, view.displayTransformationParameters)
        assertEquals(displayTransformationMatrix, view.displayTransformationMatrix)

        // now display rect is no longer defined
        assertNull(view.displayRect)
    }

    @Test
    fun setBitmap_whenNullAfterPreviousBitmap_returnsAndSetsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set initial bitmap
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)
        justRun { bitmap.recycle() }

        view.bitmap = bitmap

        // check
        assertSame(bitmap, view.bitmap)

        // base transformation matrix has been computed
        val baseTransformationMatrix = view.baseTransformationMatrix
        assertNotEquals(identity, baseTransformationMatrix)

        // transformation parameters are reset
        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        val transformationMatrix = view.transformationMatrix
        assertEquals(identity, transformationMatrix)

        // display transformation parameters have changed because base transformation matrix is
        // no longer the identity
        val displayTransformationParameters = view.displayTransformationParameters
        assertNotEquals(1.0, displayTransformationParameters.scale, 0.0)
        assertEquals(0.0, displayTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, displayTransformationParameters.horizontalTranslation, 0.0)
        assertNotEquals(0.0, displayTransformationParameters.verticalTranslation, 0.0)

        val displayTransformationMatrix = view.displayTransformationMatrix
        assertNotEquals(identity, displayTransformationMatrix)

        // now display rect is defined
        val displayRect = view.displayRect
        assertNotNull(displayRect)

        // set second bitmap as null
        view.setBitmap(null, true)

        // check
        assertNull(view.bitmap)
        verify(exactly = 1) { bitmap.recycle() }

        // transformation matrices are not modified
        assertEquals(baseTransformationMatrix, view.baseTransformationMatrix)
        assertEquals(transformationParameters, view.transformationParameters)
        assertEquals(transformationMatrix, view.transformationMatrix)
        assertEquals(displayTransformationParameters, view.displayTransformationParameters)
        assertEquals(displayTransformationMatrix, view.displayTransformationMatrix)

        // now display rect is no longer defined
        assertNull(view.displayRect)
    }

    @Test
    fun setBitmap_whenMatricesAreNotReset_shouldKeepPreviousTransformation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set initial bitmap
        val bitmap1 = mockk<Bitmap>()
        every { bitmap1.isRecycled }.returns(false)
        every { bitmap1.width }.returns(640)
        every { bitmap1.height }.returns(480)
        justRun { bitmap1.recycle() }

        view.bitmap = bitmap1

        // check
        assertSame(bitmap1, view.bitmap)
        verify(exactly = 0) { bitmap1.recycle() }

        val baseTransformationParameters = view.baseTransformationParameters
        assertTrue(baseTransformationParameters.scale > 1.0)
        assertEquals(0.0, baseTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, baseTransformationParameters.horizontalTranslation, 0.0)
        assertTrue(baseTransformationParameters.verticalTranslation > 0.0)

        val baseTransformationMatrix = view.baseTransformationMatrix
        assertNotEquals(identity, baseTransformationMatrix)

        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        val transformationMatrix = view.transformationMatrix
        assertEquals(identity, transformationMatrix)

        val displayTransformationParameters = view.displayTransformationParameters
        assertTrue(displayTransformationParameters.scale > 1.0)
        assertEquals(0.0, displayTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, displayTransformationParameters.horizontalTranslation, 0.0)
        assertTrue(displayTransformationParameters.verticalTranslation > 0.0)

        val displayTransformationMatrix = view.displayTransformationMatrix
        assertNotEquals(identity, displayTransformationMatrix)

        // set second bitmap without resetting transformation matrices
        val bitmap2 = mockk<Bitmap>()

        every { bitmap2.isRecycled }.returns(false)
        every { bitmap2.width }.returns(480)
        every { bitmap2.height }.returns(640)

        view.setBitmap(bitmap2, false)

        // check
        assertSame(bitmap2, view.bitmap)
        verify(exactly = 1) { bitmap1.recycle() }

        // check transformations haven't been modified
        assertEquals(baseTransformationParameters, view.baseTransformationParameters)
        assertEquals(baseTransformationMatrix, view.baseTransformationMatrix)
        assertEquals(transformationParameters, view.transformationParameters)
        assertEquals(transformationMatrix, view.transformationMatrix)
        assertEquals(displayTransformationParameters, view.displayTransformationParameters)
        assertEquals(displayTransformationMatrix, view.displayTransformationMatrix)
    }

    @Test
    fun setDrawable_setsExpectedBitmap() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set initial bitmap
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)
        justRun { bitmap.recycle() }

        val drawable = mockk<Drawable>()
        every { drawable.intrinsicWidth }.returns(320)
        every { drawable.intrinsicHeight }.returns(240)

        // mock extension function toBitmap
        mockkStatic("androidx.core.graphics.drawable.DrawableKt")
        every { drawable.toBitmap(any(), any(), any()) }.returns(bitmap)

        view.setDrawable(drawable)

        // check
        assertSame(bitmap, view.bitmap)
        verify(exactly = 1) { drawable.toBitmap(320, 240) }
    }

    @Test
    fun displayType_whenBitmapNotAvailable_setsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set display type none
        view.displayType = GestureBitmapView.DisplayType.NONE

        // check
        assertEquals(GestureBitmapView.DisplayType.NONE, view.displayType)
        assertNull(view.bitmap)

        // transformation matrices have not changed
        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.transformationMatrix)

        val baseTransformationParameters = view.baseTransformationParameters
        assertEquals(1.0, baseTransformationParameters.scale, 0.0)
        assertEquals(0.0, baseTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, baseTransformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, baseTransformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.baseTransformationMatrix)

        val displayTransformationParameters = view.displayTransformationParameters
        assertEquals(1.0, displayTransformationParameters.scale, 0.0)
        assertEquals(0.0, displayTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, displayTransformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, displayTransformationParameters.verticalTranslation, 0.0)

        assertEquals(identity, view.displayTransformationMatrix)

        assertNull(view.displayRect)
    }

    @Test
    fun displayType_whenBitmapAvailable_setsExpectedValueAndModifiesTransformationMatrixAndKeepsDisplayMatrix() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set bitmap
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)

        view.bitmap = bitmap

        // check
        assertSame(bitmap, view.bitmap)

        // base transformation matrix has been computed
        val baseTransformationParameters1 = view.baseTransformationParameters
        assertTrue(baseTransformationParameters1.scale > 1.0)
        assertEquals(0.0, baseTransformationParameters1.rotationAngle, 0.0)
        assertEquals(0.0, baseTransformationParameters1.horizontalTranslation, 0.0)
        assertTrue(baseTransformationParameters1.verticalTranslation > 0.0)

        val baseTransformationMatrix1 = view.baseTransformationMatrix
        assertNotEquals(identity, baseTransformationMatrix1)

        // transformation parameters are reset
        val transformationParameters1 = view.transformationParameters
        assertEquals(1.0, transformationParameters1.scale, 0.0)
        assertEquals(0.0, transformationParameters1.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters1.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters1.verticalTranslation, 0.0)

        val transformationMatrix1 = view.transformationMatrix
        assertEquals(identity, transformationMatrix1)

        // display transformation parameters have not changed because base transformation matrix is
        // set to the identity
        val displayTransformationParameters1 = view.displayTransformationParameters
        assertTrue(displayTransformationParameters1.scale > 1.0)
        assertEquals(0.0, displayTransformationParameters1.rotationAngle, 0.0)
        assertEquals(0.0, displayTransformationParameters1.horizontalTranslation, 0.0)
        assertTrue(displayTransformationParameters1.verticalTranslation > 0.0)

        val displayTransformationMatrix1 = view.displayTransformationMatrix
        assertNotEquals(identity, displayTransformationMatrix1)

        // set new display type
        view.displayType = GestureBitmapView.DisplayType.FIT_Y_CENTER

        // check new display type, and that base and display transformations get modified
        assertEquals(GestureBitmapView.DisplayType.FIT_Y_CENTER, view.displayType)

        val baseTransformationParameters2 = view.baseTransformationParameters
        assertTrue(baseTransformationParameters2.scale > 1.0)
        assertEquals(0.0, baseTransformationParameters2.rotationAngle, 0.0)
        assertTrue(baseTransformationParameters2.horizontalTranslation < 0.0)
        assertEquals(0.0, baseTransformationParameters2.verticalTranslation, 0.0)

        assertNotEquals(baseTransformationParameters1, baseTransformationParameters2)
        assertNotEquals(baseTransformationMatrix1, view.baseTransformationMatrix)

        // transformation parameters are recomputed to keep display transformation constant
        val transformationParameters2 = view.transformationParameters
        assertTrue(transformationParameters2.scale < 1.0)
        assertEquals(0.0, transformationParameters2.rotationAngle, 0.0)
        assertTrue(transformationParameters2.horizontalTranslation > 0.0)
        assertTrue(transformationParameters2.verticalTranslation > 0.0)

        assertNotEquals(transformationParameters1, transformationParameters2)
        assertNotEquals(transformationMatrix1, view.transformationMatrix)

        val displayTransformationParameters2 = view.displayTransformationParameters
        assertEquals(displayTransformationParameters1, displayTransformationParameters2)
        assertEquals(displayTransformationMatrix1, view.displayTransformationMatrix)
    }

    @Test
    fun displayType_whenBitmapAvailableAndZeroSizeView_setsExpectedValueAndKeepsTransformations() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY)
        )

        // check default values
        assertNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)

        // set bitmap
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)

        view.bitmap = bitmap

        // check
        assertSame(bitmap, view.bitmap)

        // base transformation matrix has been computed
        val baseTransformationParameters1 = view.baseTransformationParameters
        assertEquals(0.0, baseTransformationParameters1.scale, 0.0)
        assertEquals(Double.NaN, baseTransformationParameters1.rotationAngle, 0.0)
        assertEquals(0.0, baseTransformationParameters1.horizontalTranslation, 0.0)
        assertEquals(0.0, baseTransformationParameters1.verticalTranslation, 0.0)

        val baseTransformationMatrix1 = view.baseTransformationMatrix
        assertNotEquals(identity, baseTransformationMatrix1)

        // transformation parameters are reset
        val transformationParameters1 = view.transformationParameters
        assertEquals(1.0, transformationParameters1.scale, 0.0)
        assertEquals(0.0, transformationParameters1.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters1.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters1.verticalTranslation, 0.0)

        val transformationMatrix1 = view.transformationMatrix
        assertEquals(identity, transformationMatrix1)

        // display transformation parameters have not changed because base transformation matrix is
        // set to the identity
        val displayTransformationParameters1 = view.displayTransformationParameters
        assertEquals(0.0, displayTransformationParameters1.scale, 0.0)
        assertEquals(Double.NaN, displayTransformationParameters1.rotationAngle, 0.0)
        assertEquals(0.0, displayTransformationParameters1.horizontalTranslation, 0.0)
        assertEquals(0.0, displayTransformationParameters1.verticalTranslation, 0.0)

        val displayTransformationMatrix1 = view.displayTransformationMatrix
        assertNotEquals(identity, displayTransformationMatrix1)

        // set new display type
        view.displayType = GestureBitmapView.DisplayType.FIT_Y_CENTER

        // check new display type, but that transformations have not changed
        assertEquals(GestureBitmapView.DisplayType.FIT_Y_CENTER, view.displayType)

        assertEquals(baseTransformationParameters1, view.baseTransformationParameters)
        assertEquals(baseTransformationMatrix1, view.baseTransformationMatrix)

        assertEquals(transformationParameters1, view.transformationParameters)
        assertEquals(transformationMatrix1, view.transformationMatrix)

        assertEquals(displayTransformationParameters1, view.displayTransformationParameters)
        assertEquals(displayTransformationMatrix1, view.displayTransformationMatrix)
    }

    @Test
    fun minScale_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertEquals(GestureBitmapView.DEFAULT_MIN_SCALE, view.minScale, 0.0f)

        // set new value
        view.minScale = 0.5f

        // check
        assertEquals(0.5f, view.minScale, 0.0f)
    }

    @Test
    fun minScale_whenZero_throwsIllegalArgumentException() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        assertThrows(IllegalArgumentException::class.java) { view.minScale = 0.0f }
    }

    @Test
    fun maxScale_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertEquals(GestureBitmapView.DEFAULT_MAX_SCALE, view.maxScale, 0.0f)

        // set new value
        view.minScale = 5.0f

        // check
        assertEquals(5.0f, view.minScale, 0.0f)
    }

    @Test
    fun maxScale_whenZero_throwsIllegalArgumentException() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        assertThrows(IllegalArgumentException::class.java) { view.maxScale = 0.0f }
    }

    @Test
    fun scaleFactorJump_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertEquals(GestureBitmapView.DEFAULT_SCALE_FACTOR_JUMP, view.scaleFactorJump, 0.0f)

        // set new value
        view.scaleFactorJump = 2.0f

        // check
        assertEquals(2.0f, view.scaleFactorJump, 0.0f)
    }

    @Test
    fun scaleFactorJump_whenLessThanOne_throwsIllegalArgumentException() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        assertThrows(IllegalArgumentException::class.java) { view.scaleFactorJump = 0.5f }
    }

    @Test
    fun minScaleMargin_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertEquals(GestureBitmapView.DEFAULT_MIN_SCALE_MARGIN, view.minScaleMargin, 0.0f)

        // set new value
        view.minScaleMargin = 0.5f

        // check
        assertEquals(0.5f, view.minScaleMargin, 0.0f)
    }

    @Test
    fun maxScaleMargin_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertEquals(GestureBitmapView.DEFAULT_MAX_SCALE_MARGIN, view.maxScaleMargin, 0.0f)

        // set new value
        view.maxScaleMargin = 5.0f

        // check
        assertEquals(5.0f, view.maxScaleMargin, 0.0f)
    }

    @Test
    fun leftScrollMargin_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertEquals(GestureBitmapView.DEFAULT_SCROLL_MARGIN, view.leftScrollMargin, 0.0f)

        // set new value
        view.leftScrollMargin = 200.0f

        // check
        assertEquals(200.0f, view.leftScrollMargin, 0.0f)
    }

    @Test
    fun leftScrollMargin_whenNegative_throwsIllegalArgumentException() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertThrows(IllegalArgumentException::class.java) { view.leftScrollMargin = -1.0f }
    }

    @Test
    fun topScrollMargin_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertEquals(GestureBitmapView.DEFAULT_SCROLL_MARGIN, view.topScrollMargin, 0.0f)

        // set new value
        view.topScrollMargin = 201.0f

        // check
        assertEquals(201.0f, view.topScrollMargin, 0.0f)
    }

    @Test
    fun topScrollMargin_whenNegative_throwsIllegalArgumentException() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertThrows(IllegalArgumentException::class.java) { view.topScrollMargin = -1.0f }
    }

    @Test
    fun rightScrollMargin_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertEquals(GestureBitmapView.DEFAULT_SCROLL_MARGIN, view.rightScrollMargin, 0.0f)

        // set new value
        view.rightScrollMargin = 202.0f

        // check
        assertEquals(202.0f, view.rightScrollMargin, 0.0f)
    }

    @Test
    fun rightScrollMargin_whenNegative_throwsIllegalArgumentException() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertThrows(IllegalArgumentException::class.java) { view.rightScrollMargin = -1.0f }
    }

    @Test
    fun bottomScrollMargin_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertEquals(GestureBitmapView.DEFAULT_SCROLL_MARGIN, view.bottomScrollMargin, 0.0f)

        // set new value
        view.bottomScrollMargin = 203.0f

        // check
        assertEquals(203.0f, view.bottomScrollMargin, 0.0f)
    }

    @Test
    fun bottomScrollMargin_whenNegative_throwsIllegalArgumentException() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        assertThrows(IllegalArgumentException::class.java) { view.bottomScrollMargin = -1.0f }
    }

    @Test
    fun resetTransformationParameters_setsTheirValueToTheInitialValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        val transformationParameters1 = view.transformationParameters
        assertEquals(1.0, transformationParameters1.scale, 0.0)
        assertEquals(0.0, transformationParameters1.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters1.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters1.verticalTranslation, 0.0)

        val identity = Matrix()
        assertEquals(identity, view.transformationMatrix)
        val displayTransformationParameters1 = view.displayTransformationParameters
        val displayTransformationMatrix1 = view.displayTransformationMatrix

        var tmp = Matrix(view.transformationMatrix)
        tmp.preConcat(view.baseTransformationMatrix)
        assertEquals(tmp, displayTransformationMatrix1)
        assertEquals(displayTransformationParameters1.toMatrix(), displayTransformationMatrix1)

        val baseTransformationParameters1 = view.baseTransformationParameters
        val baseTransformationMatrix1 = view.baseTransformationMatrix

        // set new value
        val transformationParameters2 = getTransformationParameters()
        view.transformationParameters = transformationParameters2

        // check new value is set, display transformation is updated
        // and base transformation remains unmodified
        val transformationParameters2b = view.transformationParameters
        assertEquals(
            transformationParameters2.scale,
            transformationParameters2b.scale,
            ABSOLUTE_ERROR
        )
        assertEquals(
            transformationParameters2.rotationAngle,
            transformationParameters2b.rotationAngle,
            ABSOLUTE_ERROR
        )
        assertEquals(
            transformationParameters2.horizontalTranslation,
            transformationParameters2b.horizontalTranslation,
            ABSOLUTE_ERROR
        )
        assertEquals(
            transformationParameters2.verticalTranslation,
            transformationParameters2b.verticalTranslation,
            ABSOLUTE_ERROR
        )
        assertNotEquals(identity, view.transformationMatrix)
        tmp = Matrix(view.transformationMatrix)
        tmp.preConcat(view.baseTransformationMatrix)
        assertEquals(tmp, view.displayTransformationMatrix)
        assertEquals(baseTransformationParameters1, view.baseTransformationParameters)
        assertEquals(baseTransformationMatrix1, view.baseTransformationMatrix)

        // reset transformation parameters
        view.resetTransformationParameters()

        // check that transformations are reset to their initial values
        assertEquals(transformationParameters1, view.transformationParameters)
        assertEquals(identity, view.transformationMatrix)
        assertEquals(baseTransformationParameters1, view.baseTransformationParameters)
        assertEquals(baseTransformationMatrix1, view.baseTransformationMatrix)
        assertEquals(displayTransformationParameters1, view.displayTransformationParameters)
        assertEquals(displayTransformationMatrix1, view.displayTransformationMatrix)
    }

    @Test
    fun transformationParameters_returnsExpectedValue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check default value
        val transformationParameters1 = view.transformationParameters
        assertEquals(1.0, transformationParameters1.scale, 0.0)
        assertEquals(0.0, transformationParameters1.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters1.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters1.verticalTranslation, 0.0)

        val identity = Matrix()
        assertEquals(identity, view.transformationMatrix)
        val displayTransformationParameters1 = view.displayTransformationParameters
        val displayTransformationMatrix1 = view.displayTransformationMatrix

        var tmp = Matrix(view.transformationMatrix)
        tmp.preConcat(view.baseTransformationMatrix)
        assertEquals(tmp, displayTransformationMatrix1)
        assertEquals(displayTransformationParameters1.toMatrix(), displayTransformationMatrix1)

        val baseTransformationParameters1 = view.baseTransformationParameters
        val baseTransformationMatrix1 = view.baseTransformationMatrix

        // set new value
        val transformationParameters2 = getTransformationParameters()
        view.transformationParameters = transformationParameters2

        // check new value is set, display transformation is updated
        // and base transformation remains unmodified
        val transformationParameters2b = view.transformationParameters
        assertEquals(
            transformationParameters2.scale,
            transformationParameters2b.scale,
            ABSOLUTE_ERROR
        )
        assertEquals(
            transformationParameters2.rotationAngle,
            transformationParameters2b.rotationAngle,
            ABSOLUTE_ERROR
        )
        assertEquals(
            transformationParameters2.horizontalTranslation,
            transformationParameters2b.horizontalTranslation,
            ABSOLUTE_ERROR
        )
        assertEquals(
            transformationParameters2.verticalTranslation,
            transformationParameters2b.verticalTranslation,
            ABSOLUTE_ERROR
        )
        assertNotEquals(identity, view.transformationMatrix)
        tmp = Matrix(view.transformationMatrix)
        tmp.preConcat(view.baseTransformationMatrix)
        assertEquals(tmp, view.displayTransformationMatrix)
        assertEquals(baseTransformationParameters1, view.baseTransformationParameters)
        assertEquals(baseTransformationMatrix1, view.baseTransformationMatrix)
    }

    @Test
    fun onTouchEvent_whenNoEvent_returnsFalseWithoutInteractions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val (rotationGestureDetectorSpy, scaleGestureDetectorSpy, gestureDetectorSpy) = addSpies(
            view
        )

        view.onTouchEvent(null)

        assertTrue(view.rotationEnabled)
        assertTrue(view.scaleEnabled)
        assertTrue(view.scrollEnabled)
        assertTrue(view.twoFingerScrollEnabled)

        verify { rotationGestureDetectorSpy wasNot Called }
        verify { scaleGestureDetectorSpy wasNot Called }
        verify { gestureDetectorSpy wasNot Called }
    }

    @Test
    fun onTouchEvent_whenEverythingDisabled_doesNotCallAnyGestureDetector() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        view.twoFingerScrollEnabled = false
        view.scrollEnabled = false
        view.scaleEnabled = false
        view.rotationEnabled = false

        val (rotationGestureDetectorSpy, scaleGestureDetectorSpy, gestureDetectorSpy) = addSpies(
            view
        )

        val event = mockk<MotionEvent>()
        every { event.action }.returns(MotionEvent.ACTION_DOWN)

        view.onTouchEvent(event)

        verify { rotationGestureDetectorSpy wasNot Called }
        verify { scaleGestureDetectorSpy wasNot Called }
        verify { gestureDetectorSpy wasNot Called }
    }

    @Test
    fun onTouchEvent_whenEverythingEnabled_callsAllGestureDetectors() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        view.twoFingerScrollEnabled = true
        view.scrollEnabled = true
        view.scaleEnabled = true
        view.rotationEnabled = true

        val (rotationGestureDetectorSpy, scaleGestureDetectorSpy, gestureDetectorSpy) = addSpies(
            view
        )

        val event = mockk<MotionEvent>()
        every { event.action }.returns(MotionEvent.ACTION_DOWN)

        every { gestureDetectorSpy.onTouchEvent(event) }.returns(false)
        every { rotationGestureDetectorSpy.onTouchEvent(event) }.returns(false)
        every { scaleGestureDetectorSpy.onTouchEvent(event) }.returns(false)

        view.onTouchEvent(event)

        assertTrue(view.rotationEnabled)
        assertTrue(view.scaleEnabled)
        assertTrue(view.scrollEnabled)
        assertTrue(view.twoFingerScrollEnabled)

        verify(exactly = 1) { gestureDetectorSpy.onTouchEvent(event) }
        confirmVerified(gestureDetectorSpy)
        verify(exactly = 1) { scaleGestureDetectorSpy.onTouchEvent(event) }
        confirmVerified(scaleGestureDetectorSpy)
        verify(exactly = 1) { rotationGestureDetectorSpy.onTouchEvent(event) }
        confirmVerified(rotationGestureDetectorSpy)
    }

    @Test
    fun onTouchEvent_whenScaleNotEnabled_doesNotCallScaleGestureDetector() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        view.scaleEnabled = false

        val (rotationGestureDetectorSpy, scaleGestureDetectorSpy, gestureDetectorSpy) = addSpies(
            view
        )

        val event = mockk<MotionEvent>()
        every { event.action }.returns(MotionEvent.ACTION_DOWN)

        every { gestureDetectorSpy.onTouchEvent(event) }.returns(false)
        every { rotationGestureDetectorSpy.onTouchEvent(event) }.returns(false)

        view.onTouchEvent(event)

        assertTrue(view.rotationEnabled)
        assertFalse(view.scaleEnabled)
        assertTrue(view.scrollEnabled)
        assertTrue(view.twoFingerScrollEnabled)

        verify(exactly = 1) { rotationGestureDetectorSpy.onTouchEvent(event) }
        verify { scaleGestureDetectorSpy wasNot Called }
        verify(exactly = 1) { gestureDetectorSpy.onTouchEvent(event) }
    }

    @Test
    fun onTouchEvent_whenScaleEnabled_doesCallScaleGestureDetectorButNotGestureDetector() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        view.scaleEnabled = true

        val (rotationGestureDetectorSpy, scaleGestureDetectorSpy, gestureDetectorSpy) = addSpies(
            view
        )

        val event = mockk<MotionEvent>()
        every { event.action }.returns(MotionEvent.ACTION_DOWN)

        every { gestureDetectorSpy.onTouchEvent(event) }.returns(false)
        every { rotationGestureDetectorSpy.onTouchEvent(event) }.returns(false)
        every { scaleGestureDetectorSpy.onTouchEvent(event) }.returns(true)

        view.onTouchEvent(event)

        assertTrue(view.rotationEnabled)
        assertTrue(view.scaleEnabled)
        assertTrue(view.scrollEnabled)
        assertTrue(view.twoFingerScrollEnabled)

        verify(exactly = 1) { gestureDetectorSpy.onTouchEvent(event) }
        confirmVerified(gestureDetectorSpy)
        verify(exactly = 1) { scaleGestureDetectorSpy.onTouchEvent(event) }
        confirmVerified(scaleGestureDetectorSpy)
        verify(exactly = 1) { rotationGestureDetectorSpy.onTouchEvent(event) }
        confirmVerified(rotationGestureDetectorSpy)
    }

    @Test
    fun onTouchEvent_whenRotationNotEnabled_doesNotCallRotationGestureDetector() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        view.rotationEnabled = false

        val (rotationGestureDetectorSpy, scaleGestureDetectorSpy, gestureDetectorSpy) = addSpies(
            view
        )

        val event = mockk<MotionEvent>()
        every { event.action }.returns(MotionEvent.ACTION_DOWN)

        every { gestureDetectorSpy.onTouchEvent(event) }.returns(false)

        view.onTouchEvent(event)

        assertFalse(view.rotationEnabled)
        assertTrue(view.scaleEnabled)
        assertTrue(view.scrollEnabled)
        assertTrue(view.twoFingerScrollEnabled)

        verify { rotationGestureDetectorSpy wasNot Called }
        verify(exactly = 1) { scaleGestureDetectorSpy.onTouchEvent(event) }
        verify(exactly = 1) { gestureDetectorSpy.onTouchEvent(event) }
    }

    @Test
    fun onTouchEvent_whenRotationEnabled_doesCallsRotationGestureDetector() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        view.rotationEnabled = true

        val (rotationGestureDetectorSpy, scaleGestureDetectorSpy, gestureDetectorSpy) = addSpies(
            view
        )

        val event = mockk<MotionEvent>()
        every { event.action }.returns(MotionEvent.ACTION_DOWN)

        every { gestureDetectorSpy.onTouchEvent(event) }.returns(false)
        every { rotationGestureDetectorSpy.onTouchEvent(event) }.returns(false)

        view.onTouchEvent(event)

        assertTrue(view.rotationEnabled)
        assertTrue(view.scaleEnabled)
        assertTrue(view.scrollEnabled)
        assertTrue(view.twoFingerScrollEnabled)

        verify(exactly = 1) { rotationGestureDetectorSpy.onTouchEvent(event) }
        verify(exactly = 1) { scaleGestureDetectorSpy.onTouchEvent(event) }
        verify(exactly = 1) { gestureDetectorSpy.onTouchEvent(event) }
    }

    @Test
    fun onTouchEvent_whenActionUpAndScaleWithinRange_doesNotLimitsScale() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        view.scaleEnabled = true

        val (rotationGestureDetectorSpy, scaleGestureDetectorSpy, gestureDetectorSpy) = addSpies(
            view
        )

        val transformationParameters = view.transformationParameters
        assertTrue(transformationParameters.scale >= view.minScale)
        assertTrue(transformationParameters.scale < view.maxScale)

        val event = mockk<MotionEvent>()
        every { event.action }.returns(MotionEvent.ACTION_UP)

        every { gestureDetectorSpy.onTouchEvent(event) }.returns(false)
        every { rotationGestureDetectorSpy.onTouchEvent(event) }.returns(false)
        every { scaleGestureDetectorSpy.onTouchEvent(event) }.returns(true)

        view.onTouchEvent(event)

        assertTrue(view.rotationEnabled)
        assertTrue(view.scaleEnabled)
        assertTrue(view.scrollEnabled)
        assertTrue(view.twoFingerScrollEnabled)

        verify(exactly = 1) { scaleGestureDetectorSpy.onTouchEvent(event) }
        confirmVerified(scaleGestureDetectorSpy)

        // scale is not smoothly scaled
        assertNull(view.getPrivateProperty("scaleAnimator"))
    }

    @LooperMode(LooperMode.Mode.PAUSED)
    @Test
    fun onTouchEvent_whenActionUpAndScaleTooSmall_smoothlyLimitsScale() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val minScaleReachedListener =
            mockk<GestureBitmapView.OnMinScaleReachedListener>(relaxUnitFun = true)
        view.minScaleReachedListener = minScaleReachedListener
        val scaleAnimationCompletedListener =
            mockk<GestureBitmapView.OnScaleAnimationCompletedListener>(relaxUnitFun = true)
        view.scaleAnimationCompletedListener = scaleAnimationCompletedListener

        view.scaleEnabled = true

        val (rotationGestureDetectorSpy, scaleGestureDetectorSpy, gestureDetectorSpy) = addSpies(
            view
        )

        val transformationParameters = view.transformationParameters
        transformationParameters.scale = GestureBitmapView.DEFAULT_MIN_SCALE / 2.0
        view.transformationParameters = transformationParameters

        assertTrue(transformationParameters.scale < view.minScale)
        assertTrue(transformationParameters.scale < view.maxScale)

        val event = mockk<MotionEvent>()
        every { event.action }.returns(MotionEvent.ACTION_UP)

        every { gestureDetectorSpy.onTouchEvent(event) }.returns(false)
        every { rotationGestureDetectorSpy.onTouchEvent(event) }.returns(false)
        every { scaleGestureDetectorSpy.onTouchEvent(event) }.returns(true)
        every { scaleGestureDetectorSpy.focusX }.returns(100.0f)
        every { scaleGestureDetectorSpy.focusY }.returns(50.0f)

        view.onTouchEvent(event)

        assertTrue(view.rotationEnabled)
        assertTrue(view.scaleEnabled)
        assertTrue(view.scrollEnabled)
        assertTrue(view.twoFingerScrollEnabled)

        verify(exactly = 1) { scaleGestureDetectorSpy.onTouchEvent(event) }
        verify(exactly = 1) { scaleGestureDetectorSpy.focusX }
        verify(exactly = 1) { scaleGestureDetectorSpy.focusY }
        confirmVerified(scaleGestureDetectorSpy)

        // scale is smoothly scaled
        assertNotNull(view.getPrivateProperty("scaleAnimator"))

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // check that proper listeners are called
        verify(exactly = 1) { minScaleReachedListener.onMinScaleReached(view) }
        verify(exactly = 1) { scaleAnimationCompletedListener.onScaleAnimationCompleted(view) }
    }

    @LooperMode(LooperMode.Mode.PAUSED)
    @Test
    fun onTouchEvent_whenActionUpAndScaleTooLarge_smoothlyLimitsScale() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val maxScaleReachedListener =
            mockk<GestureBitmapView.OnMaxScaleReachedListener>(relaxUnitFun = true)
        view.maxScaleReachedListener = maxScaleReachedListener
        val scaleAnimationCompletedListener =
            mockk<GestureBitmapView.OnScaleAnimationCompletedListener>(relaxUnitFun = true)
        view.scaleAnimationCompletedListener = scaleAnimationCompletedListener

        view.scaleEnabled = true

        val (rotationGestureDetectorSpy, scaleGestureDetectorSpy, gestureDetectorSpy) = addSpies(
            view
        )

        val transformationParameters = view.transformationParameters
        transformationParameters.scale = 2.0 * GestureBitmapView.DEFAULT_MAX_SCALE
        view.transformationParameters = transformationParameters

        assertTrue(transformationParameters.scale > view.minScale)
        assertTrue(transformationParameters.scale > view.maxScale)

        val event = mockk<MotionEvent>()
        every { event.action }.returns(MotionEvent.ACTION_UP)

        every { gestureDetectorSpy.onTouchEvent(event) }.returns(false)
        every { rotationGestureDetectorSpy.onTouchEvent(event) }.returns(false)
        every { scaleGestureDetectorSpy.onTouchEvent(event) }.returns(true)
        every { scaleGestureDetectorSpy.focusX }.returns(100.0f)
        every { scaleGestureDetectorSpy.focusY }.returns(50.0f)

        view.onTouchEvent(event)

        assertTrue(view.rotationEnabled)
        assertTrue(view.scaleEnabled)
        assertTrue(view.scrollEnabled)
        assertTrue(view.twoFingerScrollEnabled)

        verify(exactly = 1) { scaleGestureDetectorSpy.onTouchEvent(event) }
        verify(exactly = 1) { scaleGestureDetectorSpy.focusX }
        verify(exactly = 1) { scaleGestureDetectorSpy.focusY }
        confirmVerified(scaleGestureDetectorSpy)

        // scale is smoothly scaled
        assertNotNull(view.getPrivateProperty("scaleAnimator"))

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // check that proper listeners are called
        verify(exactly = 1) { maxScaleReachedListener.onMaxScaleReached(view) }
        verify(exactly = 1) { scaleAnimationCompletedListener.onScaleAnimationCompleted(view) }
    }

    @Test
    fun onTouchEvent_whenActionUpScaleOutOfRangeAndAnimatorAlreadyRunning_cancelsPreviousAnimator() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        view.scaleEnabled = true

        val (rotationGestureDetectorSpy, scaleGestureDetectorSpy, gestureDetectorSpy) = addSpies(
            view
        )

        val transformationParameters = view.transformationParameters
        transformationParameters.scale = GestureBitmapView.DEFAULT_MIN_SCALE / 2.0
        view.transformationParameters = transformationParameters

        assertTrue(transformationParameters.scale < view.minScale)
        assertTrue(transformationParameters.scale < view.maxScale)

        val event = mockk<MotionEvent>()
        every { event.action }.returns(MotionEvent.ACTION_UP)

        every { gestureDetectorSpy.onTouchEvent(event) }.returns(false)
        every { rotationGestureDetectorSpy.onTouchEvent(event) }.returns(false)
        every { scaleGestureDetectorSpy.onTouchEvent(event) }.returns(true)
        every { scaleGestureDetectorSpy.focusX }.returns(100.0f)
        every { scaleGestureDetectorSpy.focusY }.returns(50.0f)

        val translateAnimator = mockk<ValueAnimator>()
        every { translateAnimator.isRunning }.returns(true)
        justRun { translateAnimator.cancel() }
        view.setPrivateProperty("translateAnimator", translateAnimator)

        val scaleAnimator = mockk<ValueAnimator>()
        every { scaleAnimator.isRunning }.returns(true)
        justRun { scaleAnimator.cancel() }
        view.setPrivateProperty("scaleAnimator", scaleAnimator)

        val rotateAndTranslateAnimator = mockk<ValueAnimator>()
        every { rotateAndTranslateAnimator.isRunning }.returns(true)
        justRun { rotateAndTranslateAnimator.cancel() }
        view.setPrivateProperty("rotateAndTranslateAnimator", rotateAndTranslateAnimator)

        view.onTouchEvent(event)

        // previous animators are cancelled and scale animator is reset
        verify(exactly = 1) { translateAnimator.isRunning }
        verify(exactly = 1) { translateAnimator.cancel() }
        verify(exactly = 1) { scaleAnimator.isRunning }
        verify(exactly = 1) { scaleAnimator.cancel() }
        verify(exactly = 1) { rotateAndTranslateAnimator.isRunning }
        verify(exactly = 1) { rotateAndTranslateAnimator.cancel() }
        assertNotSame(scaleAnimator, view.getPrivateProperty("scaleAnimator"))
    }

    @Test
    fun onDraw_whenNoCanvas_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = spyk(GestureBitmapView(context))

        view.callPrivateFunc("onDraw", null)

        verify(exactly = 1) { view.bitmap }
        confirmVerified(view)
    }

    @Test
    fun onDraw_whenNoBitmap_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = spyk(GestureBitmapView(context))

        val canvas = mockk<Canvas>()
        view.callPrivateFunc("onDraw", canvas)

        verify(exactly = 1) { view.bitmap }
        confirmVerified(view)
    }

    @Test
    fun onDraw_whenNonRecycledBitmapButNoCanvas_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)

        view.bitmap = bitmap

        assertSame(bitmap, view.bitmap)
        verify(exactly = 2) { bitmap.isRecycled }

        view.callPrivateFunc("onDraw", null)

        verify(exactly = 3) { bitmap.isRecycled }
    }

    @Test
    fun onDraw_whenRecycledBitmapWithCanvas_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returnsMany(false, false, true)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)

        view.bitmap = bitmap

        assertSame(bitmap, view.bitmap)
        verify(exactly = 2) { bitmap.isRecycled }

        val canvas = mockk<Canvas>()
        view.callPrivateFunc("onDraw", canvas)

        verify(exactly = 3) { bitmap.isRecycled }
        verify { canvas wasNot Called }
    }

    @Test
    fun onDraw_whenNonRecycledBitmapWithCanvas_drawsBitmap() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)

        view.bitmap = bitmap

        assertSame(bitmap, view.bitmap)
        verify(exactly = 2) { bitmap.isRecycled }

        val canvas = mockk<Canvas>(relaxUnitFun = true)
        view.callPrivateFunc("onDraw", canvas)

        verify(exactly = 3) { bitmap.isRecycled }
        verify(exactly = 1) { canvas.drawBitmap(bitmap, any(), any()) }
    }

    @Test
    fun onSaveInstanceState_returnsExpectedParcelable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val bundle: Bundle? = view.callPrivateFuncWithResult("onSaveInstanceState")
        require(bundle != null)

        assertTrue(bundle.containsKey(SUPER_STATE_KEY))

        assertTrue(bundle.containsKey(BASE_MATRIX_KEY))
        val baseMatrix: Matrix? = view.getPrivateProperty(BASE_MATRIX_KEY)
        val baseMatrixValues = FloatArray(MetricTransformationParameters.MATRIX_VALUES_LENGTH)
        baseMatrix?.getValues(baseMatrixValues)
        assertArrayEquals(baseMatrixValues, bundle.getFloatArray(BASE_MATRIX_KEY), 0.0f)

        assertTrue(bundle.containsKey(PARAMS_MATRIX_KEY))
        val paramsMatrix: Matrix? = view.getPrivateProperty(PARAMS_MATRIX_KEY)
        val paramsMatrixValues = FloatArray(MetricTransformationParameters.MATRIX_VALUES_LENGTH)
        paramsMatrix?.getValues(paramsMatrixValues)
        assertArrayEquals(paramsMatrixValues, bundle.getFloatArray(PARAMS_MATRIX_KEY), 0.0f)

        assertTrue(bundle.containsKey(DISPLAY_MATRIX_KEY))
        val displayMatrix: Matrix? = view.getPrivateProperty(DISPLAY_MATRIX_KEY)
        val displayMatrixValues = FloatArray(MetricTransformationParameters.MATRIX_VALUES_LENGTH)
        displayMatrix?.getValues(displayMatrixValues)
        assertArrayEquals(displayMatrixValues, bundle.getFloatArray(DISPLAY_MATRIX_KEY), 0.0f)

        assertEquals(view.rotationEnabled, bundle.getBoolean(ROTATION_ENABLED_KEY))
        assertEquals(view.scaleEnabled, bundle.getBoolean(SCALE_ENABLED_KEY))
        assertEquals(view.scrollEnabled, bundle.getBoolean(SCROLL_ENABLED_KEY))
        assertEquals(view.twoFingerScrollEnabled, bundle.getBoolean(TWO_FINGER_SCROLL_ENABLED_KEY))
        assertEquals(
            view.exclusiveTwoFingerScrollEnabled, bundle.getBoolean(
                EXCLUSIVE_TWO_FINGER_SCROLL_ENABLED_KEY
            )
        )
        assertEquals(view.doubleTapEnabled, bundle.getBoolean(DOUBLE_TAP_ENABLED_KEY))
        assertEquals(view.displayType, bundle.getSerializable(DISPLAY_TYPE_KEY))
        assertEquals(view.minScale, bundle.getFloat(MIN_SCALE_KEY), 0.0f)
        assertEquals(view.maxScale, bundle.getFloat(MAX_SCALE_KEY), 0.0f)
        assertEquals(view.scaleFactorJump, bundle.getFloat(SCALE_FACTOR_JUMP_KEY), 0.0f)
        assertEquals(view.minScaleMargin, bundle.getFloat(MIN_SCALE_MARGIN_KEY), 0.0f)
        assertEquals(view.maxScaleMargin, bundle.getFloat(MAX_SCALE_MARGIN_KEY), 0.0f)
        assertEquals(view.leftScrollMargin, bundle.getFloat(LEFT_SCROLL_MARGIN_KEY), 0.0f)
        assertEquals(view.topScrollMargin, bundle.getFloat(TOP_SCROLL_MARGIN_KEY), 0.0f)
        assertEquals(view.rightScrollMargin, bundle.getFloat(RIGHT_SCROLL_MARGIN_KEY), 0.0f)
        assertEquals(view.bottomScrollMargin, bundle.getFloat(BOTTOM_SCROLL_MARGIN_KEY), 0.0f)
    }

    @Test
    fun onRestoreInstanceState_whenViewState_setsExpectedValues() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check initial values
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)
        assertEquals(identity, view.transformationMatrix)
        assertEquals(identity, view.displayTransformationMatrix)
        assertTrue(view.rotationEnabled)
        assertTrue(view.scaleEnabled)
        assertTrue(view.scrollEnabled)
        assertTrue(view.twoFingerScrollEnabled)
        assertFalse(view.exclusiveTwoFingerScrollEnabled)
        assertTrue(view.doubleTapEnabled)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        assertEquals(1.0f, view.minScale, 0.0f)
        assertEquals(10.0f, view.maxScale, 0.0f)
        assertEquals(3.0f, view.scaleFactorJump, 0.0f)
        assertEquals(0.1f, view.minScaleMargin, 0.0f)
        assertEquals(1.0f, view.maxScaleMargin, 0.0f)
        assertEquals(100.0f, view.leftScrollMargin, 0.0f)
        assertEquals(100.0f, view.topScrollMargin, 0.0f)
        assertEquals(100.0f, view.rightScrollMargin, 0.0f)
        assertEquals(100.0f, view.bottomScrollMargin, 0.0f)

        // prepare bundle to restore from
        val bundle = Bundle()
        bundle.putParcelable(SUPER_STATE_KEY, null)

        val baseMatrix = getTransformationParameters().toMatrix()
        val baseMatrixValues = FloatArray(MetricTransformationParameters.MATRIX_VALUES_LENGTH)
        baseMatrix.getValues(baseMatrixValues)
        bundle.putFloatArray(BASE_MATRIX_KEY, baseMatrixValues)

        val paramsMatrix = getTransformationParameters().toMatrix()
        val paramsMatrixValues = FloatArray(MetricTransformationParameters.MATRIX_VALUES_LENGTH)
        paramsMatrix.getValues(paramsMatrixValues)
        bundle.putFloatArray(PARAMS_MATRIX_KEY, paramsMatrixValues)

        val displayMatrix = getTransformationParameters().toMatrix()
        val displayMatrixValues = FloatArray(MetricTransformationParameters.MATRIX_VALUES_LENGTH)
        displayMatrix.getValues(displayMatrixValues)
        bundle.putFloatArray(DISPLAY_MATRIX_KEY, displayMatrixValues)

        bundle.putBoolean(ROTATION_ENABLED_KEY, false)
        bundle.putBoolean(SCALE_ENABLED_KEY, false)
        bundle.putBoolean(SCROLL_ENABLED_KEY, false)
        bundle.putBoolean(TWO_FINGER_SCROLL_ENABLED_KEY, false)
        bundle.putBoolean(EXCLUSIVE_TWO_FINGER_SCROLL_ENABLED_KEY, true)
        bundle.putBoolean(DOUBLE_TAP_ENABLED_KEY, false)

        bundle.putSerializable(DISPLAY_TYPE_KEY, GestureBitmapView.DisplayType.NONE)

        bundle.putFloat(MIN_SCALE_KEY, 0.5f)
        bundle.putFloat(MAX_SCALE_KEY, 3.0f)
        bundle.putFloat(SCALE_FACTOR_JUMP_KEY, 5.0f)
        bundle.putFloat(MIN_SCALE_MARGIN_KEY, 0.2f)
        bundle.putFloat(MAX_SCALE_MARGIN_KEY, 10.0f)
        bundle.putFloat(LEFT_SCROLL_MARGIN_KEY, 101.0f)
        bundle.putFloat(TOP_SCROLL_MARGIN_KEY, 102.0f)
        bundle.putFloat(RIGHT_SCROLL_MARGIN_KEY, 103.0f)
        bundle.putFloat(BOTTOM_SCROLL_MARGIN_KEY, 104.0f)

        // restore state
        view.callPrivateFunc("onRestoreInstanceState", bundle)

        // check
        assertEquals(baseMatrix, view.baseTransformationMatrix)
        assertEquals(paramsMatrix, view.transformationMatrix)
        assertEquals(displayMatrix, view.displayTransformationMatrix)
        assertFalse(view.rotationEnabled)
        assertFalse(view.scaleEnabled)
        assertFalse(view.scrollEnabled)
        assertFalse(view.twoFingerScrollEnabled)
        assertTrue(view.exclusiveTwoFingerScrollEnabled)
        assertFalse(view.doubleTapEnabled)
        assertEquals(GestureBitmapView.DisplayType.NONE, view.displayType)
        assertEquals(0.5f, view.minScale, 0.0f)
        assertEquals(3.0f, view.maxScale, 0.0f)
        assertEquals(5.0f, view.scaleFactorJump, 0.0f)
        assertEquals(0.2f, view.minScaleMargin, 0.0f)
        assertEquals(10.0f, view.maxScaleMargin, 0.0f)
        assertEquals(101.0f, view.leftScrollMargin, 0.0f)
        assertEquals(102.0f, view.topScrollMargin, 0.0f)
        assertEquals(103.0f, view.rightScrollMargin, 0.0f)
        assertEquals(104.0f, view.bottomScrollMargin, 0.0f)
    }

    @Test
    fun onRestoreInstanceState_whenNoViewState_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // check initial values
        val identity = Matrix()
        assertEquals(identity, view.baseTransformationMatrix)
        assertEquals(identity, view.transformationMatrix)
        assertEquals(identity, view.displayTransformationMatrix)
        assertTrue(view.rotationEnabled)
        assertTrue(view.scaleEnabled)
        assertTrue(view.scrollEnabled)
        assertTrue(view.twoFingerScrollEnabled)
        assertFalse(view.exclusiveTwoFingerScrollEnabled)
        assertTrue(view.doubleTapEnabled)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        assertEquals(1.0f, view.minScale, 0.0f)
        assertEquals(10.0f, view.maxScale, 0.0f)
        assertEquals(3.0f, view.scaleFactorJump, 0.0f)
        assertEquals(0.1f, view.minScaleMargin, 0.0f)
        assertEquals(1.0f, view.maxScaleMargin, 0.0f)
        assertEquals(100.0f, view.leftScrollMargin, 0.0f)
        assertEquals(100.0f, view.topScrollMargin, 0.0f)
        assertEquals(100.0f, view.rightScrollMargin, 0.0f)
        assertEquals(100.0f, view.bottomScrollMargin, 0.0f)

        // prepare bundle to restore from
        val parcelable = mockk<AbsSavedState>()

        // restore state
        view.callPrivateFunc("onRestoreInstanceState", parcelable)

        // check initial values are preserved
        assertEquals(identity, view.baseTransformationMatrix)
        assertEquals(identity, view.transformationMatrix)
        assertEquals(identity, view.displayTransformationMatrix)
        assertTrue(view.rotationEnabled)
        assertTrue(view.scaleEnabled)
        assertTrue(view.scrollEnabled)
        assertTrue(view.twoFingerScrollEnabled)
        assertFalse(view.exclusiveTwoFingerScrollEnabled)
        assertTrue(view.doubleTapEnabled)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        assertEquals(1.0f, view.minScale, 0.0f)
        assertEquals(10.0f, view.maxScale, 0.0f)
        assertEquals(3.0f, view.scaleFactorJump, 0.0f)
        assertEquals(0.1f, view.minScaleMargin, 0.0f)
        assertEquals(1.0f, view.maxScaleMargin, 0.0f)
        assertEquals(100.0f, view.leftScrollMargin, 0.0f)
        assertEquals(100.0f, view.topScrollMargin, 0.0f)
        assertEquals(100.0f, view.rightScrollMargin, 0.0f)
        assertEquals(100.0f, view.bottomScrollMargin, 0.0f)
    }

    fun onSizeChanged_whenNegativeSize_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = spyk(GestureBitmapView(context))

        view.callPrivateFunc("onSizeChanged", 0, 1920, 1080, 1921)

        verify(exactly = 0) { view.bitmap }
        verify(exactly = 0) { view.invalidate() }

        view.callPrivateFunc("onSizeChanged", 1080, 0, 1081, 1921)

        verify(exactly = 0) { view.bitmap }
        verify(exactly = 0) { view.invalidate() }

    }

    @Test
    fun onSizeChanged_whenNoChangeInSize_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = spyk(GestureBitmapView(context))

        view.callPrivateFunc("onSizeChanged", 1080, 1920, 1080, 1920)

        verify(exactly = 0) { view.bitmap }
        verify(exactly = 0) { view.invalidate() }
    }

    @Test
    fun onSizeChanged_whenNoBitmap_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = spyk(GestureBitmapView(context))

        view.callPrivateFunc("onSizeChanged", 1080, 1920, 0, 0)

        verify(exactly = 1) { view.bitmap }
        verify(exactly = 0) { view.invalidate() }
    }

    @Test
    fun onSizeChanged_whenSizeHasChangedAndBitmapAvailable_updatesBaseAndDisplayTransformations() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        // set view size
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )

        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(640)
        every { bitmap.height }.returns(480)

        view.bitmap = bitmap

        val displayType = view.displayType
        val baseMatrix = view.baseTransformationMatrix
        val displayMatrix = view.displayTransformationMatrix

        view.measure(
            View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY)
        )

        view.callPrivateFunc("onSizeChanged", 640, 480, 0, 0)

        // check that display type remains the same, but base and display transformations have
        // changed
        assertEquals(displayType, view.displayType)
        assertNotEquals(baseMatrix, view.baseTransformationMatrix)
        assertNotEquals(displayMatrix, view.displayTransformationMatrix)
    }

    @Test
    fun gestureSingleTapConfirmed_performsClick() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        // execute single tap confirmed
        val clickListener = mockk<View.OnClickListener>(relaxUnitFun = true)
        view.setOnClickListener(clickListener)

        assertTrue(gestureDetectorListener.onSingleTapConfirmed(null))

        // check
        verify(exactly = 1) { clickListener.onClick(view) }
    }

    @Test
    fun gestureDoubleTap_whenDisabled_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val doubleTapListener = mockk<GestureBitmapView.OnDoubleTapListener>(relaxUnitFun = true)
        view.doubleTapListener = doubleTapListener
        view.doubleTapEnabled = false

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        // execute double tap
        assertFalse(gestureDetectorListener.onDoubleTap(null))

        // check
        verify(exactly = 1) { doubleTapListener.onDoubleTap(view) }

        assertEquals(1.0, view.transformationParameters.scale, 0.0)
    }

    @LooperMode(LooperMode.Mode.PAUSED)
    @Test
    fun gestureDoubleTap_whenEnabledButNoEvent_smoothlyScalesToViewCenterAndJumpsToNextScaleFactor() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val minScaleReachedListener =
            mockk<GestureBitmapView.OnMinScaleReachedListener>(relaxUnitFun = true)
        view.minScaleReachedListener = minScaleReachedListener
        val maxScaleReachedListener =
            mockk<GestureBitmapView.OnMaxScaleReachedListener>(relaxUnitFun = true)
        view.maxScaleReachedListener = maxScaleReachedListener
        val scaleAnimationCompletedListener =
            mockk<GestureBitmapView.OnScaleAnimationCompletedListener>(relaxUnitFun = true)
        view.scaleAnimationCompletedListener = scaleAnimationCompletedListener

        val doubleTapListener = mockk<GestureBitmapView.OnDoubleTapListener>(relaxUnitFun = true)
        view.doubleTapListener = doubleTapListener
        view.doubleTapEnabled = true

        view.measure(
            View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 640, 480)

        assertEquals(10.0f, view.maxScale, 0.0f)
        assertEquals(1.0f, view.minScale, 0.0f)
        assertEquals(3.0f, view.scaleFactorJump, 0.0f)
        var transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        // execute double tap
        assertFalse(gestureDetectorListener.onDoubleTap(null))

        // check
        verify(exactly = 1) { doubleTapListener.onDoubleTap(view) }

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        transformationParameters = view.transformationParameters
        assertEquals(4.0, transformationParameters.scale, 0.0)
        assertEquals(
            -view.width / 2 * (transformationParameters.scale - 1.0),
            transformationParameters.horizontalTranslation,
            1.0
        )
        assertEquals(
            -view.height / 2 * (transformationParameters.scale - 1),
            transformationParameters.verticalTranslation, 1.0
        )
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)

        verify(exactly = 1) { scaleAnimationCompletedListener.onScaleAnimationCompleted(view) }

        // execute double tap again
        assertFalse(gestureDetectorListener.onDoubleTap(null))

        // check
        verify(exactly = 2) { doubleTapListener.onDoubleTap(view) }

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        transformationParameters = view.transformationParameters
        assertEquals(7.0, transformationParameters.scale, 0.0)
        assertEquals(
            -view.width / 2 * (transformationParameters.scale - 1.0),
            transformationParameters.horizontalTranslation,
            1.0
        )
        assertEquals(
            -view.height / 2 * (transformationParameters.scale - 1),
            transformationParameters.verticalTranslation, 1.0
        )
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)

        verify(exactly = 2) { scaleAnimationCompletedListener.onScaleAnimationCompleted(view) }

        // execute double tap again
        assertFalse(gestureDetectorListener.onDoubleTap(null))

        // check
        verify(exactly = 3) { doubleTapListener.onDoubleTap(view) }

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        transformationParameters = view.transformationParameters
        assertEquals(10.0, transformationParameters.scale, 0.0)
        assertEquals(
            -view.width / 2 * (transformationParameters.scale - 1.0),
            transformationParameters.horizontalTranslation,
            1.0
        )
        assertEquals(
            -view.height / 2 * (transformationParameters.scale - 1),
            transformationParameters.verticalTranslation, 1.0
        )
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)

        verify(exactly = 3) { scaleAnimationCompletedListener.onScaleAnimationCompleted(view) }
        verify(exactly = 1) { maxScaleReachedListener.onMaxScaleReached(view) }

        // execute double tap again
        assertFalse(gestureDetectorListener.onDoubleTap(null))

        // check
        verify(exactly = 4) { doubleTapListener.onDoubleTap(view) }

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(
            -view.width / 2 * (transformationParameters.scale - 1.0),
            transformationParameters.horizontalTranslation,
            1.0
        )
        assertEquals(
            -view.height / 2 * (transformationParameters.scale - 1),
            transformationParameters.verticalTranslation, 1.0
        )
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)

        verify(exactly = 4) { scaleAnimationCompletedListener.onScaleAnimationCompleted(view) }
        verify(exactly = 1) { minScaleReachedListener.onMinScaleReached(view) }
    }

    @LooperMode(LooperMode.Mode.PAUSED)
    @Test
    fun gestureDoubleTap_whenEnabledWithEvent_smoothlyScalesAndJumpsToNextScaleFactor() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val minScaleReachedListener =
            mockk<GestureBitmapView.OnMinScaleReachedListener>(relaxUnitFun = true)
        view.minScaleReachedListener = minScaleReachedListener
        val maxScaleReachedListener =
            mockk<GestureBitmapView.OnMaxScaleReachedListener>(relaxUnitFun = true)
        view.maxScaleReachedListener = maxScaleReachedListener
        val scaleAnimationCompletedListener =
            mockk<GestureBitmapView.OnScaleAnimationCompletedListener>(relaxUnitFun = true)
        view.scaleAnimationCompletedListener = scaleAnimationCompletedListener

        val doubleTapListener = mockk<GestureBitmapView.OnDoubleTapListener>(relaxUnitFun = true)
        view.doubleTapListener = doubleTapListener
        view.doubleTapEnabled = true

        view.measure(
            View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 640, 480)

        assertEquals(10.0f, view.maxScale, 0.0f)
        assertEquals(1.0f, view.minScale, 0.0f)
        assertEquals(3.0f, view.scaleFactorJump, 0.0f)
        var transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        // execute double tap
        val motionEvent = mockk<MotionEvent>()
        every { motionEvent.x }.returns(0.0f)
        every { motionEvent.y }.returns(0.0f)
        assertFalse(gestureDetectorListener.onDoubleTap(motionEvent))

        // check
        verify(exactly = 1) { doubleTapListener.onDoubleTap(view) }

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        transformationParameters = view.transformationParameters
        assertEquals(4.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)

        verify(exactly = 1) { scaleAnimationCompletedListener.onScaleAnimationCompleted(view) }

        // execute double tap again
        assertFalse(gestureDetectorListener.onDoubleTap(motionEvent))

        // check
        verify(exactly = 2) { doubleTapListener.onDoubleTap(view) }

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        transformationParameters = view.transformationParameters
        assertEquals(7.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)

        verify(exactly = 2) { scaleAnimationCompletedListener.onScaleAnimationCompleted(view) }

        // execute double tap again
        assertFalse(gestureDetectorListener.onDoubleTap(motionEvent))

        // check
        verify(exactly = 3) { doubleTapListener.onDoubleTap(view) }

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        transformationParameters = view.transformationParameters
        assertEquals(10.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)

        verify(exactly = 3) { scaleAnimationCompletedListener.onScaleAnimationCompleted(view) }
        verify(exactly = 1) { maxScaleReachedListener.onMaxScaleReached(view) }

        // execute double tap again
        assertFalse(gestureDetectorListener.onDoubleTap(motionEvent))

        // check
        verify(exactly = 4) { doubleTapListener.onDoubleTap(view) }

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)

        verify(exactly = 4) { scaleAnimationCompletedListener.onScaleAnimationCompleted(view) }
        verify(exactly = 1) { minScaleReachedListener.onMinScaleReached(view) }
    }

    @LooperMode(LooperMode.Mode.PAUSED)
    @Test
    fun gestureDoubleTap_whenEnabledWithEventAndAnimatorIsRunning_cancelsAnimatorAndSmoothlyScalesAndJumpsToNextScaleFactor() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val minScaleReachedListener =
            mockk<GestureBitmapView.OnMinScaleReachedListener>(relaxUnitFun = true)
        view.minScaleReachedListener = minScaleReachedListener
        val maxScaleReachedListener =
            mockk<GestureBitmapView.OnMaxScaleReachedListener>(relaxUnitFun = true)
        view.maxScaleReachedListener = maxScaleReachedListener
        val scaleAnimationCompletedListener =
            mockk<GestureBitmapView.OnScaleAnimationCompletedListener>(relaxUnitFun = true)
        view.scaleAnimationCompletedListener = scaleAnimationCompletedListener

        val translateAnimator = mockk<ValueAnimator>()
        every { translateAnimator.isRunning }.returns(true)
        justRun { translateAnimator.cancel() }
        view.setPrivateProperty("translateAnimator", translateAnimator)
        val scaleAnimator = mockk<ValueAnimator>()
        every { scaleAnimator.isRunning }.returns(true)
        justRun { scaleAnimator.cancel() }
        view.setPrivateProperty("scaleAnimator", scaleAnimator)
        val rotateAndTranslateAnimator = mockk<ValueAnimator>()
        every { rotateAndTranslateAnimator.isRunning }.returns(true)
        justRun { rotateAndTranslateAnimator.cancel() }
        view.setPrivateProperty("rotateAndTranslateAnimator", rotateAndTranslateAnimator)

        val doubleTapListener = mockk<GestureBitmapView.OnDoubleTapListener>(relaxUnitFun = true)
        view.doubleTapListener = doubleTapListener
        view.doubleTapEnabled = true

        view.measure(
            View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 640, 480)

        assertEquals(10.0f, view.maxScale, 0.0f)
        assertEquals(1.0f, view.minScale, 0.0f)
        assertEquals(3.0f, view.scaleFactorJump, 0.0f)
        var transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        // execute double tap
        val motionEvent = mockk<MotionEvent>()
        every { motionEvent.x }.returns(0.0f)
        every { motionEvent.y }.returns(0.0f)
        assertFalse(gestureDetectorListener.onDoubleTap(motionEvent))

        // check
        verify(exactly = 1) { doubleTapListener.onDoubleTap(view) }
        verify(exactly = 1) { translateAnimator.cancel() }
        verify(exactly = 1) { scaleAnimator.cancel() }
        verify(exactly = 1) { rotateAndTranslateAnimator.cancel() }

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        transformationParameters = view.transformationParameters
        assertEquals(4.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)

        verify(exactly = 1) { scaleAnimationCompletedListener.onScaleAnimationCompleted(view) }

        // execute double tap again
        assertFalse(gestureDetectorListener.onDoubleTap(motionEvent))

        // check
        verify(exactly = 2) { doubleTapListener.onDoubleTap(view) }

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        transformationParameters = view.transformationParameters
        assertEquals(7.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)

        verify(exactly = 2) { scaleAnimationCompletedListener.onScaleAnimationCompleted(view) }

        // execute double tap again
        assertFalse(gestureDetectorListener.onDoubleTap(motionEvent))

        // check
        verify(exactly = 3) { doubleTapListener.onDoubleTap(view) }

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        transformationParameters = view.transformationParameters
        assertEquals(10.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)

        verify(exactly = 3) { scaleAnimationCompletedListener.onScaleAnimationCompleted(view) }
        verify(exactly = 1) { maxScaleReachedListener.onMaxScaleReached(view) }

        // execute double tap again
        assertFalse(gestureDetectorListener.onDoubleTap(motionEvent))

        // check
        verify(exactly = 4) { doubleTapListener.onDoubleTap(view) }

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)

        verify(exactly = 4) { scaleAnimationCompletedListener.onScaleAnimationCompleted(view) }
        verify(exactly = 1) { minScaleReachedListener.onMinScaleReached(view) }
    }

    @Test
    fun gestureFling_whenScrollDisabled_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = false

        val transformationParameters = view.transformationParameters
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        // execute fling
        assertFalse(gestureDetectorListener.onFling(null, null, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)
    }

    @Test
    fun gestureFling_whenScrollEnabledButNoEvents_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = true

        val transformationParameters = view.transformationParameters
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        // execute fling
        assertFalse(gestureDetectorListener.onFling(null, null, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)

        // execute fling
        val event = mockk<MotionEvent>()
        assertFalse(gestureDetectorListener.onFling(event, null, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)

        // execute fling
        assertFalse(gestureDetectorListener.onFling(null, event, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)
    }

    @Test
    fun gestureFling_whenExclusiveAndTwoFingerScrollEnabledAnd1PointerCount_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = true
        view.twoFingerScrollEnabled = true
        view.exclusiveTwoFingerScrollEnabled = true

        val transformationParameters = view.transformationParameters
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        val event1 = mockk<MotionEvent>()
        every { event1.pointerCount }.returns(1)

        val event2 = mockk<MotionEvent>()
        every { event2.pointerCount }.returns(2)

        // execute fling
        assertFalse(gestureDetectorListener.onFling(event1, event1, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)

        // execute fling
        assertFalse(gestureDetectorListener.onFling(event2, event1, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)

        // execute fling
        assertFalse(gestureDetectorListener.onFling(event1, event2, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)
    }

    @Test
    fun gestureFling_whenExclusiveAndTwoFingerScrollDisabledAnd2PointerCount_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = true
        view.twoFingerScrollEnabled = false
        view.exclusiveTwoFingerScrollEnabled = true

        val transformationParameters = view.transformationParameters
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        val event1 = mockk<MotionEvent>()
        every { event1.pointerCount }.returns(1)

        val event2 = mockk<MotionEvent>()
        every { event2.pointerCount }.returns(2)

        // execute fling
        assertFalse(gestureDetectorListener.onFling(event2, event2, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)

        // execute fling
        assertFalse(gestureDetectorListener.onFling(event1, event2, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)

        // execute fling
        assertFalse(gestureDetectorListener.onFling(event2, event1, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)
    }

    @Test
    fun gestureFling_whenNonExclusiveAndUnitScale_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = true
        view.twoFingerScrollEnabled = true

        val event2 = mockk<MotionEvent>()
        every { event2.pointerCount }.returns(2)

        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)

        // execute fling
        assertTrue(gestureDetectorListener.onFling(event2, event2, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)
    }

    @LooperMode(LooperMode.Mode.PAUSED)
    @Test
    fun gestureFling_whenNonExclusiveAndGreaterThan1Scale_makesScroll() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = true
        view.twoFingerScrollEnabled = true
        view.exclusiveTwoFingerScrollEnabled = false

        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1080, 1920)

        val event2 = mockk<MotionEvent>()
        every { event2.pointerCount }.returns(2)

        val transformationParameters1 = view.transformationParameters
        assertEquals(1.0, transformationParameters1.scale, 0.0)

        // set bitmap (make it large enough so that scroll can be made at larger scale)
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(1080)
        every { bitmap.height }.returns(1920)

        view.bitmap = bitmap

        // set a scale larger than one after setting bitmap (otherwise scale is reset)
        transformationParameters1.scale = 2.0
        // make sure that bitmap is centered so that sroll is not limited
        transformationParameters1.horizontalTranslation = (-view.width / 2).toDouble()
        transformationParameters1.verticalTranslation = (-view.height / 2).toDouble()
        view.transformationParameters = transformationParameters1

        val scrollAnimationCompletedListener =
            mockk<GestureBitmapView.OnScrollAnimationCompletedListener>(relaxUnitFun = true)
        view.scrollAnimationCompletedListener = scrollAnimationCompletedListener

        // execute fling
        assertTrue(gestureDetectorListener.onFling(event2, event2, 1.0f, 1.0f))

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // check that transformation has changed with a certain amount of translation
        verify(exactly = 1) { scrollAnimationCompletedListener.onScrollAnimationCompleted(view) }
        val transformationParameters2 = view.transformationParameters
        assertNotEquals(transformationParameters1, transformationParameters2)
        assertEquals(transformationParameters1.scale, transformationParameters2.scale, 0.0)
        assertEquals(
            transformationParameters1.rotationAngle,
            transformationParameters2.rotationAngle,
            0.0
        )
        assertNotEquals(
            transformationParameters1.horizontalTranslation,
            transformationParameters2.horizontalTranslation, 0.0
        )
        assertNotEquals(
            transformationParameters1.verticalTranslation,
            transformationParameters2.verticalTranslation, 0.0
        )
    }

    @Test
    fun gestureFling_whenAnimatorsAreRunning_cancelsPreviousAnimators() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = true
        view.twoFingerScrollEnabled = true

        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1080, 1920)

        val scaleGestureDetector: ScaleGestureDetector? =
            view.getPrivateProperty("scaleGestureDetector")
        assertNotNull(scaleGestureDetector)

        val scaleGestureDetectorSpy = spyk(scaleGestureDetector as ScaleGestureDetector)
        view.setPrivateProperty("scaleGestureDetector", scaleGestureDetectorSpy)

        val translateAnimator = mockk<ValueAnimator>()
        every { translateAnimator.isRunning }.returns(true)
        justRun { translateAnimator.cancel() }
        view.setPrivateProperty("translateAnimator", translateAnimator)
        val scaleAnimator = mockk<ValueAnimator>()
        every { scaleAnimator.isRunning }.returns(true)
        justRun { scaleAnimator.cancel() }
        view.setPrivateProperty("scaleAnimator", scaleAnimator)
        val rotateAndTranslateAnimator = mockk<ValueAnimator>()
        every { rotateAndTranslateAnimator.isRunning }.returns(true)
        justRun { rotateAndTranslateAnimator.cancel() }
        view.setPrivateProperty("rotateAndTranslateAnimator", rotateAndTranslateAnimator)

        val event2 = mockk<MotionEvent>()
        every { event2.pointerCount }.returns(2)

        val transformationParameters1 = view.transformationParameters
        assertEquals(1.0, transformationParameters1.scale, 0.0)

        // set bitmap (make it large enough so that scroll can be made at larger scale)
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(1080)
        every { bitmap.height }.returns(1920)

        view.bitmap = bitmap

        verify(exactly = 1) { translateAnimator.cancel() }
        verify(exactly = 1) { scaleAnimator.cancel() }
        verify(exactly = 1) { rotateAndTranslateAnimator.cancel() }

        // set a scale larger than one after setting bitmap (otherwise scale is reset)
        transformationParameters1.scale = 2.0
        // make sure that bitmap is centered so that sroll is not limited
        transformationParameters1.horizontalTranslation = (-view.width / 2).toDouble()
        transformationParameters1.verticalTranslation = (-view.height / 2).toDouble()
        view.transformationParameters = transformationParameters1

        verify(exactly = 2) { translateAnimator.cancel() }
        verify(exactly = 2) { scaleAnimator.cancel() }
        verify(exactly = 2) { rotateAndTranslateAnimator.cancel() }

        // execute fling
        assertTrue(gestureDetectorListener.onFling(event2, event2, 1.0f, 1.0f))

        // check
        verify(exactly = 3) { translateAnimator.cancel() }
        verify(exactly = 3) { scaleAnimator.cancel() }
        verify(exactly = 3) { rotateAndTranslateAnimator.cancel() }
    }

    @LooperMode(LooperMode.Mode.PAUSED)
    @Test
    fun gestureFling_whenWhenWholeImageFitsVertically_limitsVerticalScroll() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = true
        view.twoFingerScrollEnabled = true
        view.displayType = GestureBitmapView.DisplayType.NONE

        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1080, 1920)

        val scaleGestureDetector: ScaleGestureDetector? =
            view.getPrivateProperty("scaleGestureDetector")
        assertNotNull(scaleGestureDetector)

        val event2 = mockk<MotionEvent>()
        every { event2.pointerCount }.returns(2)

        val transformationParameters1 = view.transformationParameters
        assertEquals(1.0, transformationParameters1.scale, 0.0)

        // set bitmap
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(1080)
        every { bitmap.height }.returns(960)

        view.bitmap = bitmap

        // set a scale larger than one after setting bitmap (otherwise scale is reset)
        transformationParameters1.scale = 2.0
        transformationParameters1.horizontalTranslation = (-view.width / 2).toDouble()
        view.transformationParameters = transformationParameters1

        val scrollAnimationCompletedListener =
            mockk<GestureBitmapView.OnScrollAnimationCompletedListener>(relaxUnitFun = true)
        view.scrollAnimationCompletedListener = scrollAnimationCompletedListener

        // execute fling
        assertTrue(gestureDetectorListener.onFling(event2, event2, 1.0f, 1.0f))

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // check that transformation has changed with a certain amount of translation
        verify(exactly = 1) { scrollAnimationCompletedListener.onScrollAnimationCompleted(view) }
        val transformationParameters2 = view.transformationParameters
        assertNotEquals(transformationParameters1, transformationParameters2)
        // scale and rotation is not modified during scroll
        assertEquals(transformationParameters1.scale, transformationParameters2.scale, 0.0)
        assertEquals(
            transformationParameters1.rotationAngle,
            transformationParameters2.rotationAngle,
            0.0
        )

        // horizontal translation is modified during scroll
        assertNotEquals(
            transformationParameters1.horizontalTranslation,
            transformationParameters2.horizontalTranslation, 0.0
        )

        // vertical scroll has been limited
        assertEquals(
            transformationParameters1.verticalTranslation,
            transformationParameters2.verticalTranslation, 0.0
        )
    }

    @LooperMode(LooperMode.Mode.PAUSED)
    @Test
    fun gestureFling_whenWhenWholeImageFitsHorizontally_limitsHorizontalScroll() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = true
        view.twoFingerScrollEnabled = true
        view.displayType = GestureBitmapView.DisplayType.NONE

        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1080, 1920)

        val scaleGestureDetector: ScaleGestureDetector? =
            view.getPrivateProperty("scaleGestureDetector")
        assertNotNull(scaleGestureDetector)

        val event2 = mockk<MotionEvent>()
        every { event2.pointerCount }.returns(2)

        val transformationParameters1 = view.transformationParameters
        assertEquals(1.0, transformationParameters1.scale, 0.0)

        // set bitmap
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(540)
        every { bitmap.height }.returns(1920)

        view.bitmap = bitmap

        // set a scale larger than one after setting bitmap (otherwise scale is reset)
        transformationParameters1.scale = 2.0
        transformationParameters1.verticalTranslation = (-view.height / 2).toDouble()
        view.transformationParameters = transformationParameters1

        val scrollAnimationCompletedListener =
            mockk<GestureBitmapView.OnScrollAnimationCompletedListener>(relaxUnitFun = true)
        view.scrollAnimationCompletedListener = scrollAnimationCompletedListener

        // execute fling
        assertTrue(gestureDetectorListener.onFling(event2, event2, 1.0f, 1.0f))

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // check that transformation has changed with a certain amount of translation
        verify(exactly = 1) { scrollAnimationCompletedListener.onScrollAnimationCompleted(view) }
        val transformationParameters2 = view.transformationParameters
        assertNotEquals(transformationParameters1, transformationParameters2)
        // scale and rotation is not modified during scroll
        assertEquals(transformationParameters1.scale, transformationParameters2.scale, 0.0)
        assertEquals(
            transformationParameters1.rotationAngle,
            transformationParameters2.rotationAngle,
            0.0
        )

        // horizontal scroll has been limited
        assertEquals(
            transformationParameters1.horizontalTranslation,
            transformationParameters2.horizontalTranslation, 0.0
        )

        // vertical translation is modified during scroll
        assertNotEquals(
            transformationParameters1.verticalTranslation,
            transformationParameters2.verticalTranslation, 0.0
        )
    }

    @LooperMode(LooperMode.Mode.PAUSED)
    @Test
    fun gestureFling_whenTopLimitReached_limitsVerticalScrollAndNotifies() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = true
        view.twoFingerScrollEnabled = true
        view.displayType = GestureBitmapView.DisplayType.NONE

        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1080, 1920)

        val event2 = mockk<MotionEvent>()
        every { event2.pointerCount }.returns(2)

        val transformationParameters1 = view.transformationParameters
        assertEquals(1.0, transformationParameters1.scale, 0.0)

        // set bitmap
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(1080)
        every { bitmap.height }.returns(1920)

        view.bitmap = bitmap

        // set a scale larger than one after setting bitmap (otherwise scale is reset)
        transformationParameters1.scale = 2.0
        transformationParameters1.horizontalTranslation = (-view.width / 2).toDouble()
        transformationParameters1.verticalTranslation = -0.1
        view.transformationParameters = transformationParameters1

        val scrollAnimationCompletedListener =
            mockk<GestureBitmapView.OnScrollAnimationCompletedListener>(relaxUnitFun = true)
        view.scrollAnimationCompletedListener = scrollAnimationCompletedListener

        val topBoundReachedListener =
            mockk<GestureBitmapView.OnTopBoundReachedListener>(relaxUnitFun = true)
        view.topBoundReachedListener = topBoundReachedListener

        // update start displayed rectangle coordinates
        view.callPrivateFunc("updateStartDisplayedRectangleCoordinates")

        // execute fling
        assertTrue(gestureDetectorListener.onFling(event2, event2, 1.0f, 1.0f))

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // check that transformation has changed with a certain amount of translation
        verify(exactly = 1) { scrollAnimationCompletedListener.onScrollAnimationCompleted(view) }
        verify(exactly = 1) { topBoundReachedListener.onTopBoundReached(view) }
        val transformationParameters2 = view.transformationParameters
        assertNotEquals(transformationParameters1, transformationParameters2)
        // scale and rotation is not modified during scroll
        assertEquals(transformationParameters1.scale, transformationParameters2.scale, 0.0)
        assertEquals(
            transformationParameters1.rotationAngle,
            transformationParameters2.rotationAngle,
            0.0
        )
        // horizontal translation is modified during scroll
        assertNotEquals(
            transformationParameters1.horizontalTranslation,
            transformationParameters2.horizontalTranslation, 0.0
        )

        // vertical scroll has been snapped to top border
        assertEquals(0.0, transformationParameters2.verticalTranslation, 0.0)
    }

    @LooperMode(LooperMode.Mode.PAUSED)
    @Test
    fun gestureFling_whenBottomLimitReached_limitsVerticalScrollAndNotifies() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = true
        view.twoFingerScrollEnabled = true
        view.displayType = GestureBitmapView.DisplayType.NONE

        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1080, 1920)

        val event2 = mockk<MotionEvent>()
        every { event2.pointerCount }.returns(2)

        val transformationParameters1 = view.transformationParameters
        assertEquals(1.0, transformationParameters1.scale, 0.0)

        // set bitmap
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(1080)
        every { bitmap.height }.returns(1920)

        view.bitmap = bitmap

        // set a scale larger than one after setting bitmap (otherwise scale is reset)
        transformationParameters1.scale = 2.0
        transformationParameters1.horizontalTranslation = 0.0
        transformationParameters1.verticalTranslation = -view.height.toDouble() + 0.1
        view.transformationParameters = transformationParameters1

        val scrollAnimationCompletedListener =
            mockk<GestureBitmapView.OnScrollAnimationCompletedListener>(relaxUnitFun = true)
        view.scrollAnimationCompletedListener = scrollAnimationCompletedListener

        val bottomBoundReachedListener =
            mockk<GestureBitmapView.OnBottomBoundReachedListener>(relaxUnitFun = true)
        view.bottomBoundReachedListener = bottomBoundReachedListener

        // update start displayed rectangle coordinates
        view.callPrivateFunc("updateStartDisplayedRectangleCoordinates")

        // execute fling
        assertTrue(gestureDetectorListener.onFling(event2, event2, -1.0f, -1.0f))

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // check that transformation has changed with a certain amount of translation
        verify(exactly = 1) { scrollAnimationCompletedListener.onScrollAnimationCompleted(view) }
        verify(exactly = 1) { bottomBoundReachedListener.onBottomBoundReached(view) }
        val transformationParameters2 = view.transformationParameters
        assertNotEquals(transformationParameters1, transformationParameters2)
        // scale and rotation is not modified during scroll
        assertEquals(transformationParameters1.scale, transformationParameters2.scale, 0.0)
        assertEquals(
            transformationParameters1.rotationAngle,
            transformationParameters2.rotationAngle,
            0.0
        )
        // horizontal translation is modified during scroll
        assertNotEquals(
            transformationParameters1.horizontalTranslation,
            transformationParameters2.horizontalTranslation, 0.0
        )

        // vertical scroll has been snapped to bottom border
        assertEquals(
            -view.height.toDouble(),
            transformationParameters2.verticalTranslation, 0.0
        )
    }

    @LooperMode(LooperMode.Mode.PAUSED)
    @Test
    fun gestureFling_whenLeftLimitReached_limitsHorizontalScrollAndNotifies() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = true
        view.twoFingerScrollEnabled = true
        view.displayType = GestureBitmapView.DisplayType.NONE

        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1080, 1920)

        val event2 = mockk<MotionEvent>()
        every { event2.pointerCount }.returns(2)

        val transformationParameters1 = view.transformationParameters
        assertEquals(1.0, transformationParameters1.scale, 0.0)

        // set bitmap
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(1080)
        every { bitmap.height }.returns(1920)

        view.bitmap = bitmap

        // set a scale larger than one after setting bitmap (otherwise scale is reset)
        transformationParameters1.scale = 2.0
        transformationParameters1.horizontalTranslation = -0.1
        transformationParameters1.verticalTranslation = (-view.height / 2).toDouble()
        view.transformationParameters = transformationParameters1

        val scrollAnimationCompletedListener =
            mockk<GestureBitmapView.OnScrollAnimationCompletedListener>(relaxUnitFun = true)
        view.scrollAnimationCompletedListener = scrollAnimationCompletedListener

        val leftBoundReachedListener =
            mockk<GestureBitmapView.OnLeftBoundReachedListener>(relaxUnitFun = true)
        view.leftBoundReachedListener = leftBoundReachedListener

        // update start displayed rectangle coordinates
        view.callPrivateFunc("updateStartDisplayedRectangleCoordinates")

        // execute fling
        assertTrue(gestureDetectorListener.onFling(event2, event2, 1.0f, 1.0f))

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // check that transformation has changed with a certain amount of translation
        verify(exactly = 1) { scrollAnimationCompletedListener.onScrollAnimationCompleted(view) }
        verify(exactly = 1) { leftBoundReachedListener.onLeftBoundReached(view) }
        val transformationParameters2 = view.transformationParameters
        assertNotEquals(transformationParameters1, transformationParameters2)
        // scale and rotation is not modified during scroll
        assertEquals(transformationParameters1.scale, transformationParameters2.scale, 0.0)
        assertEquals(
            transformationParameters1.rotationAngle,
            transformationParameters2.rotationAngle,
            0.0
        )
        // horizontal scroll has been snapped to left border
        assertEquals(0.0, transformationParameters2.horizontalTranslation, 0.0)

        // vertical translation is modified during scroll
        assertNotEquals(
            transformationParameters1.verticalTranslation,
            transformationParameters2.verticalTranslation, 0.0
        )
    }

    @LooperMode(LooperMode.Mode.PAUSED)
    @Test
    fun gestureFling_whenRightLimitReached_limitsVerticalScrollAndNotifies() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = true
        view.twoFingerScrollEnabled = true
        view.displayType = GestureBitmapView.DisplayType.NONE

        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1080, 1920)

        val event2 = mockk<MotionEvent>()
        every { event2.pointerCount }.returns(2)

        val transformationParameters1 = view.transformationParameters
        assertEquals(1.0, transformationParameters1.scale, 0.0)

        // set bitmap
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(1080)
        every { bitmap.height }.returns(1920)

        view.bitmap = bitmap

        // set a scale larger than one after setting bitmap (otherwise scale is reset)
        transformationParameters1.scale = 2.0
        transformationParameters1.horizontalTranslation = -view.width.toDouble() + 0.1
        transformationParameters1.verticalTranslation = 0.0
        view.transformationParameters = transformationParameters1

        val scrollAnimationCompletedListener =
            mockk<GestureBitmapView.OnScrollAnimationCompletedListener>(relaxUnitFun = true)
        view.scrollAnimationCompletedListener = scrollAnimationCompletedListener

        val rightBoundReachedListener =
            mockk<GestureBitmapView.OnRightBoundReachedListener>(relaxUnitFun = true)
        view.rightBoundReachedListener = rightBoundReachedListener

        // update start displayed rectangle coordinates
        view.callPrivateFunc("updateStartDisplayedRectangleCoordinates")

        // execute fling
        assertTrue(gestureDetectorListener.onFling(event2, event2, -1.0f, -1.0f))

        //finish animation
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // check that transformation has changed with a certain amount of translation
        verify(exactly = 1) { scrollAnimationCompletedListener.onScrollAnimationCompleted(view) }
        verify(exactly = 1) { rightBoundReachedListener.onRightBoundReached(view) }
        val transformationParameters2 = view.transformationParameters
        assertNotEquals(transformationParameters1, transformationParameters2)
        // scale and rotation is not modified during scroll
        assertEquals(transformationParameters1.scale, transformationParameters2.scale, 0.0)
        assertEquals(
            transformationParameters1.rotationAngle,
            transformationParameters2.rotationAngle,
            0.0
        )
        // horizontal scroll has been snapped to right border
        assertEquals(
            -view.width.toDouble(),
            transformationParameters2.horizontalTranslation, 0.0
        )

        // vertical translation is modified during scroll
        assertNotEquals(
            transformationParameters1.verticalTranslation,
            transformationParameters2.verticalTranslation, 0.0
        )
    }

    @Test
    fun gestureScroll_whenScrollDisabled_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = false

        val transformationParameters = view.transformationParameters
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        // execute scroll
        assertFalse(gestureDetectorListener.onScroll(null, null, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)
    }

    @Test
    fun gestureScroll_whenScrollEnabledButNoEvents_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = true

        val transformationParameters = view.transformationParameters
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        // execute scroll
        assertFalse(gestureDetectorListener.onScroll(null, null, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)

        // execute scroll
        val event = mockk<MotionEvent>()
        assertFalse(gestureDetectorListener.onScroll(event, null, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)

        // execute scroll
        assertFalse(gestureDetectorListener.onScroll(null, event, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)
    }

    @Test
    fun gestureScroll_whenExclusiveAndTwoFingerScrollEnabledAnd1PointerCount_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = true
        view.twoFingerScrollEnabled = true
        view.exclusiveTwoFingerScrollEnabled = true

        val transformationParameters = view.transformationParameters
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        val event1 = mockk<MotionEvent>()
        every { event1.pointerCount }.returns(1)

        val event2 = mockk<MotionEvent>()
        every { event2.pointerCount }.returns(2)

        // execute scroll
        assertFalse(gestureDetectorListener.onScroll(event1, event1, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)

        // execute scroll
        assertFalse(gestureDetectorListener.onScroll(event2, event1, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)

        // execute scroll
        assertFalse(gestureDetectorListener.onScroll(event1, event2, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)
    }

    @Test
    fun gestureScroll_whenExclusiveAndTwoFingerScrollDisabledAnd2PointerCount_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = true
        view.twoFingerScrollEnabled = false
        view.exclusiveTwoFingerScrollEnabled = true

        val transformationParameters = view.transformationParameters
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        val event1 = mockk<MotionEvent>()
        every { event1.pointerCount }.returns(1)

        val event2 = mockk<MotionEvent>()
        every { event2.pointerCount }.returns(2)

        // execute scroll
        assertFalse(gestureDetectorListener.onScroll(event2, event2, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)

        // execute scroll
        assertFalse(gestureDetectorListener.onScroll(event1, event2, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)

        // execute scroll
        assertFalse(gestureDetectorListener.onScroll(event2, event1, 1.0f, 1.0f))

        // check that transformation has not changed
        assertEquals(transformationParameters, view.transformationParameters)
    }

    @Test
    fun gestureScroll_whenNonExclusiveAndUnitScale_makesScroll() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = true
        view.twoFingerScrollEnabled = true

        val event2 = mockk<MotionEvent>()
        every { event2.pointerCount }.returns(2)

        val transformationParameters1 = view.transformationParameters
        assertEquals(1.0, transformationParameters1.scale, 0.0)

        // execute scroll
        assertTrue(gestureDetectorListener.onScroll(event2, event2, 1.0f, 1.0f))

        // check that transformation has changed
        val transformationParameters2 = view.transformationParameters
        assertNotEquals(transformationParameters1, transformationParameters2)
        assertEquals(transformationParameters1.scale, transformationParameters2.scale, 0.0)
        assertEquals(
            transformationParameters1.rotationAngle,
            transformationParameters2.rotationAngle,
            0.0
        )
        assertNotEquals(
            transformationParameters1.horizontalTranslation,
            transformationParameters2.horizontalTranslation, 0.0
        )
        assertNotEquals(
            transformationParameters1.verticalTranslation,
            transformationParameters2.verticalTranslation, 0.0
        )
    }

    @Test
    fun gestureScroll_whenNonExclusiveAndGreaterThan1Scale_makesScroll() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        view.scrollEnabled = true
        view.twoFingerScrollEnabled = true

        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1080, 1920)

        val event2 = mockk<MotionEvent>()
        every { event2.pointerCount }.returns(2)

        val transformationParameters1 = view.transformationParameters
        assertEquals(1.0, transformationParameters1.scale, 0.0)

        // set bitmap (make it large enough so that scroll can be made at larger scale)
        val bitmap = mockk<Bitmap>()
        every { bitmap.isRecycled }.returns(false)
        every { bitmap.width }.returns(1080)
        every { bitmap.height }.returns(1920)

        view.bitmap = bitmap

        // set a scale larger than one after setting bitmap (otherwise scale is reset)
        transformationParameters1.scale = 2.0
        // make sure that bitmap is centered so that sroll is not limited
        transformationParameters1.horizontalTranslation = (-view.width / 2).toDouble()
        transformationParameters1.verticalTranslation = (-view.height / 2).toDouble()
        view.transformationParameters = transformationParameters1

        // execute scroll
        assertTrue(gestureDetectorListener.onScroll(event2, event2, 1.0f, 1.0f))

        // check that transformation has changed with a certain amount of translation
        val transformationParameters2 = view.transformationParameters
        assertNotEquals(transformationParameters1, transformationParameters2)
        assertEquals(transformationParameters1.scale, transformationParameters2.scale, 0.0)
        assertEquals(
            transformationParameters1.rotationAngle,
            transformationParameters2.rotationAngle,
            0.0
        )
        assertNotEquals(
            transformationParameters1.horizontalTranslation,
            transformationParameters2.horizontalTranslation, 0.0
        )
        assertNotEquals(
            transformationParameters1.verticalTranslation,
            transformationParameters2.verticalTranslation, 0.0
        )
    }

    @Test
    fun gestureLongPress_whenNotLongClickable_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        // execute long press
        val longClickListener = mockk<View.OnLongClickListener>()
        every { longClickListener.onLongClick(view) }.returns(true)
        view.setOnLongClickListener(longClickListener)
        view.isLongClickable = false

        gestureDetectorListener.onLongPress(null)

        // check
        verify { longClickListener wasNot Called }
    }

    @Test
    fun gestureLongPress_whenScaleGestureDetectorIsInProgress_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        val scaleGestureDetector: ScaleGestureDetector? =
            view.getPrivateProperty("scaleGestureDetector")
        assertNotNull(scaleGestureDetector)

        val scaleGestureDetectorSpy = spyk(scaleGestureDetector as ScaleGestureDetector)
        view.setPrivateProperty("scaleGestureDetector", scaleGestureDetectorSpy)
        every { scaleGestureDetectorSpy.isInProgress }.returns(true)

        // execute long press
        val longClickListener = mockk<View.OnLongClickListener>()
        every { longClickListener.onLongClick(view) }.returns(true)
        view.setOnLongClickListener(longClickListener)
        view.isLongClickable = true

        gestureDetectorListener.onLongPress(null)

        // check
        verify { longClickListener wasNot Called }
    }

    @Test
    fun gestureLongPress_whenScaleGestureDetectorIsNotInProgress_performsClick() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
        val gestureDetectorListener: GestureDetector.SimpleOnGestureListener? =
            gestureDetector?.getPrivateProperty("mListener")

        require(gestureDetectorListener != null)

        // execute long press
        val longClickListener = mockk<View.OnLongClickListener>()
        every { longClickListener.onLongClick(view) }.returns(true)
        view.setOnLongClickListener(longClickListener)
        view.isLongClickable = true

        gestureDetectorListener.onLongPress(null)

        // check
        verify(exactly = 1) { longClickListener.onLongClick(view) }
    }

    @Test
    fun gestureScale_whenNoDetector_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val scaleGestureDetector: ScaleGestureDetector? =
            view.getPrivateProperty("scaleGestureDetector")
        val scaleGestureDetectorListener: ScaleGestureDetector.SimpleOnScaleGestureListener? =
            scaleGestureDetector?.getPrivateProperty("mListener")

        require(scaleGestureDetectorListener != null)

        assertEquals(1.0, view.transformationParameters.scale, 0.0)

        // execute onScale
        assertFalse(scaleGestureDetectorListener.onScale(null))

        // scale remains unchanged
        assertEquals(1.0, view.transformationParameters.scale, 0.0)
    }

    @Test
    fun gestureScale_whenNoSpanChange_makesNoAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val scaleGestureDetector: ScaleGestureDetector? =
            view.getPrivateProperty("scaleGestureDetector")
        val scaleGestureDetectorListener: ScaleGestureDetector.SimpleOnScaleGestureListener? =
            scaleGestureDetector?.getPrivateProperty("mListener")

        require(scaleGestureDetectorListener != null)
        assertEquals(scaleGestureDetector.currentSpan, scaleGestureDetector.previousSpan, 0.0f)

        // execute onScale
        assertFalse(scaleGestureDetectorListener.onScale(scaleGestureDetector))

        // scale remains unchanged
        assertEquals(1.0, view.transformationParameters.scale, 0.0)
    }

    @Test
    fun gestureScale_whenSpanChange_modifiesScale() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val scaleGestureDetector: ScaleGestureDetector? =
            view.getPrivateProperty("scaleGestureDetector")
        val scaleGestureDetectorListener: ScaleGestureDetector.SimpleOnScaleGestureListener? =
            scaleGestureDetector?.getPrivateProperty("mListener")

        require(scaleGestureDetectorListener != null)

        val scaleGestureDetectorSpy = spyk(scaleGestureDetector)
        every { scaleGestureDetectorSpy.currentSpan }.returns(100.0f)
        every { scaleGestureDetectorSpy.scaleFactor }.returns(2.0f)

        // execute onScale
        assertTrue(scaleGestureDetectorListener.onScale(scaleGestureDetectorSpy))

        // scale has changed
        assertEquals(2.0, view.transformationParameters.scale, 0.0)
    }

    @Test
    fun gestureRotation_modifiesRotationAngle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = GestureBitmapView(context)

        val rotationGestureDetector: RotationGestureDetector? =
            view.getPrivateProperty("rotationGestureDetector")
        require(rotationGestureDetector != null)

        val rotationGestureDetectorListener = rotationGestureDetector.listener
        require(rotationGestureDetectorListener != null)

        // initial rotation is zero
        assertEquals(0.0, view.transformationParameters.rotationAngle, 0.0)

        // execute onRotation
        val angle = Random.Default.nextFloat()
        assertTrue(
            rotationGestureDetectorListener.onRotation(
                rotationGestureDetector,
                angle,
                0.0f,
                0.0f
            )
        )

        // check new rotation angle
        assertEquals(angle.toDouble(), view.transformationParameters.rotationAngle, ABSOLUTE_ERROR)
    }

    companion object {
        private const val MIN_ANGLE_DEGREES = -180.0
        private const val MAX_ANGLE_DEGREES = 180.0

        private const val MIN_RANDOM_VALUE = -100.0
        private const val MAX_RANDOM_VALUE = 100.0

        private const val ABSOLUTE_ERROR = 1e-5

        private const val SUPER_STATE_KEY = "superState"
        private const val BASE_MATRIX_KEY = "baseMatrix"
        private const val PARAMS_MATRIX_KEY = "paramsMatrix"
        private const val DISPLAY_MATRIX_KEY = "displayMatrix"
        private const val ROTATION_ENABLED_KEY = "rotationEnabled"
        private const val SCALE_ENABLED_KEY = "scaleEnabled"
        private const val SCROLL_ENABLED_KEY = "scrollEnabled"
        private const val TWO_FINGER_SCROLL_ENABLED_KEY = "twoFingerScrollEnabled"
        private const val EXCLUSIVE_TWO_FINGER_SCROLL_ENABLED_KEY =
            "exclusiveTwoFingerScrollEnabled"
        private const val DOUBLE_TAP_ENABLED_KEY = "doubleTapEnabled"
        private const val DISPLAY_TYPE_KEY = "displayType"
        private const val MIN_SCALE_KEY = "minScale"
        private const val MAX_SCALE_KEY = "maxScale"
        private const val SCALE_FACTOR_JUMP_KEY = "scaleFactorJump"
        private const val MIN_SCALE_MARGIN_KEY = "minScaleMargin"
        private const val MAX_SCALE_MARGIN_KEY = "maxScaleMargin"
        private const val LEFT_SCROLL_MARGIN_KEY = "leftScrollMargin"
        private const val TOP_SCROLL_MARGIN_KEY = "topScrollMargin"
        private const val RIGHT_SCROLL_MARGIN_KEY = "rightScrollMargin"
        private const val BOTTOM_SCROLL_MARGIN_KEY = "bottomScrollMargin"

        private fun getTransformationParameters(): MetricTransformationParameters {
            val randomizer = UniformRandomizer()

            val scale = randomizer.nextDouble(0.0, MAX_RANDOM_VALUE)
            val rotationAngle = Math.toRadians(
                randomizer.nextDouble(
                    MIN_ANGLE_DEGREES,
                    MAX_ANGLE_DEGREES
                )
            )

            val horizontalTranslation = randomizer.nextDouble(MIN_RANDOM_VALUE, MAX_RANDOM_VALUE)
            val verticalTranslation = randomizer.nextDouble(MIN_RANDOM_VALUE, MAX_RANDOM_VALUE)

            return MetricTransformationParameters(
                scale,
                rotationAngle,
                horizontalTranslation,
                verticalTranslation
            )
        }

        private fun addSpies(view: GestureBitmapView): Spies {
            val rotationGestureDetector: RotationGestureDetector? =
                view.getPrivateProperty("rotationGestureDetector")
            assertNotNull(rotationGestureDetector)

            val rotationGestureDetectorSpy =
                spyk(rotationGestureDetector as RotationGestureDetector)
            view.setPrivateProperty("rotationGestureDetector", rotationGestureDetectorSpy)

            val scaleGestureDetector: ScaleGestureDetector? =
                view.getPrivateProperty("scaleGestureDetector")
            assertNotNull(scaleGestureDetector)

            val scaleGestureDetectorSpy = spyk(scaleGestureDetector as ScaleGestureDetector)
            view.setPrivateProperty("scaleGestureDetector", scaleGestureDetectorSpy)

            val gestureDetector: GestureDetector? = view.getPrivateProperty("gestureDetector")
            assertNotNull(gestureDetector)

            val gestureDetectorSpy = spyk(gestureDetector as GestureDetector)
            view.setPrivateProperty("gestureDetector", gestureDetectorSpy)

            return Spies(rotationGestureDetectorSpy, scaleGestureDetectorSpy, gestureDetectorSpy)
        }
    }

    private data class Spies(
        val rotationGestureDetector: RotationGestureDetector,
        val scaleGestureDetector: ScaleGestureDetector,
        val gestureDetector: GestureDetector
    )
}