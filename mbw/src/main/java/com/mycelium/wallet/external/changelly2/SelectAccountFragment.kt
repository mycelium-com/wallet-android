package com.mycelium.wallet.external.changelly2

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.mycelium.giftbox.GiftboxPreference
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.addaccount.ERC20EthAccountAdapter
import com.mycelium.wallet.activity.modern.model.accounts.AccountListItem
import com.mycelium.wallet.activity.modern.model.accounts.AccountViewModel
import com.mycelium.wallet.activity.modern.model.accounts.AccountsGroupModel
import com.mycelium.wallet.activity.modern.model.accounts.AccountsListModel
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.FragmentChangelly2SelectAccountBinding
import com.mycelium.wallet.databinding.LayoutSelectEthAccountToErc20Binding
import com.mycelium.wallet.external.changelly2.adapter.AddAccountModel
import com.mycelium.wallet.external.changelly2.adapter.GroupModel
import com.mycelium.wallet.external.changelly2.adapter.SelectAccountAdapter
import com.mycelium.wallet.external.changelly2.viewmodel.ExchangeViewModel
import com.mycelium.wapi.wallet.Util
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.getActiveEthAccounts


class SelectAccountFragment : DialogFragment() {

    val adapter = SelectAccountAdapter()
    var binding: FragmentChangelly2SelectAccountBinding? = null
    val viewModel: ExchangeViewModel by activityViewModels()
    val listModel: AccountsListModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentChangelly2SelectAccountBinding.inflate(inflater).apply {
                binding = this
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.list?.adapter = adapter
        adapter.accountClickListener = { accountItem ->
            if (arguments?.getString(KEY_TYPE) == VALUE_SELL) {
                viewModel.fromAccount.value = viewModel.mbwManager.getWalletManager(false).getAccount(accountItem.accountId)
            } else {
                viewModel.toAccount.value = viewModel.mbwManager.getWalletManager(false).getAccount(accountItem.accountId)
            }
            dismissAllowingStateLoss()
        }
        adapter.addAccountListener = { addAccount ->
            showEthAccountsOptions()
        }
        listModel.accountsData.observe(viewLifecycleOwner) {
            generateAccountList(it)
        }
        val accountsGroupsList = listModel.accountsData.value!!
        generateAccountList(accountsGroupsList)
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
                                    && viewModel.currencies.contains(Util.trimTestnetSymbolDecoration(it.coinType.symbol).toLowerCase())

                        } else {
                            it.accountId != viewModel.fromAccount.value?.id
                                    && viewModel.currencies.contains(Util.trimTestnetSymbolDecoration(it.coinType.symbol).toLowerCase())
                        }
                    }
            if (accounts.isNotEmpty()) {
                val group = AccountsGroupModel(
                        accountsGroup.titleId, accountsGroup.getType(), accountsGroup.sum, accounts,
                        accountsGroup.coinType, accountsGroup.isInvestmentAccount
                )
                group.isCollapsed = !GiftboxPreference.isGroupOpen(accountsGroup.getTitle(requireContext()))
                accountsList.add(group)
                if (!group.isCollapsed) {
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
                viewModel.currencies.contains(Util.trimTestnetSymbolDecoration(it.value.symbol).toLowerCase())
                        && !alreadyHave.contains(it.value.symbol)
            }.map {
                AddAccountModel(it.value)
            }
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

    private fun showEthAccountsOptions() {
        val arrayAdapter = ERC20EthAccountAdapter(requireContext(), R.layout.checked_item)
        val accounts = viewModel.mbwManager.getWalletManager(false).getActiveEthAccounts()
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
                }
                .show()
    }

    private fun getEthAccountsForView(accounts: List<WalletAccount<*>>): List<String> =
            accounts.sortedBy { (it as EthAccount).accountIndex }
                    .map { account ->
                        val denominatedValue = account.accountBalance.spendable.toStringWithUnit(viewModel.mbwManager.getDenomination(account.coinType))
                        account.label + " (" + denominatedValue + ")"
                    }

    companion object {
        const val KEY_TYPE = "type"
        const val VALUE_SELL = "sell"
        const val VALUE_BUY = "buy"
    }
}