package com.mycelium.bequant.market.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_bequant_account.view.*

class AccountItem(val symbol: String, val name: String, val value: String)

class BequantAccountAdapter : ListAdapter<AccountItem, RecyclerView.ViewHolder>(DiffCallback()) {
    var addCoinListener: ((String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): RecyclerView.ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_account, parent, false))

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        viewHolder.itemView.symbol.text = item.symbol
        viewHolder.itemView.name.text = item.name
        viewHolder.itemView.value.text = item.value
        viewHolder.itemView.addButton.setOnClickListener {
            addCoinListener?.invoke(getItem(viewHolder.adapterPosition).symbol)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<AccountItem>() {
        override fun areItemsTheSame(oldItem: AccountItem, newItem: AccountItem): Boolean =
                oldItem == newItem

        override fun areContentsTheSame(oldItem: AccountItem, newItem: AccountItem): Boolean =
                oldItem.name == newItem.name
                        && oldItem.symbol == newItem.symbol
                        && oldItem.value == newItem.value
    }
}