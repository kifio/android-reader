/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.reader

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import dev.chrisbanes.insetter.applyInsetter
import me.kifio.kreader.android.Application
import me.kifio.kreader.android.R
import me.kifio.kreader.android.databinding.ActivityReaderBinding
import me.kifio.kreader.android.outline.OutlineContract
import me.kifio.kreader.android.outline.OutlineFragment
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication


/*
 * An activity to read a publication
 *
 * This class can be used as it is or be inherited from.
 */
open class ReaderActivity : AppCompatActivity() {

    private lateinit var modelFactory: ReaderViewModel.Factory
    private lateinit var model: ReaderViewModel
    private lateinit var binding: ActivityReaderBinding
    private lateinit var readerFragment: VisualReaderFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        val arguments = ReaderActivityContract.parseIntent(this)
        val app = applicationContext as Application
        modelFactory = ReaderViewModel.Factory(app, arguments)
        model = ViewModelProvider(this)[ReaderViewModel::class.java]

        /*
         * [ReaderViewModel.Factory] provides dummy publications if the [ReaderActivity] is restored
         * after the app process was killed because the [ReaderRepository] is empty.
         * In that case, finish the activity as soon as possible and go back to the previous one.
         */
        if (model.publication.readingOrder.isEmpty()) {
            finish()
        }

        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val binding = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this.binding = binding

        binding.appBar.applyInsetter {
            type(statusBars = true) {
                margin(top = true)
            }
        }

        binding.activityContainer.applyInsetter {
            type(navigationBars = true) {
                margin(bottom = true)
            }
        }

        binding.bottomAppBar.applyInsetter {
            type(navigationBars = true) {
                margin(bottom = true)
            }
        }

        val readerFragment = supportFragmentManager.findFragmentByTag(READER_FRAGMENT_TAG)
            ?.let { it as VisualReaderFragment }
            ?: run { createReaderFragment(model.readerInitData) }

        readerFragment?.let { this.readerFragment = it }

        model.activityChannel.receive(this) { handleReaderFragmentEvent(it) }

        supportFragmentManager.setFragmentResultListener(
            OutlineContract.REQUEST_KEY,
            this
        ) { _, result ->
            val locator = OutlineContract.parseResult(result).destination
            closeOutlineFragment(locator)
        }

        setSupportActionBar(binding.appBar)

        title = model.publication.metadata.title

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_reader, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }

//            override fun onOptionsItemSelected(item: MenuItem): Boolean {
//                when (item.itemId) {
//                    android.R.id.home -> {
//                        supportFragmentManager.popBackStack()
//                        return true
//                    }
//                }
//                return super.onOptionsItemSelected(item)
//            }
        })
    }

    private fun createReaderFragment(readerData: ReaderInitData): VisualReaderFragment? {
        val readerClass: Class<out Fragment>? = when {
            readerData.publication.conformsTo(Publication.Profile.EPUB) ->
                EpubReaderFragment::class.java
            readerData.publication.conformsTo(Publication.Profile.PDF) ->
                PdfReaderFragment::class.java
            readerData.publication.conformsTo(Publication.Profile.DIVINA) ->
                ImageReaderFragment::class.java
            else ->
                // The Activity should stop as soon as possible because readerData are fake.
                null
        }

        readerClass?.let { it ->
            supportFragmentManager.commitNow {
                add(R.id.activity_container, it, Bundle(), READER_FRAGMENT_TAG)
            }
        }

        return supportFragmentManager.findFragmentByTag(READER_FRAGMENT_TAG) as VisualReaderFragment?
    }

    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return modelFactory
    }

    override fun finish() {
        setResult(Activity.RESULT_OK, intent)
        super.finish()
    }

    private fun handleReaderFragmentEvent(event: ReaderViewModel.Event) {
        when(event) {
            is ReaderViewModel.Event.OpenOutlineRequested -> showOutlineFragment()
            is ReaderViewModel.Event.ToggleUIVisibilityRequested -> toggleUI(event.navigated)
            is ReaderViewModel.Event.UpdateBookmarkRequested -> updateBookmarkIcon(event.isBookmarkedPage)
        }
    }

    private fun showOutlineFragment() {
        supportFragmentManager.commit {
            add(R.id.activity_container, OutlineFragment::class.java, Bundle(), OUTLINE_FRAGMENT_TAG)
            hide(readerFragment)
            addToBackStack(null)
        }
    }

    private fun closeOutlineFragment(locator: Locator) {
        readerFragment.go(locator, true)
        supportFragmentManager.popBackStack()
    }

    companion object {
        const val READER_FRAGMENT_TAG = "reader"
        const val OUTLINE_FRAGMENT_TAG = "outline"
    }

    private fun toggleUI(navigated: Boolean) {
        if (navigated) return
        with (supportActionBar?.isShowing != true) {
            binding.appBar.isVisible = this
            binding.bottomAppBar.isVisible = this
        }
    }

    private fun updateBookmarkIcon(isBookmarkedPage: Boolean) {
        binding.appBar.menu.findItem(R.id.bookmark)?.setIcon(
            when (isBookmarkedPage) {
                true -> R.drawable.ic_baseline_bookmark_24
                false -> R.drawable.ic_baseline_bookmark_border_24
            }
        )
    }
}
