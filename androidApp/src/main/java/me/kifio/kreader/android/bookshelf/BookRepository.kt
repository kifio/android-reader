/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.bookshelf

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.kifio.kreader.android.db.BooksDao
import me.kifio.kreader.android.model.Book
import me.kifio.kreader.android.model.Bookmark
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.indexOfFirstWithHref
import org.readium.r2.shared.util.mediatype.MediaType

val Metadata.authorName: String
    get() = authors.firstOrNull()?.name ?: ""

class BookRepository(private val booksDao: BooksDao) {

    suspend fun books(): List<Book> = withContext(Dispatchers.IO) {
        booksDao.getAllBooks()
    }

    suspend fun get(id: Long) = withContext(Dispatchers.IO) {
        booksDao.get(id)
    }

    suspend fun insertBook(href: String, mediaType: MediaType, publication: Publication): Long =
        withContext(Dispatchers.IO) {
            val book = Book(
                creation = System.currentTimeMillis(),
                title = publication.metadata.title,
                author = publication.metadata.authorName,
                href = href,
                identifier = publication.metadata.identifier ?: "",
                type = mediaType.toString(),
                progression = "{}"
            )
            booksDao.insertBook(book)
        }

    suspend fun deleteBook(id: Long) = withContext(Dispatchers.IO) {
        booksDao.deleteBook(id)
    }

    suspend fun saveProgression(locator: Locator, bookId: Long) = withContext(Dispatchers.IO) {
        booksDao.saveProgression(locator.toJSON().toString(), bookId)
    }

    suspend fun insertBookmark(bookId: Long, publication: Publication, locator: Locator): Bookmark =
        withContext(Dispatchers.IO) {
            val resource = publication.readingOrder.indexOfFirstWithHref(locator.href)!!
            val bookmark = Bookmark(
                creation = System.currentTimeMillis(),
                bookId = bookId,
                publicationId = publication.metadata.identifier ?: publication.metadata.title,
                resourceIndex = resource.toLong(),
                resourceHref = locator.href,
                resourceType = locator.type,
                resourceTitle = locator.title.orEmpty(),
                location = locator.locations.toJSON().toString(),
                locatorText = Locator.Text().toJSON().toString()
            )
            val id = booksDao.insertBookmark(bookmark)
            bookmark.copy(id = id)
        }

    suspend fun bookmarksForBook(bookId: Long): MutableList<Bookmark> =
        withContext(Dispatchers.IO) {
            booksDao.getBookmarksForBook(bookId)
        }

    suspend fun deleteBookmark(bookmark: Bookmark) = withContext(Dispatchers.IO) {
        booksDao.deleteBookmark(bookmark)
    }
}