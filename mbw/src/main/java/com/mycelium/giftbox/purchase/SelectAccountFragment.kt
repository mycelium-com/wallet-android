package com.mycelium.giftbox.purchase

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.receive.adapter.*
import com.mycelium.wallet.ExchangeRateManager
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.util.getSpendableBalance
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wapi.wallet.Util
import com.mycelium.wapi.wallet.fiat.coins.FiatType
import kotlinx.android.synthetic.main.fragment_bequant_select_account.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SelectAccountFragment : Fragment(R.layout.fragment_giftbox_select_account), ExchangeRateManager.Observer {
    val adapter = AccountAdapter(AccountAdapterConfig(
            R.layout.item_giftbox_select_account,
            R.layout.item_giftbox_select_account_group,
            R.layout.item_giftbox_select_account_total
    ))
    val args by navArgs<SelectAccountFragmentArgs>()

    val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currencyList = mbwManager.exchangeRateManager.currencyList
        val fiatType = FiatType(args.product.currencyCode)
        if (!currencyList.contains(fiatType)) {
            mbwManager.exchangeRateManager.setCurrencyList((currencyList + listOf(fiatType)).toSet())
            mbwManager.exchangeRateManager.subscribe(this)
            loader(true)
            mbwManager.exchangeRateManager.requestRefresh()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        generateAccountList()
        list.adapter = adapter
    }

    private fun generateAccountList() {
        val currencies = args.currencies.mapNotNull { it.name }
        val accounts = mbwManager.getWalletManager(false).getActiveSpendingAccounts()
                .filter { account ->
                    currencies.find { it.equals(Util.trimTestnetSymbolDecoration(account.coinType.symbol), true) } != null
                }
        val walletsAccounts = accounts.map { it.coinType }.toSet()
                .map { coin -> coin.name to accounts.filter { it.coinType == coin } }

        val accountsList = mutableListOf<AccountListItem>()
        walletsAccounts.forEach {
            if (it.second.isNotEmpty()) {
                accountsList.add(AccountGroupItem(true, it.first, it.second.getSpendableBalance()))
                accountsList.addAll(it.second.map {
                    AccountItem(it.label,
                            mbwManager.exchangeRateManager.get(it.accountBalance.confirmed, FiatType(args.product.currencyCode)))
                })
            }
        }
        adapter.submitList(accountsList)

        adapter.accountClickListener = { accountItem ->
            val selectedAccount = walletsAccounts.map { it.second }.flatten().find { it.label == accountItem.label }
            findNavController().navigate(SelectAccountFragmentDirections.actionNext(selectedAccount?.id!!, args.product))
        }
    }

    override fun onDestroy() {
        mbwManager.exchangeRateManager.unsubscribe(this)
        super.onDestroy()
    }

    override fun refreshingExchangeRatesSucceeded() {
        lifecycleScope.launch(Dispatchers.Main) {
            loader(false)
            generateAccountList()
        }
    }

    override fun refreshingExchangeRatesFailed() {
        lifecycleScope.launch(Dispatchers.Main) {
            loader(false)
        }
    }

    override fun exchangeSourceChanged() {
        lifecycleScope.launch(Dispatchers.Main) {
            generateAccountList()
        }
    }
}