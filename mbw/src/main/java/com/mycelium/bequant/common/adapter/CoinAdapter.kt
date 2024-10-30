package com.mycelium.bequant.common.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.bequant.BequantConstants.TYPE_ITEM
import com.mycelium.bequant.BequantConstants.TYPE_SEARCH
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.bequant.common.holder.ItemViewHolder
import com.mycelium.bequant.common.holder.SearchHolder
import com.mycelium.bequant.common.holder.SpaceHolder
import com.mycelium.bequant.common.model.CoinListItem
import com.mycelium.bequant.exchange.CoinAdapter
import com.mycelium.wallet.R
import com.mycelium.wapi.wallet.coins.AssetInfo


class CoinAdapter : ListAdapter<CoinListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    var coinClickListener: ((AssetInfo) -> Unit)? = null
    var searchChangeListener: ((String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                CoinAdapter.TYPE_SEARCH -> {
                    SearchHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_search, parent, false))
                }
                CoinAdapter.TYPE_ITEM -> {
                    ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_coin_expanded, parent, false))
                }
                else -> {
                    SpaceHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_space, parent, false))
                }
            }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (item.type) {
            TYPE_SEARCH -> {
                holder as SearchHolder
                holder.binding.search.doOnTextChanged { text, start, count, after ->
                    searchChangeListener?.invoke(text?.toString() ?: "")
                }
            }
            TYPE_ITEM -> {
                holder as ItemViewHolder
                holder.binding.coinId.text = item.coin?.symbol
                holder.binding.coinFullName.text = item.coin?.name
                holder.itemView.setOnClickListener {
                    coinClickListener?.invoke(getItem(holder.adapterPosition).coin!!)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).type

    class DiffCallback : DiffUtil.ItemCallback<CoinListItem>() {
        override fun areItemsTheSame(oldItem: CoinListItem, newItem: CoinListItem): Boolean =
                equalsValuesBy(oldItem, newItem, { it.type }, { it.coin })

        override fun areContentsTheSame(oldItem: CoinListItem, newItem: CoinListItem): Boolean =
                equalsValuesBy(oldItem, newItem, { it.coin?.symbol })
    }
}