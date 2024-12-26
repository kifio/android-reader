/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.reader

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            fragmentChannel.send(FragmentEvent.ViewModelReady)
        }
    }

    fun updateProgression(locator: Locator) = viewModelScope.launch {
        val asdf = bookRepository.saveProgression(locator, bookId)

        var totalProgress: Double? = locator.locations.totalProgression

        if (totalProgress == null) {
            totalProgress = (locator.locations.position ?: 0).toDouble() / publication.positions().size
        }

        fragmentChannel.send(
            FragmentEvent.UpdateCurrentPage(
                locator.locations.position ?: -1,
                publication.positions().size,
                totalProgress
            )
        )
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

    fun seekToPage(page: Int) = viewModelScope.launch {
        fragmentChannel.send(FragmentEvent.GoToLocator(_positions[page]))
    }

    fun closePublication(ctx: Context) {
        val readerRepository = (ctx.applicationContext as Application).readerRepository
        readerRepository.close()
    }

    sealed class FragmentEvent {
        object ViewModelReady : FragmentEvent()
        object BookmarkSuccessfullyAdded : FragmentEvent()
        object BookmarkSuccessfullyRemoved : FragmentEvent()
        data class GoToLocator(val locator: Locator) : FragmentEvent()
        data class UpdateCurrentPage(
            val currentPage: Int,
            val totalCount: Int,
            val totalProgress: Double
        ) : FragmentEvent()
    }

    class Factory(private val application: Application) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            when {
                modelClass.isAssignableFrom(ReaderViewModel::class.java) -> {
                    val readerInitData = application.readerRepository.get()
                    ReaderViewModel(readerInitData, application.bookRepository) as T
                }
                else ->
                    throw IllegalStateException("Cannot create ViewModel for class ${modelClass.simpleName}.")
            }

        private fun dummyReaderInitData(bookId: Long): ReaderInitData {
            val metadata = Metadata(identifier = "dummy", localizedTitle = LocalizedString(""))
            val publication = Publication(Manifest(metadata = metadata))
            return ReaderInitData(bookId, publication)
        }
    }
}
