/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.reader

import android.os.Bundle
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import me.kifio.kreader.android.R
import me.kifio.kreader.android.outline.NavigationFragment
import me.kifio.kreader.android.outline.OutlineFragment
import org.readium.r2.navigator.ExperimentalDecorator
import org.readium.r2.navigator.Navigator
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.css.Color
import org.readium.r2.navigator.epub.css.RsProperties
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

@OptIn(ExperimentalDecorator::class)
class EpubReaderFragment : ReaderFragment(), EpubNavigatorFragment.Listener {

    companion object {
        const val NAVIGATOR_FRAGMENT_TAG = "EpubNavigatorFragment"
    }

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

        childFragmentManager.commitNow {
            add(
                R.id.content_container,
                EpubNavigatorFragment::class.java,
                Bundle(),
                NAVIGATOR_FRAGMENT_TAG
            )
        }

        navigator = childFragmentManager.findFragmentByTag(NAVIGATOR_FRAGMENT_TAG) as Navigator
        super.onPublicationReady(publication, initialLocator)
    }

    override fun showBookmarks() = findNavController().navigate(
        EpubReaderFragmentDirections.epubToBookmarks()
    )

    override fun showContents() = findNavController().navigate(
        EpubReaderFragmentDirections.epubToContents()
    )
}