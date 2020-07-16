package com.mycelium.bequant.receive

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.receive.adapter.AccountAdapter
import com.mycelium.bequant.receive.adapter.AccountGroupItem
import com.mycelium.bequant.receive.adapter.AccountItem
import com.mycelium.bequant.receive.adapter.AccountListItem
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.getBTCSingleAddressAccounts
import com.mycelium.wallet.exchange.ValueSum
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.bip44.getBTCBip44Accounts
import com.mycelium.wapi.wallet.eth.getEthAccounts
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_bequant_select_account.*


class SelectAccountFragment : Fragment(R.layout.fragment_bequant_select_account) {
    val adapter = AccountAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        generateAccountList()
        list.adapter = adapter

    }

    private fun generateAccountList() {

        val accountsList = mutableListOf<AccountListItem>()
        val walletManager = MbwManager.getInstance(requireContext()).getWalletManager(false)
        val walletsAccounts = listOf(R.string.active_hd_accounts_name to walletManager.getBTCBip44Accounts(),
                R.string.active_bitcoin_sa_group_name to walletManager.getBTCSingleAddressAccounts(),
                R.string.eth_accounts_name to walletManager.getEthAccounts())
        walletsAccounts.forEach {
            if (it.second.isNotEmpty()) {
                accountsList.add(AccountGroupItem(true, getString(it.first), getSpendableBalance(it.second)))
                accountsList.addAll(it.second.map { AccountItem(it.label, it.accountBalance.confirmed) })
            }
        }
//        accountsList.add(TotalItem(getSpendableBalance()))
        adapter.submitList(accountsList)

        adapter.accountClickListener = { accountItem ->
            val selectedAccount = walletsAccounts.map { it.second }.flatten().find { it.label == accountItem.label }
            val accountData = AccountData(selectedAccount?.label)
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(Intent(CHOOSE_ACCOUNT_ACTION).putExtra(ACCOUNT_EXTRA, accountData))
            findNavController().popBackStack()
        }
    }

    @Parcelize
    data class AccountData(val label: String?) : Parcelable

    companion object {

        val CHOOSE_ACCOUNT_ACTION = "chooseAccount"
        val ACCOUNT_EXTRA = "account"

        private fun getSpendableBalance(walletAccountList: List<WalletAccount<out GenericAddress>>): ValueSum {
            val sum = ValueSum()
            for (account in walletAccountList) {
                if (account.isActive) {
                    sum.add(account.accountBalance.spendable)
                }
            }
            return sum
        }
    }
}