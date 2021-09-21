package com.irurueta.android.gesturebitmap.test

import android.content.Intent
import android.graphics.Matrix
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.irurueta.android.gesturebitmap.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.ceil

@RunWith(AndroidJUnit4::class)
class GestureBitmapViewTest {

    @get:Rule
    val activityRule = ActivityTestRule(GestureBitmapViewActivity::class.java, true)

    @get:Rule
    var conditionalIgnoreRule = ConditionalIgnoreRule()

    private var activity: GestureBitmapViewActivity? = null
    private var view: GestureBitmapView? = null

    private val lock = Object()

    private var topBoundReached = 0
    private var bottomBoundReached = 0
    private var leftBoundReached = 0
    private var rightBoundReached = 0
    private var minScaleReached = 0
    private var maxScaleReached = 0
    private var rotationAndTranslateAnimationCompleted = 0
    private var scaleAnimationCompleted = 0
    private var scrollAnimationCompleted = 0
    private var doubleTap = 0

    private val topBoundReachedListener = object : GestureBitmapView.OnTopBoundReachedListener {
        override fun onTopBoundReached(view: GestureBitmapView) {
            synchronized(lock) {
                topBoundReached++
                lock.notifyAll()
            }
        }
    }

    private val bottomBoundReachedListener =
        object : GestureBitmapView.OnBottomBoundReachedListener {
            override fun onBottomBoundReached(view: GestureBitmapView) {
                synchronized(lock) {
                    bottomBoundReached++
                    lock.notifyAll()
                }
            }
        }

    private val leftBoundReachedListener = object : GestureBitmapView.OnLeftBoundReachedListener {
        override fun onLeftBoundReached(view: GestureBitmapView) {
            synchronized(lock) {
                leftBoundReached++
                lock.notifyAll()
            }
        }
    }

    private val rightBoundReachedListener = object : GestureBitmapView.OnRightBoundReachedListener {
        override fun onRightBoundReached(view: GestureBitmapView) {
            synchronized(lock) {
                rightBoundReached++
                lock.notifyAll()
            }
        }
    }

    private val minScaleReachedListener = object : GestureBitmapView.OnMinScaleReachedListener {
        override fun onMinScaleReached(view: GestureBitmapView) {
            synchronized(lock) {
                minScaleReached++
                lock.notifyAll()
            }
        }
    }

    private val maxScaleReachedListener = object : GestureBitmapView.OnMaxScaleReachedListener {
        override fun onMaxScaleReached(view: GestureBitmapView) {
            synchronized(lock) {
                maxScaleReached++
                lock.notifyAll()
            }
        }
    }

    private val rotateAndTranslateAnimationCompletedListener =
        object : GestureBitmapView.OnRotateAndTranslateAnimationCompletedListener {
            override fun onRotateAndTranslateAnimationCompleted(view: GestureBitmapView) {
                synchronized(lock) {
                    rotationAndTranslateAnimationCompleted++
                    lock.notifyAll()
                }
            }
        }

    private val scaleAnimationCompletedListener =
        object : GestureBitmapView.OnScaleAnimationCompletedListener {
            override fun onScaleAnimationCompleted(view: GestureBitmapView) {
                synchronized(lock) {
                    scaleAnimationCompleted++
                    lock.notifyAll()
                }
            }
        }

    private val scrollAnimationCompletedListener =
        object : GestureBitmapView.OnScrollAnimationCompletedListener {
            override fun onScrollAnimationCompleted(view: GestureBitmapView) {
                synchronized(lock) {
                    scrollAnimationCompleted++
                    lock.notifyAll()
                }
            }
        }

    private val doubleTapListener = object : GestureBitmapView.OnDoubleTapListener {
        override fun onDoubleTap(view: GestureBitmapView) {
            synchronized(lock) {
                doubleTap++
                lock.notifyAll()
            }
        }
    }

    @Before
    fun setUp() {
        activity = activityRule.activity
        view = activity?.findViewById(R.id.gesture_bitmap_view_test)
        reset()
    }

    @After
    fun tearDown() {
        view = null
        activity = null
        reset()
    }

    @Test
    fun initializedView_setsValuesInLayoutXml() {
        val view = this.view ?: return fail()

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
        assertTrue(view.doubleTapEnabled)
        assertNotNull(view.bitmap)
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        assertEquals(GestureBitmapView.DEFAULT_MIN_SCALE, view.minScale, 0.0f)
        assertEquals(GestureBitmapView.DEFAULT_MAX_SCALE, view.maxScale, 0.0f)
        assertEquals(GestureBitmapView.DEFAULT_SCALE_FACTOR_JUMP, view.scaleFactorJump, 0.0f)

        val transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)

        val identity = Matrix()
        assertEquals(identity, view.transformationMatrix)

        val baseTransformationParameters = view.baseTransformationParameters
        assertTrue(baseTransformationParameters.scale < 1.0)
        assertEquals(0.0, baseTransformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, baseTransformationParameters.horizontalTranslation, 0.0)
        assertNotEquals(0.0, baseTransformationParameters.verticalTranslation, 0.0)

        assertNotEquals(identity, view.baseTransformationMatrix)

        val displayTransformationParameters = view.displayTransformationParameters
        assertEquals(baseTransformationParameters, displayTransformationParameters)

