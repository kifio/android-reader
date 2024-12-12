package me.kifio.kreader.android.bookshelf

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Surface
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import me.kifio.kreader.android.reader.ReaderActivityContract
import org.readium.r2.shared.extensions.tryOrLog

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    val colors = if (darkTheme) {
        darkColors(
            background = Color(0xFF3C3C3C),
            onBackground = Color.White,
        )
    } else {
        lightColors(
            background = Color.White,
            onBackground = Color.Black,
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

class BookshelfFragment: Fragment() {

    private val bookShelfVM: BookshelfViewModel by activityViewModels()

    private val getContent =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { bookShelfVM.saveBookToLocalStorage(requireContext(), it) }
        }

    private val readerLauncher: ActivityResultLauncher<ReaderActivityContract.Arguments> =
        registerForActivityResult(ReaderActivityContract()) { input ->
            input?.let { tryOrLog { bookShelfVM.closeBook(requireContext(), input.bookId) } }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            // Dispose of the Composition when the view's LifecycleOwner
            // is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MyApplicationTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background
                    ) {
                        BookshelfView(requireContext(), bookShelfVM, ::openFilePicker, ::openBook)
                    }
                }
            }
        }
    }

    private fun openFilePicker() =
        getContent.launch(arrayOf("application/epub+zip", "application/pdf"))

    private fun openBook(bookId: Long) =
        readerLauncher.launch(ReaderActivityContract.Arguments(bookId))
}