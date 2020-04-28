package com.mycelium.bequant.withdraw

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.mycelium.bequant.receive.adapter.AccountPagerAdapter
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.layout_bequant_accounts_pager.*

class WithdrawWalletFragment : Fragment(R.layout.fragment_bequant_withdraw_mycelium_wallet) {
    val adapter = AccountPagerAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mbwManager = MbwManager.getInstance(requireContext())
        val coinSymbol = "ETH"
        accountList.adapter = adapter
        val accounts = mbwManager.getWalletManager(false).getAllActiveAccounts()
                .filter { it.coinType.symbol == coinSymbol }
        adapter.submitList(accounts)
    }
}