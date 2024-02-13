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
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import me.kifio.kreader.android.Application
import me.kifio.kreader.android.bookshelf.BookRepository
import me.kifio.kreader.android.model.Bookmark
import me.kifio.kreader.android.utils.EventChannel
import org.json.JSONObject
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.ExperimentalDecorator
import org.readium.r2.shared.publication.*
import org.readium.r2.shared.publication.services.positions

@OptIn(
    ExperimentalDecorator::class,
    ExperimentalCoroutinesApi::class,
)
class ReaderViewModel(
    val readerInitData: ReaderInitData,
    private val bookRepository: BookRepository,
) : ViewModel() {

    val publication: Publication =
        readerInitData.publication

    val pagesCount: Int
        get() = _positions.size

    val bookId: Long =
        readerInitData.bookId

    val activityChannel: EventChannel<ActivityEvent> =
        EventChannel(Channel(Channel.BUFFERED), viewModelScope)

    val fragmentChannel: EventChannel<FragmentEvent> =
        EventChannel(Channel(Channel.BUFFERED), viewModelScope)

    private var _positions: MutableList<Locator> = mutableListOf()
    private var _bookmarks: MutableList<Bookmark> = mutableListOf()
    private var _bookmarksLocations: MutableList<Locator.Locations> = mutableListOf()

    val bookmarks: List<Bookmark>
        get() = _bookmarks

    val locations: List<Locator.Locations>
        get() = _bookmarksLocations

    init {
        viewModelScope.launch {
            _bookmarks.addAll(bookRepository.bookmarksForBook(bookId = bookId))
            _bookmarksLocations.addAll(bookmarks.map { it.locations() })
            _positions.addAll(publication.positions())
            activityChannel.send(ActivityEvent.ViewModelReady)
        }
    }

    fun updateProgression(locator: Locator) = viewModelScope.launch {
        bookRepository.saveProgression(locator, bookId)
        activityChannel.send(
            ActivityEvent.UpdateCurrentPage(
                locator.locations.position ?: -1,
                publication.positions().size
            )
        )

        var totalProgress: Double? = locator.locations.totalProgression

        if (totalProgress == null) {
            totalProgress = (locator.locations.position ?: 0).toDouble() / publication.positions().size
        }

        activityChannel.send(ActivityEvent.UpdateProgressBar(totalProgress))
    }

    fun insertBookmark(locator: Locator) = viewModelScope.launch {
        with(bookRepository.insertBookmark(bookId, publication, locator)) {
            _bookmarks.add(this)
            _bookmarksLocations.add(this.locations())
            fragmentChannel.send(FragmentEvent.BookmarkSuccessfullyAdded)
        }
    }

    fun deleteBookmark(locator: Locator) = viewModelScope.launch {
        val bookmark: Bookmark = _bookmarks.find {
            val l = Locator.Locations.fromJSON(JSONObject(it.location))
            l == locator.locations
        } ?: return@launch
        _bookmarksLocations.remove(bookmark.locations())
        deleteBookmark(bookmark)
    }

    fun deleteBookmark(bookmark: Bookmark) = viewModelScope.launch {
        _bookmarks.remove(bookmark)
        _bookmarksLocations.remove(bookmark.locations())
        bookRepository.deleteBookmark(bookmark)
        fragmentChannel.send(FragmentEvent.BookmarkSuccessfullyRemoved)
    }

    fun Bookmark.locations(): Locator.Locations {
        return Locator.Locations.fromJSON(JSONObject(this.location))
    }

    fun updateBookmarkIcon(isBookmarkPage: Boolean) {
        activityChannel.send(ActivityEvent.UpdateBookmarkRequested(isBookmarkPage))
    }

    fun toggleUIVisibility(navigated: Boolean) {
        activityChannel.send(ActivityEvent.ToggleUIVisibilityRequested(navigated))
    }

    fun fragmentBackPressed() {
        activityChannel.send(ActivityEvent.FragmentOnBackPressed)
    }

    fun seekToPage(page: Int) = viewModelScope.launch {
        fragmentChannel.send(FragmentEvent.GoToLocator(_positions[page]))
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

    sealed class ActivityEvent {
        object ViewModelReady : ActivityEvent()
        object FragmentOnBackPressed : ActivityEvent()
        data class ToggleUIVisibilityRequested(val navigated: Boolean) : ActivityEvent()
        data class UpdateBookmarkRequested(val isBookmarkedPage: Boolean) : ActivityEvent()
        data class UpdateCurrentPage(val currentPage: Int, val totalCount: Int) : ActivityEvent()
        data class UpdateProgressBar(val totalProgress: Double) : ActivityEvent()
    }


    sealed class FragmentEvent {
        object BookmarkSuccessfullyAdded : FragmentEvent()
        object BookmarkSuccessfullyRemoved : FragmentEvent()
        data class GoToLocator(val locator: Locator) : FragmentEvent()
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
