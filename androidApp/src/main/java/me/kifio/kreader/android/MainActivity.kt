package me.kifio.kreader.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.kifio.kreader.android.bookshelf.BookshelfView
import me.kifio.kreader.android.bookshelf.BookshelfViewModel
import me.kifio.kreader.android.reader.ReaderActivityContract
import org.readium.r2.shared.extensions.tryOrLog

sealed class Screen(val route: String) {
    object Splash : Screen("splash_screen")
    object Home : Screen("home_screen")
}

@Composable
fun SplashScreen(navController: NavHostController, splashViewModel: SplashViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (splashViewModel.readyState) {
            navController.navigate(Screen.Home.route)
        }
    }
}

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    bookShelfViewModel: BookshelfViewModel,
    splashViewModel: SplashViewModel,
    openFilePicker: () -> Unit,
    openBook: (Long) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(route = Screen.Splash.route) {
            SplashScreen(navController = navController, splashViewModel = splashViewModel)
        }
        composable(route = Screen.Home.route) {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    BookshelfView(
                        navController.context,
                        bookShelfViewModel,
                        openFilePicker,
                        openBook
                    )
                }
            }
        }
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    val colors = if (darkTheme) {
        darkColors(
            background = Color.White,
            onBackground = Color.Black,
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

class MainActivity : ComponentActivity() {

    private val bookShelfViewModel: BookshelfViewModel by viewModels()
    private val splashViewModel: SplashViewModel by viewModels()

    private val getContent =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { bookShelfViewModel.saveBookToLocalStorage(this, it) }
        }

    private val readerLauncher: ActivityResultLauncher<ReaderActivityContract.Arguments> =
        registerForActivityResult(ReaderActivityContract()) { input ->
            input?.let { tryOrLog { bookShelfViewModel.closeBook(this, input.bookId) } }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        bookShelfViewModel.setup(this)
        splashViewModel.showSplash()

        setContent {
            val navController = rememberNavController()
            SetupNavGraph(
                navController = navController,
                bookShelfViewModel = bookShelfViewModel,
                splashViewModel = splashViewModel,
                openBook = ::openBook,
                openFilePicker = ::openFilePicker
            )
        }
    }

    private fun openFilePicker() =
        getContent.launch(arrayOf("application/epub+zip", "application/pdf"))

    private fun openBook(bookId: Long) =
        readerLauncher.launch(ReaderActivityContract.Arguments(bookId))
}