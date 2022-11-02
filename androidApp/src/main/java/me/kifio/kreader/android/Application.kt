/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android

import android.content.*
import android.os.IBinder
import kotlinx.coroutines.*
import me.kifio.kreader.android.BuildConfig.DEBUG
import org.readium.r2.lcp.LcpService
import org.readium.r2.shared.util.Try
import org.readium.r2.streamer.Streamer
import org.readium.r2.streamer.server.Server
import me.kifio.kreader.android.bookshelf.BookRepository
import me.kifio.kreader.android.db.BookDatabase
import me.kifio.kreader.android.reader.ReaderRepository
import java.io.IOException
import java.net.ServerSocket
import java.util.*

class Application : android.app.Application() {

    lateinit var server: Server
        private set

    lateinit var bookRepository: BookRepository
        private set

    lateinit var readerRepository: Deferred<ReaderRepository>
        private set

    private val coroutineScope: CoroutineScope =
        MainScope()

    override fun onCreate() {
        super.onCreate()

        /*
         * Starting HTTP server.
         */

        val s = ServerSocket(if (DEBUG) 8080 else 0)
        s.close()
        server = Server(s.localPort, applicationContext)
        startServer()

        /*
         * Initializing repositories
         */

        val lcpService = LcpService(this)
            ?.let { Try.success(it) }
            ?: Try.failure(Exception("liblcp is missing on the classpath"))

        val streamer = Streamer(
            this,
            contentProtections = listOfNotNull(
                lcpService.getOrNull()?.contentProtection()
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
                    server,
                    bookRepository
                )
            }

    }

    override fun onTerminate() {
        super.onTerminate()
        stopServer()
    }

    private fun startServer() {
        if (!server.isAlive) {
            try {
                server.start()
            } catch (e: IOException) {
                // do nothing
            }
            if (server.isAlive) {
//                // Add your own resources here
//                server.loadCustomResource(assets.open("scripts/test.js"), "test.js")
//                server.loadCustomResource(assets.open("styles/test.css"), "test.css")
//                server.loadCustomFont(assets.open("fonts/test.otf"), applicationContext, "test.otf")
            }
        }
    }

    private fun stopServer() {
        if (server.isAlive) {
            server.stop()
        }
    }

    private fun computeAppDirectory(): String {
        val properties = Properties()
        val inputStream = assets.open("configs/config.properties")
        properties.load(inputStream)
        val useExternalFileDir =
            properties.getProperty("useExternalFileDir", "false")!!.toBoolean()
        return if (useExternalFileDir) {
            getExternalFilesDir(null)?.path + "/"
        } else {
            filesDir?.path + "/"
        }
    }
}


val Context.resolver: ContentResolver
    get() = applicationContext.contentResolver
