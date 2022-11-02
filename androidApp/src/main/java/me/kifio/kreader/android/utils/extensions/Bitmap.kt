/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.utils.extensions

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Converts the receiver bitmap into a data URL ready to be used in HTML or CSS.
 *
 * @see https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/Data_URIs
 */
fun Bitmap.toDataUrl(): String {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, stream)
        .also { success -> if (!success) throw Exception("Can't compress image to PNG") }
    return "data:image/png;base64,${Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)}"
}