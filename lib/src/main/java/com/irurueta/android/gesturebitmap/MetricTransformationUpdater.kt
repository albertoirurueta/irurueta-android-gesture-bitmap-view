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

import kotlin.math.cos
import kotlin.math.sin

/**
 * Updates the parameters of a metric transformation so that its scale or rotation changes around
 * a pivot point, which stays where it is.
 */
internal object MetricTransformationUpdater {

    /**
     * Sets a new scale keeping provided pivot point fixed.
     * This method does NOT check that new scale is within any allowed range of values.
     *
     * @param parameters parameters to update, containing current scale, rotation and translation.
     * @param scale new scale to be set.
     * @param pivotX horizontal coordinate of pivot point expressed in new scale terms.
     * @param pivotY vertical coordinate of pivot point expressed in new scale terms.
     */
    fun scaleAt(
        parameters: MetricTransformationParameters,
        scale: Double,
        pivotX: Float,
        pivotY: Float
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
    }

    /**
     * Sets a new rotation angle keeping provided pivot point fixed.
     *
     * @param parameters parameters to update, containing current scale, rotation and translation.
     * @param rotationAngle new rotation angle to be set, expressed in radians.
     * @param pivotX horizontal coordinate of pivot point.
     * @param pivotY vertical coordinate of pivot point.
     */
    fun rotateAt(
        parameters: MetricTransformationParameters,
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
    }
}
