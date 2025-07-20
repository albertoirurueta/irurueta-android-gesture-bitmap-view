package com.irurueta.android.gesturebitmap

import android.os.Build
import org.junit.Assume
import org.junit.Test
import org.junit.rules.MethodRule
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import java.lang.reflect.Modifier

/**
 * Rule to be used to ignore certain tests under certain conditions based on test
 * methods annotated with [ConditionalIgnore] or [RequiresEmulator].
 */
class ConditionalIgnoreRule : MethodRule {

    /**
     * Modifies [Statement] to be executed by a [Test] method.
     * This method is used to detect whether a given test method is annotated with
     * [ConditionalIgnore] or [RequiresEmulator], and if so a new [Statement] is
     * returned that will ignore the execution of the test.
     *
     * @param base base statement provided by other rules, if any other rule is also used on the
     * same test suite.
     * @param method test method to be executed where rule is applied.
     * @param target the test suite instance where the method will be run.
     * @return provided base [Statement] if no annotation is detected, or a new [IgnoreStatement]
     * if either [ConditionalIgnore] or [RequiresEmulator] annotation is found.
     */
    override fun apply(base: Statement?, method: FrameworkMethod?, target: Any?): Statement? {
        var result = base
        if (hasConditionalIgnoreAnnotation(method)) {
            val condition = getIgnoreCondition(target, method)
            if (condition?.isSatisfied == true) {
                result = IgnoreStatement(condition)
            }
        } else if (hasRequiresEmulatorAnnotation(method)) {
            val condition = NotRunningOnEmulator()
            if (condition.isSatisfied) {
                result = IgnoreStatement(condition)
            }
        }
        return result
    }

