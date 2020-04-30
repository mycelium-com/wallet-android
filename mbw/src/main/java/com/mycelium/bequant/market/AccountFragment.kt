package com.mycelium.bequant.market

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.Constants
import com.mycelium.bequant.InvestmentAccount
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.market.adapter.AccountItem
import com.mycelium.bequant.market.adapter.BequantAccountAdapter
import com.mycelium.bequant.remote.ApiRepository
import com.mycelium.bequant.remote.model.BequantBalance
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_account.*


class AccountFragment : Fragment(R.layout.fragment_bequant_account) {
    var receiveListener: (() -> Unit)? = null
    var withdrawListener: (() -> Unit)? = null

    val account = InvestmentAccount()
    val adapter = BequantAccountAdapter()
    var balancesData = listOf<BequantBalance>()

    val receive = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            requestBalances()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receive, IntentFilter(Constants.ACTION_BEQUANT_KEYS))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deposit.setOnClickListener {
            receiveListener?.invoke()
        }
        withdraw.setOnClickListener {
            withdrawListener?.invoke()
        }
        hideZeroBalance.isChecked = BequantPreference.hideZeroBalance()
        hideZeroBalance.setOnCheckedChangeListener { _, checked ->
            BequantPreference.setHideZeroBalance(checked)
            updateList()
        }
        list.adapter = adapter
        adapter.addCoinListener = {
            receiveListener?.invoke()
        }
        requestBalances()
    }

    override fun onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receive)
        super.onDestroyView()
    }

    private fun requestBalances() {
        if (BequantPreference.hasKeys()) {
            ApiRepository.repository.balances({
                balancesData = it
                updateList()
            }, { code, error ->
                ErrorHandler(requireContext()).handle(error)
            })
        }
    }

    fun updateList() {
        adapter.submitList(balancesData
                .filter { !BequantPreference.hideZeroBalance() || it.available != "0" }
                .map { AccountItem(it.currency, it.currency, it.available) })
    }
}