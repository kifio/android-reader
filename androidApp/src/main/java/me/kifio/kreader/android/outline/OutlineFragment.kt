package me.kifio.kreader.android.outline

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import dev.chrisbanes.insetter.applyInsetter
import me.kifio.kreader.android.R
import me.kifio.kreader.android.databinding.FragmentOutlineBinding
import me.kifio.kreader.android.reader.ReaderViewModel
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

abstract class OutlineFragment : Fragment(R.layout.fragment_outline) {

    companion object {
        val FRAGMENT_REQUEST_KEY = "OUTLINE_FRAGMENT_REQUEST"
        val SELECTED_LOCATOR = "SELECTED_LOCATOR"
    }

    protected abstract var titleRes: Int

    protected val publication: Publication
        get() = model.publication

    protected lateinit var binding: FragmentOutlineBinding

    protected val model: ReaderViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOutlineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.navigateUp.setOnClickListener {
            setFragmentResult(FRAGMENT_REQUEST_KEY, Bundle.EMPTY)
            findNavController().navigateUp()
        }

        binding.title.setText(titleRes)

        binding.root.applyInsetter {
            type(statusBars = true) {
                margin(top = true)
            }
        }
    }

    protected fun seekTo(locator: Locator) {
        setFragmentResult(FRAGMENT_REQUEST_KEY, Bundle().apply {
            putParcelable(SELECTED_LOCATOR, locator)
        })
        findNavController().navigateUp()
    }
}