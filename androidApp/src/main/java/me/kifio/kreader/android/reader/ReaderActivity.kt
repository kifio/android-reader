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
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
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
    private var buttonClicked: Boolean = false

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

        binding.contentContainer.applyInsetter {
            type(statusBars = true, navigationBars = true) {
                margin(bottom = true, top = true)
            }
        }

        binding.bottomAppBar.applyInsetter {
            type(navigationBars = true) {
                margin(bottom = true)
            }
        }

        binding.pagesCount.applyInsetter {
            type(navigationBars = true) {
                margin(bottom = true)
            }
        }

        binding.navigateUp.applyInsetter {
            type(statusBars = true) {
                margin(top = true)
            }
        }

        binding.contents.applyInsetter {
            type(statusBars = true) {
                margin(top = true)
            }
        }

        binding.bookmarks.applyInsetter {
            type(statusBars = true) {
                margin(top = true)
            }
        }

        binding.outlineContainer.applyInsetter {
            type(statusBars = true, navigationBars = true) {
                margin(bottom = true, top = true)
            }
        }

        val readerFragment =
            supportFragmentManager.findFragmentByTag(VisualReaderFragment::class.simpleName)
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

        title = null

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_reader, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem) = false
        })

        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        binding.contents.setOnClickListener {
            handleClick(it) { showOutlineFragment(OutlineFragment.Outline.Contents) }
        }

        binding.bookmarks.setOnClickListener {
            handleClick(it) { showOutlineFragment(OutlineFragment.Outline.Bookmarks) }
        }

        binding.navigateUp.setOnClickListener {
            finishAfterTransition()
        }
    }

    private fun onViewModelReady() {
        binding.bottomBarProgress.max = model.pagesCount
        binding.bottomBarProgress.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                userInitiated: Boolean
            ) {
            }

            override fun onStartTrackingTouch(p0: SeekBar) {}

            override fun onStopTrackingTouch(p0: SeekBar) {
                model.seekToPage(p0.progress)
            }
        })
    }

    private fun createReaderFragment(readerData: ReaderInitData): VisualReaderFragment? {
        val readerClass: Class<out Fragment>? = when {
            readerData.publication.conformsTo(Publication.Profile.EPUB) ->
                EpubReaderFragment::class.java
            readerData.publication.conformsTo(Publication.Profile.PDF) ->
                PdfReaderFragment::class.java
            else ->
                // The Activity should stop as soon as possible because readerData are fake.
                null
        }

        readerClass?.let { it ->
            supportFragmentManager.commitNow {
                add(R.id.content_container, it, Bundle(), VisualReaderFragment::class.simpleName)
            }
        }

        return supportFragmentManager.findFragmentByTag(VisualReaderFragment::class.simpleName) as VisualReaderFragment?
    }

    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return modelFactory
    }

    override fun finish() {
        setResult(Activity.RESULT_OK, intent)
        super.finish()
    }

    private fun handleReaderFragmentEvent(event: ReaderViewModel.ActivityEvent) {
        when (event) {
            ReaderViewModel.ActivityEvent.ViewModelReady -> onViewModelReady()
            ReaderViewModel.ActivityEvent.FragmentOnBackPressed -> fragmentBackPressed()
            is ReaderViewModel.ActivityEvent.ToggleUIVisibilityRequested -> toggleUI(event.navigated)
            is ReaderViewModel.ActivityEvent.UpdateBookmarkRequested -> updateBookmarkIcon(event.isBookmarkedPage)
            is ReaderViewModel.ActivityEvent.UpdateCurrentPage -> updateCurrentPage(
                event.currentPage,
                event.totalCount
            )
            is ReaderViewModel.ActivityEvent.UpdateProgressBar -> updateProgressBar(event.totalProgress)
        }
    }

    private fun showOutlineFragment(outline: OutlineFragment.Outline) {
        binding.outlineContainer.isVisible = true
        supportFragmentManager.commit {
            add(
                R.id.outline_container,
                OutlineFragment.newInstance(outline), OutlineFragment::class.simpleName
            )
            addToBackStack(OutlineFragment::class.simpleName)
        }
    }

    private fun closeOutlineFragment(locator: Locator) {
        fragmentBackPressed()
        readerFragment.go(locator, true)
    }

    private fun fragmentBackPressed() {
        binding.outlineContainer.isVisible = false
    }

    private fun handleClick(view: View, action: (View) -> Unit) {
        buttonClicked = true
        view.post {
            action(view)
            buttonClicked = false
        }
    }

    private fun toggleUI(navigated: Boolean) {
        if (navigated) return
        with(supportActionBar?.isShowing != true) {
            binding.appBar.isVisible = this
            binding.navigateUp.isVisible = this
            binding.bookmarks.isVisible = this
            binding.contents.isVisible = this
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

    private fun updateCurrentPage(page: Int, total: Int) {
        binding.bottomBarPagesCount.text = "$page/$total"
        binding.pagesCount.text = page.toString()
    }

    private fun updateProgressBar(progress: Double) {
        binding.bottomBarProgress.progress = (progress * binding.bottomBarProgress.max).toInt()
    }
}
