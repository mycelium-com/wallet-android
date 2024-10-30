package com.mycelium.bequant.receive

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.mycelium.wallet.databinding.FragmentBequantSelectAccountBinding
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.bip44.getBTCBip44Accounts
import com.mycelium.wapi.wallet.eth.getEthAccounts
import kotlinx.android.parcel.Parcelize


class SelectAccountFragment : Fragment(R.layout.fragment_bequant_select_account) {
    val adapter = AccountAdapter()

    val args by navArgs<SelectAccountFragmentArgs>()
    var binding: FragmentBequantSelectAccountBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentBequantSelectAccountBinding.inflate(inflater, container, false)
        .apply {
            binding = this
        }
        .root
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        generateAccountList()
        binding?.list?.adapter = adapter
    }

    private fun generateAccountList() {

        val accountsList = mutableListOf<AccountListItem>()
        val walletManager = MbwManager.getInstance(requireContext()).getWalletManager(false)

        val btcWallets = listOf(R.string.active_hd_accounts_name to walletManager.getBTCBip44Accounts(),
                R.string.active_bitcoin_sa_group_name to walletManager.getBTCSingleAddressAccounts())
        val ethWallets = listOf(R.string.eth_accounts_name to walletManager.getEthAccounts())
        val walletsAccounts = mutableListOf<Pair<Int,List<WalletAccount<*>>>>()
        if (args.currency?.toLowerCase() == "btc") {
            walletsAccounts+=btcWallets
        }
        if (args.currency?.toLowerCase() == "eth") {
            walletsAccounts+=ethWallets
        }
        if(args.currency.isNullOrEmpty()){
            walletsAccounts+= btcWallets + ethWallets
        }
        walletsAccounts.forEach {
            if (it.second.isNotEmpty()) {
                accountsList.add(AccountGroupItem(true, getString(it.first), it.second.getSpendableBalance()))
                accountsList.addAll(it.second.map { AccountItem(it.label, it.accountBalance.confirmed) })
            }
        }
        adapter.submitList(accountsList)

        adapter.accountClickListener = { accountItem ->
            val selectedAccount = walletsAccounts.map { it.second }.flatten().find { it.label == accountItem.label }
            val accountData = AccountData(selectedAccount?.label)
            findNavController().previousBackStackEntry?.savedStateHandle?.set(ACCOUNT_KEY, accountData)
            findNavController().popBackStack()
        }
    }

    @Parcelize
    data class AccountData(val label: String?) : Parcelable

    companion object {
        const val ACCOUNT_KEY = "chooseAccount"
    }
}