    companion object {
        /**
         * Indicates whether a given test method has a [ConditionalIgnore] annotation.
         *
         * @param method test method to be checked.
         * @return true if method has a [ConditionalIgnore] annotation, false otherwise.
         */
        fun hasConditionalIgnoreAnnotation(method: FrameworkMethod?): Boolean {
            return method?.getAnnotation(ConditionalIgnore::class.java) != null
        }

        /**
         * Gets a new [IgnoreCondition] based on provided as a [ConditionalIgnore.condition]
         * Ignore conditions must be defined as a class implementing [IgnoreCondition] with no
         * argument constructor, that will define the condition under which a test method will be
         * ignored.
         *
         * @return created ignore condition.
         */
        fun getIgnoreCondition(target: Any?, method: FrameworkMethod?): IgnoreCondition? {
            val annotation =
                method?.getAnnotation(ConditionalIgnore::class.java)
            return IgnoreConditionCreator(target, annotation).create()
        }

        /**
         * Indicates whether a given test method has a [RequiresEmulator] annotation.
         *
         * @param method test method to be checked.
         *
         */
        fun hasRequiresEmulatorAnnotation(method: FrameworkMethod?): Boolean {
            return method?.getAnnotation(RequiresEmulator::class.java) != null
        }

        /**
         * Indicates whether the test application is being executed on an android emulator.
         *
         * @return true if instrumentation test application is being executed on an emulator,
         * false otherwise.
         */
        fun isEmulator(): Boolean {
            return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.HARDWARE.contains("goldfish")
                    || Build.HARDWARE.contains("ranchu")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.MANUFACTURER.contains("Genymotion")
                    || Build.PRODUCT.contains("sdk_google")
                    || Build.PRODUCT.contains("google_sdk")
                    || Build.PRODUCT.contains("sdk")
                    || Build.PRODUCT.contains("sdk_x86")
                    || Build.PRODUCT.contains("sdk_gphone64_arm64")
                    || Build.PRODUCT.contains("vbox86p")
                    || Build.PRODUCT.contains("emulator")
                    || Build.PRODUCT.contains("simulator"))
        }
    }

    /**
     * Creates a new [IgnoreCondition] instance based on provided [ConditionalIgnore.condition]
     * class provided within a [ConditionalIgnore] annotation of a given test method.
     *
     * @property target test suite instance being executed.
     * @property annotation annotation found on a test method to be used to instantiate its
     * [IgnoreCondition].
     */
    private class IgnoreConditionCreator(
        private val target: Any?,
        private val annotation: ConditionalIgnore?
    ) {

        /**
         * Class of [IgnoreCondition] to be instantiated by this creator.
         */
        private val conditionType
            get() = annotation?.condition

        /**
         * Creates a new [IgnoreCondition] instance provided as a [ConditionalIgnore.condition]
         * class.
         *
         * This method will fail if provided [IgnoreCondition] is not defined either as a
         * standalone type or as an inner class within the test suite class.
         */
        fun create(): IgnoreCondition? {
            checkConditionType()
            return createCondition()
        }

        /**
         * Checks if provided [IgnoreCondition] is defined either as a standalone
         * type or as an inner class within the test suite class.
         *
         * @throws IllegalArgumentException if [IgnoreCondition] is not properly defined.
         */
        private fun checkConditionType() {
            if (!isConditionTypeStandalone && !isConditionTypeDeclaredInTarget) {
                val msg = """
                    Conditional class '%s' is a member class but was not declared inside the test case using it.
                    Either make this class a static class, standalone class (by declaring it in it's own file) or move it inside the test case using it
                    """.trimIndent()
                throw IllegalArgumentException(String.format(msg, conditionType?.java?.name))
            }
        }

        /**
         * Checks if provided [IgnoreCondition] class within a [ConditionalIgnore] annotation is
         * defined as a standalone type (not as an inner class within another class).
         *
         * @return true if [IgnoreCondition] is a standalone class, false otherwise.
         */
        private val isConditionTypeStandalone: Boolean
            get() {
                val conditionType = this.conditionType ?: return false
                return !conditionType.java.isMemberClass || Modifier.isStatic(conditionType.java.modifiers)
            }

        /**
         * Checks if provided [IgnoreCondition] class with a [ConditionalIgnore] annotation
         * is defined as an inner class within the test suite being executed.
         *
         * @return true if [IgnoreCondition] is an inner class within the test suite.
         */
        private val isConditionTypeDeclaredInTarget: Boolean
            get() {
                val target = this.target ?: return false
                val declaringClass = conditionType?.java?.declaringClass ?: return false
                return target.javaClass.isAssignableFrom(declaringClass)
            }

        /**
         * Creates an instance of [IgnoreCondition] to be executed to determine whether
         * test method must be ignored or not.
         *
         * @return a new instance of [IgnoreCondition].
         */
        private fun createCondition(): IgnoreCondition? {
            return if (isConditionTypeStandalone) {
                conditionType?.java?.getDeclaredConstructor()?.newInstance()
            } else {
                conditionType?.java?.getDeclaredConstructor(target?.javaClass)
                    ?.newInstance(target)
            }
        }
    }

    /**
     * Statement to be executed if a [ConditionalIgnore] and its [IgnoreCondition]
     * is satisfied, or if a [RequiresEmulator] annotation is found on a test method.
     * This statement will ignore the test method and will indicate the condition why
     * it was ignored.
     *
     * @param condition instantiated condition to ignore test method.
     */
    private class IgnoreStatement(val condition: IgnoreCondition?) : Statement() {
        override fun evaluate() {
            Assume.assumeTrue("Ignored by " + condition?.javaClass?.simpleName, false)
        }

    }

    /**
     * An implementation of [IgnoreCondition] that can be used to ignore test
     * methods not running on an Android emulator.
     */
    class NotRunningOnEmulator : IgnoreCondition {

        /**
         * Indicates whether ignore condition is satisfied or not.
         * When ignore condition is satisfied on an annotated test method, such
         * method is ignored.
         * This implementation is satisfied if test method is not executed on an
         * Android emulator.
         *
         * @return true if test method is not executed on an Android emulator,
         * false otherwise.
         */
        override val isSatisfied: Boolean
            get() = !isEmulator()
    }
}
