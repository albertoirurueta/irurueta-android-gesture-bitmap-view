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

import android.graphics.Bitmap
import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageSourceTest {

    @Test
    fun contentChanged_defaultImplementation_doesNothing() {
        val source = object : ImageSource {
            override val storedWidth = 10
            override val storedHeight = 20
            override val orientation = 1

            override fun decodeRegion(
                region: Rect,
                sampleSize: Int,
                config: Bitmap.Config,
                reuse: Bitmap?
            ): Bitmap? = null

            override fun close() {
                // no-op
            }
        }

        source.contentChanged()

        assertEquals(10, source.storedWidth)
        assertEquals(20, source.storedHeight)
        assertEquals(1, source.orientation)
        assertNull(source.decodeRegion(Rect(), 1, Bitmap.Config.ARGB_8888, null))
    }
}
