/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.outline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import me.kifio.kreader.android.R
import me.kifio.kreader.android.databinding.FragmentOutlineBinding
import me.kifio.kreader.android.reader.ReaderActivity
import me.kifio.kreader.android.reader.ReaderViewModel
import me.kifio.kreader.android.utils.viewLifecycle
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.opds.images

class OutlineFragment : Fragment() {

    private lateinit var publication: Publication
    private var binding: FragmentOutlineBinding by viewLifecycle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewModelProvider(requireActivity()).get(ReaderViewModel::class.java).let {
            publication = it.publication
        }

        (activity as ReaderActivity?)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        childFragmentManager.setFragmentResultListener(
            OutlineContract.REQUEST_KEY,
            this
        ) { requestKey, bundle -> setFragmentResult(requestKey, bundle) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOutlineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val outlines: List<Outline> = listOf(Outline.Contents, Outline.Bookmarks)

        binding.outlinePager.adapter = OutlineFragmentStateAdapter(this, publication, outlines)
        TabLayoutMediator(binding.outlineTabLayout, binding.outlinePager) { tab, idx -> tab.setText(outlines[idx].label) }.attach()
    }
}

private class OutlineFragmentStateAdapter(fragment: Fragment, val publication: Publication, val outlines: List<Outline>)
    : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return outlines.size
    }

    override fun createFragment(position: Int): Fragment {
        return when (this.outlines[position]) {
            Outline.Bookmarks -> BookmarksFragment()
            Outline.Contents -> createContentsFragment()
        }
    }

    private fun createContentsFragment() =
        NavigationFragment.newInstance(
            when {
                publication.tableOfContents.isNotEmpty() -> publication.tableOfContents
                publication.readingOrder.isNotEmpty() -> publication.readingOrder
                publication.images.isNotEmpty() -> publication.images
                else -> mutableListOf()
            }
        )
}

private enum class Outline(val label: Int) {
    Contents(R.string.contents_tab_label),
    Bookmarks(R.string.bookmarks_tab_label),
}