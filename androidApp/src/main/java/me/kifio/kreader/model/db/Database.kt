package me.kifio.kreader.model.db

import android.content.Context
import androidx.room.*
import me.kifio.kreader.model.entity.BookEntity
import me.kifio.kreader.model.entity.BookFormat

@Database(
    entities = [BookEntity::class],
    version = 1,
    exportSchema = false
)

abstract class BookDatabase : RoomDatabase() {

    abstract fun booksDao(): BooksDao

    companion object {
        @Volatile
        private var INSTANCE: BookDatabase? = null

        fun getDatabase(context: Context): BookDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BookDatabase::class.java,
                    "app_database"
                )
                    .addTypeConverter(BookFormatConverter::class).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}

@ProvidedTypeConverter
class BookFormatConverter {

    @TypeConverter
    fun fromBookFormat(format: BookFormat): String {
        return format.name
    }

    @TypeConverter
    fun toBookFormat(formatString: String): BookFormat {
        return BookFormat.valueOf(formatString)
    }

}