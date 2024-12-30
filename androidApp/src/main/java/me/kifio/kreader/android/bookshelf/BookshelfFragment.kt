package me.kifio.kreader.android.bookshelf

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import me.kifio.kreader.android.R
import me.kifio.kreader.android.model.Book
import me.kifio.kreader.android.reader.ReaderFragment
import me.kifio.kreader.android.reader.ReaderViewModel
import org.readium.r2.shared.publication.Publication

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
    private val readerVM: ReaderViewModel by activityViewModels()

    private val getContent =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { bookShelfVM.saveBookToLocalStorage(requireContext(), it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setFragmentResultListener(ReaderFragment.FRAGMENT_REQUEST_KEY) { _ , _ ->
            readerVM.closePublication()
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.Default)
            post {
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
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    activity?.finish()
                }
            }
        )
    }

    private fun openFilePicker() =
        getContent.launch(arrayOf("application/epub+zip"))

    private fun openBook(book: Book) {
        viewLifecycleOwner.lifecycleScope.launch {
            val publication = readerVM.openPublication(book.id)

            if (publication == null) {
                Toast.makeText(requireContext(), R.string.publication_opening_error, Toast.LENGTH_SHORT).show()
                return@launch
            }

            when {
                publication.conformsTo(Publication.Profile.EPUB) ->
                    BookshelfFragmentDirections.actionBookshelfToEpub()
                else -> null
            }?.let { findNavController().navigate(it) }
        }
    }
}