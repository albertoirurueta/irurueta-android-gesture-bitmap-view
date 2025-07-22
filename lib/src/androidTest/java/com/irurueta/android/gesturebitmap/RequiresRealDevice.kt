package com.irurueta.android.gesturebitmap

/**
 * Annotation that can be used on test method that need to be ignored if
 * they are not executed on a real Android device.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class RequiresRealDevice
