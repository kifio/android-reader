package me.kifio.kreader.android

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver.OnPreDrawListener
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import me.kifio.kreader.android.bookshelf.BookshelfViewModel
import me.kifio.kreader.android.databinding.ActivityMainBinding
import me.kifio.kreader.android.outline.BookmarksFragment
import me.kifio.kreader.android.outline.NavigationFragment
import me.kifio.kreader.android.outline.OutlineFragment
import me.kifio.kreader.android.reader.ReaderViewModel
import org.readium.r2.shared.publication.Locator

class MainActivity : AppCompatActivity() {

    private val bookShelfVM: BookshelfViewModel by viewModels {
        BookshelfViewModel.Factory(
            application as Application
        )
    }
    private val readerVM: ReaderViewModel by viewModels { ReaderViewModel.Factory(application as Application) }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        installSplashScreen()

        if (savedInstanceState == null) {

            window.decorView.viewTreeObserver.addOnPreDrawListener(
                object : OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        return if (bookShelfVM.shelfState != null) {
                            setContentView(binding.root)
                            window.decorView.viewTreeObserver.removeOnPreDrawListener(this)
                            true
                        } else {
                            false
                        }
                    }
                }
            )
        } else {
            setContentView(binding.root)
        }

        bookShelfVM.setup()

        readerVM.activityChannel.receive(this) { event ->
            when (event) {
                ReaderViewModel.ActivityEvent.OpenContents -> showOutlineFragment(NavigationFragment())
                ReaderViewModel.ActivityEvent.OpenBookmarks -> showOutlineFragment(BookmarksFragment())
                ReaderViewModel.ActivityEvent.CloseOutlineFragment -> removeOutlineFragment()
            }
        }

        supportFragmentManager.setFragmentResultListener(
            OutlineFragment.FRAGMENT_REQUEST_KEY,
            this
        ) { _, bundle ->
            val locator: Locator? = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(OutlineFragment.SELECTED_LOCATOR)
            } else {
                bundle.getParcelable(OutlineFragment.SELECTED_LOCATOR, Locator::class.java)
            }
            removeOutlineFragment()
            readerVM.updateLocator(locator)
        }
    }

    private fun showOutlineFragment(fragment: OutlineFragment) {
        binding.outlineContainer.visibility = View.VISIBLE
        supportFragmentManager
            .beginTransaction()
            .add(R.id.outline_container, fragment, OutlineFragment.TAG)
            .addToBackStack(OutlineFragment.TAG)
            .commit()
    }

    private fun removeOutlineFragment(): Boolean {
        binding.outlineContainer.visibility = View.GONE
        val outlineFragment = supportFragmentManager.findFragmentByTag(OutlineFragment.TAG)
            ?: return false

        supportFragmentManager
            .beginTransaction()
            .remove(outlineFragment)
            .commit()

        return true
    }
}