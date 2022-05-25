package com.mycelium.wallet.external.changelly2

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.view.DividerItemDecoration
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentChangelly2HistoryBinding
import com.mycelium.wallet.external.adapter.TxHistoryAdapter
import com.mycelium.wallet.external.adapter.TxItem
import com.mycelium.wallet.external.changelly2.remote.Changelly2Repository
import java.text.DateFormat
import java.util.*


class HistoryFragment : DialogFragment() {

    var binding: FragmentChangelly2HistoryBinding? = null
    val pref by lazy { requireContext().getSharedPreferences(ExchangeFragment.PREF_FILE, Context.MODE_PRIVATE) }
    val adapter = TxHistoryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Dialog_Changelly)
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
                    putString(ExchangeResultFragment.KEY_CHANGELLY_TX_ID, it.id)
                    putString(ExchangeResultFragment.KEY_CHAIN_TX, pref.getString("tx_id_${it.id}", null))
                    putSerializable(ExchangeResultFragment.KEY_ACCOUNT_FROM_ID,
                            pref.getString("account_from_id_${it.id}", null)?.let { UUID.fromString(it) })
                    putSerializable(ExchangeResultFragment.KEY_ACCOUNT_TO_ID,
                            pref.getString("account_to_id_${it.id}", null)?.let { UUID.fromString(it) })
                }
            }.show(parentFragmentManager, "")
        }
        binding?.list?.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider_bequant), LinearLayout.VERTICAL))
        binding?.list?.adapter = adapter
        val txIds = (pref.getStringSet(ExchangeFragment.KEY_HISTORY, null) ?: setOf()).toList()
        loader(true)
        Changelly2Repository.getTransactions(lifecycleScope, txIds.toList(),
                {
                    it?.result?.let {
                        adapter.submitList(it.map {
                            TxItem(it.id,
                                    it.amountExpectedFrom.toString(), it.amountExpectedTo.toString(),
                                    it.currencyFrom, it.currencyTo,
                                    DateFormat.getDateInstance(DateFormat.LONG).format(Date(it.createdAt * 1000L)),
                                    it.getReadableStatus())
                        })
                    }
                },
                { _, _ ->

                },
                {
                    loader(false)
                })
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