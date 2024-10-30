package com.mycelium.bequant.market.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.bequant.BequantConstants.TYPE_ITEM
import com.mycelium.bequant.BequantConstants.TYPE_SEARCH
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.bequant.common.holder.ItemAccountViewHolder
import com.mycelium.bequant.common.holder.SearchHolder
import com.mycelium.bequant.common.holder.SpaceHolder
import com.mycelium.wallet.R

class AccountItem(val type: Int, val symbol: String = "", val name: String = "", val value: String = "")

class BequantAccountAdapter : ListAdapter<AccountItem, RecyclerView.ViewHolder>(DiffCallback()) {
    var addCoinListener: ((String) -> Unit)? = null
    var searchChangeListener: ((String) -> Unit)? = null
    var searchClearListener: (() -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                TYPE_SEARCH -> SearchHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_search, parent, false))
                TYPE_ITEM -> ItemAccountViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_account, parent, false))
                else -> SpaceHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_space, parent, false))
            }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (item.type) {
            TYPE_SEARCH -> (viewHolder as SearchHolder).binding.run {
                search.doOnTextChanged { text, _, _, _ ->
                    searchChangeListener?.invoke(text?.toString() ?: "")
                }
                clear.setOnClickListener {
                    viewHolder.binding.search.text = null
                    searchClearListener?.invoke()
                }
            }
            TYPE_ITEM -> (viewHolder as ItemAccountViewHolder).binding.run {
                symbol.text = item.symbol
                name.text = item.name
                value.text = item.value
                addButton.setOnClickListener {
                    addCoinListener?.invoke(getItem(viewHolder.adapterPosition).symbol)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).type

    class DiffCallback : DiffUtil.ItemCallback<AccountItem>() {
        override fun areItemsTheSame(oldItem: AccountItem, newItem: AccountItem): Boolean =
                equalsValuesBy(oldItem, newItem, { it.name }, { it.symbol }, { it.value })

        override fun areContentsTheSame(oldItem: AccountItem, newItem: AccountItem): Boolean =
                areItemsTheSame(oldItem, newItem)
    }
}