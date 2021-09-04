package com.irurueta.android.gesturebitmap

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InvalidMatrixExceptionTest {

    @Test
    fun constructor_returnsExpectedInstance() {
        val exception = InvalidMatrixException()

        assertNotNull(exception)
        assertNull(exception.message)
        assertNull(exception.cause)
    }
}