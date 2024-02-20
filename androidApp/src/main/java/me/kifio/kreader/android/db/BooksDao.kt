/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import me.kifio.kreader.android.model.Book
import me.kifio.kreader.android.model.Bookmark


@Dao
interface BooksDao {

    /**
     * Inserts a book
     * @param book The book to insert
     * @return ID of the book that was added (primary key)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBook(book: Book): Long

    /**
     * Deletes a book
     * @param bookId The ID of the book
     */
    @Query("DELETE FROM " + Book.TABLE_NAME + " WHERE " + Book.ID + " = :bookId")
    fun deleteBook(bookId: Long)

    /**
     * Retrieve a book from its ID.
     */
    @Query("SELECT * FROM " + Book.TABLE_NAME + " WHERE " + Book.ID + " = :id")
    fun get(id: Long): Book?

    /**
     * Retrieve all books
     * @return List of books as LiveData
     */
    @Query("SELECT * FROM " + Book.TABLE_NAME + " ORDER BY " + Book.CREATION_DATE + " desc")
    fun getAllBooks(): List<Book>

    /**
     * Retrieve all bookmarks for a specific book
     * @param bookId The ID of the book
     * @return List of bookmarks for the book as LiveData
     */
    @Query("SELECT * FROM " + Bookmark.TABLE_NAME + " WHERE " + Bookmark.BOOK_ID + " = :bookId")
    fun getBookmarksForBook(bookId: Long): MutableList<Bookmark>

    /**
     * Inserts a bookmark
     * @param bookmark The bookmark to insert
     * @return The ID of the bookmark that was added (primary key)
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertBookmark(bookmark: Bookmark): Long

    /**
     * Deletes a bookmark
     */
    @Delete(entity = Bookmark::class)
    fun deleteBookmark(bookmark: Bookmark)

    /**
     * Saves book progression
     * @param locator Location of the book
     * @param id The book to update
     */
    @Query("UPDATE " + Book.TABLE_NAME + " SET " + Book.PROGRESSION + " = :locator WHERE " + Book.ID + "= :id")
    fun saveProgression(locator: String, id: Long)
}