package com.mycelium.bequant.receive.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.bequant.receive.adapter.AccountAdapter.Companion.ACCOUNT_GROUP_TYPE
import com.mycelium.bequant.receive.adapter.AccountAdapter.Companion.ACCOUNT_TYPE
import com.mycelium.bequant.receive.adapter.AccountAdapter.Companion.TOTAL_TYPE
import com.mycelium.bequant.receive.adapter.holder.AccountGroupViewHolder
import com.mycelium.bequant.receive.adapter.holder.AccountItemViewHolder
import com.mycelium.bequant.receive.adapter.holder.TotalViewHolder
import com.mycelium.view.Denomination
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.exchange.ValueSum
import com.mycelium.wapi.wallet.coins.Value

open class AccountListItem(val viewType: Int)
class AccountGroupItem(val isOpened: Boolean, val label: String, val value: ValueSum) : AccountListItem(ACCOUNT_GROUP_TYPE)
class AccountItem(val label: String, val value: Value) : AccountListItem(ACCOUNT_TYPE)
class TotalItem(val value: ValueSum) : AccountListItem(TOTAL_TYPE)

class AccountAdapter : ListAdapter<AccountListItem, RecyclerView.ViewHolder>(ItemListDiffCallback()) {

    var accountClickListener: (() -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                ACCOUNT_TYPE ->
                    AccountItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_select_account, parent, false))
                ACCOUNT_GROUP_TYPE ->
                    AccountGroupViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_select_account_group, parent, false))
                TOTAL_TYPE ->
                    TotalViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_select_account_total, parent, false))
                else -> {
                    TODO("no type implementation")
                }
            }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (item.viewType) {
            ACCOUNT_TYPE -> {
                val accountHolder = viewHolder as AccountItemViewHolder
                val accountItem = item as AccountItem
                accountHolder.label.text = accountItem.label
                accountHolder.value.text = accountItem.value.toStringWithUnit(Denomination.UNIT)
                accountHolder.itemView.setOnClickListener {
                    accountClickListener?.invoke()
                }
            }
            ACCOUNT_GROUP_TYPE -> {
                val accountGroupHolder = viewHolder as AccountGroupViewHolder
                val accountGroupItem = item as AccountGroupItem
                accountGroupHolder.label.text = accountGroupItem.label
//                accountGroupHolder.value.text = accountGroupItem.value
            }
            TOTAL_TYPE -> {
                val totalHolder = viewHolder as TotalViewHolder
                val totalItem = item as TotalItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
            getItem(position).viewType

    class ItemListDiffCallback : DiffUtil.ItemCallback<AccountListItem>() {
        override fun areItemsTheSame(oldItem: AccountListItem, newItem: AccountListItem): Boolean =
                oldItem.viewType == newItem.viewType &&
                        oldItem == newItem

        override fun areContentsTheSame(oldItem: AccountListItem, newItem: AccountListItem): Boolean =
                when (oldItem.viewType) {
                    ACCOUNT_TYPE -> {
                        equalsValuesBy(oldItem as AccountItem, newItem as AccountItem,
                                { it.label }, { it.value })
                    }
                    ACCOUNT_GROUP_TYPE -> {
                        equalsValuesBy(oldItem as AccountGroupItem, newItem as AccountGroupItem,
                                { it.label }, { it.value }, { it.isOpened })
                    }
                    TOTAL_TYPE -> {
                        equalsValuesBy(oldItem as TotalItem, newItem as TotalItem,
                                { it.value })
                    }
                    else -> oldItem == newItem
                }
    }

    companion object {
        const val ACCOUNT_TYPE = 1
        const val ACCOUNT_GROUP_TYPE = 2
        const val TOTAL_TYPE = 3
    }
}