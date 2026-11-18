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

import android.content.ContentResolver
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.TypedValue
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Factories of [ImageSource] instances backed by an encoded image (JPEG, PNG or WebP) that is decoded
 * one region at a time.
 */
object ImageSources {

    private const val NINE_PATCH_SUFFIX = ".9.png"
    private const val XML_SUFFIX = ".xml"

    private val REGION_DECODABLE_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")

    /**
     * Creates an image source from a raw drawable resource.
     *
     * @param resources resources containing the drawable.
     * @param resId identifier of the drawable resource.
     * @return created image source, which must be closed when no longer needed.
     * @throws IOException if the resource cannot be read or its format is not supported.
     */
    @JvmStatic
    fun fromResource(resources: Resources, resId: Int): ImageSource =
        RegionDecoderImageSource { resources.openRawResource(resId) }

    /**
     * Creates an image source from an asset.
     *
     * @param assets asset manager containing the asset.
     * @param path path of the asset.
     * @return created image source, which must be closed when no longer needed.
     * @throws IOException if the asset cannot be read or its format is not supported.
     */
    @JvmStatic
    fun fromAsset(assets: AssetManager, path: String): ImageSource =
        RegionDecoderImageSource { assets.open(path) }

    /**
     * Creates an image source from a file.
     *
     * @param file file containing the encoded image.
     * @return created image source, which must be closed when no longer needed.
     * @throws IOException if the file cannot be read or its format is not supported.
     */
    @JvmStatic
    fun fromFile(file: File): ImageSource = RegionDecoderImageSource { FileInputStream(file) }

    /**
     * Creates an image source from a content URI.
     *
     * @param contentResolver resolver used to open the URI.
     * @param uri URI of the encoded image.
     * @return created image source, which must be closed when no longer needed.
     * @throws IOException if the URI cannot be opened or its format is not supported.
     */
    @JvmStatic
    fun fromUri(contentResolver: ContentResolver, uri: Uri): ImageSource =
        RegionDecoderImageSource {
            contentResolver.openInputStream(uri)
                ?: throw FileNotFoundException("Unable to open $uri")
        }

    /**
     * Creates an image source from an encoded image held in memory.
     *
     * The array is not copied and must not be modified while the source is in use.
     *
     * @param bytes encoded image.
     * @return created image source, which must be closed when no longer needed.
     * @throws IOException if the format is not supported.
     */
    @JvmStatic
    fun fromBytes(bytes: ByteArray): ImageSource =
        RegionDecoderImageSource { ByteArrayInputStream(bytes) }

    /**
     * Indicates whether a drawable resource can be displayed through a region decoder.
     *
     * XML drawables and nine-patches are rejected by their resource file name. Any other resource must
     * be reported as JPEG, PNG or WebP by a bounds-only decode.
     *
     * @param resources resources containing the drawable.
     * @param resId identifier of the drawable resource.
     * @return true if the resource can be region decoded, false otherwise (including when it does not
     * exist).
     */
    internal fun isRegionDecodable(resources: Resources, resId: Int): Boolean {
        return try {
            val value = TypedValue()
            resources.getValue(resId, value, true)
            val name = value.string?.toString()?.lowercase() ?: return false
            if (name.endsWith(XML_SUFFIX) || name.endsWith(NINE_PATCH_SUFFIX)) {
                return false
            }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resources.openRawResource(resId).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            options.outMimeType?.lowercase() in REGION_DECODABLE_MIME_TYPES
        } catch (_: Resources.NotFoundException) {
            false
        } catch (_: IOException) {
            false
        }
    }
}
