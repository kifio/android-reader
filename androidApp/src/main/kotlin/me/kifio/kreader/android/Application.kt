/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android

import me.kifio.kreader.android.bookshelf.BookRepository
import me.kifio.kreader.android.db.BookDatabase
import org.readium.r2.lcp.LcpService
import org.readium.r2.streamer.Streamer

class Application : android.app.Application() {

    lateinit var bookRepository: BookRepository
        private set

    lateinit var streamer: Streamer
        private set

    override fun onCreate() {
        super.onCreate()

        streamer = Streamer(
            this,
            contentProtections = listOfNotNull(
                LcpService(this)?.contentProtection()
            )
        )

        bookRepository =
            BookDatabase.getDatabase(this).booksDao()
                .let {  BookRepository(it) }
    }
}