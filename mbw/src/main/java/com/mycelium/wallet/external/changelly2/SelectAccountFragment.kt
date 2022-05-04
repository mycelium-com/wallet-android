package com.mycelium.wallet.external.changelly2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.mycelium.giftbox.GiftboxPreference
import com.mycelium.giftbox.purchase.adapter.AccountAdapter
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.model.accounts.AccountListItem
import com.mycelium.wallet.activity.modern.model.accounts.AccountViewModel
import com.mycelium.wallet.activity.modern.model.accounts.AccountsGroupModel
import com.mycelium.wallet.activity.modern.model.accounts.AccountsListModel
import com.mycelium.wallet.databinding.FragmentChangelly2SelectAccountBinding


class SelectAccountFragment : DialogFragment() {

    val adapter = AccountAdapter()
    var binding: FragmentChangelly2SelectAccountBinding? = null
    val listModel: AccountsListModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomDialog);
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            FragmentChangelly2SelectAccountBinding.inflate(inflater).apply {
                binding = this
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.list?.adapter = adapter
        adapter.accountClickListener = { accountItem ->
            
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
                    .filter { it.canSpend && it.balance?.spendable?.moreThanZero() == true }
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
        binding?.emptyList?.visibility = if (accountsList.isEmpty()) View.VISIBLE else View.GONE
        binding?.selectAccountLabel?.visibility = if (accountsList.isEmpty()) View.GONE else View.VISIBLE
        adapter.submitList(accountsList)
    }
}