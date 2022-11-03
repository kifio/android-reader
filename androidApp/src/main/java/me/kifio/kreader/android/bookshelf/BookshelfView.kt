package me.kifio.kreader.android.bookshelf

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import me.kifio.kreader.android.R
import me.kifio.kreader.android.model.Book
import java.io.File

@Composable
fun BookshelfView(ctx: Context, viewModel: BookshelfViewModel, openFilePicker: () -> Unit, openBook: (Long) -> Unit) =
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier
                    .height(
                        WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 56.dp
                    ),
                title = { AppBarTitle(viewModel) },
                actions = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_add_circle_outline_24),
                        contentDescription = "",
                        modifier = Modifier
                            .padding(
                                top = WindowInsets.statusBars
                                    .asPaddingValues()
                                    .calculateTopPadding(),
                                end = 8.dp
                            )
                            .clickable(onClick = openFilePicker),
                    )
                }
            )
        },
        bottomBar = {
            BottomNavigation(
                modifier = Modifier
                    .height(
                        WindowInsets.navigationBars
                            .asPaddingValues()
                            .calculateBottomPadding() + 56.dp
                    )
                    .background(color = Color.Green),
            ) {

            }
        },
        content = {
            Content(ctx, viewModel, openBook)
        }
    )

@Composable
fun AppBarTitle(viewModel: BookshelfViewModel) {
    Text(
        modifier = Modifier
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
        text = "Bookshelf",
        color = Color.Black,
        style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
fun Content(ctx: Context, viewModel: BookshelfViewModel, openBook: (Long) -> Unit) {
    val books = viewModel.shelfState
    when (books != null) {
        true -> BookshelfContent(ctx = ctx, books = books, viewModel = viewModel, openBook = openBook)
        false -> ProgressBar()
    }
}

@Composable
fun BookshelfContent(ctx: Context, books: List<Book>, viewModel: BookshelfViewModel, openBook: (Long) -> Unit) {
    LazyColumn(

    ) {
        items(books.size) { index ->
            BookItem(ctx = ctx, book = books[index], viewModel = viewModel, openBook = openBook)
        }
    }
}

@Composable
fun ProgressBar() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalUnitApi::class)
@Composable
fun BookItem(ctx: Context, book: Book, viewModel: BookshelfViewModel, openBook: (Long) -> Unit) {
    val configuration = LocalConfiguration.current

    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { viewModel.openBook(ctx, book) { openBook(it) } })
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        AsyncImage(
            model = File("${ctx.filesDir}covers/${book.id}.png"),
            contentDescription = "",
            modifier = Modifier
                .width(screenWidth.div(2.5f))
                .shadow(elevation = 8.dp, shape = MaterialTheme.shapes.medium)
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = book.title,
                color = Color.Black,
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Medium),
                fontSize = TextUnit(value = 20F, type = TextUnitType.Sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = book.author ?: stringResource(R.string.unknown_author), color = Color.Gray,
                style = MaterialTheme.typography.h6.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = TextUnit(value = 14F, type = TextUnitType.Sp)
                ),
            )
        }
    }
}