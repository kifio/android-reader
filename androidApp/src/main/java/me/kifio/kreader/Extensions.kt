package me.kifio.kreader

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.util.mediatype.MediaType
import java.io.File
import java.io.InputStream
import java.util.*

suspend fun Uri.copyToTempFile(context: Context, dir: String): File? = tryOrNull {
    val filename = UUID.randomUUID().toString()
    val mediaType = MediaType.ofUri(this, context.contentResolver)
    val path = "$dir$filename.${mediaType?.fileExtension ?: "tmp"}"
    ContentResolverUtil.getContentInputStream(context, this, path)
    return@tryOrNull File(path)
}

suspend fun InputStream.toFile(path: String) {
    withContext(Dispatchers.IO) {
        use { input ->
            File(path).outputStream().use { input.copyTo(it) }
        }
    }
}

suspend fun InputStream.copyToTempFile(dir: String): File? = tryOrNull {
    val filename = UUID.randomUUID().toString()
    File(dir + filename)
        .also { toFile(it.path) }
}