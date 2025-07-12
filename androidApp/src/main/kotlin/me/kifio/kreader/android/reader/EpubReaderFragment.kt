/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.reader

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.commitNow
import me.kifio.kreader.android.R
import org.readium.r2.navigator.ExperimentalDecorator
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.epub.EpubSettings
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.shared.ExperimentalReadiumApi

@OptIn(ExperimentalDecorator::class)
class EpubReaderFragment : ReaderFragment(), EpubNavigatorFragment.Listener {

    companion object {
        val PREFS_NAME = "me.kifio.kreader.epub"
        val EPUB_BG_COLOR = "me.kifio.kreader.epub.bg_color"
        val EPUB_TEXT_COLOR = "me.kifio.kreader.epub.text_color"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = context?.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val defaultBackgroundColor = resources.getColor(R.color.background, null)
        val defaultTextColor = resources.getColor(R.color.primary, null)

        val backgroundColor = prefs?.getInt(EPUB_BG_COLOR, defaultBackgroundColor) ?: defaultBackgroundColor
        val textColor = prefs?.getInt(EPUB_TEXT_COLOR, defaultTextColor) ?: defaultTextColor

        childFragmentManager.fragmentFactory =
            EpubNavigatorFragment.createFactory(
                publication = model.publication,
                initialLocator = model.locator,
                listener = this,
                initialPreferences = EpubPreferences(
                    backgroundColor = org.readium.r2.navigator.preferences.Color(backgroundColor),
                    textColor = org.readium.r2.navigator.preferences.Color(textColor)
                )
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
                    R.id.content_container,
                    EpubNavigatorFragment::class.java,
                    Bundle(),
                    navigatorFragmentTag
                )
            }

            childFragmentManager.findFragmentByTag(navigatorFragmentTag)?.retainInstance = true
        }

        navigator = childFragmentManager.findFragmentByTag(navigatorFragmentTag) as Navigator
        return view
    }

    @SuppressLint("CommitPrefEdits")
    @OptIn(ExperimentalReadiumApi::class)
    override fun selectColor(color: Int, textColor: Int) {
        super.selectColor(color, textColor)

// FIXME:
//  1) Создать темы с нужными primary и background цветами
//  2) Вместо цветов передавать сюда id темы и делать activity?.recreate()

        val epubPreferences = EpubPreferences(
            backgroundColor = org.readium.r2.navigator.preferences.Color(color),
            textColor = org.readium.r2.navigator.preferences.Color(textColor)
        )

        context?.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)?.edit()?.let {
            it.putInt(EPUB_BG_COLOR, color)
            it.putInt(EPUB_TEXT_COLOR, textColor)
            it.apply()
        }

        (navigator as Configurable<EpubSettings, EpubPreferences>).submitPreferences(epubPreferences)
    }
}