package com.irurueta.android.gesturebitmap

/**
 * Annotation that can be used on test method that need to be ignored if
 * they are not executed on an Android emulator.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class RequiresEmulator
