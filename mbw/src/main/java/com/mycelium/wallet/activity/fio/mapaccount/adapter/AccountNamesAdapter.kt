package com.mycelium.wallet.activity.fio.mapaccount.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.mapaccount.adapter.viewholder.FIODomainViewHolder
import com.mycelium.wallet.activity.fio.mapaccount.adapter.viewholder.GroupViewHolder
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.ItemFioRegisterBinding
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.fio.FIODomain
import com.mycelium.wapi.wallet.fio.RegisteredFIOName
import java.text.DateFormat
import java.text.SimpleDateFormat


class AccountItem(val account: WalletAccount<*>, var isClosed: Boolean) : Item(AccountNamesAdapter.TYPE_ACCOUNT)
class FIONameItem(val fioName: RegisteredFIOName) : Item(AccountNamesAdapter.TYPE_FIO_NAME)
class FIODomainItem(val domain: FIODomain) : Item(AccountNamesAdapter.TYPE_FIO_DOMAIN)
class RegisterFIONameItem(val account: WalletAccount<*>) : Item(AccountNamesAdapter.TYPE_REGISTER_FIO_NAME)
class RegisterFIODomainItem(val account: WalletAccount<*>) : Item(AccountNamesAdapter.TYPE_REGISTER_FIO_DOMAIN)

class AccountNamesAdapter : ListAdapter<Item, RecyclerView.ViewHolder>(DiffCallback()) {
    companion object {
        const val TYPE_FIO_NAME = 0
        const val TYPE_ACCOUNT = 1
        const val TYPE_FIO_DOMAIN = 2
        const val TYPE_REGISTER_FIO_NAME = 3
        const val TYPE_REGISTER_FIO_DOMAIN = 4
    }

    var fioNameClickListener: ((RegisteredFIOName) -> Unit)? = null
    var domainClickListener: ((FIODomain) -> Unit)? = null
    var switchGroupVisibilityListener: ((String) -> Unit)? = null
    var registerFIONameListener: ((WalletAccount<*>) -> Unit)? = null
    var registerFIODomainListener: ((WalletAccount<*>) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                TYPE_FIO_NAME -> FIONameViewHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_fio_name, parent, false))
                TYPE_FIO_DOMAIN -> FIODomainViewHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_fio_name, parent, false))
                TYPE_ACCOUNT -> GroupViewHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_fio_account_mapping_group, parent, false))
                TYPE_REGISTER_FIO_NAME -> RegisterFIONameViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_fio_register, parent, false)
                ).apply {
                    binding.action.text = binding.action.context.getString(R.string.fio_register_name)
                }
                TYPE_REGISTER_FIO_DOMAIN -> RegisterFIODomainViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_fio_register, parent, false)
                ).apply {
                    binding.action.text =
                        binding.action.context.getString(R.string.fio_register_domain)
                }
                else -> TODO("Not implemented")
            }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is GroupViewHolder -> {
                (getItem(position) as AccountItem).let { item ->
                    holder.title.text = item.account.label
                    holder.expandIcon.rotation = if (item.isClosed) 180f else 0f
                    holder.balance.text = item.account.accountBalance.spendable.toStringWithUnit()
                    holder.itemView.setOnClickListener {
                        item.isClosed = !item.isClosed
                        holder.expandIcon.rotation = if (item.isClosed) 180f else 0f
                        switchGroupVisibilityListener?.invoke(item.account.label)
                    }
                }
            }
            is FIONameViewHolder -> {
                (getItem(position) as FIONameItem).run {
                    holder.fioName.text = this.fioName.name
                    holder.expireDate.text = holder.expireDate.resources.getString(R.string.expiration_date) + " " +
                            SimpleDateFormat.getDateInstance(DateFormat.LONG).format(this.fioName.expireDate)
                    holder.itemView.setOnClickListener {
                        fioNameClickListener?.invoke(this.fioName)
                    }
                }
            }
            is FIODomainViewHolder -> {
                (getItem(position) as FIODomainItem).run {
                    holder.title.text = "@${this.domain.domain}"
                    holder.expireDate.text = holder.expireDate.resources.getString(R.string.expiration_date) + " " +
                            SimpleDateFormat.getDateInstance(DateFormat.LONG).format(this.domain.expireDate)
                    holder.itemView.setOnClickListener {
                        domainClickListener?.invoke(this.domain)
                    }
                }
            }
            is RegisterFIONameViewHolder -> {
                holder.itemView.setOnClickListener {
                    (getItem(position) as RegisterFIONameItem).run {
                        registerFIONameListener?.invoke(this.account)
                    }
                }
            }
            is RegisterFIODomainViewHolder -> {
                holder.itemView.setOnClickListener {
                    (getItem(position) as RegisterFIODomainItem).run {
                        registerFIODomainListener?.invoke(this.account)
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).type

    class RegisterFIONameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemFioRegisterBinding.bind(itemView)
    }

    class RegisterFIODomainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemFioRegisterBinding.bind(itemView)
    }

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
                        oldItem.account == newItem.account &&
                                oldItem.isClosed == newItem.isClosed
                    }
                    TYPE_FIO_DOMAIN -> {
                        newItem as FIODomainItem
                        oldItem as FIODomainItem
                        oldItem.domain.domain == newItem.domain.domain &&
                                oldItem.domain.expireDate == newItem.domain.expireDate
                    }
                    TYPE_REGISTER_FIO_NAME -> {
                        newItem as RegisterFIONameItem
                        oldItem as RegisterFIONameItem
                        newItem.account == oldItem.account
                    }
                    TYPE_REGISTER_FIO_DOMAIN -> {
                        newItem as RegisterFIODomainItem
                        oldItem as RegisterFIODomainItem
                        newItem.account == oldItem.account
                    }
                    else -> oldItem == newItem
                }
    }
}