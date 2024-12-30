package me.kifio.kreader.android.outline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.kifio.kreader.android.R
import me.kifio.kreader.android.databinding.ItemRecycleNavigationBinding
import me.kifio.kreader.android.utils.extensions.outlineTitle
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.opds.images

class NavigationFragment : OutlineFragment() {

    private lateinit var navAdapter: NavigationAdapter

    override var titleRes: Int = R.string.contents_tab_label

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navAdapter = NavigationAdapter(
            onLinkSelected = { link -> publication.locatorFromLink(link)?.let { seekTo(it) } }
        )

        val links: List<Link> = when {
            publication.tableOfContents.isNotEmpty() -> publication.tableOfContents
            publication.readingOrder.isNotEmpty() -> publication.readingOrder
            publication.images.isNotEmpty() -> publication.images
            else -> mutableListOf()
        }

        val flatLinks = mutableListOf<Pair<Int, Link>>()

        for (link in links) {
            val children = childrenOf(Pair(0, link))
            // Append parent.
            flatLinks.add(Pair(0, link))
            // Append children, and their children... recursive.
            flatLinks.addAll(children)
        }

        binding.listView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = navAdapter
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    LinearLayoutManager.VERTICAL
                )
            )
        }

        binding.placeholder.setText(R.string.contents_placeholder)

        when (flatLinks.isEmpty()) {
            true -> {
                binding.placeholder.setText(R.string.bookmarks_placeholder)
                binding.listView.isVisible = false
            }
            false -> {
                navAdapter.submitList(flatLinks)
                binding.placeholder.isVisible = false
                binding.listView.isVisible = true
            }
        }
    }
}

class NavigationAdapter(private val onLinkSelected: (Link) -> Unit) :
        ListAdapter<Pair<Int, Link>, NavigationAdapter.ViewHolder>(NavigationDiff()) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
    ): ViewHolder {
        return ViewHolder(
            ItemRecycleNavigationBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(private val binding: ItemRecycleNavigationBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Pair<Int, Link>) {
            binding.navigationTextView.text = item.second.outlineTitle
            binding.indentation.layoutParams = LinearLayout.LayoutParams(item.first * 50, ViewGroup.LayoutParams.MATCH_PARENT)
            binding.root.setOnClickListener {
                onLinkSelected(item.second)
            }
        }
    }
}

private class NavigationDiff : DiffUtil.ItemCallback<Pair<Int, Link>>() {

    override fun areItemsTheSame(
            oldItem: Pair<Int, Link>,
            newItem: Pair<Int, Link>
    ): Boolean {
        return oldItem.first == newItem.first
                && oldItem.second == newItem.second
    }

    override fun areContentsTheSame(
            oldItem: Pair<Int, Link>,
            newItem: Pair<Int, Link>
    ): Boolean {
        return oldItem.first == newItem.first
                && oldItem.second == newItem.second
    }
}

fun childrenOf(parent: Pair<Int, Link>): MutableList<Pair<Int, Link>> {
    val indentation = parent.first + 1
    val children = mutableListOf<Pair<Int, Link>>()
    for (link in parent.second.children) {
        children.add(Pair(indentation, link))
        children.addAll(childrenOf(Pair(indentation, link)))
    }
    return children
}
