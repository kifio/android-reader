/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.model.db

import androidx.room.*
import me.kifio.kreader.model.BookEntity
import me.kifio.kreader.model.entity.Constants

@Dao
interface BooksDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBook(book: BookEntity): Long

    @Query("DELETE FROM " + Constants.BOOKS_TABLE + " WHERE " + Constants.ID + " = :bookId")
    fun deleteBook(bookId: Long): Int

    @Query("SELECT * FROM " + Constants.BOOKS_TABLE + " WHERE " + Constants.ID + " = :id")
    fun get(id: Long): BookEntity?

    @Query("SELECT * FROM " + Constants.BOOKS_TABLE + " ORDER BY " + Constants.UPDATED_AT + " desc")
    fun getAllBooks(): List<BookEntity>

    @Query("SELECT ${Constants.BOOKMARKS} FROM " + Constants.BOOKS_TABLE + " WHERE " + Constants.ID + " = :bookId")
    fun getBookmarksForBook(bookId: Long): String

    @Update(entity = BookEntity::class, onConflict = OnConflictStrategy.IGNORE)
    fun updateBook(book: BookEntity)
}