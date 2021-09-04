package com.irurueta.android.gesturebitmap

import android.graphics.Matrix
import com.irurueta.geometry.MetricTransformation2D
import com.irurueta.geometry.Rotation2D
import com.irurueta.statistics.UniformRandomizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
class MetricTransformationParametersTest {

    @Test
    fun constructor_whenEmpty_returnsDefaultValues() {
        val parameters = MetricTransformationParameters()

        assertEquals(1.0, parameters.scale, 0.0)
        assertEquals(0.0, parameters.rotationAngle, 0.0)
        assertEquals(0.0, parameters.horizontalTranslation, 0.0)
        assertEquals(0.0, parameters.verticalTranslation, 0.0)
    }

    @Test
    fun constructor_whenNotEmpty_returnsExpectedValues() {
        val scale = Random.Default.nextDouble()
        val rotationAngle = Random.Default.nextDouble()
        val horizontalTranslation = Random.Default.nextDouble()
        val verticalTranslation = Random.Default.nextDouble()

        val parameters = MetricTransformationParameters(
            scale,
            rotationAngle,
            horizontalTranslation,
            verticalTranslation
        )

        assertEquals(scale, parameters.scale, 0.0)
        assertEquals(rotationAngle, parameters.rotationAngle, 0.0)
        assertEquals(horizontalTranslation, parameters.horizontalTranslation, 0.0)
        assertEquals(verticalTranslation, parameters.verticalTranslation, 0.0)
    }

    @Test
    fun scale_returnsExpectedValue() {
        val parameters = MetricTransformationParameters()

        assertEquals(1.0, parameters.scale, 0.0)

        // set new value
        val scale = Random.Default.nextDouble()
        parameters.scale = scale

        // check
        assertEquals(scale, parameters.scale, 0.0)
    }

    @Test
    fun rotationAngle_returnsExpectedValue() {
        val parameters = MetricTransformationParameters()

        assertEquals(0.0, parameters.rotationAngle, 0.0)

        // set new value
        val rotationAngle = Random.Default.nextDouble()
        parameters.rotationAngle = rotationAngle

        // check
        assertEquals(rotationAngle, parameters.rotationAngle, 0.0)
    }

    @Test
    fun horizontalTranslation_returnsExpectedValue() {
        val parameters = MetricTransformationParameters()

        assertEquals(0.0, parameters.horizontalTranslation, 0.0)

        // set new value
        val horizontalTranslation = Random.Default.nextDouble()
        parameters.horizontalTranslation = horizontalTranslation

        // check
        assertEquals(horizontalTranslation, parameters.horizontalTranslation, 0.0)
    }

    @Test
    fun verticalTranslation_returnsExpectedValue() {
        val parameters = MetricTransformationParameters()

        assertEquals(0.0, parameters.verticalTranslation, 0.0)

        // set new value
        val verticalTranslation = Random.Default.nextDouble()
        parameters.verticalTranslation = verticalTranslation

        // check
        assertEquals(verticalTranslation, parameters.verticalTranslation, 0.0)
    }

    @Test
    fun fromMatrix_whenInvalidValues1_throwsInvalidMatrixException() {
        val transformation = getMetricTransformation(true)
        val matrix = toMatrix(transformation)
        val values = FloatArray(MetricTransformationParameters.MATRIX_VALUES_LENGTH)
        matrix.getValues(values)
        values[6] = 1.0f
        matrix.setValues(values)

        val parameters = MetricTransformationParameters()
        assertThrows(InvalidMatrixException::class.java) {
            parameters.fromMatrix(matrix)
        }
    }

    @Test
    fun fromMatrix_whenInvalidValues2_throwsInvalidMatrixException() {
        val transformation = getMetricTransformation(true)
        val matrix = toMatrix(transformation)
        val values = FloatArray(MetricTransformationParameters.MATRIX_VALUES_LENGTH)
        matrix.getValues(values)
        values[7] = 1.0f
        matrix.setValues(values)

        val parameters = MetricTransformationParameters()
        assertThrows(InvalidMatrixException::class.java) {
            parameters.fromMatrix(matrix)
        }
    }

