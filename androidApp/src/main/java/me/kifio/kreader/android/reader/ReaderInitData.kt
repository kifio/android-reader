/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.reader

import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import java.net.URL

data class ReaderInitData(
    val bookId: Long,
    val publication: Publication,
    val initialLocation: Locator? = null
)