        assertEquals(view.baseTransformationMatrix, view.displayTransformationMatrix)

        assertNotNull(view.displayRect)
    }

    @Test
    fun constructor_setsDefaultValues() {
        val activity = this.activity ?: return fail()
        activityRule.runOnUiThread {
            val view = GestureBitmapView(activity)

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
            assertTrue(view.doubleTapEnabled)
            assertNull(view.bitmap)
            assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
            assertEquals(GestureBitmapView.DEFAULT_MIN_SCALE, view.minScale, 0.0f)
            assertEquals(GestureBitmapView.DEFAULT_MAX_SCALE, view.maxScale, 0.0f)
            assertEquals(GestureBitmapView.DEFAULT_SCALE_FACTOR_JUMP, view.scaleFactorJump, 0.0f)

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
    }

    @Test
    fun displayType_whenNone_keepsDisplayTransformationAndUpdatesOtherTransformations() {
        val view = this.view ?: return fail()

        // check default value
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val baseTransformation1 = view.baseTransformationParameters
        val transformation1 = view.transformationParameters
        val displayTransformation1 = view.displayTransformationParameters

        // set display type none
        activityRule.runOnUiThread {
            view.displayType = GestureBitmapView.DisplayType.NONE
        }

        // check
        assertEquals(GestureBitmapView.DisplayType.NONE, view.displayType)

        val baseTransformation2 = view.baseTransformationParameters
        val transformation2 = view.transformationParameters
        val displayTransformation2 = view.displayTransformationParameters

        assertNotEquals(baseTransformation1, baseTransformation2)
        assertNotEquals(transformation1, transformation2)
        assertEquals(displayTransformation1, displayTransformation2)
    }

    @Test
    fun displayType_whenFitIfBigger_keepsTransformations() {
        val view = this.view ?: return fail()

        // check default value
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val baseTransformation1 = view.baseTransformationParameters
        val transformation1 = view.transformationParameters
        val displayTransformation1 = view.displayTransformationParameters

        // set display type fit if bigger
        activityRule.runOnUiThread {
            view.displayType = GestureBitmapView.DisplayType.FIT_IF_BIGGER
        }

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_IF_BIGGER, view.displayType)

        val baseTransformation2 = view.baseTransformationParameters
        val transformation2 = view.transformationParameters
        val displayTransformation2 = view.displayTransformationParameters

        assertEquals(baseTransformation1, baseTransformation2)
        assertEquals(transformation1, transformation2)
        assertEquals(displayTransformation1, displayTransformation2)
    }

    @Test
    fun displayType_whenFitXTop_keepsDisplayTransformationAndUpdatesOtherTransformations() {
        val view = this.view ?: return fail()

        // check default value
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val baseTransformation1 = view.baseTransformationParameters
        val transformation1 = view.transformationParameters
        val displayTransformation1 = view.displayTransformationParameters

        // set display type fit x top
        activityRule.runOnUiThread {
            view.displayType = GestureBitmapView.DisplayType.FIT_X_TOP
        }

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_X_TOP, view.displayType)

        val baseTransformation2 = view.baseTransformationParameters
        val transformation2 = view.transformationParameters
        val displayTransformation2 = view.displayTransformationParameters

        assertNotEquals(baseTransformation1, baseTransformation2)
        assertNotEquals(transformation1, transformation2)
        assertEquals(displayTransformation1, displayTransformation2)
    }

    @Test
    fun displayType_whenFitXBottom_keepsDisplayTransformationAndUpdatesOtherTransformations() {
        val view = this.view ?: return fail()

        // check default value
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val baseTransformation1 = view.baseTransformationParameters
        val transformation1 = view.transformationParameters
        val displayTransformation1 = view.displayTransformationParameters

        // set display type fit x bottom
        activityRule.runOnUiThread {
            view.displayType = GestureBitmapView.DisplayType.FIT_X_BOTTOM
        }

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_X_BOTTOM, view.displayType)

        val baseTransformation2 = view.baseTransformationParameters
        val transformation2 = view.transformationParameters
        val displayTransformation2 = view.displayTransformationParameters

        assertNotEquals(baseTransformation1, baseTransformation2)
        assertNotEquals(transformation1, transformation2)
        assertEquals(displayTransformation1, displayTransformation2)
    }

    @Test
    fun displayType_whenFitXCenter_keepsTransformations() {
        val view = this.view ?: return fail()

        // check default value
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val baseTransformation1 = view.baseTransformationParameters
        val transformation1 = view.transformationParameters
        val displayTransformation1 = view.displayTransformationParameters

        // set display type fit x center
        activityRule.runOnUiThread {
            view.displayType = GestureBitmapView.DisplayType.FIT_X_CENTER
        }

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)

        val baseTransformation2 = view.baseTransformationParameters
        val transformation2 = view.transformationParameters
        val displayTransformation2 = view.displayTransformationParameters

        assertEquals(baseTransformation1, baseTransformation2)
        assertEquals(transformation1, transformation2)
        assertEquals(displayTransformation1, displayTransformation2)
    }

    @Test
    fun displayType_whenFitYLeft_keepsDisplayTransformationAndUpdatesOtherTransformations() {
        val view = this.view ?: return fail()

        // check default value
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val baseTransformation1 = view.baseTransformationParameters
        val transformation1 = view.transformationParameters
        val displayTransformation1 = view.displayTransformationParameters

        // set display type fit y left
        activityRule.runOnUiThread {
            view.displayType = GestureBitmapView.DisplayType.FIT_Y_LEFT
        }

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_Y_LEFT, view.displayType)

        val baseTransformation2 = view.baseTransformationParameters
        val transformation2 = view.transformationParameters
        val displayTransformation2 = view.displayTransformationParameters

        assertNotEquals(baseTransformation1, baseTransformation2)
        assertNotEquals(transformation1, transformation2)
        assertEquals(displayTransformation1.scale, displayTransformation2.scale, ABSOLUTE_ERROR)
        assertEquals(
            displayTransformation1.rotationAngle,
            displayTransformation2.rotationAngle,
            0.0
        )
        assertEquals(
            displayTransformation1.horizontalTranslation,
            displayTransformation2.horizontalTranslation,
            0.0
        )
        assertEquals(
            displayTransformation1.verticalTranslation,
            displayTransformation2.verticalTranslation,
            0.0
        )
    }

    @Test
    fun displayType_whenFitYRight_keepsDisplayTransformationAndUpdatesOtherTransformations() {
        val view = this.view ?: return fail()

        // check default value
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val baseTransformation1 = view.baseTransformationParameters
        val transformation1 = view.transformationParameters
        val displayTransformation1 = view.displayTransformationParameters

        // set display type fit y right
        activityRule.runOnUiThread {
            view.displayType = GestureBitmapView.DisplayType.FIT_Y_RIGHT
        }

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_Y_RIGHT, view.displayType)

        val baseTransformation2 = view.baseTransformationParameters
        val transformation2 = view.transformationParameters
        val displayTransformation2 = view.displayTransformationParameters

        assertNotEquals(baseTransformation1, baseTransformation2)
        assertNotEquals(transformation1, transformation2)
        assertEquals(displayTransformation1.scale, displayTransformation2.scale, ABSOLUTE_ERROR)
        assertEquals(
            displayTransformation1.rotationAngle,
            displayTransformation2.rotationAngle,
            ABSOLUTE_ERROR
        )
        assertEquals(
            displayTransformation1.horizontalTranslation,
            displayTransformation2.horizontalTranslation,
            ABSOLUTE_ERROR
        )
        assertEquals(
            displayTransformation1.verticalTranslation,
            displayTransformation2.verticalTranslation,
            ABSOLUTE_ERROR
        )
    }

    @Test
    fun displayType_whenFitYCenter_keepsDisplayTransformationAndUpdatesOtherTransformations() {
        val view = this.view ?: return fail()

        // check default value
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val baseTransformation1 = view.baseTransformationParameters
        val transformation1 = view.transformationParameters
        val displayTransformation1 = view.displayTransformationParameters

        // set display type fit y center
        activityRule.runOnUiThread {
            view.displayType = GestureBitmapView.DisplayType.FIT_Y_CENTER
        }

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_Y_CENTER, view.displayType)

        val baseTransformation2 = view.baseTransformationParameters
        val transformation2 = view.transformationParameters
        val displayTransformation2 = view.displayTransformationParameters

        assertNotEquals(baseTransformation1, baseTransformation2)
        assertNotEquals(transformation1, transformation2)
        assertEquals(displayTransformation1.scale, displayTransformation2.scale, ABSOLUTE_ERROR)
        assertEquals(
            displayTransformation1.rotationAngle,
            displayTransformation2.rotationAngle,
            ABSOLUTE_ERROR
        )
        assertEquals(
            displayTransformation1.horizontalTranslation,
            displayTransformation2.horizontalTranslation,
            ABSOLUTE_ERROR
        )
        assertEquals(
            displayTransformation1.verticalTranslation,
            displayTransformation2.verticalTranslation,
            ABSOLUTE_ERROR
        )
    }

    @Test
    fun displayType_whenCenterCrop_keepsDisplayTransformationAndUpdatesOtherTransformations() {
        val view = this.view ?: return fail()

        // check default value
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val baseTransformation1 = view.baseTransformationParameters
        val transformation1 = view.transformationParameters
        val displayTransformation1 = view.displayTransformationParameters

        // set display type center crop
        activityRule.runOnUiThread {
            view.displayType = GestureBitmapView.DisplayType.CENTER_CROP
        }

        // check
        assertEquals(GestureBitmapView.DisplayType.CENTER_CROP, view.displayType)

        val baseTransformation2 = view.baseTransformationParameters
        val transformation2 = view.transformationParameters
        val displayTransformation2 = view.displayTransformationParameters

        assertNotEquals(baseTransformation1, baseTransformation2)
        assertNotEquals(transformation1, transformation2)
        assertEquals(displayTransformation1.scale, displayTransformation2.scale, ABSOLUTE_ERROR)
        assertEquals(
            displayTransformation1.rotationAngle,
            displayTransformation2.rotationAngle,
            ABSOLUTE_ERROR
        )
        assertEquals(
            displayTransformation1.horizontalTranslation,
            displayTransformation2.horizontalTranslation,
            ABSOLUTE_ERROR
        )
        assertEquals(
            displayTransformation1.verticalTranslation,
            displayTransformation2.verticalTranslation,
            ABSOLUTE_ERROR
        )
    }

    @Test
    fun displayType_whenNoneAndIdentityTransformation_updatesDisplayTransformation() {
        val view = this.view ?: return fail()

        // check default value
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val baseTransformation1 = view.baseTransformationParameters
        val transformation1 = view.transformationParameters
        val displayTransformation1 = view.displayTransformationParameters

        // set display type none
        activityRule.runOnUiThread {
            view.displayType = GestureBitmapView.DisplayType.NONE
            view.transformationParameters = MetricTransformationParameters()
        }

        // check
        assertEquals(GestureBitmapView.DisplayType.NONE, view.displayType)

        val baseTransformation2 = view.baseTransformationParameters
        val transformation2 = view.transformationParameters
        val displayTransformation2 = view.displayTransformationParameters

        assertNotEquals(baseTransformation1, baseTransformation2)
        assertEquals(transformation1, transformation2)
        assertNotEquals(displayTransformation1, displayTransformation2)
    }

    @Test
    fun displayType_whenFitIfBiggerAndIdentityTransformation_keepsTransformations() {
        val view = this.view ?: return fail()

        // check default value
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val baseTransformation1 = view.baseTransformationParameters
        val transformation1 = view.transformationParameters
        val displayTransformation1 = view.displayTransformationParameters

        // set display type fit if bigger
        activityRule.runOnUiThread {
            view.displayType = GestureBitmapView.DisplayType.FIT_IF_BIGGER
            view.transformationParameters = MetricTransformationParameters()
        }

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_IF_BIGGER, view.displayType)

        val baseTransformation2 = view.baseTransformationParameters
        val transformation2 = view.transformationParameters
        val displayTransformation2 = view.displayTransformationParameters

        assertEquals(baseTransformation1, baseTransformation2)
        assertEquals(transformation1, transformation2)
        assertEquals(displayTransformation1, displayTransformation2)
    }

    @Test
    fun displayType_whenFitXTopAndIdentityTransformation_updatesDisplayTransformation() {
        val view = this.view ?: return fail()

        // check default value
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val baseTransformation1 = view.baseTransformationParameters
        val transformation1 = view.transformationParameters
        val displayTransformation1 = view.displayTransformationParameters

        // set display type fit x top
        activityRule.runOnUiThread {
            view.displayType = GestureBitmapView.DisplayType.FIT_X_TOP
            view.transformationParameters = MetricTransformationParameters()
        }

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_X_TOP, view.displayType)

        val baseTransformation2 = view.baseTransformationParameters
        val transformation2 = view.transformationParameters
        val displayTransformation2 = view.displayTransformationParameters

        assertNotEquals(baseTransformation1, baseTransformation2)
        assertEquals(transformation1, transformation2)
        assertNotEquals(displayTransformation1, displayTransformation2)
    }

    @Test
    fun displayType_whenFitXBottomAndIdentityTransformation_updatesDisplayTransformation() {
        val view = this.view ?: return fail()

        // check default value
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val baseTransformation1 = view.baseTransformationParameters
        val transformation1 = view.transformationParameters
        val displayTransformation1 = view.displayTransformationParameters

        // set display type fit x bottom
        activityRule.runOnUiThread {
            view.displayType = GestureBitmapView.DisplayType.FIT_X_BOTTOM
            view.transformationParameters = MetricTransformationParameters()
        }

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_X_BOTTOM, view.displayType)

        val baseTransformation2 = view.baseTransformationParameters
        val transformation2 = view.transformationParameters
        val displayTransformation2 = view.displayTransformationParameters

        assertNotEquals(baseTransformation1, baseTransformation2)
        assertEquals(transformation1, transformation2)
        assertNotEquals(displayTransformation1, displayTransformation2)
    }

    @Test
    fun displayType_whenFitXCenterAndIdentityTransformation_keepsTransformations() {
        val view = this.view ?: return fail()

        // check default value
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val baseTransformation1 = view.baseTransformationParameters
        val transformation1 = view.transformationParameters
        val displayTransformation1 = view.displayTransformationParameters

        // set display type fit x center
        activityRule.runOnUiThread {
            view.displayType = GestureBitmapView.DisplayType.FIT_X_CENTER
            view.transformationParameters = MetricTransformationParameters()
        }

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)

        val baseTransformation2 = view.baseTransformationParameters
        val transformation2 = view.transformationParameters
        val displayTransformation2 = view.displayTransformationParameters

        assertEquals(baseTransformation1, baseTransformation2)
        assertEquals(transformation1, transformation2)
        assertEquals(displayTransformation1, displayTransformation2)
    }

    @Test
    fun displayType_whenFitYLeftAndIdentityTransformation_updatesDisplayTransformation() {
        val view = this.view ?: return fail()

        // check default value
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val baseTransformation1 = view.baseTransformationParameters
        val transformation1 = view.transformationParameters
        val displayTransformation1 = view.displayTransformationParameters

        // set display type fit y left
        activityRule.runOnUiThread {
            view.displayType = GestureBitmapView.DisplayType.FIT_Y_LEFT
            view.transformationParameters = MetricTransformationParameters()
        }

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_Y_LEFT, view.displayType)

        val baseTransformation2 = view.baseTransformationParameters
        val transformation2 = view.transformationParameters
        val displayTransformation2 = view.displayTransformationParameters

        assertNotEquals(baseTransformation1, baseTransformation2)
        assertEquals(transformation1, transformation2)
        assertNotEquals(displayTransformation1, displayTransformation2)
    }

    @Test
    fun displayType_whenFitYRightAndIdentityTransformation_updatesDisplayTransformation() {
        val view = this.view ?: return fail()

        // check default value
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val baseTransformation1 = view.baseTransformationParameters
        val transformation1 = view.transformationParameters
        val displayTransformation1 = view.displayTransformationParameters

        // set display type fit y right
        activityRule.runOnUiThread {
            view.displayType = GestureBitmapView.DisplayType.FIT_Y_RIGHT
            view.transformationParameters = MetricTransformationParameters()
        }

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_Y_RIGHT, view.displayType)

        val baseTransformation2 = view.baseTransformationParameters
        val transformation2 = view.transformationParameters
        val displayTransformation2 = view.displayTransformationParameters

        assertNotEquals(baseTransformation1, baseTransformation2)
        assertEquals(transformation1, transformation2)
        assertNotEquals(displayTransformation1, displayTransformation2)
    }

    @Test
    fun displayType_whenFitYCenterAndIdentityTransformation_updatesDisplayTransformation() {
        val view = this.view ?: return fail()

        // check default value
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val baseTransformation1 = view.baseTransformationParameters
        val transformation1 = view.transformationParameters
        val displayTransformation1 = view.displayTransformationParameters

        // set display type fit y center
        activityRule.runOnUiThread {
            view.displayType = GestureBitmapView.DisplayType.FIT_Y_CENTER
            view.transformationParameters = MetricTransformationParameters()
        }

        // check
        assertEquals(GestureBitmapView.DisplayType.FIT_Y_CENTER, view.displayType)

        val baseTransformation2 = view.baseTransformationParameters
        val transformation2 = view.transformationParameters
        val displayTransformation2 = view.displayTransformationParameters

        assertNotEquals(baseTransformation1, baseTransformation2)
        assertEquals(transformation1, transformation2)
        assertNotEquals(displayTransformation1, displayTransformation2)
    }

    @Test
    fun displayType_whenCenterCropAndIdentityTransformation_updatesDisplayTransformation() {
        val view = this.view ?: return fail()

        // check default value
        assertEquals(GestureBitmapView.DisplayType.FIT_X_CENTER, view.displayType)
        val baseTransformation1 = view.baseTransformationParameters
        val transformation1 = view.transformationParameters
        val displayTransformation1 = view.displayTransformationParameters

        // set display type center crop
        activityRule.runOnUiThread {
            view.displayType = GestureBitmapView.DisplayType.CENTER_CROP
            view.transformationParameters = MetricTransformationParameters()
        }

        // check
        assertEquals(GestureBitmapView.DisplayType.CENTER_CROP, view.displayType)

        val baseTransformation2 = view.baseTransformationParameters
        val transformation2 = view.transformationParameters
        val displayTransformation2 = view.displayTransformationParameters

        assertNotEquals(baseTransformation1, baseTransformation2)
        assertEquals(transformation1, transformation2)
        assertNotEquals(displayTransformation1, displayTransformation2)
    }

    @Test
    fun transformationParametersAndResetTransformationParameters_updatesDisplayTransformation() {
        val view = this.view ?: return fail()

        // check initial transformation
        var transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)
        val identity = Matrix()
        assertEquals(identity, view.transformationMatrix)
        assertNotEquals(identity, view.baseTransformationMatrix)
        assertNotEquals(identity, view.displayTransformationMatrix)

        // set new scale
        activityRule.runOnUiThread {
            transformationParameters.scale = 1.5
            view.transformationParameters = transformationParameters
        }

        // check
        transformationParameters = view.transformationParameters
        assertEquals(1.5, transformationParameters.scale, ABSOLUTE_ERROR)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)
        assertNotEquals(identity, view.transformationMatrix)
        assertNotEquals(identity, view.baseTransformationMatrix)
        assertNotEquals(identity, view.displayTransformationMatrix)

        // set new rotation
        activityRule.runOnUiThread {
            transformationParameters.rotationAngle = Math.PI / 4.0
            view.transformationParameters = transformationParameters
        }

        // check
        transformationParameters = view.transformationParameters
        assertEquals(1.5, transformationParameters.scale, ABSOLUTE_ERROR)
        assertEquals(Math.PI / 4.0, transformationParameters.rotationAngle, ABSOLUTE_ERROR)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)
        assertNotEquals(identity, view.transformationMatrix)
        assertNotEquals(identity, view.baseTransformationMatrix)
        assertNotEquals(identity, view.displayTransformationMatrix)

        // set horizontal translation
        activityRule.runOnUiThread {
            transformationParameters.horizontalTranslation = 40.0
            view.transformationParameters = transformationParameters
        }

        // check
        transformationParameters = view.transformationParameters
        assertEquals(1.5, transformationParameters.scale, ABSOLUTE_ERROR)
        assertEquals(Math.PI / 4.0, transformationParameters.rotationAngle, ABSOLUTE_ERROR)
        assertEquals(40.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)
        assertNotEquals(identity, view.transformationMatrix)
        assertNotEquals(identity, view.baseTransformationMatrix)
        assertNotEquals(identity, view.displayTransformationMatrix)

        // set vertical translation
        activityRule.runOnUiThread {
            transformationParameters.verticalTranslation = 20.0
            view.transformationParameters = transformationParameters
        }

        // check
        transformationParameters = view.transformationParameters
        assertEquals(1.5, transformationParameters.scale, ABSOLUTE_ERROR)
        assertEquals(Math.PI / 4.0, transformationParameters.rotationAngle, ABSOLUTE_ERROR)
        assertEquals(40.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(20.0, transformationParameters.verticalTranslation, 0.0)
        assertNotEquals(identity, view.transformationMatrix)
        assertNotEquals(identity, view.baseTransformationMatrix)
        assertNotEquals(identity, view.displayTransformationMatrix)

        // reset
        activityRule.runOnUiThread {
            view.resetTransformationParameters()
        }

        // check
        transformationParameters = view.transformationParameters
        assertEquals(1.0, transformationParameters.scale, 0.0)
        assertEquals(0.0, transformationParameters.rotationAngle, 0.0)
        assertEquals(0.0, transformationParameters.horizontalTranslation, 0.0)
        assertEquals(0.0, transformationParameters.verticalTranslation, 0.0)
        assertEquals(identity, view.transformationMatrix)
        assertNotEquals(identity, view.baseTransformationMatrix)
        assertNotEquals(identity, view.displayTransformationMatrix)
    }

    @Test
    fun saveRestore_restoresViewTransformationParameters() {
        var view = this.view ?: return fail()
        var activity = this.activity ?: return fail()

        // change parameters
        activityRule.runOnUiThread {
            val transformationParameters = view.transformationParameters
            transformationParameters.scale = 1.1
            transformationParameters.rotationAngle = Math.PI / 10.0
            transformationParameters.horizontalTranslation = 10.0
            transformationParameters.verticalTranslation = 15.0
            view.transformationParameters = transformationParameters
        }

        view.minScale = 0.5f
        view.maxScale = 100.0f
        view.scaleFactorJump = 1.0f
        view.minScaleMargin = 0.2f
        view.maxScaleMargin = 2.0f
        view.leftScrollMargin = 50.0f
        view.topScrollMargin = 51.0f
        view.rightScrollMargin = 52.0f
        view.bottomScrollMargin = 53.0f

        // save state
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val state = Bundle()
        activityRule.runOnUiThread {
            // save activity state
            instrumentation.callActivityOnSaveInstanceState(activity, state)
        }

        activity.finish()

        // restore state
        val intent = Intent(
            InstrumentationRegistry.getInstrumentation().context,
            GestureBitmapViewActivity::class.java
        )
        activity = activityRule.launchActivity(intent)
        this.activity = activity
        view = activity.findViewById(R.id.gesture_bitmap_view_test)
        this.view = view

        // when activity has been launched, parameters have not been restored yet
        val identity = Matrix()
        assertEquals(identity, view.transformationMatrix)

        activityRule.runOnUiThread {
            instrumentation.callActivityOnRestoreInstanceState(activity, state)
        }

        // after state has been recovered, parameters are restored.
        // Bitmaps are not restored, since they will be recycled and it would make
        // bundles to exceed their maximum size, however in this test, the bitmap
        // reappears because it is provided during construction as part of the layout
        // xml parameters
        assertNotNull(view.bitmap)
        val transformationParameters = view.transformationParameters
        assertEquals(transformationParameters.scale, 1.1, ABSOLUTE_ERROR)
        assertEquals(transformationParameters.rotationAngle, Math.PI / 10.0, ABSOLUTE_ERROR)
        assertEquals(transformationParameters.horizontalTranslation, 10.0, ABSOLUTE_ERROR)
        assertEquals(transformationParameters.verticalTranslation, 15.0, ABSOLUTE_ERROR)
        assertEquals(0.5f, view.minScale, 0.0f)
        assertEquals(100.0f, view.maxScale, 0.0f)
        assertEquals(1.0f, view.scaleFactorJump, 0.0f)
        assertEquals(0.2f, view.minScaleMargin, 0.0f)
        assertEquals(2.0f, view.maxScaleMargin, 0.0f)
        assertEquals(50.0f, view.leftScrollMargin, 0.0f)
        assertEquals(51.0f, view.topScrollMargin, 0.0f)
        assertEquals(52.0f, view.rightScrollMargin, 0.0f)
        assertEquals(53.0f, view.bottomScrollMargin, 0.0f)
    }

    @Test
    fun doubleTapGesture_modifiesScaleAndNotifies() {
        val view = this.view ?: return fail()

        view.doubleTapListener = doubleTapListener
        view.minScaleReachedListener = minScaleReachedListener
        view.maxScaleReachedListener = maxScaleReachedListener
        view.scaleAnimationCompletedListener = scaleAnimationCompletedListener
        view.rotateAndTranslateAnimationCompletedListener =
            rotateAndTranslateAnimationCompletedListener

        // ensure that gestures are enabled
        assertTrue(view.scrollEnabled)
        assertTrue(view.rotationEnabled)
        assertTrue(view.scaleEnabled)
        assertTrue(view.doubleTapEnabled)
        assertTrue(view.twoFingerScrollEnabled)

        assertEquals(1.0f, view.minScale, 0.0f)
        assertEquals(10.0f, view.maxScale, 0.0f)
        assertEquals(3.0f, view.scaleFactorJump, 0.0f)
        assertEquals(0.1f, view.minScaleMargin, 0.0f)
        assertEquals(1.0f, view.maxScaleMargin, 0.0f)
        assertEquals(100.0f, view.leftScrollMargin, 0.0f)
        assertEquals(100.0f, view.topScrollMargin, 0.0f)
        assertEquals(100.0f, view.rightScrollMargin, 0.0f)
        assertEquals(100.0f, view.bottomScrollMargin, 0.0f)

        val maxTimes = ((view.maxScale - view.minScale) / view.scaleFactorJump).toInt()
        for (i in 1..maxTimes) {
            reset()
            assertEquals(0, doubleTap)
            assertEquals(0, minScaleReached)
            assertEquals(0, maxScaleReached)
            assertEquals(0, scaleAnimationCompleted)
            assertEquals(0, rotationAndTranslateAnimationCompleted)

            InstrumentationTestHelper.doubleTap(view)

            waitOnCondition({ doubleTap == 0 })

            assertEquals(1, doubleTap)

            waitOnCondition({ scaleAnimationCompleted == 0 })

            assertEquals(1, scaleAnimationCompleted)
        }

        assertEquals(1, maxScaleReached)

        // double tap again to return to minimum scale
        reset()
        assertEquals(0, doubleTap)
        assertEquals(0, minScaleReached)
        assertEquals(0, maxScaleReached)
        assertEquals(0, scaleAnimationCompleted)
        assertEquals(0, rotationAndTranslateAnimationCompleted)

        InstrumentationTestHelper.doubleTap(view)

        waitOnCondition({ doubleTap == 0 })

        assertEquals(1, doubleTap)

        waitOnCondition({ scaleAnimationCompleted == 0 })

        assertEquals(1, scaleAnimationCompleted)

        waitOnCondition({ rotationAndTranslateAnimationCompleted == 0 })

        assertEquals(1, rotationAndTranslateAnimationCompleted)
    }

    //@RequiresEmulator
    @Test
    fun scrollAndFlingGestures_whenReachesBounds_notifies() {
        val view = this.view ?: return fail()

        view.doubleTapListener = doubleTapListener
        view.scaleAnimationCompletedListener = scaleAnimationCompletedListener
        view.bottomBoundReachedListener = bottomBoundReachedListener
        view.rightBoundReachedListener = rightBoundReachedListener
        view.topBoundReachedListener = topBoundReachedListener
        view.leftBoundReachedListener = leftBoundReachedListener
        view.scrollAnimationCompletedListener = scrollAnimationCompletedListener

        // ensure that gestures are enabled
        assertTrue(view.scrollEnabled)
        assertTrue(view.rotationEnabled)
        assertTrue(view.scaleEnabled)
        assertTrue(view.twoFingerScrollEnabled)

        assertEquals(1.0f, view.minScale, 0.0f)
        assertEquals(10.0f, view.maxScale, 0.0f)
        assertEquals(3.0f, view.scaleFactorJump, 0.0f)
        assertEquals(0.1f, view.minScaleMargin, 0.0f)
        assertEquals(1.0f, view.maxScaleMargin, 0.0f)
        assertEquals(100.0f, view.leftScrollMargin, 0.0f)
        assertEquals(100.0f, view.topScrollMargin, 0.0f)
        assertEquals(100.0f, view.rightScrollMargin, 0.0f)
        assertEquals(100.0f, view.bottomScrollMargin, 0.0f)

        // make double tap to increase zoom
        reset()
        assertEquals(0, doubleTap)
        assertEquals(0, scaleAnimationCompleted)

        InstrumentationTestHelper.doubleTap(view)

        waitOnCondition({ doubleTap == 0 })

        assertEquals(1, doubleTap)

        waitOnCondition({ scaleAnimationCompleted == 0 })

        assertEquals(1, scaleAnimationCompleted)

        val xy = IntArray(2)
        view.getLocationOnScreen(xy)

        val viewLeft = xy[0]
        val viewTop = xy[1]

        val rec = view.displayRect ?: return fail()
        val viewWidth = view.width
        val viewHeight = view.height

        val viewCenterX = viewLeft + viewWidth / 2
        val viewCenterY = viewTop + viewHeight / 2

        val viewBottom = viewTop + viewHeight

        val timesH = ceil(rec.width() / viewWidth).toInt()
        val timesV = ceil(rec.height() / viewHeight).toInt()

        reset()
        assertEquals(0, bottomBoundReached)
        assertEquals(0, rightBoundReached)
        assertEquals(0, topBoundReached)
        assertEquals(0, leftBoundReached)

        // drag until bottom border
        for (t in 0 until timesV) {
            InstrumentationTestHelper.drag(
                viewCenterX,
                viewCenterY,
                viewCenterX,
                viewTop
            )
        }

        waitOnCondition({ bottomBoundReached == 0 })

        assertTrue(bottomBoundReached > 0)

        // drag until right border
        for (t in 0 until timesH) {
            InstrumentationTestHelper.drag(
                viewCenterX,
                viewCenterY,
                viewLeft,
                viewCenterY
            )
        }

        waitOnCondition({ rightBoundReached == 0 })

        assertTrue(rightBoundReached > 0)

        // drag until top border
        for (t in 0 until timesV) {
            InstrumentationTestHelper.drag(
                viewCenterX,
                viewCenterY,
                viewCenterX,
                viewBottom
            )
        }

        waitOnCondition({ topBoundReached == 0 })

        assertTrue(topBoundReached > 0)

        // drag until left border
        val fromX = viewCenterX - viewWidth / 3
        val toX = viewCenterX + viewWidth / 3
        for (t in 0 until timesH + 1) {
            InstrumentationTestHelper.drag(
                fromX,
                viewCenterY,
                toX,
                viewCenterY
            )
        }

        waitOnCondition({ leftBoundReached == 0 }, 2 * MAX_RETRIES, 2 * TIMEOUT)

        assertTrue(leftBoundReached > 0)

        assertTrue(scrollAnimationCompleted > 0)
    }

    @Test
    fun pinchGesture_modifiesScale() {
        val view = this.view ?: return fail()

        view.scaleAnimationCompletedListener = scaleAnimationCompletedListener

        // ensure that scale is enabled
        assertTrue(view.scaleEnabled)

        assertEquals(1.0f, view.minScale, 0.0f)
        assertEquals(10.0f, view.maxScale, 0.0f)

        val xy = IntArray(2)
        view.getLocationOnScreen(xy)

        val viewLeft = xy[0]
        val viewTop = xy[1]

        val viewWidth = view.width
        val viewHeight = view.height

        val viewCenterX = viewLeft + viewWidth / 2
        val viewCenterY = viewTop + viewHeight / 2

        val viewWidthHalf = viewWidth / 2
        val distance = viewWidthHalf / 3

        val verticalPosition = viewCenterY - distance
        val startX1 = viewCenterX - distance
        val startX2 = viewCenterX + distance
        val endX1 = viewCenterX - 2 * distance
        val endX2 = viewCenterX + 2 * distance

        // check initial scale
        assertEquals(1.0, view.transformationParameters.scale, 0.0)

        // zoom in
        InstrumentationTestHelper.pinch(
            startX1, verticalPosition, startX2, verticalPosition,
            endX1, verticalPosition, endX2, verticalPosition
        )

        // check scale after gesture
        assertTrue(view.transformationParameters.scale > 1.0)

        // zoom out
        InstrumentationTestHelper.pinch(
            endX1, verticalPosition, endX2, verticalPosition,
            startX1, verticalPosition, startX2, verticalPosition
        )

        // repeat zoom out to reach minimum scale
        InstrumentationTestHelper.pinch(
            endX1, verticalPosition, endX2, verticalPosition,
            startX1, verticalPosition, startX2, verticalPosition
        )

        waitOnCondition({ scaleAnimationCompleted == 0 })

        // check
        assertTrue(scaleAnimationCompleted > 0)
        assertEquals(1.0, view.transformationParameters.scale, 0.0)
    }

    @Test
    fun rotateGesture_modifiesRotationAngle() {
        val view = this.view ?: return fail()

        view.scaleAnimationCompletedListener = scaleAnimationCompletedListener

        // ensure that scale is enabled
        assertTrue(view.scaleEnabled)

        assertEquals(1.0f, view.minScale, 0.0f)
        assertEquals(10.0f, view.maxScale, 0.0f)

        val xy = IntArray(2)
        view.getLocationOnScreen(xy)

        val viewLeft = xy[0]
        val viewTop = xy[1]

        val viewWidth = view.width
        val viewHeight = view.height

        val viewCenterX = viewLeft + viewWidth / 2
        val viewCenterY = viewTop + viewHeight / 2

        val viewWidthHalf = viewWidth / 2
        val distance = viewWidthHalf / 3

        val startX1 = viewCenterX - distance
        val startY1 = viewCenterY - distance
        val startX2 = viewCenterX + distance
        val startY2 = viewCenterY + distance
        val endX1 = viewCenterX + distance
        val endY1 = viewCenterY - distance
        val endX2 = viewCenterX - distance
        val endY2 = viewCenterY + distance

        // check initial rotation angle
        assertEquals(0.0, view.transformationParameters.rotationAngle, 0.0)

        // rotate using a pinch gesture
        InstrumentationTestHelper.pinch(
            startX1, startY1, startX2, startY2,
            endX1, endY1, endX2, endY2
        )

        // check rotation angle after gesture
        assertNotEquals(0.0, view.transformationParameters.rotationAngle, ABSOLUTE_ERROR)
    }

    private fun reset() {
        topBoundReached = 0
        bottomBoundReached = 0
        leftBoundReached = 0
        rightBoundReached = 0
        minScaleReached = 0
        maxScaleReached = 0
        rotationAndTranslateAnimationCompleted = 0
        scaleAnimationCompleted = 0
        scrollAnimationCompleted = 0
        doubleTap = 0
    }

    private fun waitOnCondition(
        condition: () -> Boolean,
        maxRetries: Int = MAX_RETRIES,
        timeout: Long = TIMEOUT
    ) {
        synchronized(lock) {
            var count = 0
            while (condition() && count < maxRetries) {
                lock.wait(timeout)
                count++
            }
        }
    }

    private companion object {
        private const val ABSOLUTE_ERROR = 1e-4
        private const val MAX_RETRIES = 2
        private const val TIMEOUT = 2000L
    }
}