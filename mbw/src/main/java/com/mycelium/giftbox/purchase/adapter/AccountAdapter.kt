package com.mycelium.giftbox.purchase.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.giftbox.purchase.adapter.holder.AccountGroupViewHolder
import com.mycelium.giftbox.purchase.adapter.holder.AccountItemViewHolder
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.adapter.AccountListAdapter
import com.mycelium.wallet.activity.modern.model.accounts.AccountListItem
import com.mycelium.wallet.activity.modern.model.accounts.AccountViewModel
import com.mycelium.wallet.activity.modern.model.accounts.AccountsGroupModel
import com.mycelium.wallet.activity.util.toStringFriendlyWithUnit


open class AccountAdapter : ListAdapter<AccountListItem, RecyclerView.ViewHolder>(DiffCallback()) {
    var accountClickListener: ((AccountViewModel) -> Unit)? = null
    var groupClickListener: ((AccountsGroupModel) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (AccountListItem.Type.fromId(viewType)) {
                AccountListItem.Type.ACCOUNT_TYPE ->
                    AccountItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_select_account, parent, false))
                AccountListItem.Type.GROUP_TITLE_TYPE ->
                    AccountGroupViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_select_account_group, parent, false))
                else -> throw IllegalArgumentException("Unknown account type")
            }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        val viewType = getItemViewType(position)
        when (AccountListItem.Type.fromId(viewType)) {
            AccountListItem.Type.ACCOUNT_TYPE -> {
                val accountHolder = holder as AccountItemViewHolder
                val account = item as AccountViewModel
                accountHolder.label.text = account.label
                accountHolder.coinType.text = account.coinType.symbol
                accountHolder.value.text = account.additional["fiat"]?.toString()
                accountHolder.value2.text = account.balance?.spendable?.toStringFriendlyWithUnit()
                accountHolder.itemView.setOnClickListener {
                    accountClickListener?.invoke(account)
                }
            }
            AccountListItem.Type.GROUP_TITLE_TYPE -> {
                val groupHolder = holder as AccountGroupViewHolder
                val group = item as AccountsGroupModel
                groupHolder.label.text = group.getTitle(groupHolder.label.context)
                groupHolder.count.text = "(${group.accountsList.size})"
                groupHolder.chevron.rotation = (if (!group.isCollapsed) 180 else 0).toFloat()
                groupHolder.itemView.setOnClickListener {
                    groupClickListener?.invoke(group)
                }
            }

            else -> {}
        }
    }

    override fun getItemViewType(position: Int) = getItem(position).getType().typeId

    class DiffCallback : AccountListAdapter.ItemListDiffCallback() {
        override fun areContentsTheSame(oldItem: AccountListItem, newItem: AccountListItem): Boolean =
                when (oldItem.getType()) {
                    AccountListItem.Type.ACCOUNT_TYPE -> {
                        newItem as AccountViewModel
                        oldItem as AccountViewModel
                        super.areContentsTheSame(oldItem, newItem)
                                && newItem.additional["fiat"].toString() == oldItem.additional["fiat"].toString()
                    }
                    else -> super.areContentsTheSame(oldItem, newItem)
                }
    }
}