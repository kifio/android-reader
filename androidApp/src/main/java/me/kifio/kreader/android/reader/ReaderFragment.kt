package me.kifio.kreader.android.reader

import android.graphics.PointF
import android.os.Build
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
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.kifio.kreader.android.R
import me.kifio.kreader.android.databinding.FragmentReaderBinding
import me.kifio.kreader.android.outline.OutlineFragment
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.NavigatorDelegate
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.util.EdgeTapNavigation
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

abstract class ReaderFragment : Fragment(), VisualNavigator.Listener, NavigatorDelegate {

    protected val model: ReaderViewModel by activityViewModels()

    protected var navigator: Navigator? = null

    private var navigatorFlow: Flow<Locator>? = null
    private var navigatorFlowJob: Job? = null
    private lateinit var binding: FragmentReaderBinding

    abstract fun showBookmarks()

    abstract fun showContents()

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

        arrayOf(binding.contentContainer).forEach {
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
            showContents()
        }

        binding.bookmarks.setOnClickListener {
            showBookmarks()
        }

        binding.navigateUp.setOnClickListener {
            findNavController().navigateUp()
        }

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

        model.fragmentChannel.receive(this) { event ->
            when (event) {
                is ReaderViewModel.FragmentEvent.PublicationReady -> {
                    onPublicationReady(event.publication, event.initialLocator)
                }
                is ReaderViewModel.FragmentEvent.GoToLocator -> {
                    navigator?.go(event.locator, true)
                }
                is ReaderViewModel.FragmentEvent.BookmarkSuccessfullyAdded -> {
                    updateBookmarkIcon(R.drawable.ic_baseline_bookmark_24)
                }
                is ReaderViewModel.FragmentEvent.BookmarkSuccessfullyRemoved -> {
                    updateBookmarkIcon(R.drawable.ic_baseline_bookmark_border_24)
                }
                is ReaderViewModel.FragmentEvent.UpdateCurrentPage -> {
                    updateProgressBar(event.totalProgress)
                    updateCurrentPage(event.currentPage, event.totalCount)
                }
            }
        }

        setFragmentResultListener(OutlineFragment.FRAGMENT_REQUEST_KEY) { _, bundle ->
            val locator: Locator? = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(OutlineFragment.SELECTED_LOCATOR)
            } else {
                bundle.getParcelable(OutlineFragment.SELECTED_LOCATOR, Locator::class.java)
            }

            model.updateLocator(locator)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model.openReader()
    }

    override fun onDestroy() {
        super.onDestroy()
        model.closePublication()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navigatorFlow = null
        navigatorFlowJob?.cancel()
    }

    protected open fun onPublicationReady(publication: Publication, initialLocator: Locator?) {
        if (publication.readingOrder.isEmpty()) {
            findNavController().navigateUp()
        }

        binding.bottomBarProgress.max = model.pagesCount

        navigator?.currentLocator
            ?.onEach {
                model.updateProgression(it)
                updateBookmarkIcon(
                    when (model.locations.contains(it.locations)) {
                        true -> R.drawable.ic_baseline_bookmark_24
                        false -> R.drawable.ic_baseline_bookmark_border_24
                    }
                )
            }
            ?.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun addMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_reader, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.bookmark -> {
                        val navigator = this@ReaderFragment.navigator ?: return true
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

    private fun updateBookmarkIcon(icon: Int) {
        binding.appBar.menu.findItem(R.id.bookmark)?.setIcon(icon)
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
        toggleUI(edgeTapNavigation.onTap(point, requireView()))
        return true
    }

    private val edgeTapNavigation by lazy {
        EdgeTapNavigation(navigator = navigator as VisualNavigator)
    }
}
