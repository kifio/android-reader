package me.kifio.kreader.android.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import me.kifio.kreader.android.bookshelf.BookshelfView
import me.kifio.kreader.android.bookshelf.BookshelfViewModel
import me.kifio.kreader.android.reader.ReaderActivityContract
import org.readium.r2.shared.extensions.tryOrLog

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

//    FontFamily(
//        fonts = arrayOf(
//            Font(R.font.montserrat_regular, weight = FontWeight.Normal),
//            Font(R.font.montserrat_medium, weight = FontWeight.Medium),
//            Font(R.font.montserrat_bold, weight = FontWeight.Bold),
//            Font(R.font.montserrat_semibold, weight = FontWeight.SemiBold),
//        )
//    )

    val colors = if (darkTheme) {
        darkColors(
            primary = Color.White,
            primaryVariant = Color(0xFF3700B3),
            secondary = Color.White,
            onSurface = Color.Black,
        )
    } else {
        lightColors(
            primary = Color.White,
            primaryVariant = Color(0xFF3700B3),
            secondary = Color.White,
            onSurface = Color.Black,
        )
    }
    val typography = Typography(
        defaultFontFamily = FontFamily.Serif
    )
    val shapes = Shapes(
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(0.dp)
    )

    MaterialTheme(
        colors = colors,
        typography = typography,
        shapes = shapes,
        content = content
    )
}

class MainActivity : ComponentActivity() {

    private val bookShelfVM: BookshelfViewModel by viewModels()

    private val getContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { bookShelfVM.saveBookToLocalStorage(this, it) }
        }

    private val readerLauncher: ActivityResultLauncher<ReaderActivityContract.Arguments> =
        registerForActivityResult(ReaderActivityContract()) { input ->
            input?.let { tryOrLog { bookShelfVM.closeBook(input.bookId) } }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        bookShelfVM.setup(this)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    BookshelfView(this, bookShelfVM, ::openFilePicker, ::openBook)
                }
            }
        }
    }

    private fun openFilePicker() = getContent.launch("application/*")

    private fun openBook(bookId: Long) = readerLauncher.launch(ReaderActivityContract.Arguments(bookId))
}

@Preview
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
//        BookshelfView()
    }
}