    @Test
    fun fromMatrix_whenInvalidValues3_throwsInvalidMatrixException() {
        val transformation = getMetricTransformation(true)
        val matrix = toMatrix(transformation)
        val values = FloatArray(MetricTransformationParameters.MATRIX_VALUES_LENGTH)
        matrix.getValues(values)
        values[4] *= -1.0f
        matrix.setValues(values)

        val parameters = MetricTransformationParameters()
        assertThrows(InvalidMatrixException::class.java) {
            parameters.fromMatrix(matrix)
        }
    }

    @Test
    fun fromMatrix_whenInvalidValues4_throwsInvalidMatrixException() {
        val transformation = getMetricTransformation(true)
        val matrix = toMatrix(transformation)
        val values = FloatArray(MetricTransformationParameters.MATRIX_VALUES_LENGTH)
        matrix.getValues(values)
        values[1] *= -1.0f
        matrix.setValues(values)

        val parameters = MetricTransformationParameters()
        assertThrows(InvalidMatrixException::class.java) {
            parameters.fromMatrix(matrix)
        }
    }

    @Test
    fun fromMatrix_whenInvalidValues5_throwsInvalidMatrixException() {
        val transformation = getMetricTransformation(true)
        val matrix = toMatrix(transformation)
        val values = FloatArray(MetricTransformationParameters.MATRIX_VALUES_LENGTH)
        matrix.getValues(values)
        values[8] = 0.0f
        matrix.setValues(values)

        val parameters = MetricTransformationParameters()
        assertThrows(InvalidMatrixException::class.java) {
            parameters.fromMatrix(matrix)
        }
    }

    @Test
    fun fromMatrix_whenPositiveScale_setsExpectedParameters() {
        val parameters = MetricTransformationParameters()

        // check initial values
        assertEquals(1.0, parameters.scale, 0.0)
        assertEquals(0.0, parameters.rotationAngle, 0.0)
        assertEquals(0.0, parameters.horizontalTranslation, 0.0)
        assertEquals(0.0, parameters.verticalTranslation, 0.0)

        // set new parameters from matrix
        val transformation = getMetricTransformation(true)
        val matrix = toMatrix(transformation)

        parameters.fromMatrix(matrix)

        // check
        assertEquals(transformation.scale, parameters.scale, ABSOLUTE_ERROR)
        assertEquals(transformation.rotation.theta, parameters.rotationAngle, ABSOLUTE_ERROR)
        assertEquals(transformation.translationX, parameters.horizontalTranslation, ABSOLUTE_ERROR)
        assertEquals(transformation.translationY, parameters.verticalTranslation, ABSOLUTE_ERROR)
    }

    @Test
    fun fromMatrix_whenNegativeScale_setsExpectedParameters() {
        val parameters = MetricTransformationParameters()

        // check initial values
        assertEquals(1.0, parameters.scale, 0.0)
        assertEquals(0.0, parameters.rotationAngle, 0.0)
        assertEquals(0.0, parameters.horizontalTranslation, 0.0)
        assertEquals(0.0, parameters.verticalTranslation, 0.0)

        // set new parameters from matrix
        val transformation = getMetricTransformation(false)
        val matrix = toMatrix(transformation)

        parameters.fromMatrix(matrix, false)

        // check
        assertEquals(transformation.scale, parameters.scale, ABSOLUTE_ERROR)
        assertEquals(transformation.rotation.theta, parameters.rotationAngle, ABSOLUTE_ERROR)
        assertEquals(transformation.translationX, parameters.horizontalTranslation, ABSOLUTE_ERROR)
        assertEquals(transformation.translationY, parameters.verticalTranslation, ABSOLUTE_ERROR)
    }

