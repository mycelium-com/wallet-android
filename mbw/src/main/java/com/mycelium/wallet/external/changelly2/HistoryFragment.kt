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
import java.util.*


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
                    putString(ExchangeResultFragment.KEY_CHAIN_TX, pref.getString("tx_id_${it}", null))
                    putSerializable(ExchangeResultFragment.KEY_ACCOUNT_FROM_ID,
                            pref.getString("account_from_id_${it}", null)?.let { UUID.fromString(it) })
                    putSerializable(ExchangeResultFragment.KEY_ACCOUNT_TO_ID,
                            pref.getString("account_to_id_${it}", null)?.let { UUID.fromString(it) })
                }
            }.show(parentFragmentManager, "")
        }
        binding?.list?.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider_bequant), LinearLayout.VERTICAL))
        binding?.list?.adapter = adapter
        adapter.submitList((pref.getStringSet(ExchangeFragment.KEY_HISTORY, null)
                ?: setOf()).toList())
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