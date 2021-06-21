package com.mycelium.giftbox.purchase

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.bequant.receive.adapter.*
import com.mycelium.wallet.ExchangeRateManager
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.modern.model.accounts.AccountViewModel
import com.mycelium.wallet.activity.modern.model.accounts.AccountsGroupModel
import com.mycelium.wallet.activity.modern.model.accounts.AccountsListModel
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
    private val listModel: AccountsListModel by activityViewModels()

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
        listModel.accountsData.observe(viewLifecycleOwner, Observer {
            generateAccountList(it)
        })
        val accountsGroupsList = listModel.accountsData.value!!
        generateAccountList(accountsGroupsList)
        list.adapter = adapter
    }

    private fun generateAccountList(accountView: List<AccountsGroupModel>) {
        val currencies = args.currencies.mapNotNull { it.name }
        val accountsList = mutableListOf<AccountListItem>()
        for (accountsGroup in accountView) {
            if (accountsGroup.getType() == com.mycelium.wallet.activity.modern.model.accounts.AccountListItem.Type.GROUP_ARCHIVED_TITLE_TYPE) {
                continue
            }
            val accounts = accountsGroup.accountsList
                    .filterIsInstance(AccountViewModel::class.java)
                    .filter {
                        it.canSpend && it.balance?.spendable?.moreThanZero() == true
                                && currencies.find { cur -> cur.equals(Util.trimTestnetSymbolDecoration(it.coinType.symbol), true) } != null
                    }
                    .map { AccountItem(it.label, it.balance?.spendable, it.accountId) }

            if (accounts.isNotEmpty()) {
                val groupModel = AccountGroupItem(!accountsGroup.isCollapsed, accountsGroup.getTitle(requireContext()), accountsGroup.sum!!)
                accountsList.add(groupModel)
                if (groupModel.isOpened) {
                    accountsList.addAll(accounts)
                }
            }
        }
        adapter.submitList(accountsList)

        adapter.accountClickListener = { accountItem ->
            findNavController().navigate(SelectAccountFragmentDirections.actionNext(accountItem.accountId!!, args.product))
        }
    }

    override fun onDestroy() {
        mbwManager.exchangeRateManager.unsubscribe(this)
        super.onDestroy()
    }

    override fun refreshingExchangeRatesSucceeded() {
        lifecycleScope.launch(Dispatchers.Main) {
            loader(false)
            generateAccountList(listModel.accountsData.value!!)
        }
    }

    override fun refreshingExchangeRatesFailed() {
        lifecycleScope.launch(Dispatchers.Main) {
            loader(false)
        }
    }

    override fun exchangeSourceChanged() {
        lifecycleScope.launch(Dispatchers.Main) {
            generateAccountList(listModel.accountsData.value!!)
        }
    }
}