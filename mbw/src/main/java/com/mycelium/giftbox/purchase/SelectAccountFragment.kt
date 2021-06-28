package com.mycelium.giftbox.purchase

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.giftbox.GiftboxPreference
import com.mycelium.giftbox.purchase.adapter.AccountAdapter
import com.mycelium.wallet.ExchangeRateManager
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.modern.model.accounts.AccountListItem
import com.mycelium.wallet.activity.modern.model.accounts.AccountViewModel
import com.mycelium.wallet.activity.modern.model.accounts.AccountsGroupModel
import com.mycelium.wallet.activity.modern.model.accounts.AccountsListModel
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.view.VerticalSpaceItemDecoration
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.event.AccountListChanged
import com.mycelium.wapi.wallet.Util
import com.mycelium.wapi.wallet.fiat.coins.FiatType
import kotlinx.android.synthetic.main.fragment_giftbox_select_account.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SelectAccountFragment : Fragment(R.layout.fragment_giftbox_select_account), ExchangeRateManager.Observer {
    val adapter = AccountAdapter()
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
        list.addItemDecoration(VerticalSpaceItemDecoration(resources.getDimensionPixelOffset(R.dimen.fio_list_item_space)))
        adapter.accountClickListener = { accountItem ->
            findNavController().navigate(SelectAccountFragmentDirections.actionNext(accountItem.accountId, args.product))
        }
        adapter.groupClickListener = {
            val title = it.getTitle(requireContext())
            GiftboxPreference.setGroupOpen(title, !GiftboxPreference.isGroupOpen(title))
            MbwManager.getEventBus().post(AccountListChanged())
        }
    }

    private fun generateAccountList(accountView: List<AccountsGroupModel>) {
        val currencies = args.currencies.mapNotNull { it.name }
        val accountsList = mutableListOf<AccountListItem>()
        for (accountsGroup in accountView) {
            if (accountsGroup.getType() == AccountListItem.Type.GROUP_ARCHIVED_TITLE_TYPE) {
                continue
            }
            val accounts = accountsGroup.accountsList
                    .filterIsInstance(AccountViewModel::class.java)
                    .filter {
                        it.canSpend && it.balance?.spendable?.moreThanZero() == true
                                && currencies.find { cur -> cur.equals(Util.trimTestnetSymbolDecoration(it.coinType.symbol), true) } != null
                    }
            if (accounts.isNotEmpty()) {
                val group = AccountsGroupModel(
                        accountsGroup.titleId, accountsGroup.getType(), accountsGroup.sum, accounts,
                        accountsGroup.coinType, accountsGroup.isInvestmentAccount
                )
                group.isCollapsed = !GiftboxPreference.isGroupOpen(accountsGroup.getTitle(requireContext()))
                accountsList.add(group)
                if (!group.isCollapsed) {
                    val fiatType = FiatType(args.product.currencyCode)
                    accounts.forEach { model ->
                        mbwManager.exchangeRateManager.get(model.balance?.spendable, fiatType)?.let {
                            model.additional["fiat"] = it.toStringWithUnit()
                        }
                    }
                    accountsList.addAll(accounts)
                }
            }
        }
        adapter.submitList(accountsList)
    }

    override fun onDestroy() {
        mbwManager.exchangeRateManager.unsubscribe(this)
        super.onDestroy()
    }

    override fun refreshingExchangeRatesSucceeded() {
        lifecycleScope.launch(Dispatchers.Main) {
            loader(false)
            MbwManager.getEventBus().post(AccountListChanged())
        }

    }

    override fun refreshingExchangeRatesFailed() {
        lifecycleScope.launch(Dispatchers.Main) {
            loader(false)
        }
    }

    override fun exchangeSourceChanged() {
        lifecycleScope.launch(Dispatchers.Main) {
            MbwManager.getEventBus().post(AccountListChanged())
        }
    }
}