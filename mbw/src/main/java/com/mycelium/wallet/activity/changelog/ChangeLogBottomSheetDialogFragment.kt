package com.mycelium.wallet.activity.changelog

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.toSpannable
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.changelog.adapter.ChangeLogListAdapter
import com.mycelium.wallet.databinding.FragmentChangeLogBinding

internal class ChangeLogBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private val _adapter by lazy(::ChangeLogListAdapter)
    private val _viewModel by viewModels<ChangeLogViewModel>()
    private var _binding: FragmentChangeLogBinding? = null

    override fun getTheme(): Int = R.style.MyceliumModern_BottomSheetDialogTheme_Transparent

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentChangeLogBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListeners()
        initObservers()
    }

    override fun onDestroyView() {
        removeListeners()
        _binding = null
        super.onDestroyView()
    }

    private fun initView() {
        _binding?.recyclerView?.apply {
            adapter = _adapter
            itemAnimator = null
        }

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            isFitToContents = true
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun initObservers() {
        _viewModel.releases.observe(viewLifecycleOwner, _adapter::submitList)
        _viewModel.displayingListType.observe(viewLifecycleOwner) { type ->
            _binding?.listTypeTextView?.text = when (type) {
                ChangeLogViewModel.DisplayingListType.FULL -> R.string.changelog_button_show_less
                else -> R.string.changelog_button_show_more
            }.let {
                val result = resources.getString(it).toSpannable()
                result.setSpan(UnderlineSpan(), 0, result.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                result
            }
        }
    }

    private fun initListeners() {
        _binding?.apply {
            listTypeTextView.setOnClickListener { _viewModel.toggleDisplayingListType() }
            closeButton.setOnClickListener { dismiss() }
        }
    }

    private fun removeListeners() {
        _binding?.apply {
            listTypeTextView.setOnClickListener(null)
            closeButton.setOnClickListener(null)
        }
    }
}
