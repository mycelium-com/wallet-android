package com.mycelium.wallet.activity.fio.mapaccount.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.fio.domain.adapter.viewholder.FIONameViewHolder
import com.mycelium.wallet.activity.fio.mapaccount.adapter.viewholder.AccountViewHolder
import com.mycelium.wallet.activity.fio.mapaccount.adapter.viewholder.FIODomainViewHolder
import com.mycelium.wallet.activity.fio.mapaccount.adapter.viewholder.GroupRowViewHolder
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.fio.FIODomain
import com.mycelium.wapi.wallet.fio.RegisteredFIOName
import java.text.DateFormat
import java.text.SimpleDateFormat


class AccountItem(val account: WalletAccount<*>, val accountType: String) : Item(AccountNamesAdapter.TYPE_ACCOUNT)
class FIONameItem(val fioName: RegisteredFIOName) : Item(AccountNamesAdapter.TYPE_FIO_NAME)
class FIODomainItem(val domain: FIODomain) : Item(AccountNamesAdapter.TYPE_FIO_DOMAIN)

class AccountNamesAdapter : ListAdapter<Item, RecyclerView.ViewHolder>(DiffCallback()) {
    companion object {
        const val TYPE_FIO_NAME = 0
        const val TYPE_ACCOUNT = 1
        const val TYPE_FIO_DOMAIN = 2
        const val TYPE_REGISTER_FIO_NAME = 3
        const val TYPE_REGISTER_FIO_DOMAIN = 4
    }

    var fioNameClickListener: ((String) -> Unit)? = null
    var switchGroupVisibilityListener: ((String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                TYPE_FIO_NAME -> FIONameViewHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_fio_name, parent, false))
                TYPE_FIO_DOMAIN -> FIODomainViewHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_fio_name, parent, false))
                TYPE_ACCOUNT -> AccountViewHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_fio_name_details_account, parent, false))
                else -> TODO("Not implemented")
            }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is GroupRowViewHolder -> {
                (getItem(position) as FIONameItem).let { item ->
//                    holder.title.text = item.title + " (${item.mappedAccountCount})"
//                    holder.expandIcon.rotation = if (item.isClosed) 180f else 0f
//                    holder.itemView.setOnClickListener {
//                        fioNameClickListener?.invoke(item.title)
//                    }
//                    holder.expandIcon.setOnClickListener {
//                        item.isClosed = !item.isClosed
//                        holder.expandIcon.rotation = if (item.isClosed) 180f else 0f
//                        switchGroupVisibilityListener?.invoke(item.title)
//                    }
                }
            }
            is FIONameViewHolder -> {
                (getItem(position) as FIONameItem).run {
                    holder.fioName.text = this.fioName.name
                    holder.expireDate.text = holder.expireDate.resources.getString(R.string.expiration_date) + " " +
                            SimpleDateFormat.getDateInstance(DateFormat.LONG).format(this.fioName.expireDate)
                }
            }
            is FIODomainViewHolder -> {
                (getItem(position) as FIODomainItem).run {
                    holder.title.text = this.domain.domain
                    holder.expireDate.text = holder.expireDate.resources.getString(R.string.expiration_date) + " " +
                            SimpleDateFormat.getDateInstance(DateFormat.LONG).format(this.domain.expireDate)
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
                        oldItem.fioName.name == newItem.fioName.name &&
                                oldItem.fioName.expireDate == newItem.fioName.expireDate
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