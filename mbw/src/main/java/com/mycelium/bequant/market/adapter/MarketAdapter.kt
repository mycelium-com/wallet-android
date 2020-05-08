package com.mycelium.bequant.market.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.bequant.market.viewmodel.*
import com.mycelium.view.Denomination
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wapi.wallet.coins.Value
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
                val quoteCurrency = item.currencies.substring(6)
                holder.itemView.currencies.text = item.currencies
                holder.itemView.volume.text = when {
                    quoteCurrency.equals("BTC", true) -> {
                        "Vol ${Value.valueOf(Utils.getBtcCoinType(), item.volume.toBigDecimal().unscaledValue()).toString(Denomination.UNIT)}"
                    }
                    quoteCurrency.equals("ETH", true) -> {
                        "Vol ${Value.valueOf(Utils.getEthCoinType(), item.volume.toBigDecimal().unscaledValue()).toString(Denomination.UNIT)}"
                    }
                    else -> {
                        "Vol ${item.volume}"
                    }
                }
                holder.itemView.rate.text = if (item.price == null) {
                    "N/A"
                } else when {
                    quoteCurrency.equals("BTC", true) -> {
                        Value.valueOf(Utils.getBtcCoinType(), item.price.toBigDecimal().unscaledValue()).toString(Denomination.UNIT)
                    }
                    quoteCurrency.equals("ETH", true) -> {
                        Value.valueOf(Utils.getEthCoinType(), item.price.toBigDecimal().unscaledValue()).toString(Denomination.UNIT)
                    }
                    else -> {
                        "${item.price}"
                    }
                }
                holder.itemView.fiatPrice.text = if (item.fiatPrice == null) "N/A" else {
                    "$${item.fiatPrice}"
                }
                holder.itemView.change.text = if (item.change == null) {
                    "N/A"
                } else if (item.change < 0) {
                    holder.itemView.change.setBackgroundColor(Color.parseColor("#1ACE3C4E"))
                    holder.itemView.change.setTextColor(Color.parseColor("#CE3C4E"))
                    "%.${2}f".format(item.change)
                } else {
                    holder.itemView.change.setBackgroundColor(Color.parseColor("#1A5EBA89"))
                    holder.itemView.change.setTextColor(Color.parseColor("#5EBA89"))
                    "+%.${2}f".format(item.change)
                }
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