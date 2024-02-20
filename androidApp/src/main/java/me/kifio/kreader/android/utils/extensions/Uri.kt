/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.utils.extensions

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import org.readium.r2.shared.extensions.tryOrNull
import java.io.File
import java.util.*

fun Uri.copyToLocalFile(context: Context): File? {
    val filename = getNameFromURI(context, this)
    val out = File(context.filesDir, filename)

    context.contentResolver.openInputStream(this).use { input ->
        out.outputStream().use { input?.copyTo(it) }
    }

    return out
}

private fun getNameFromURI(context: Context, uri: Uri): String {
    var result: String? = null
    var cursor: Cursor? = null
    try {
        cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.let {
            it.moveToFirst()
            result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        cursor?.close()
    }
    return result ?: UUID.randomUUID().toString()
}
