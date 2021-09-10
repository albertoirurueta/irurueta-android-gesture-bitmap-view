package com.irurueta.android.gesturebitmap

/**
 * Interface to be implemented by conditions provided
 * in [ConditionalIgnore] annotations.
 */
interface IgnoreCondition {

    /**
     * Indicates whether ignore condition is satisfied or not.
     * When ignore condition is satisfied on an annotated test method, such
     * method is ignored.
     *
     * @return true if test method must be ignored, false otherwise.
     */
    val isSatisfied: Boolean
}