/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android

import android.content.ContentResolver
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import me.kifio.kreader.android.bookshelf.BookRepository
import me.kifio.kreader.android.db.BookDatabase
import me.kifio.kreader.android.reader.ReaderRepository
import org.readium.r2.lcp.LcpService
import org.readium.r2.streamer.Streamer
import org.readium.r2.streamer.server.Server
import java.io.IOException
import java.util.*

class Application : android.app.Application() {

    lateinit var bookRepository: BookRepository
        private set

    lateinit var readerRepository: Deferred<ReaderRepository>
        private set

    private val coroutineScope: CoroutineScope =
        MainScope()

    override fun onCreate() {
        super.onCreate()
        /*
         * Initializing repositories
         */

        val streamer = Streamer(
            this,
            contentProtections = listOfNotNull(
                LcpService(this)?.contentProtection()
            )
        )

        bookRepository =
            BookDatabase.getDatabase(this).booksDao()
                .let {  BookRepository(it) }

        readerRepository =
            coroutineScope.async {
                ReaderRepository(
                    this@Application,
                    streamer,
                    bookRepository
                )
            }

    }
}


val Context.resolver: ContentResolver
    get() = applicationContext.contentResolver
