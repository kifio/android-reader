/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.reader

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import kotlinx.coroutines.delay
import org.readium.r2.navigator.ExperimentalDecorator
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.html.HtmlDecorationTemplate
import org.readium.r2.navigator.html.toCss
import org.readium.r2.shared.APPEARANCE_REF
import org.readium.r2.shared.ReadiumCSSName
import org.readium.r2.shared.SCROLL_REF
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import me.kifio.kreader.android.R
import me.kifio.kreader.android.epub.UserSettings
import me.kifio.kreader.android.utils.extensions.toDataUrl

@OptIn(ExperimentalDecorator::class)
class EpubReaderFragment : VisualReaderFragment(), EpubNavigatorFragment.Listener {

    override lateinit var model: ReaderViewModel
    override lateinit var navigator: Navigator

    private lateinit var publication: Publication
    private lateinit var navigatorFragment: EpubNavigatorFragment
    private lateinit var menuSearch: MenuItem
    private lateinit var userSettings: UserSettings
    private var isScreenReaderVisible = false
    private var isSearchViewIconified = true

    // Accessibility
    private var isExploreByTouchEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            isScreenReaderVisible = savedInstanceState.getBoolean(IS_SCREEN_READER_VISIBLE_KEY)
            isSearchViewIconified = savedInstanceState.getBoolean(IS_SEARCH_VIEW_ICONIFIED)
        }

        ViewModelProvider(requireActivity())[ReaderViewModel::class.java].let {
            model = it
            publication = it.publication
        }

        val readerData = model.readerInitData as VisualReaderInitData
        val baseUrl = checkNotNull(readerData.baseUrl).toString()

        childFragmentManager.fragmentFactory =
            EpubNavigatorFragment.createFactory(
                publication = publication,
                baseUrl = baseUrl,
                initialLocator = readerData.initialLocation,
                listener = this,
                config = EpubNavigatorFragment.Configuration().apply {
                    // Register the HTML template for our custom [DecorationStyleAnnotationMark].
                    decorationTemplates[DecorationStyleAnnotationMark::class] =
                        annotationMarkTemplate(requireActivity())
                }
            )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val navigatorFragmentTag = getString(org.readium.r2.navigator.R.string.epub_navigator_tag)

        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                add(
                    R.id.fragment_reader_container,
                    EpubNavigatorFragment::class.java,
                    Bundle(),
                    navigatorFragmentTag
                )
            }
        }
        navigator = childFragmentManager.findFragmentByTag(navigatorFragmentTag) as Navigator
        navigatorFragment = navigator as EpubNavigatorFragment

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()
        userSettings =
            UserSettings(navigatorFragment.preferences, activity, publication.userSettingsUIPreset)

        // This is a hack to draw the right background color on top and bottom blank spaces
        navigatorFragment.lifecycleScope.launchWhenStarted {
            val appearancePref = navigatorFragment.preferences.getInt(APPEARANCE_REF, 0)
            val backgroundsColors = mutableListOf("#ffffff", "#faf4e8", "#000000")
            navigatorFragment.resourcePager.setBackgroundColor(Color.parseColor(backgroundsColors[appearancePref]))
        }
    }

    override fun onResume() {
        super.onResume()
        val activity = requireActivity()

        userSettings.resourcePager = navigatorFragment.resourcePager

        // If TalkBack or any touch exploration service is activated we force scroll mode (and
        // override user preferences)
        val am =
            activity.getSystemService(AppCompatActivity.ACCESSIBILITY_SERVICE) as AccessibilityManager
        isExploreByTouchEnabled = am.isTouchExplorationEnabled

        if (isExploreByTouchEnabled) {
            // Preset & preferences adapted
            publication.userSettingsUIPreset[ReadiumCSSName.ref(SCROLL_REF)] = true
            navigatorFragment.preferences.edit().putBoolean(SCROLL_REF, true)
                .apply() //overriding user preferences
            userSettings.saveChanges()

            lifecycleScope.launchWhenResumed {
                delay(500)
                userSettings.updateViewCSS(SCROLL_REF)
            }
        } else {
            if (publication.cssStyle != "cjk-vertical") {
                publication.userSettingsUIPreset.remove(ReadiumCSSName.ref(SCROLL_REF))
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_SCREEN_READER_VISIBLE_KEY, isScreenReaderVisible)
        outState.putBoolean(IS_SEARCH_VIEW_ICONIFIED, isSearchViewIconified)
    }

    companion object {

        private const val IS_SCREEN_READER_VISIBLE_KEY = "isScreenReaderVisible"

        private const val IS_SEARCH_VIEW_ICONIFIED = "isSearchViewIconified"
    }
}

/**
 * Example of an HTML template for a custom Decoration Style.
 *
 * This one will display a tinted "pen" icon in the page margin to show that a highlight has an
 * associated note.
 */
@OptIn(ExperimentalDecorator::class)
private fun annotationMarkTemplate(
    context: Context,
    @ColorInt defaultTint: Int = Color.YELLOW
): HtmlDecorationTemplate {
    // Converts the pen icon to a base 64 data URL, to be embedded in the decoration stylesheet.
    // Alternatively, serve the image with the local HTTP server and use its URL.
    val imageUrl = ContextCompat.getDrawable(context, R.drawable.ic_baseline_edit_24)
        ?.toBitmap()?.toDataUrl()
    requireNotNull(imageUrl)

    val className = "testapp-annotation-mark"
    return HtmlDecorationTemplate(
        layout = HtmlDecorationTemplate.Layout.BOUNDS,
        width = HtmlDecorationTemplate.Width.PAGE,
        element = { decoration ->
            val style = decoration.style as? DecorationStyleAnnotationMark
            val tint = style?.tint ?: defaultTint
            // Using `data-activable=1` prevents the whole decoration container from being
            // clickable. Only the icon will respond to activation events.
            """
            <div><div data-activable="1" class="$className" style="background-color: ${tint.toCss()} !important"/></div>"
            """
        },
        stylesheet = """
            .$className {
                float: left;
                margin-left: 8px;
                width: 30px;
                height: 30px;
                border-radius: 50%;
                background: url('$imageUrl') no-repeat center;
                background-size: auto 50%;
                opacity: 0.8;
            }
            """
    )
}
