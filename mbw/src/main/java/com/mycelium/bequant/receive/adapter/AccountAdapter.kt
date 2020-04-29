package com.mycelium.bequant.receive.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R

class AccountListItem

class AccountAdapter : ListAdapter<AccountListItem, RecyclerView.ViewHolder>(ItemListDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_select_account, parent, false) )

    override fun onBindViewHolder(p0: RecyclerView.ViewHolder, p1: Int) {
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class ItemListDiffCallback : DiffUtil.ItemCallback<AccountListItem>() {
        override fun areItemsTheSame(oldItem: AccountListItem, newItem: AccountListItem): Boolean =
                oldItem == newItem

        override fun areContentsTheSame(p0: AccountListItem, p1: AccountListItem): Boolean =
                true

    }

}