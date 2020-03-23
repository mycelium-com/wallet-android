package com.mycelium.bequant.market

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.bequant.market.model.MarketItem
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_bequant_market.view.*


class MarketAdapter : ListAdapter<MarketItem, RecyclerView.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_market, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.currencies.text = item.currencies
        holder.itemView.volume.text = item.volume
        holder.itemView.rate.text = item.price
        holder.itemView.fiatPrice.text = item.fiatPrice
        holder.itemView.change.text = item.change
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<MarketItem>() {
        override fun areItemsTheSame(p0: MarketItem, p1: MarketItem): Boolean = false

        override fun areContentsTheSame(p0: MarketItem, p1: MarketItem): Boolean = false
    }
}