/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.commitNow
import me.kifio.kreader.android.R
import org.readium.r2.navigator.ExperimentalDecorator
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.css.Color
import org.readium.r2.navigator.epub.css.RsProperties

@OptIn(ExperimentalDecorator::class)
class EpubReaderFragment : ReaderFragment(), EpubNavigatorFragment.Listener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager.fragmentFactory =
            EpubNavigatorFragment.createFactory(
                publication = model.publication,
                initialLocator = model.locator,
                listener = this,
                config = EpubNavigatorFragment.Configuration(
                    readiumCssRsProperties = RsProperties(
                        textColor = Color.Int(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.primary,
                                null
                            )
                        ),
                        backgroundColor = Color.Int(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.background,
                                null
                            )
                        ),
                    )
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
}