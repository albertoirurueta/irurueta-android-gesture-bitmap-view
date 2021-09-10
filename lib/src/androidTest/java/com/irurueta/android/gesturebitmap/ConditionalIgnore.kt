package com.irurueta.android.gesturebitmap

import kotlin.reflect.KClass

/**
 * Annotation that can be used on test methods that need to be ignored if
 * provided [IgnoreCondition] is satisfied.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ConditionalIgnore(val condition: KClass<out IgnoreCondition>)
