/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.outline

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import kotlinx.parcelize.Parcelize
import me.kifio.kreader.android.R
import me.kifio.kreader.android.databinding.FragmentOutlineBinding
import me.kifio.kreader.android.reader.ReaderViewModel
import me.kifio.kreader.android.utils.viewLifecycle
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.opds.images

class OutlineFragment : Fragment() {

    private lateinit var publication: Publication
    private lateinit var outline: Outline
    private lateinit var model: ReaderViewModel
    private var binding: FragmentOutlineBinding by viewLifecycle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model = ViewModelProvider(requireActivity())[ReaderViewModel::class.java]
        publication = model.publication

        outline = when (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
             true -> arguments?.getSerializable(OUTLINE_CONTENT_ARG, Outline::class.java)
             false -> arguments?.getSerializable(OUTLINE_CONTENT_ARG) as Outline
        } ?: throw java.lang.IllegalStateException()

        activity?.onBackPressedDispatcher?.addCallback(
            this,
            object: OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    model.fragmentBackPressed()
                }
            }
        )

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
        binding.navigateUp.setOnClickListener { model.fragmentBackPressed() }
        binding.title.setText(outline.title)
        val fragment = createFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.outline_content_conatiner, fragment, fragment::class.simpleName)
            .commit()
    }

    private fun createFragment(): Fragment {
        return when (outline) {
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

    fun destroy() {
        childFragmentManager.findFragmentById(R.id.outline_content_conatiner)?.let {
            childFragmentManager.beginTransaction().remove(it).commit()
        }
    }

    companion object {
        const val OUTLINE_CONTENT_ARG = "OUTLINE_CONTENT_ARG"

        fun newInstance(outline: Outline) = OutlineFragment().apply {
            arguments = bundleOf(OUTLINE_CONTENT_ARG to outline)
        }
    }

    sealed class Outline(val title: Int) : java.io.Serializable {
        object Contents: Outline(title = R.string.contents_tab_label)
        object Bookmarks: Outline(title = R.string.bookmarks_tab_label)
    }
}