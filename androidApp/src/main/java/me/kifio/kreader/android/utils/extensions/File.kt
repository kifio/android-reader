/* Module: r2-testapp-kotlin
* Developers: Quentin Gliosca, Aferdita Muriqi, Clément Baumann
*
* Copyright (c) 2020. European Digital Reading Lab. All rights reserved.
* Licensed to the Readium Foundation under one or more contributor license agreements.
* Use of this source code is governed by a BSD-style license which is detailed in the
* LICENSE file present in the project repository where this source code is maintained.
*/

package me.kifio.kreader.android.utils.extensions

import org.readium.r2.shared.util.mediatype.MediaType
import java.io.File

suspend fun File.mediaType(mediaTypeHint: String? = null): MediaType =
    MediaType.ofFile(this, mediaType = mediaTypeHint) ?: MediaType.BINARY