package com.mycelium.wallet.external.changelly2

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.view.DividerItemDecoration
import com.mycelium.wallet.databinding.FragmentChangelly2HistoryBinding
import com.mycelium.wallet.external.adapter.TxHistoryAdapter


class HistoryFragment : DialogFragment() {

    var binding: FragmentChangelly2HistoryBinding? = null
    val pref by lazy { requireContext().getSharedPreferences(ExchangeFragment.PREF_FILE, Context.MODE_PRIVATE) }
    val adapter = TxHistoryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentChangelly2HistoryBinding.inflate(inflater).apply {
                binding = this
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.toolbar?.setNavigationOnClickListener {
            dismissAllowingStateLoss()
        }
        adapter.clickListener = {
            ExchangeResultFragment().apply {
                arguments = Bundle().apply {
                    putString(ExchangeResultFragment.KEY_TX_ID, it)
                }
            }.show(parentFragmentManager, "")
        }
        binding?.list?.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider_bequant), LinearLayout.VERTICAL))
        binding?.list?.adapter = adapter
        adapter.submitList((pref.getStringSet(ExchangeFragment.KEY_HISTORY, null) ?: setOf()).toList())
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}