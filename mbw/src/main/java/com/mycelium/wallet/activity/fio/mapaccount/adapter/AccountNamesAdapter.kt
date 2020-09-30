package com.mycelium.wallet.activity.fio.mapaccount.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.fio.mapaccount.adapter.viewholder.AccountViewHolder
import com.mycelium.wallet.activity.fio.mapaccount.adapter.viewholder.GroupRowViewHolder
import com.mycelium.wapi.wallet.WalletAccount


class FIONameItem(val title: String, val mappedAccountCount: Int = 0, var isClosed: Boolean = true) : Item(AccountNamesAdapter.TYPE_FIO_NAME)
class AccountItem(val account: WalletAccount<*>, val accountType: String) : Item(AccountNamesAdapter.TYPE_ACCOUNT)

class AccountNamesAdapter : ListAdapter<Item, RecyclerView.ViewHolder>(DiffCallback()) {
    companion object {
        const val TYPE_FIO_NAME = 0
        const val TYPE_ACCOUNT = 1
    }

    var fioNameClickListener: ((String) -> Unit)? = null
    var switchGroupVisibilityListener: ((String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                TYPE_FIO_NAME -> GroupRowViewHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_fio_group_row, parent, false))
                TYPE_ACCOUNT -> AccountViewHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_fio_name_details_account, parent, false))
                else -> TODO("Not implemented")
            }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is GroupRowViewHolder -> {
                (getItem(position) as FIONameItem).let { item ->
                    holder.title.text = item.title + " (${item.mappedAccountCount})"
                    holder.expandIcon.rotation = if (item.isClosed) 180f else 0f
                    holder.itemView.setOnClickListener {
                        fioNameClickListener?.invoke(item.title)
                    }
                    holder.expandIcon.setOnClickListener {
                        item.isClosed = !item.isClosed
                        holder.expandIcon.rotation = if (item.isClosed) 180f else 0f
                        switchGroupVisibilityListener?.invoke(item.title)
                    }
                }
            }
            is AccountViewHolder -> {
                (getItem(position) as AccountItem).let {
                    holder.label.text = it.account.label
                    holder.type.text = it.accountType
                    holder.icon.setImageDrawable(Utils.getDrawableForAccount(it.account, false, holder.icon.resources))
                    holder.balance.coinType = it.account.coinType
                    holder.balance.setValue(it.account.accountBalance.spendable)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).type

    class DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean =
                oldItem.type == newItem.type && oldItem == newItem

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean =
                when (newItem.type) {
                    TYPE_FIO_NAME -> {
                        newItem as FIONameItem
                        oldItem as FIONameItem
                        oldItem.title == newItem.title &&
                                oldItem.isClosed == newItem.isClosed &&
                                oldItem.mappedAccountCount == newItem.mappedAccountCount
                    }
                    TYPE_ACCOUNT -> {
                        newItem as AccountItem
                        oldItem as AccountItem
                        oldItem.accountType == newItem.accountType &&
                                oldItem.account == newItem.account
                    }
                    else -> oldItem == newItem
                }
    }
}