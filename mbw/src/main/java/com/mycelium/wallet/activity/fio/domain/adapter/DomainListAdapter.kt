package com.mycelium.wallet.activity.fio.domain.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.domain.adapter.DomainListAdapter.Companion.TYPE_FIO_DOMAIN
import com.mycelium.wallet.activity.fio.domain.adapter.DomainListAdapter.Companion.TYPE_FIO_NAME
import com.mycelium.wallet.activity.fio.domain.adapter.viewholder.FIONameViewHolder
import com.mycelium.wallet.activity.fio.mapaccount.adapter.Item
import com.mycelium.wallet.activity.fio.mapaccount.adapter.viewholder.GroupRowViewHolder
import com.mycelium.wapi.wallet.fio.FIODomain
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class FIODomainItem(val domain: FIODomain, val nameCount: Int = 0, var isClosed: Boolean = true) : Item(TYPE_FIO_DOMAIN)
class FIONameItem(val title: String, val expireDate: Date) : Item(TYPE_FIO_NAME)


class DomainListAdapter : ListAdapter<Item, RecyclerView.ViewHolder>(DiffCallback()) {
    companion object {
        const val TYPE_FIO_DOMAIN = 0
        const val TYPE_FIO_NAME = 1
    }

    var fioNameClickListener: ((String) -> Unit)? = null
    var fioDomainClickListener: ((FIODomain) -> Unit)? = null
    var switchGroupVisibilityListener: ((String) -> Unit)? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                TYPE_FIO_DOMAIN -> GroupRowViewHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_fio_group_row, parent, false))

                TYPE_FIO_NAME -> FIONameViewHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_fio_name, parent, false))
                else -> TODO("Not implemented")
            }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is GroupRowViewHolder -> {
                (getItem(position) as FIODomainItem).let { item ->
                    holder.title.text = "@${item.domain.domain} (${item.nameCount})"
                    holder.expandIcon.rotation = if (item.isClosed) 180f else 0f
                    holder.itemView.setOnClickListener {
                        fioDomainClickListener?.invoke(item.domain)
                    }
                    holder.expandIcon.setOnClickListener {
                        item.isClosed = !item.isClosed
                        holder.expandIcon.rotation = if (item.isClosed) 180f else 0f
                        switchGroupVisibilityListener?.invoke(item.domain.domain)
                    }
                }
            }
            is FIONameViewHolder -> {
                (getItem(position) as FIONameItem).run {
                    holder.fioName.text = title
                    holder.fioNameExpireDate.text =
                            holder.fioNameExpireDate.resources.getString(R.string.expiration_date) + " " +
                                    SimpleDateFormat.getDateInstance(DateFormat.LONG).format(expireDate)
                    holder.itemView.setOnClickListener {
                        fioNameClickListener?.invoke(title)
                    }
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
                    TYPE_FIO_DOMAIN -> {
                        newItem as FIODomainItem
                        oldItem as FIODomainItem
                        oldItem.domain.domain == newItem.domain.domain &&
                                oldItem.domain.expireDate == newItem.domain.expireDate &&
                                oldItem.isClosed == newItem.isClosed &&
                                oldItem.nameCount == newItem.nameCount
                    }
                    else -> oldItem == newItem
                }
    }
}