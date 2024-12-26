/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.reader

import android.os.Bundle
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.commitNow
import me.kifio.kreader.android.R
import org.readium.r2.navigator.ExperimentalDecorator
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.css.Color
import org.readium.r2.navigator.epub.css.RsProperties
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

@OptIn(ExperimentalDecorator::class)
class EpubReaderFragment : ReaderFragment(), EpubNavigatorFragment.Listener {

    override lateinit var navigator: Navigator

    override fun onPublicationReady(publication: Publication, initialLocator: Locator?) {
        childFragmentManager.fragmentFactory =
            EpubNavigatorFragment.createFactory(
                publication = publication,
                initialLocator = initialLocator,
                listener = this,
                config = EpubNavigatorFragment.Configuration(
                    readiumCssRsProperties = RsProperties(
                        textColor = Color.Int(ResourcesCompat.getColor(resources, R.color.primary, null)),
                        backgroundColor = Color.Int(ResourcesCompat.getColor(resources, R.color.background, null)),
                    )
                )
            )

        val navigatorFragmentTag = getString(org.readium.r2.navigator.R.string.epub_navigator_tag)

        childFragmentManager.commitNow(allowStateLoss = true) {
            add(
                R.id.content_container,
                EpubNavigatorFragment::class.java,
                Bundle(),
                navigatorFragmentTag
            )
        }

        navigator = childFragmentManager.findFragmentByTag(navigatorFragmentTag) as Navigator

        super.onPublicationReady(publication, initialLocator)
    }
}