    @Test
    fun fromMatrix_whenNonUnitMatrixScale_setsExpectedParameters() {
        val parameters = MetricTransformationParameters()

        // check initial values
        assertEquals(1.0, parameters.scale, 0.0)
        assertEquals(0.0, parameters.rotationAngle, 0.0)
        assertEquals(0.0, parameters.horizontalTranslation, 0.0)
        assertEquals(0.0, parameters.verticalTranslation, 0.0)

        // set new parameters from matrix
        val transformation = getMetricTransformation(true)
        val matrix = toMatrix(transformation)
        var values = FloatArray(MetricTransformationParameters.MATRIX_VALUES_LENGTH)
        matrix.getValues(values)
        values = values.map { it * 0.5f }.toFloatArray()
        matrix.setValues(values)

        parameters.fromMatrix(matrix)

        // check
        assertEquals(transformation.scale, parameters.scale, ABSOLUTE_ERROR)
        assertEquals(transformation.rotation.theta, parameters.rotationAngle, ABSOLUTE_ERROR)
        assertEquals(transformation.translationX, parameters.horizontalTranslation, ABSOLUTE_ERROR)
        assertEquals(transformation.translationY, parameters.verticalTranslation, ABSOLUTE_ERROR)
    }

    @Test
    fun fromMatrix_whenCompanionObject_setsExpectedParameters() {
        val parameters = MetricTransformationParameters()

        // check initial values
        assertEquals(1.0, parameters.scale, 0.0)
        assertEquals(0.0, parameters.rotationAngle, 0.0)
        assertEquals(0.0, parameters.horizontalTranslation, 0.0)
        assertEquals(0.0, parameters.verticalTranslation, 0.0)

        // set new parameters from matrix
        val transformation = getMetricTransformation(true)
        val matrix = toMatrix(transformation)

        MetricTransformationParameters.fromMatrix(matrix, parameters)

        // check
        assertEquals(transformation.scale, parameters.scale, ABSOLUTE_ERROR)
        assertEquals(transformation.rotation.theta, parameters.rotationAngle, ABSOLUTE_ERROR)
        assertEquals(transformation.translationX, parameters.horizontalTranslation, ABSOLUTE_ERROR)
        assertEquals(transformation.translationY, parameters.verticalTranslation, ABSOLUTE_ERROR)
    }

    @Test
    fun fromMatrix_whenPositiveScaleAndReturnNew_setsExpectedParameters() {
        // set parameters from matrix
        val transformation = getMetricTransformation(true)
        val matrix = toMatrix(transformation)

        val parameters = MetricTransformationParameters.fromMatrix(matrix)

        // check
        assertEquals(transformation.scale, parameters.scale, ABSOLUTE_ERROR)
        assertEquals(transformation.rotation.theta, parameters.rotationAngle, ABSOLUTE_ERROR)
        assertEquals(transformation.translationX, parameters.horizontalTranslation, ABSOLUTE_ERROR)
        assertEquals(transformation.translationY, parameters.verticalTranslation, ABSOLUTE_ERROR)
    }

    @Test
    fun fromMatrix_whenNegativeScaleAndReturnNew_setsExpectedParameters() {
        // set parameters from matrix
        val transformation = getMetricTransformation(false)
        val matrix = toMatrix(transformation)

        val parameters = MetricTransformationParameters.fromMatrix(matrix, false)

        // check
        assertEquals(transformation.scale, parameters.scale, ABSOLUTE_ERROR)
        assertEquals(transformation.rotation.theta, parameters.rotationAngle, ABSOLUTE_ERROR)
        assertEquals(transformation.translationX, parameters.horizontalTranslation, ABSOLUTE_ERROR)
        assertEquals(transformation.translationY, parameters.verticalTranslation, ABSOLUTE_ERROR)
    }

    @Test
    fun toMatrix_whenPositiveScale_setsExpectedParameters() {
        val transformation = getMetricTransformation(true)
        val parameters = MetricTransformationParameters(
            transformation.scale,
            transformation.rotation.theta,
            transformation.translationX,
            transformation.translationY
        )

        // convert to matrix
        val matrix = Matrix()
        parameters.toMatrix(matrix)

        // check
        val expectedMatrix = toMatrix(transformation)
        assertEquals(expectedMatrix, matrix)
    }

