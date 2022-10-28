package me.kifio.kreader.model

enum class BookFormat {
    EPUB, FB2, PDF, MP3
}

data class Book(
    val id: Int,
    val title: String,
    val author: String,
    val cover: String,  // base64 string. cover or first page.
    val format: BookFormat,
    val progress: Int,
    val assetFileName: String
)