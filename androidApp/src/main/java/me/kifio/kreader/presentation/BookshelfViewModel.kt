package me.kifio.kreader.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.kifio.kreader.copyToLocalFile
import me.kifio.kreader.model.BookRepository
import me.kifio.kreader.model.db.BookDatabase
import me.kifio.kreader.model.entity.BookEntity
import me.kifio.kreader.screenWidth
import org.readium.r2.lcp.LcpService
import org.readium.r2.shared.extensions.mediaType
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.services.coverFitting
import org.readium.r2.streamer.Streamer
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

sealed class BookShelfError {
    object BookNotAddedError : BookShelfError()
    object FileNotCreatedError : BookShelfError()
    object BookCannotBeOpenedError : BookShelfError()
}

data class Book(
    val entity: BookEntity,
    val cover: Bitmap
)

class BookshelfViewModel : ViewModel() {

    var shelfState by mutableStateOf<List<Book>?>(null)
        private set

    var errorsState by mutableStateOf<BookShelfError?>(null)
        private set

    private var booksRepository: BookRepository? = null
    private var streamer: Streamer? = null

    fun setup(ctx: Context) {
        viewModelScope.launch(context = Dispatchers.IO) {

            booksRepository = BookDatabase.getDatabase(ctx).booksDao()
                .let { BookRepository(it) }

            streamer = Streamer(
                ctx,
                contentProtections = listOfNotNull(
                    LcpService(ctx)?.contentProtection()
                )
            )
        }
    }

    fun loadBooks() {
        viewModelScope.launch(context = Dispatchers.IO) {
            val books = booksRepository?.getAllBooks()
                ?.map { entity ->
                    Book(
                        entity = entity,
                        cover = BitmapFactory.decodeFile(entity.coverPath) // FIXME: В качестве превью - рендер первой страницы
                    )
                } ?: emptyList()

            withContext(context = Dispatchers.Main) {
                shelfState = books
            }
        }
    }

    fun saveBookToLocalStorage(ctx: Context, uri: Uri) {
        viewModelScope.launch(context = Dispatchers.Default) {
            when (val localFile = uri.copyToLocalFile(ctx, ctx.filesDir.absolutePath)) {
                null -> errorsState = BookShelfError.FileNotCreatedError
                else -> importPublication(ctx = ctx, localFile = localFile)
            }
        }
    }

    private suspend fun importPublication(ctx: Context, localFile: File) {
        val mediaType = localFile.mediaType()
        val publicationAsset: FileAsset = FileAsset(localFile, mediaType)
        val fileName = "${UUID.randomUUID()}.${mediaType.fileExtension}"
        val libraryAsset = FileAsset(localFile, mediaType)

        streamer?.open(libraryAsset, allowUserInteraction = false)
            ?.onSuccess { publication ->
                try {
                    val coverPath = storeCoverImage(ctx, publication)
                    booksRepository?.insertBook(publication, localFile.extension, coverPath)
                } catch (e: java.lang.IllegalArgumentException) {
                    e.printStackTrace()
                    errorsState = BookShelfError.BookNotAddedError
                }
            }
            ?.onFailure {
                tryOrNull { localFile.delete() }
            }
    }

    private suspend fun storeCoverImage(ctx: Context, publication: Publication): String? =
        withContext(Dispatchers.IO) {
            val coverImageDir = File("${ctx.filesDir}covers/")
            if (!coverImageDir.exists()) {
                coverImageDir.mkdirs()
            }

            val coverImageFile = File("${ctx.filesDir}covers/${publication.metadata.title}.png")

            val bitmap: Bitmap = publication.coverFitting(
                Size(ctx.screenWidth / 2, ctx.screenWidth / 2)
            ) ?: return@withContext null

            var fos: FileOutputStream? = null

            try {
                fos = FileOutputStream(coverImageFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 80, fos)
                return@withContext coverImageFile.absolutePath
            } catch (e: IOException) {
                e.printStackTrace()
                return@withContext null
            } finally {
                fos?.flush()
                fos?.close()
            }
        }
}