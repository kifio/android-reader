package me.kifio.kreader.android.bookshelf

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.kifio.kreader.android.R
import me.kifio.kreader.android.model.Book
import java.io.File


@Composable
fun BookshelfView(
    ctx: Context,
    viewModel: BookshelfViewModel,
    openFilePicker: () -> Unit,
    openBook: (Long) -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier
                    .height(
                        WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 56.dp
                    ),
                backgroundColor = MaterialTheme.colors.background,
                contentColor = MaterialTheme.colors.onBackground,
                title = { AppBarTitle(viewModel) },
                actions = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_add_circle_outline_24),
                        contentDescription = "",
                        colorFilter = ColorFilter.tint(MaterialTheme.colors.onBackground),
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
        content = { padding ->
            Box(
                modifier = Modifier
                    .padding(
                        top = padding.calculateTopPadding(),
                        start = padding.calculateStartPadding(LayoutDirection.Ltr),
                        end = padding.calculateEndPadding(LayoutDirection.Ltr),
                        bottom = WindowInsets.navigationBars
                            .asPaddingValues()
                            .calculateBottomPadding()
                    )
            ) {
                Content(ctx, viewModel, scaffoldState, coroutineScope, openBook)
            }
        }
    )
}

@Composable
fun AppBarTitle(viewModel: BookshelfViewModel) {
    Text(
        modifier = Modifier
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
        text = stringResource(id = R.string.bookshelf_screen_title),
        style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
fun Content(
    ctx: Context,
    viewModel: BookshelfViewModel,
    scaffoldState: ScaffoldState,
    coroutineScope: CoroutineScope,
    openBook: (Long) -> Unit
) {
    val books = viewModel.shelfState
    when (viewModel.errorsState) {
        BookShelfError.BookAlreadyExist -> Toast.makeText(
            ctx,
            stringResource(id = R.string.book_already_exist),
            Toast.LENGTH_SHORT
        ).show()
        BookShelfError.FileNotCreatedError -> Toast.makeText(
            ctx,
            stringResource(id = R.string.file_not_created_error),
            Toast.LENGTH_SHORT
        ).show()
        BookShelfError.PublicationOpeningError -> Toast.makeText(
            ctx,
            stringResource(id = R.string.publication_opening_error),
            Toast.LENGTH_SHORT
        ).show()
        null -> {}
    }
    viewModel.clearError()
    when {
        books == null -> ProgressBar()
        books.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                color = MaterialTheme.colors.onBackground,
                fontSize = 14.sp,
                text = stringResource(id = R.string.bookshelf_is_empty)
            )
        }
        else -> BookshelfContent(
            ctx = ctx,
            books = books,
            viewModel = viewModel,
            openBook = openBook
        )
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun BookshelfContent(
    ctx: Context,
    books: List<Book>,
    viewModel: BookshelfViewModel,
    openBook: (Long) -> Unit
) {
    LazyColumn {
        itemsIndexed(
            items = books,
            key = { _, book ->
                book.id
            }
        ) { _, book ->
            val dismissState = rememberDismissState(
                initialValue = DismissValue.Default,
                confirmStateChange = {
                    if (it == DismissValue.DismissedToStart) {
                        viewModel.deleteBook(book)
                    }
                    true
                }
            )

            SwipeToDismiss(
                state = dismissState,
                directions = setOf(DismissDirection.EndToStart),
                dismissThresholds = { FractionalThreshold(0.2f) },
                modifier = Modifier.animateItemPlacement(),
                background = {

                    val color by animateColorAsState(
                        when (dismissState.targetValue) {
                            DismissValue.Default -> MaterialTheme.colors.background
                            else -> Color.Red
                        }
                    )

                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(color)
                            .padding(end = with(LocalDensity.current) { (ctx.screenWidth * 0.1F).toDp() }),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        if (dismissState.targetValue != DismissValue.Default) {
                            Icon(
                                Icons.Default.Delete,
                                tint = Color.White,
                                contentDescription = "Delete Icon",
                                modifier = Modifier.scale(1.5F)
                            )
                        }
                    }
                },
                dismissContent = {
                    BookItem(
                        ctx = ctx,
                        book = book,
                        viewModel = viewModel,
                        openBook = openBook
                    )
                }
            )
        }
    }
}

@Composable
fun ProgressBar() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colors.onBackground)
    }
}

@OptIn(ExperimentalUnitApi::class)
@Composable
fun BookItem(ctx: Context, book: Book, viewModel: BookshelfViewModel, openBook: (Long) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { viewModel.openBook(ctx, book) { openBook(it) } })
            .background(MaterialTheme.colors.background)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        AsyncImage(
            model = File("${ctx.filesDir}covers/${book.id}.png"),
            contentDescription = "",
            modifier = Modifier
                .width((LocalConfiguration.current.screenWidthDp.dp).div(2.5f))
                .shadow(elevation = 4.dp, shape = MaterialTheme.shapes.small)
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = book.title,
                color = MaterialTheme.colors.onBackground,
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