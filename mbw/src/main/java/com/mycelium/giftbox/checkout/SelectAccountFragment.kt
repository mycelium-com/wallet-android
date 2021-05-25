package com.mycelium.giftbox.checkout

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.receive.adapter.AccountAdapter
import com.mycelium.bequant.receive.adapter.AccountGroupItem
import com.mycelium.bequant.receive.adapter.AccountItem
import com.mycelium.bequant.receive.adapter.AccountListItem
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.getBTCSingleAddressAccounts
import com.mycelium.wallet.activity.util.getSpendableBalance
import com.mycelium.wapi.wallet.btc.bip44.getBTCBip44Accounts
import com.mycelium.wapi.wallet.eth.getEthAccounts
import kotlinx.android.synthetic.main.fragment_bequant_select_account.*


class SelectAccountFragment : Fragment(R.layout.fragment_giftbox_select_account) {
    val adapter = AccountAdapter()
    val args by navArgs<SelectAccountFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        generateAccountList()
        list.adapter = adapter
    }

    private fun generateAccountList() {
        val walletManager = MbwManager.getInstance(requireContext()).getWalletManager(false)
        val btcWallets = listOf(R.string.active_hd_accounts_name to walletManager.getBTCBip44Accounts(),
                R.string.active_bitcoin_sa_group_name to walletManager.getBTCSingleAddressAccounts())
        val ethWallets = listOf(R.string.eth_accounts_name to walletManager.getEthAccounts())
        val walletsAccounts = btcWallets + ethWallets

        val accountsList = mutableListOf<AccountListItem>()
        walletsAccounts.forEach {
            if (it.second.isNotEmpty()) {
                accountsList.add(AccountGroupItem(true, getString(it.first), it.second.getSpendableBalance()))
                accountsList.addAll(it.second.map { AccountItem(it.label, it.accountBalance.confirmed) })
            }
        }
        adapter.submitList(accountsList)

        adapter.accountClickListener = { accountItem ->
            val selectedAccount = walletsAccounts.map { it.second }.flatten().find { it.label == accountItem.label }
            findNavController().navigate(SelectAccountFragmentDirections.actionCheckout(args.product, selectedAccount?.id!!, 100, 0))
        }
    }
}