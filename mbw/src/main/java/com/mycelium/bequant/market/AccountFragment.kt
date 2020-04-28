package com.mycelium.bequant.market

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.InvestmentAccount
import com.mycelium.bequant.market.adapter.AccountItem
import com.mycelium.bequant.market.adapter.AccountsAdapter
import com.mycelium.bequant.remote.ApiRepository
import com.mycelium.bequant.remote.model.BequantBalance
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.fragment_bequant_account.*


class AccountFragment : Fragment(R.layout.fragment_bequant_account) {
    var receiveListener: (() -> Unit)? = null
    var withdrawListener: (() -> Unit)? = null

    val account = InvestmentAccount()
    val adapter = AccountsAdapter()
    var balancesData = listOf<BequantBalance>()

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
        ApiRepository.repository.balances({
            balancesData = it
            updateList()
        }, { code, error ->

        })
    }

    fun updateList() {
        adapter.submitList(balancesData
                .filter { !BequantPreference.hideZeroBalance() || it.available != "0" }
                .map { AccountItem(it.currency, it.currency, it.available) })
    }
}