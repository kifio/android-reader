/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.reader

import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.kifio.kreader.android.Application
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.ExperimentalDecorator
import org.readium.r2.shared.Search
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.search.SearchIterator
import org.readium.r2.shared.publication.services.search.SearchTry
import org.readium.r2.shared.util.Try
import me.kifio.kreader.android.bookshelf.BookRepository
import me.kifio.kreader.android.model.Bookmark
import me.kifio.kreader.android.search.SearchPagingSource
import me.kifio.kreader.android.utils.EventChannel
import org.json.JSONObject

@OptIn(
    Search::class,
    ExperimentalDecorator::class,
    ExperimentalCoroutinesApi::class,
)
class ReaderViewModel(
    val readerInitData: ReaderInitData,
    private val bookRepository: BookRepository,
) : ViewModel() {

    val publication: Publication =
        readerInitData.publication

    val bookId: Long =
        readerInitData.bookId

    val activityChannel: EventChannel<Event> =
        EventChannel(Channel(Channel.BUFFERED), viewModelScope)

    val fragmentChannel: EventChannel<FeedbackEvent> =
        EventChannel(Channel(Channel.BUFFERED), viewModelScope)

    private var _bookmarks: MutableList<Bookmark> = mutableListOf()
    private var _locations: MutableList<Locator.Locations> = mutableListOf()

    val bookmarks: List<Bookmark>
        get() = _bookmarks

    val locations: List<Locator.Locations>
        get() = _locations

    init {
        viewModelScope.launch {
            _bookmarks.addAll(bookRepository.bookmarksForBook(bookId = bookId))
            _locations.addAll(bookmarks.map { it.locations() })
        }
    }

    fun saveProgression(locator: Locator) = viewModelScope.launch {
        bookRepository.saveProgression(locator, bookId)
    }

    fun insertBookmark(locator: Locator) = viewModelScope.launch {
        with(bookRepository.insertBookmark(bookId, publication, locator)) {
            _bookmarks.add(this)
            _locations.add(this.locations())
            fragmentChannel.send(FeedbackEvent.BookmarkSuccessfullyAdded)
        }
    }

    fun deleteBookmark(locator: Locator) = viewModelScope.launch {
        val bookmark: Bookmark = _bookmarks.find {
            val l = Locator.Locations.fromJSON(JSONObject(it.location))
            l == locator.locations
        } ?: return@launch

        val id = bookmark.id ?: return@launch

        _locations.remove(bookmark.locations())
        deleteBookmark(id)
    }

    fun deleteBookmark(id: Long) = viewModelScope.launch {
        _bookmarks.removeIf { it.id == id }
        bookRepository.deleteBookmark(id)
        fragmentChannel.send(FeedbackEvent.BookmarkSuccessfullyRemoved)
    }

    fun Bookmark.locations(): Locator.Locations {
        return Locator.Locations.fromJSON(JSONObject(this.location))
    }

    fun updateBookmarkIcon(isBookmarkPage: Boolean) {
        activityChannel.send(Event.UpdateBookmarkRequested(isBookmarkPage))
    }

    fun toggleUIVisibility(navigated: Boolean) {
        activityChannel.send(Event.ToggleUIVisibilityRequested(navigated))
    }

    fun openOutlineFragment() {
        activityChannel.send(Event.OpenOutlineRequested)
    }

    val searchLocators: StateFlow<List<Locator>> get() = _searchLocators
    private var _searchLocators = MutableStateFlow<List<Locator>>(emptyList())

    /**
     * Maps the current list of search result locators into a list of [Decoration] objects to
     * underline the results in the navigator.
     */
    val searchDecorations: Flow<List<Decoration>> by lazy {
        searchLocators.map {
            it.mapIndexed { index, locator ->
                Decoration(
                    // The index in the search result list is a suitable Decoration ID, as long as
                    // we clear the search decorations between two searches.
                    id = index.toString(),
                    locator = locator,
                    style = Decoration.Style.Underline(tint = Color.RED)
                )
            }
        }
    }

    private var lastSearchQuery: String? = null

    private var searchIterator: SearchIterator? = null

    private val pagingSourceFactory = InvalidatingPagingSourceFactory {
        SearchPagingSource(listener = PagingSourceListener())
    }

    inner class PagingSourceListener : SearchPagingSource.Listener {
        override suspend fun next(): SearchTry<LocatorCollection?> {
            val iterator = searchIterator ?: return Try.success(null)
            return iterator.next().onSuccess {
                _searchLocators.value += (it?.locators ?: emptyList())
            }
        }
    }

    val searchResult: Flow<PagingData<Locator>> =
        Pager(PagingConfig(pageSize = 20), pagingSourceFactory = pagingSourceFactory)
            .flow.cachedIn(viewModelScope)

    sealed class Event {
        object OpenOutlineRequested : Event()
        data class ToggleUIVisibilityRequested(val navigated: Boolean) : Event()
        data class UpdateBookmarkRequested(val isBookmarkedPage: Boolean) : Event()
    }


    sealed class FeedbackEvent {
        object BookmarkSuccessfullyAdded : FeedbackEvent()
        object BookmarkSuccessfullyRemoved : FeedbackEvent()
    }

    class Factory(
        private val application: Application,
        private val arguments: ReaderActivityContract.Arguments,
    ) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            when {
                modelClass.isAssignableFrom(ReaderViewModel::class.java) -> {
                    val readerInitData =
                        try {
                            val readerRepository = application.readerRepository.getCompleted()
                            readerRepository[arguments.bookId]!!
                        } catch (e: Exception) {
                            // Fallbacks on a dummy Publication to avoid crashing the app until the Activity finishes.
                            dummyReaderInitData(arguments.bookId)
                        }
                    ReaderViewModel(readerInitData, application.bookRepository) as T
                }
                else ->
                    throw IllegalStateException("Cannot create ViewModel for class ${modelClass.simpleName}.")
            }

        private fun dummyReaderInitData(bookId: Long): ReaderInitData {
            val metadata = Metadata(identifier = "dummy", localizedTitle = LocalizedString(""))
            val publication = Publication(Manifest(metadata = metadata))
            return VisualReaderInitData(bookId, publication)
        }
    }
}
