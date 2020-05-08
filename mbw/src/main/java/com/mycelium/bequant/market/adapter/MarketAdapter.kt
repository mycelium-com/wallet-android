package com.mycelium.bequant.market.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.bequant.market.viewmodel.*
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_bequant_market.view.*


class MarketAdapter : ListAdapter<AdapterItem, RecyclerView.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                MARKET_ITEM ->
                    ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_market, parent, false))
                MARKET_TITLE_ITEM ->
                    ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_market_title, parent, false))
                else -> TODO("not implemented")
            }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (item.viewType) {
            MARKET_ITEM -> {
                item as MarketItem
                holder.itemView.currencies.text = item.currencies
                holder.itemView.volume.text = item.volume
                holder.itemView.rate.text = item.price
                holder.itemView.fiatPrice.text = item.fiatPrice
                holder.itemView.change.text = item.change
            }
            MARKET_TITLE_ITEM -> {

            }
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).viewType

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<AdapterItem>() {
        override fun areItemsTheSame(oldItem: AdapterItem, newItem: AdapterItem): Boolean =
                oldItem.viewType == newItem.viewType
                        && oldItem == newItem


        override fun areContentsTheSame(oldItem: AdapterItem, newItem: AdapterItem): Boolean =
                when (oldItem.viewType) {
                    MARKET_ITEM -> {
                        oldItem as MarketItem
                        newItem as MarketItem
                        oldItem.volume == newItem.volume
                                && oldItem.price == newItem.price
                                && oldItem.change == newItem.change
                                && oldItem.currencies == newItem.currencies
                                && oldItem.fiatPrice == newItem.fiatPrice
                    }
                    MARKET_TITLE_ITEM -> {
                        oldItem as MarketTitleItem
                        newItem as MarketTitleItem
                        oldItem.sortBy == newItem.sortBy
                    }
                    else -> newItem == oldItem
                }
    }
}