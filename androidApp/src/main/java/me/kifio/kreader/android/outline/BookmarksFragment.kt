/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package me.kifio.kreader.android.outline

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.*
import com.google.android.material.divider.MaterialDividerItemDecoration
import me.kifio.kreader.android.R
import me.kifio.kreader.android.databinding.FragmentListviewBinding
import me.kifio.kreader.android.databinding.ItemRecycleBookmarkBinding
import me.kifio.kreader.android.model.Bookmark
import me.kifio.kreader.android.reader.ReaderViewModel
import me.kifio.kreader.android.utils.extensions.outlineTitle
import me.kifio.kreader.android.utils.viewLifecycle
import org.readium.r2.shared.publication.Publication
import kotlin.math.abs


private fun Int.toPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).toInt()

private fun Float.toPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).toInt()


class BookmarksFragment : Fragment() {

    lateinit var publication: Publication
    lateinit var viewModel: ReaderViewModel
    private lateinit var bookmarkAdapter: BookmarkAdapter
    private var binding: FragmentListviewBinding by viewLifecycle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewModelProvider(requireActivity())[ReaderViewModel::class.java].let {
            publication = it.publication
            viewModel = it
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentListviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bookmarkAdapter = BookmarkAdapter(
            publication,
            onBookmarkSelectedRequested = { bookmark -> onBookmarkSelected(bookmark) })

        binding.listView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookmarkAdapter
        }

        when (viewModel.bookmarks.isEmpty()) {
            true -> {
                binding.placeholder.setText(R.string.bookmarks_placeholder)
                binding.listView.isVisible = false
            }
            false -> {
                bookmarkAdapter.submitList(
                    viewModel.bookmarks.sortedWith(
                        compareBy({ it.resourceIndex }, { it.locator.locations.progression })
                    )
                )
                binding.placeholder.isVisible = false
                binding.listView.isVisible = true
            }
        }

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private val background: Drawable = ColorDrawable(Color.RED).apply { alpha = 0 }

            private val backgroundAnimator = ValueAnimator.ofInt(0, 255).apply {
                duration = ANIMATION_DURATION
                addUpdateListener {
                    with(it.animatedValue as Int) {
                        background.alpha = this
                    }
                }
            }

            private var shouldShowBackground = true
            private var shouldHideBackground = false

            private val deleteIcon: Drawable = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_baseline_delete_24
            ) ?: throw java.lang.IllegalStateException()

            private val deleteIconMargin = 8.toPx(requireContext())

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                viewModel.deleteBookmark(viewModel.bookmarks[position])
                bookmarkAdapter.submitList(
                    viewModel.bookmarks.sortedWith(
                        compareBy({ it.resourceIndex }, { it.locator.locations.progression })
                    )
                )
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (viewHolder.bindingAdapterPosition == -1) return

                val itemView = viewHolder.itemView
                val offset = abs(dX.toDouble() / itemView.width)

                if (offset > 0.1 && shouldShowBackground) {
                    backgroundAnimator.cancel()
                    backgroundAnimator.start()
                    shouldHideBackground = true
                    shouldShowBackground = false
                } else if (offset < 0.1 && shouldHideBackground) {
                    backgroundAnimator.cancel()
                    backgroundAnimator.reverse()
                    shouldShowBackground = true
                    shouldHideBackground = false
                }

                background.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )

                background.draw(c)

                val itemHeight = itemView.bottom - itemView.top
                val deleteIconLeft: Int =
                    itemView.right - deleteIconMargin - deleteIcon.intrinsicWidth
                val deleteIconRight: Int = itemView.right - deleteIconMargin
                val deleteIconTop = itemView.top + (itemHeight - deleteIcon.intrinsicWidth) / 2
                val deleteIconBottom = deleteIconTop + deleteIcon.intrinsicWidth

                deleteIcon.setBounds(
                    deleteIconLeft,
                    deleteIconTop,
                    deleteIconRight,
                    deleteIconBottom
                )

                deleteIcon.draw(c)

                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }

        }).attachToRecyclerView(binding.listView)
    }

    private fun onBookmarkSelected(bookmark: Bookmark) {
        setFragmentResult(
            OutlineContract.REQUEST_KEY,
            OutlineContract.createResult(bookmark.locator)
        )
    }

    companion object {
        private const val ANIMATION_DURATION = 200L
    }
}

class BookmarkAdapter(
    private val publication: Publication,
    private val onBookmarkSelectedRequested: (Bookmark) -> Unit
) :
    ListAdapter<Bookmark, BookmarkAdapter.ViewHolder>(BookmarksDiff()) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            ItemRecycleBookmarkBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(private val binding: ItemRecycleBookmarkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(bookmark: Bookmark) {
            val title: String = getBookSpineItem(bookmark.location)
                ?: itemView.resources.getString(
                    R.string.chapter_page,
                    bookmark.locator.locations.position
                )

            binding.bookmarkChapter.text = title

            binding.root.setOnClickListener {
                onBookmarkSelectedRequested(bookmark)
            }
        }
    }

    private fun getBookSpineItem(href: String): String? {
        for (link in publication.tableOfContents) {
            if (link.href == href) {
                return link.outlineTitle
            }
        }
        for (link in publication.readingOrder) {
            if (link.href == href) {
                return link.outlineTitle
            }
        }
        return null
    }
}

private class BookmarksDiff : DiffUtil.ItemCallback<Bookmark>() {

    override fun areItemsTheSame(
        oldItem: Bookmark,
        newItem: Bookmark
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: Bookmark,
        newItem: Bookmark
    ): Boolean {
        return oldItem.bookId == newItem.bookId
                && oldItem.location == newItem.location
    }
}
