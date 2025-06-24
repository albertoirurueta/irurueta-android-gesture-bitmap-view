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
import java.io.Serializable
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Contains parameters defining a 2D metric transformation.
 *
 * @property scale amount of scale.
 * @property rotationAngle rotation angle expressed in radians.
 * @property horizontalTranslation horizontal translation.
 * @property verticalTranslation vertical translation.
 */
data class MetricTransformationParameters(
    var scale: Double = 1.0,
    var rotationAngle: Double = 0.0,
    var horizontalTranslation: Double = 0.0,
    var verticalTranslation: Double = 0.0
) : Serializable {

    /**
     * Internal values of a matrix. This array is reused for each change in scale and
     * translation.
     */
    private val matrixValues = FloatArray(MATRIX_VALUES_LENGTH)

    /**
     * Converts provided matrix into metric transformation parameters.
     * Notice that matrix form contains an ambiguity respect scale and rotation, since a negative
     * scale also implies a rotation of 180 degrees.
     *
     * @param matrix matrix to extract parameters from.
     * @param assumePositiveScale true indicates that a positive scale value is assumed to solve
     * ambiguity respect scale contained in matrix form. If false, negative scale is assumed.
     * By default this is true.
     * @throws InvalidMatrixException if provided matrix does not contain values valid for a
     * metric transformation.
     */
    @Throws(InvalidMatrixException::class)
    fun fromMatrix(matrix: Matrix, assumePositiveScale: Boolean = true) {
        fromMatrix(matrix, this, assumePositiveScale)
    }

    /**
     * Converts provided metric transformation parameters into a matrix.
     *
     * @param result instance where result will be stored.
     */
    fun toMatrix(result: Matrix) {
        toMatrix(this, result)
    }

    /**
     * Converts provided metric transformation parameters into a matrix and
     * returns the result as a new instance.
     *
     * @return a new matrix containing metric transformation parameters.
     */
    fun toMatrix(): Matrix {
        val result = Matrix()
        toMatrix(result)
        return result
    }

    companion object {
        /**
         * Length of arrays to store matrix values.
         */
        const val MATRIX_VALUES_LENGTH = 9

        /**
         * Converts provided matrix into metric transformation parameters.
         * Notice that matrix form contains an ambiguity respect scale and rotation, since a
         * negative scale also implies a rotation of 180 degrees.
         *
         * @param matrix matrix to extract parameters from.
         * @param result instance where result will be stored.
         * @param assumePositiveScale true indicates that a positive scale value is assumed to solve
         * ambiguity respect scale contained in matrix form. If false, negative scale is assumed.
         * By default this is true.
         * @throws InvalidMatrixException if provided matrix does not contain values valid for a
         * metric transformation.
         */
        @Throws(InvalidMatrixException::class)
        fun fromMatrix(
            matrix: Matrix, result: MetricTransformationParameters,
            assumePositiveScale: Boolean = true
        ) {
            fromMatrix(
                matrix,
                result.matrixValues,
                result,
                assumePositiveScale
            )
        }

        /**
         * Converts provided matrix into a metric transformation parameters and
         * returns the result as a new instance.
         * Notice that matrix form contains an ambiguity respect scale and rotation, since a
         * negative scale also implies a rotation of 180 degrees.
         *
         * @param matrix matrix to extract parameters from.
         * @param assumePositiveScale true indicates that a positive scale value is assumed to solve
         * ambiguity respect scale contained in matrix form. If false, negative scale is assumed.
         * By default this is true.
         * @return a new metric transformation parameters instance.
         * @throws InvalidMatrixException if provided matrix does not contain values valid for a
         * metric transformation.
         */
        @Throws(InvalidMatrixException::class)
        fun fromMatrix(
            matrix: Matrix,
            assumePositiveScale: Boolean = true
        ): MetricTransformationParameters {
            val result = MetricTransformationParameters()
            fromMatrix(matrix, result, assumePositiveScale)
            return result
        }

        /**
         * Converts provided metric transformation parameters into a matrix.
         *
         * @param parameters metric transformation parameters to convert from.
         * @param result instance where result will be stored.
         */
        fun toMatrix(parameters: MetricTransformationParameters, result: Matrix) {
            toMatrix(parameters, parameters.matrixValues, result)
        }

        /**
         * Converts provided metric transformation parameters into a matrix.
         *
         * @param parameters metric transformation parameters to convert from.
         * @return result of conversion into matrix form.
         */
        fun toMatrix(parameters: MetricTransformationParameters): Matrix {
            val result = Matrix()
            toMatrix(parameters, result)
            return result
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
         * - s is the scale factor (assumed to be positive.
         * - and R is a 2x2 rotation matrix following the expression:
         * R =  [cos(theta)  -sin(theta)]
         *      [sin(theta) cos(theta)  ]
         * and theta is the rotation angle expressed in radians
         *
         * Notice that android.graphics.Matrix stores elements in row order.
         * Also notice that matrix form contains an ambiguity respect scale and rotation, since a
         * negative scale also implies a rotation of 180 degrees.
         *
         * @param matrix matrix to extract parameters from.
         * @param matrixValues reusable array containing values of matrix. This is ysed for memory
         * efficiency purposes.
         * @param result instance where extracted parameters will be stored.
         * @param assumePositiveScale true indicates that a positive scale value is assumed to solve
         * ambiguity respect scale contained in matrix form. If false, negative scale is assumed.
         * By default this is true.
         * @throws InvalidMatrixException if provided matrix does not contain values valid for a
         * metric transformation.
         */
        @Throws(InvalidMatrixException::class)
        private fun fromMatrix(
            matrix: Matrix,
            matrixValues: FloatArray,
            result: MetricTransformationParameters,
            assumePositiveScale: Boolean = true
        ) {
            matrix.getValues(matrixValues)

            /*
            A metric transformation matrix must follow expression below (up to scale):
            [s * cos(theta)     -s * sin(theta)     tx]
            [s * sin(theta)     s * cos(theta)      ty]
            [0                  0                   1 ]

            Because the expression is up to scale, non-zero element in third row can differ
            from one

            Notice that values in android matrix are stored in row order
             */

            val matrixScale = matrixValues[8]

            // value 0 is s * cos(theta)
            var scaleAndCosTheta = matrixValues[0]

            // value 3 is s * sin(theta)
            var scaleAndSinTheta = matrixValues[3]

            if (matrixValues[6] != 0.0f
                || matrixValues[7] != 0.0f
                || scaleAndCosTheta != matrixValues[4]
                || matrixValues[1] != -scaleAndSinTheta
                || matrixScale == 0.0f
            ) {
                throw InvalidMatrixException()
            }

            // extract scale
            scaleAndCosTheta /= matrixScale
            scaleAndSinTheta /= matrixScale

            // notice that by trigonometry sin(theta)^2 + cos(theta)^2 = 1

            // s^2*sin(theta)^2 + s^2*cos(theta)^2 = s^2
            val scale2 = scaleAndCosTheta * scaleAndCosTheta + scaleAndSinTheta * scaleAndSinTheta

            // the square root has a sign ambiguity, thus scale could be either positive or negative
            // but this implementation does not support negative scales
            val tmp = sqrt(scale2.toDouble())
            val scale = if (assumePositiveScale) tmp else -tmp

            // extract rotation

            val cosTheta = scaleAndCosTheta / scale
            val sinTheta = scaleAndSinTheta / scale

            val rotationAngle = atan2(sinTheta, cosTheta)

            // extract translation
            val horizontalTranslation = (matrixValues[2] / matrixScale).toDouble()
            val verticalTranslation = (matrixValues[5] / matrixScale).toDouble()

            // store data into result
            result.scale = scale
            result.rotationAngle = rotationAngle
            result.horizontalTranslation = horizontalTranslation
            result.verticalTranslation = verticalTranslation
        }

        /**
         * Sets provided parameters for a 2D metric transformation into provided matrix.
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
         * @param matrixValues reusable array containing values of matrix. This is ysed for memory
         * efficiency purposes.
         * @param result matrix where parameters will be stored.
         */
        private fun toMatrix(
            params: MetricTransformationParameters,
            matrixValues: FloatArray,
            result: Matrix
        ) {

            /*
            Matrix to be set has the form:
            [s * cos(theta)     -s * sin(theta)     tx]
            [s * sin(theta)     s * cos(theta)      ty]
            [0                  0                   1 ]
             */

            // Array of matrix values contains values in row order, hence:
            val s = params.scale
            val theta = params.rotationAngle
            val tx = params.horizontalTranslation
            val ty = params.verticalTranslation

            val cosTheta = cos(theta)
            val sinTheta = sin(theta)

            matrixValues[0] = (s * cosTheta).toFloat()
            matrixValues[1] = (-s * sinTheta).toFloat()
            matrixValues[2] = tx.toFloat()

            matrixValues[3] = (s * sinTheta).toFloat()
            matrixValues[4] = (s * cosTheta).toFloat()
            matrixValues[5] = ty.toFloat()

            matrixValues[6] = 0.0f
            matrixValues[7] = 0.0f
            matrixValues[8] = 1.0f

            result.setValues(matrixValues)
        }
    }
}
