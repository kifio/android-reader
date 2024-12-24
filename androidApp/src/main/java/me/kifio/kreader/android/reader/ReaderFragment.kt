/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.reader

import android.graphics.PointF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import dev.chrisbanes.insetter.applyInsetter
import me.kifio.kreader.android.Application
import me.kifio.kreader.android.R
import me.kifio.kreader.android.databinding.FragmentReaderBinding
import me.kifio.kreader.android.outline.OutlineFragment
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.NavigatorDelegate
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.util.EdgeTapNavigation
import org.readium.r2.shared.publication.Locator

abstract class ReaderFragment : Fragment(), VisualNavigator.Listener, NavigatorDelegate {

    protected abstract val bookId: Long

    protected val model: ReaderViewModel by activityViewModels() {
        ReaderViewModel.Factory(requireActivity().application as Application, bookId)
    }

    protected abstract val navigator: Navigator

    private lateinit var binding: FragmentReaderBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.appBar)
        (requireActivity() as AppCompatActivity).title = null
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

        addMenu()

        arrayOf(binding.contentContainer, binding.outlineContainer).forEach {
            it.applyInsetter {
                type(statusBars = true, navigationBars = true) {
                    margin(bottom = true, top = true)
                }
            }
        }

        arrayOf(binding.bottomAppBar, binding.pagesCount).forEach {
            it.applyInsetter {
                type(navigationBars = true) {
                    margin(bottom = true)
                }
            }
        }

        arrayOf(binding.appBar, binding.navigateUp, binding.contents, binding.bookmarks).forEach {
            it.applyInsetter {
                type(statusBars = true) {
                    margin(top = true)
                }
            }
        }

        binding.contents.setOnClickListener {
            showOutlineFragment(OutlineFragment.Outline.Contents)
        }

        binding.bookmarks.setOnClickListener {
            showOutlineFragment(OutlineFragment.Outline.Bookmarks)
        }

        binding.navigateUp.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (model.publication.readingOrder.isEmpty()) {
            findNavController().navigateUp()
        }

        super.onCreate(savedInstanceState)


        model.activityChannel.receive(this) { handleReaderFragmentEvent(it) }



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

    private fun addMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_reader, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.bookmark -> {
                        when (model.locations.contains(navigator.currentLocator.value.locations)) {
                            true -> model.deleteBookmark(navigator.currentLocator.value)
                            false -> model.insertBookmark(navigator.currentLocator.value)
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun handleReaderFragmentEvent(event: ReaderViewModel.ActivityEvent) {
        when (event) {
            ReaderViewModel.ActivityEvent.ViewModelReady -> onViewModelReady()
//            ReaderViewModel.ActivityEvent.FragmentOnBackPressed -> fragmentBackPressed()
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
        childFragmentManager.commit {
            replace(
                R.id.outline_container,
                OutlineFragment.newInstance(outline), OutlineFragment::class.simpleName
            )
        }
    }

    private fun closeOutlineFragment(locator: Locator) {
        go(locator, true)
    }


    private fun toggleUI(navigated: Boolean) {
        if (navigated) return
        with((requireActivity() as AppCompatActivity).supportActionBar?.isShowing != true) {
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


    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        requireActivity().invalidateOptionsMenu()
    }

    override fun onTap(point: PointF): Boolean {
        model.toggleUIVisibility(edgeTapNavigation.onTap(point, requireView()))
        return true
    }

    private val edgeTapNavigation by lazy {
        EdgeTapNavigation(navigator = navigator as VisualNavigator)
    }

    private fun go(locator: Locator, animated: Boolean) {
        navigator.go(locator, animated)
    }
}
