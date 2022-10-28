package me.kifio.kreader.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.kifio.kreader.model.BookEntity
import me.kifio.kreader.model.BookFormat
import org.readium.r2.lcp.LcpService
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.streamer.Streamer
import me.kifio.kreader.copyToTempFile
import org.readium.r2.shared.extensions.mediaType
import java.io.*
import java.util.*

class BookshelfViewModel : ViewModel() {

    var shelfState by mutableStateOf(mapOf<BookEntity, Bitmap>())
        private set

    private var streamer: Streamer? = null

    fun setup(ctx: Context) {
        viewModelScope.launch(context = Dispatchers.Default) {
            streamer = Streamer(ctx,
                contentProtections = listOfNotNull(
                    LcpService(ctx)?.contentProtection()
                )
            )
        }
    }

    fun saveBookToLocalStorage(ctx: Context, uri: Uri) {
        viewModelScope.launch(context = Dispatchers.Default) {
            when (val file = uri.copyToTempFile(ctx, ctx.cacheDir.absolutePath)) {
                null -> Log.e("kifio", "Не удалось создать файл для книги")
                else -> importPublication(sourceFile = file)
            }
        }
    }

    private suspend fun importPublication(sourceFile: File) {
        val mediaType = sourceFile.mediaType()
        val publicationAsset: FileAsset = FileAsset(sourceFile, mediaType)
        val fileName = "${UUID.randomUUID()}.${mediaType.fileExtension}"
//        val libraryAsset = FileAsset(File(r2Directory + fileName), mediaType)
    }

    private fun getCoverBitmap(base64cover: String) =
        with(Base64.decode(base64cover, Base64.DEFAULT)) {
            BitmapFactory.decodeByteArray(this, 0, size)
        }
}