package me.kifio.kreader.android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import me.kifio.kreader.presentation.BookshelfViewModel

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
                    BookshelfView(this, bookShelfVM, ::openFilePicker)
                }
            }
        }
    }

    private fun openFilePicker() {
//        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
//            addCategory(Intent.CATEGORY_OPENABLE)
//        }

        val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { bookShelfVM.saveBookToLocalStorage(this, it) }
        }

        getContent.launch("application/*")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}

@Preview
@Composable
fun DefaultPreview() {
    MyApplicationTheme {
//        BookshelfView()
    }
}
