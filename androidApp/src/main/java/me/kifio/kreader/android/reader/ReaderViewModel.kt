/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.reader

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import me.kifio.kreader.android.Application
import me.kifio.kreader.android.bookshelf.BookRepository
import me.kifio.kreader.android.model.Bookmark
import me.kifio.kreader.android.utils.EventChannel
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.asset.FileAsset
import org.readium.r2.shared.publication.services.isRestricted
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.publication.services.protectionError
import org.readium.r2.streamer.Streamer
import java.io.File


class ReaderViewModel(
    private val bookRepository: BookRepository,
    private val streamer: Streamer
) : ViewModel() {

    private var _publication: Publication? = null
    private var _initialLocator: Locator? = null
    private var _bookId: Long? = null
    private var _pagesCount: Int = 0
    private var _bookmarks: MutableList<Bookmark> = mutableListOf()
    private var _bookmarksLocations: MutableList<Locator.Locations> = mutableListOf()

    private val bookId: Long
        get() = _bookId ?: throw IllegalStateException()

    val publication: Publication
        get() = _publication ?: throw IllegalStateException()

    val pagesCount: Int
        get() = _pagesCount

    val fragmentChannel: EventChannel<FragmentEvent> =
        EventChannel(Channel(Channel.BUFFERED), viewModelScope)

    val activityChannel: EventChannel<ActivityEvent> =
        EventChannel(Channel(Channel.BUFFERED), viewModelScope)

    val bookmarks: List<Bookmark>
        get() = _bookmarks

    val locations: List<Locator.Locations>
        get() = _bookmarksLocations

    suspend fun openPublication(bookId: Long): Publication? {
        try {
            _bookmarks.addAll(bookRepository.bookmarksForBook(bookId = bookId))
            _bookmarksLocations.addAll(bookmarks.map { it.locations() })
            _bookId = bookId
            openPublication()
            return _publication
        } catch (e: Exception) {
            e.printStackTrace()
            closePublication()
            return null
        }
    }

    @Throws(Exception::class)
    private suspend fun openPublication() {
        val book = bookRepository.get(bookId)
            ?: throw IllegalStateException("Cannot find book in database.")

        val file = File(book.href)
        require(file.exists())
        val asset = FileAsset(file)

        val publication = streamer.open(asset, allowUserInteraction = true)
            .getOrThrow()

        if (publication.isRestricted) {
            throw publication.protectionError
                ?: IllegalStateException()
        }

        _publication = publication
        _initialLocator = book.progression?.let { Locator.fromJSON(JSONObject(it)) }
        _pagesCount = publication.positions().size
    }

    fun openReader() = viewModelScope.launch {
        fragmentChannel.send(
            FragmentEvent.PublicationReady(publication, _initialLocator)
        )
    }

    fun updateProgression(locator: Locator) = viewModelScope.launch {
        val page = locator.locations.position ?: return@launch

        _initialLocator = locator
        bookRepository.saveProgression(locator, bookId)

        var totalProgress: Double? = locator.locations.totalProgression

        if (totalProgress == null) {
            totalProgress = page.toDouble() / publication.positions().size
        }

        fragmentChannel.send(
            FragmentEvent.UpdateCurrentPage(
                page,
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
        _initialLocator = publication.positions()[page]
        _initialLocator?.let { fragmentChannel.send(FragmentEvent.GoToLocator(it)) }
    }

    fun updateLocator(locator: Locator?) = viewModelScope.launch {
        if (locator != null) {
            _initialLocator = locator
        }

//        _initialLocator?.let { fragmentChannel.send(FragmentEvent.GoToLocator(it)) }

        _initialLocator?.let {
            fragmentChannel.send(
                FragmentEvent.PublicationReady(publication, it)
            )
        }
    }

    fun closePublication() {
        _bookmarks.clear()
        _bookmarksLocations.clear()
        _bookId = bookId
        _initialLocator = null
        publication.close()
        _publication = null
    }

    sealed class FragmentEvent {
        data object BookmarkSuccessfullyAdded : FragmentEvent()
        data object BookmarkSuccessfullyRemoved : FragmentEvent()

        data class PublicationReady(
            val publication: Publication,
            val initialLocator: Locator?
        ) : FragmentEvent()

        data class GoToLocator(val locator: Locator) : FragmentEvent()

        data class UpdateCurrentPage(
            val currentPage: Int,
            val totalCount: Int,
            val totalProgress: Double
        ) : FragmentEvent()
    }

    sealed class ActivityEvent {
        data object OpenContents : ActivityEvent()
        data object OpenBookmarks : ActivityEvent()
    }

    class Factory(private val application: Application) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReaderViewModel(application.bookRepository, application.streamer) as T
        }
    }
}
