package com.mycelium.wallet.activity.fio.mapaccount.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.mapaccount.adapter.viewholder.NameViewHolder


class FIONameItem(val title: String) : Item(AccountNamesAdapter.TYPE_FIO_NAME)

class AccountNamesAdapter : ListAdapter<Item, RecyclerView.ViewHolder>(DiffCallback()) {
    companion object {
        const val TYPE_FIO_NAME = 0
        const val TYPE_ACCOUNT = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                TYPE_FIO_NAME -> NameViewHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_fio_account_mapping_name, parent, false))
                else -> TODO("Not implemented")
            }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is NameViewHolder -> {
                (getItem(position) as FIONameItem).let {
                    holder.title.text = it.title
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean =
                oldItem.type == newItem.type && oldItem == newItem

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean =
                when (newItem.type) {
                    TYPE_FIO_NAME -> {
                        newItem as FIONameItem
                        oldItem as FIONameItem
                        oldItem.title == newItem.title
                    }
                    else -> oldItem == newItem
                }
    }
}