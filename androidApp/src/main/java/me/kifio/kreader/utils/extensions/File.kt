/* Module: r2-testapp-kotlin
* Developers: Quentin Gliosca, Aferdita Muriqi, Clément Baumann
*
* Copyright (c) 2020. European Digital Reading Lab. All rights reserved.
* Licensed to the Readium Foundation under one or more contributor license agreements.
* Use of this source code is governed by a BSD-style license which is detailed in the
* LICENSE file present in the project repository where this source code is maintained.
*/

package me.kifio.kreader.utils.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.util.mediatype.MediaType
import java.io.File
import java.io.FileFilter
import java.io.IOException

suspend fun File.mediaType(mediaTypeHint: String? = null): MediaType =
    MediaType.ofFile(this, mediaType = mediaTypeHint) ?: MediaType.BINARY