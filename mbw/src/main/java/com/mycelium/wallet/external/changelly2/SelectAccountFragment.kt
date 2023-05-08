package com.mycelium.wallet.external.changelly2

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.mycelium.giftbox.GiftboxPreference
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.addaccount.ERC20EthAccountAdapter
import com.mycelium.wallet.activity.addaccount.createERC20
import com.mycelium.wallet.activity.addaccount.createETH
import com.mycelium.wallet.activity.modern.ModernMain
import com.mycelium.wallet.activity.modern.helper.MainActions
import com.mycelium.wallet.activity.modern.model.accounts.AccountListItem
import com.mycelium.wallet.activity.modern.model.accounts.AccountViewModel
import com.mycelium.wallet.activity.modern.model.accounts.AccountsGroupModel
import com.mycelium.wallet.activity.modern.model.accounts.AccountsListModel
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.view.loader
import com.mycelium.wallet.databinding.FragmentChangelly2SelectAccountBinding
import com.mycelium.wallet.databinding.LayoutSelectEthAccountToErc20Binding
import com.mycelium.wallet.event.AccountListChanged
import com.mycelium.wallet.external.changelly2.adapter.AddAccountModel
import com.mycelium.wallet.external.changelly2.adapter.GroupModel
import com.mycelium.wallet.external.changelly2.adapter.SelectAccountAdapter
import com.mycelium.wallet.external.changelly2.viewmodel.ExchangeViewModel
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.getActiveEthAccounts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class SelectAccountFragment : DialogFragment() {

    val adapter = SelectAccountAdapter()
    var binding: FragmentChangelly2SelectAccountBinding? = null
    val viewModel: ExchangeViewModel by activityViewModels()
    val listModel: AccountsListModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Dialog_Changelly)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentChangelly2SelectAccountBinding.inflate(inflater).apply {
                binding = this
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.selectAccountLabel?.text =
                if (arguments?.getString(KEY_TYPE) == VALUE_SELL) {
                    getString(R.string.changelly2_select_from)
                } else {
                    getString(R.string.changelly2_select_to)
                }
        binding?.toolbar?.setNavigationOnClickListener {
            dismissAllowingStateLoss()
        }
        binding?.accounts?.setOnClickListener {
            requireActivity().finishAffinity()
            startActivity(Intent(requireContext(), ModernMain::class.java)
                    .apply { action = MainActions.ACTION_ACCOUNTS })
        }
        binding?.list?.adapter = adapter
        binding?.list?.itemAnimator = null
        adapter.accountClickListener = { accountItem ->
            setAccount(accountItem.accountId)
        }
        adapter.addAccountListener = { addAccount ->
            if (addAccount is ERC20Token) {
                showEthAccountsOptions(addAccount)
            }
        }
        adapter.groupClickListener = {
            val title = it.getTitle(requireContext())
            GiftboxPreference.setGroupOpen(title, !GiftboxPreference.isGroupOpen(title))
            MbwManager.getEventBus().post(AccountListChanged())
        }
        adapter.groupModelClickListener = {
            GiftboxPreference.setGroupOpen(it.title, !GiftboxPreference.isGroupOpen(it.title))
            MbwManager.getEventBus().post(AccountListChanged())
        }
        listModel.accountsData.observe(viewLifecycleOwner) {
            generateAccountList(it)
        }
        val accountsGroupsList = listModel.accountsData.value!!
        generateAccountList(accountsGroupsList)
    }

    private fun setAccount(accountId: UUID) {
        val keyType = arguments?.getString(KEY_TYPE)
        lifecycleScope.launch {
            val account = viewModel.mbwManager.getWalletManager(false).getAccount(accountId)
            if (keyType == VALUE_SELL) {
                viewModel.fromAccount.postValue(account)
                viewModel.sellValue.postValue("")
            } else { viewModel.toAccount.postValue(account) }
            withContext(Dispatchers.Main.immediate) { dismissAllowingStateLoss() }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun generateAccountList(accountView: List<AccountsGroupModel>) {
        val accountsList = mutableListOf<AccountListItem>()
        accountView.forEach { accountsGroup ->
            if (accountsGroup.getType() == AccountListItem.Type.GROUP_ARCHIVED_TITLE_TYPE) {
                return@forEach
            }
            val accounts = accountsGroup.accountsList
                    .filterIsInstance(AccountViewModel::class.java)
                    .filter {
                        if (arguments?.getString(KEY_TYPE) == VALUE_SELL) {
                            it.canSpend && it.balance?.spendable?.moreThanZero() == true
                        } else {
                            it.coinType != viewModel.fromAccount.value?.coinType
                        }
                    }
                    .filter {
                        viewModel.isSupported(it.coinType)
                    }
            if (accounts.isNotEmpty()) {
                val group = AccountsGroupModel(
                        accountsGroup.titleId, accountsGroup.getType(), accountsGroup.sum, accounts,
                        accountsGroup.coinType, accountsGroup.isInvestmentAccount
                )
                group.isCollapsed = !GiftboxPreference.isGroupOpen(accountsGroup.getTitle(requireContext()))
                accountsList.add(group)
                if (!group.isCollapsed) {
                    accounts.forEach { model ->
                        viewModel.mbwManager.exchangeRateManager
                                .get(model.balance?.spendable, viewModel.mbwManager.getFiatCurrency(model.coinType))?.let {
                                    model.additional["fiat"] = it.toStringWithUnit()
                                }
                    }
                    accountsList.addAll(accounts)
                }
            }
        }
        if (arguments?.getString(KEY_TYPE) == VALUE_BUY) {
            val alreadyHave = accountView
                    .flatMap { it.accountsList }
                    .filterIsInstance(AccountViewModel::class.java)
                    .map {
                        it.coinType.symbol
                    }.toSet()
            val addAccountList = viewModel.mbwManager.supportedERC20Tokens.filter {
                viewModel.isSupported(it.value) && !alreadyHave.contains(it.value.symbol)
            }.map {
                AddAccountModel(it.value)
            }.sortedBy { it.coinType.symbol }
            if (addAccountList.isNotEmpty()) {
                val groupTitle = "All supported coins"
                val group = GroupModel(groupTitle)
                group.isCollapsed = !GiftboxPreference.isGroupOpen(groupTitle)
                accountsList.add(group)
                if (!group.isCollapsed) {
                    accountsList.addAll(addAccountList)
                }
            }
        }
        binding?.emptyList?.visibility = if (accountsList.isEmpty()) View.VISIBLE else View.GONE
        binding?.selectAccountLabel?.visibility = if (accountsList.isEmpty()) View.GONE else View.VISIBLE
        adapter.submitList(accountsList)
    }

    private fun showEthAccountsOptions(token: ERC20Token) {
        val arrayAdapter = ERC20EthAccountAdapter(requireContext(), R.layout.checked_item)
        val accounts = viewModel.mbwManager.getWalletManager(false).getActiveEthAccounts()
                .sortedBy { (it as EthAccount).accountIndex }
        arrayAdapter.addAll(getEthAccountsForView(accounts))
        arrayAdapter.add(getString(R.string.create_new_account))
        AlertDialog.Builder(requireContext(), R.style.MyceliumModern_Dialog_BlueButtons)
                .setCustomTitle(LayoutInflater.from(requireContext()).inflate(R.layout.layout_select_eth_account_to_erc20_title, null))
                .setView(LayoutSelectEthAccountToErc20Binding.inflate(LayoutInflater.from(requireContext())).apply {
                    list.adapter = arrayAdapter
                }.root)
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(getString(R.string.ok)) { _: DialogInterface?, _: Int ->
                    val selected = arrayAdapter.selected
                    if (selected == arrayAdapter.count - 1) {
                        // "Create new account" item
                        viewModel.mbwManager.createETH({
                            loader(true)
                        }, {
                            loader(false)
                            addToken(viewModel.mbwManager.getWalletManager(false).getAccount(it!!) as EthAccount, token)
                        })
                    } else {
                        val ethAccountId = accounts[selected].id
                        val ethAccount = viewModel.mbwManager.getWalletManager(false).getAccount(ethAccountId) as EthAccount
                        addToken(ethAccount, token)
                    }
                }
                .show()
    }

    private fun addToken(ethAccount: EthAccount, token: ERC20Token) {
        viewModel.mbwManager.createERC20(listOf(token), ethAccount, {
            loader(true)
        }, {
            loader(false)
            setAccount(it.first())
        })
    }

    private fun getEthAccountsForView(accounts: List<WalletAccount<*>>): List<String> =
            accounts.map { account ->
                        val denominatedValue = account.accountBalance.spendable.toStringWithUnit(viewModel.mbwManager.getDenomination(account.coinType))
                        account.label + " (" + denominatedValue + ")"
                    }

    companion object {
        const val KEY_TYPE = "type"
        const val VALUE_SELL = "sell"
        const val VALUE_BUY = "buy"
    }
}