    @Test
    fun toMatrix_whenNegativeScale_setsExpectedParameters() {
        val transformation = getMetricTransformation(false)
        val parameters = MetricTransformationParameters(
            transformation.scale,
            transformation.rotation.theta,
            transformation.translationX,
            transformation.translationY
        )

        // convert to matrix
        val matrix = Matrix()
        parameters.toMatrix(matrix)

        // check
        val expectedMatrix = toMatrix(transformation)
        assertEquals(expectedMatrix, matrix)
    }

    @Test
    fun toMatrix_whenPositiveScaleAndReturnNew_setsExpectedParameters() {
        val transformation = getMetricTransformation(true)
        val parameters = MetricTransformationParameters(
            transformation.scale,
            transformation.rotation.theta,
            transformation.translationX,
            transformation.translationY
        )

        // convert to matrix
        val matrix = parameters.toMatrix()

        // check
        val expectedMatrix = toMatrix(transformation)
        assertEquals(expectedMatrix, matrix)
    }

    @Test
    fun toMatrix_whenNegativeScaleAndReturnNew_setsExpectedParameters() {
        val transformation = getMetricTransformation(false)
        val parameters = MetricTransformationParameters(
            transformation.scale,
            transformation.rotation.theta,
            transformation.translationX,
            transformation.translationY
        )

        // convert to matrix
        val matrix = parameters.toMatrix()

        // check
        val expectedMatrix = toMatrix(transformation)
        assertEquals(expectedMatrix, matrix)
    }

    @Test
    fun toMatrix_whenCompanionObject_setsExpectedParameters() {
        val transformation = getMetricTransformation(true)
        val parameters = MetricTransformationParameters(
            transformation.scale,
            transformation.rotation.theta,
            transformation.translationX,
            transformation.translationY
        )

        // convert to matrix
        val matrix = Matrix()
        MetricTransformationParameters.toMatrix(parameters, matrix)

        // check
        val expectedMatrix = toMatrix(transformation)
        assertEquals(expectedMatrix, matrix)
    }

    @Test
    fun toMatrix_whenCompanionObjectAndReturnNew_setsExpectedParameters() {
        val transformation = getMetricTransformation(true)
        val parameters = MetricTransformationParameters(
            transformation.scale,
            transformation.rotation.theta,
            transformation.translationX,
            transformation.translationY
        )

        // convert to matrix
        val matrix = MetricTransformationParameters.toMatrix(parameters)

        // check
        val expectedMatrix = toMatrix(transformation)
        assertEquals(expectedMatrix, matrix)
    }

    companion object {
        private const val MIN_ANGLE_DEGREES = -180.0
        private const val MAX_ANGLE_DEGREES = 180.0

        private const val MIN_RANDOM_VALUE = -100.0
        private const val MAX_RANDOM_VALUE = 100.0

        private const val ABSOLUTE_ERROR = 1e-5

        private fun getMetricTransformation(positiveScale: Boolean): MetricTransformation2D {
            val randomizer = UniformRandomizer()
            val theta = Math.toRadians(
                randomizer.nextDouble(
                    MIN_ANGLE_DEGREES,
                    MAX_ANGLE_DEGREES
                )
            )
            val rotation = Rotation2D(theta)

            val translation = DoubleArray(MetricTransformation2D.NUM_TRANSLATION_COORDS)
            randomizer.fill(translation, MIN_RANDOM_VALUE, MAX_RANDOM_VALUE)

            val scale = if (positiveScale)
                randomizer.nextDouble(0.0, MAX_RANDOM_VALUE)
            else
                randomizer.nextDouble(MIN_RANDOM_VALUE, 0.0)

            return MetricTransformation2D(rotation, translation, scale)
        }

        private fun toMatrix(transformation: MetricTransformation2D): Matrix {
            // android matrix is represented in row major order, so we need to extract
            // values of transformation in row order when converting to matrix
            val array = transformation.asMatrix().toArray(false).map { it.toFloat() }.toFloatArray()

            val result = Matrix()
            result.setValues(array)

            return result
        }
    }
}