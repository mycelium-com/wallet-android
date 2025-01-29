package com.mycelium.giftbox.purchase

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mycelium.giftbox.GiftboxPreference
import com.mycelium.giftbox.client.GiftboxConstants
import com.mycelium.giftbox.purchase.adapter.AccountAdapter
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.ExchangeRateManager
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.modern.ModernMain
import com.mycelium.wallet.activity.modern.helper.MainActions
import com.mycelium.wallet.activity.modern.model.accounts.AccountListItem
import com.mycelium.wallet.activity.modern.model.accounts.AccountViewModel
import com.mycelium.wallet.activity.modern.model.accounts.AccountsGroupModel
import com.mycelium.wallet.activity.modern.model.accounts.AccountsListModel
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.view.VerticalSpaceItemDecoration
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentGiftboxSelectAccountBinding
import com.mycelium.wallet.event.AccountListChanged
import com.mycelium.wapi.wallet.Util
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.fiat.coins.FiatType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SelectAccountFragment : Fragment(), ExchangeRateManager.Observer {
    val adapter = AccountAdapter()
    val args by navArgs<SelectAccountFragmentArgs>()
    private val listModel: AccountsListModel by activityViewModels()
    val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
    var binding: FragmentGiftboxSelectAccountBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currencyList = mbwManager.exchangeRateManager.currencyList
        val fiatType = FiatType(args.mcproduct.currency)
        if (!currencyList.contains(fiatType)) {
            mbwManager.exchangeRateManager.setCurrencyList((currencyList + listOf(fiatType)).toSet())
            mbwManager.exchangeRateManager.subscribe(this)
            loader(true)
            mbwManager.exchangeRateManager.requestRefresh()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentGiftboxSelectAccountBinding.inflate(inflater, container, false)
        .apply {
            binding = this
        }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listModel.accountsData.observe(viewLifecycleOwner, Observer {
            generateAccountList(it)
        })
        val accountsGroupsList = listModel.accountsData.value!!
        generateAccountList(accountsGroupsList)
        binding?.list?.adapter = adapter
        binding?.list?.addItemDecoration(VerticalSpaceItemDecoration(resources.getDimensionPixelOffset(R.dimen.fio_list_item_space)))
        binding?.accounts?.setOnClickListener {
            requireActivity().finishAffinity()
            startActivity(Intent(requireContext(), ModernMain::class.java)
                    .apply { action = MainActions.ACTION_ACCOUNTS })
        }
        adapter.accountClickListener = { accountItem ->
            findNavController().navigate(SelectAccountFragmentDirections.actionNext(accountItem.accountId, args.mcproduct))
        }
        adapter.groupClickListener = {
            val title = it.getTitle(requireContext())
            GiftboxPreference.setGroupOpen(title, !GiftboxPreference.isGroupOpen(title))
            MbwManager.getEventBus().post(AccountListChanged())
        }
    }
    
    private fun generateAccountList(accountView: List<AccountsGroupModel>) {
        val currenciesByName =
            listOf("BTC")//args.currencies.mapNotNull { it.name }
        val currenciesByAddress =
            listOf<String>() // args.currencies.mapNotNull { it.contractAddress }
        val accountsList = mutableListOf<AccountListItem>()
        accountView.forEach { accountsGroup->
            if (accountsGroup.getType() == AccountListItem.Type.GROUP_ARCHIVED_TITLE_TYPE) {
                return@forEach
            }
            val accounts = accountsGroup.accountsList
                    .filterIsInstance<AccountViewModel>()
                    .filter {
                        it.canSpend &&
                                (it.balance?.spendable?.moreThanZero() == true || GiftboxConstants.TEST) &&
                                isAccountTypeSupported(it.coinType, currenciesByName, currenciesByAddress)
                    }
            if (accounts.isNotEmpty()) {
                val group = AccountsGroupModel(
                        accountsGroup.titleId, accountsGroup.getType(), accountsGroup.sum, accounts,
                        accountsGroup.coinType, accountsGroup.isInvestmentAccount
                )
                group.isCollapsed = !GiftboxPreference.isGroupOpen(accountsGroup.getTitle(requireContext()))
                accountsList.add(group)
                if (!group.isCollapsed) {
                    val fiatType = FiatType(args.mcproduct.currency)
                    accounts.forEach { model ->
                        mbwManager.exchangeRateManager.get(model.balance?.spendable, fiatType)?.let {
                            model.additional["fiat"] = it.toStringWithUnit()
                        }
                    }
                    accountsList.addAll(accounts)
                }
            }
        }
        binding?.emptyList?.visibility = if (accountsList.isEmpty()) VISIBLE else GONE
        binding?.selectAccountLabel?.visibility = if (accountsList.isEmpty()) GONE else VISIBLE
        adapter.submitList(accountsList)
    }

    private fun isAccountTypeSupported(
        type: CryptoCurrency,
        currenciesByName: List<String>,
        currenciesByAddress: List<String>
    ) = when (type) {
        is ERC20Token -> {
            currenciesByAddress.find { cur ->
                cur.equals(type.contractAddress, true) } != null
        }
        else -> {
            currenciesByName.find { cur ->
                cur.equals(Util.trimTestnetSymbolDecoration(type.symbol), true)
            } != null
        }
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