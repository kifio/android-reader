package me.kifio.kreader.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import me.kifio.kreader.model.entity.Constants

enum class BookFormat {
    EPUB, FB2, PDF, MP3
}

@Entity(tableName = Constants.BOOKS_TABLE)
data class BookEntity(
    @PrimaryKey
    @ColumnInfo(name = Constants.ID)
    val id: Long,
    @ColumnInfo(name = Constants.TITLE)
    val title: String,
    @ColumnInfo(name = Constants.AUTHOR)
    val author: String,
    @ColumnInfo(name = Constants.FORMAT)
    val format: BookFormat,
    @ColumnInfo(name = Constants.LAST_PROGRESS)
    val progress: Int,
    @ColumnInfo(name = Constants.BOOKMARKS)
    val bookmarks: String,
    @ColumnInfo(name = Constants.CREATED_AT)
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = Constants.UPDATED_AT)
    val updatedAt: Long = System.currentTimeMillis(),
)
