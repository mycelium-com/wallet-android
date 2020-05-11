package com.mycelium.bequant.withdraw

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import com.mycelium.bequant.receive.adapter.AccountPagerAdapter
import com.mycelium.bequant.withdraw.viewmodel.WithdrawViewModel
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_bequant_withdraw_pager_accounts.*

class WithdrawWalletFragment : Fragment(R.layout.fragment_bequant_withdraw_mycelium_wallet) {
    var parentViewModel: WithdrawViewModel? = null
    val adapter = AccountPagerAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mbwManager = MbwManager.getInstance(requireContext())
        accountList.adapter = adapter
        TabLayoutMediator(accountListTab, accountList) { tab, _ ->
        }.attach()
        parentViewModel?.currency?.observe(viewLifecycleOwner, Observer { coinSymbol ->
            val accounts = mbwManager.getWalletManager(false).getAllActiveAccounts()
                    .filter { it.coinType.symbol == parentViewModel?.currency?.value }
            adapter.submitList(accounts)
        })
        selectAccountMore.setOnClickListener {
            findNavController().navigate(WithdrawFragmentDirections.actionSelectAccount())
        }
    }
}