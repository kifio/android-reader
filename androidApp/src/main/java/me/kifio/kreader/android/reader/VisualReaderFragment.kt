/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.reader

import android.graphics.PointF
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.annotation.ColorInt
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import me.kifio.kreader.android.R
import me.kifio.kreader.android.databinding.FragmentReaderBinding
import me.kifio.kreader.android.utils.*
import org.readium.r2.navigator.*
import org.readium.r2.navigator.util.EdgeTapNavigation
import org.readium.r2.shared.publication.Locator

/*
 * Base reader fragment class
 *
 * Provides common menu items and saves last location on stop.
 */
@OptIn(ExperimentalDecorator::class)
abstract class VisualReaderFragment : Fragment(), VisualNavigator.Listener, NavigatorDelegate {

    protected abstract val model: ReaderViewModel

    protected abstract val navigator: Navigator

    private var binding: FragmentReaderBinding by viewLifecycle()

    private var navigatorFragment: Fragment? = null

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
        navigatorFragment = navigator as Fragment

        val viewScope = viewLifecycleOwner.lifecycleScope

        navigator.currentLocator
            .onEach {
                model.updateProgression(it)
                model.updateBookmarkIcon(model.locations.contains(it.locations))
            }
            .launchIn(viewScope)

        (navigator as? DecorableNavigator)?.let { navigator ->
            model.searchDecorations
                .onEach { navigator.applyDecorations(it, "search") }
                .launchIn(viewScope)
        }

        model.fragmentChannel.receive(this) { event ->
            when (event) {
                is ReaderViewModel.FragmentEvent.GoToLocator -> {
                    go(event.locator, true)
                }
                else -> {
                    model.updateBookmarkIcon(event is ReaderViewModel.FragmentEvent.BookmarkSuccessfullyAdded)
                }
            }
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}

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

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        requireActivity().invalidateOptionsMenu()
    }

    // VisualNavigator.Listener

    override fun onTap(point: PointF): Boolean {
        model.toggleUIVisibility(edgeTapNavigation.onTap(point, requireView()))
        return true
    }

    private val edgeTapNavigation by lazy {
        EdgeTapNavigation(navigator = navigator as VisualNavigator)
    }

    open fun go(locator: Locator, animated: Boolean) {
        navigator.go(locator, animated)
    }

}

/**
 * Decoration Style for a page margin icon.
 *
 * This is an example of a custom Decoration Style declaration.
 */
@Parcelize
@OptIn(ExperimentalDecorator::class)
data class DecorationStyleAnnotationMark(@ColorInt val tint: Int) : Decoration.Style
