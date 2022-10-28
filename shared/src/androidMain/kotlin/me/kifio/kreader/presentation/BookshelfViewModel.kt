package me.kifio.kreader.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.kifio.kreader.model.Book
import me.kifio.kreader.model.BookFormat
import java.io.*

actual class BookshelfViewModel : BaseViewModel() {

    var shelfState by mutableStateOf(mapOf<Book, Bitmap>())
        private set

    private var lcpService = LcpService(application)
        ?.let { Try.success(it) }
        ?: Try.failure(Exception("liblcp is missing on the classpath"))

    fun setup(ctx: Context) {
        viewModelScope.launch(context = Dispatchers.Default) {
            val books = mutableMapOf<Book, Bitmap>()
            for (i in 0..10) {
                with (getBookMock(ctx, i)) {
                    books[this] = getCoverBitmap(this.cover)
                }
            }

            shelfState = books
        }
    }

    fun saveBookToLocalStorage(ctx: Context, uri: Uri) {
        val p = Publication()

        val file = File(
            ctx.filesDir,
            "test_file.pdf"
        )

        ctx.contentResolver.openInputStream(uri).use { input ->
            file.outputStream().use { output ->
                input?.copyTo(output)
            }
        }
    }

    private fun getBookMock(ctx: Context, id: Int): Book = Book(
        id = id,
        title = "Witcher",
        author = "Anjey Sapkowski",
        cover = ctx.assets.open("witcher.txt").bufferedReader().use(BufferedReader::readText),
        format = BookFormat.EPUB,
        progress = 0,
        assetFileName = "witcher.epub"
    )

    private fun getCoverBitmap(base64cover: String) =
        with(Base64.decode(base64cover, Base64.DEFAULT)) {
            BitmapFactory.decodeByteArray(this, 0, size)
        }
}