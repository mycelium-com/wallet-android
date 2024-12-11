package com.mycelium.bequant.market.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.bequant.BequantConstants
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.bequant.market.viewmodel.*
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ItemBequantMarketBinding


class MarketAdapter(private val callback: (Int, Boolean) -> Unit) : ListAdapter<AdapterItem, RecyclerView.ViewHolder>(DiffCallback()) {
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
                holder as ViewHolder
                val resources = holder.itemView.resources
                holder.binding.run {
                    currencies.text = "${item.from} / ${item.to}"
                    volume.text = "Vol ${item.volume.toBigDecimal().toPlainString()}"
                    rate.text = if (item.price == null) "N/A" else item.price.toBigDecimal().toPlainString()
                    fiatPrice.text = if (item.fiatPrice == null) "N/A" else {
                        "$%.${2}f".format(item.fiatPrice)
                    }
                    change.text = when {
                        item.change == null -> {
                            "N/A"
                        }
                        item.change < 0 -> {
                            change.setBackgroundResource(R.drawable.bg_bequant_change_percent_red)
                            change.setTextColor(resources.getColor(R.color.bequant_red))
                            "%.${2}f%%".format(item.change)
                        }
                        else -> {
                            change.setBackgroundResource(R.drawable.bg_bequant_change_percent_green)
                            change.setTextColor(resources.getColor(R.color.bequant_green))
                            "+%.${2}f%%".format(item.change)
                        }
                    }
                    root.setOnClickListener {
                        LocalBroadcastManager.getInstance(root.context)
                                .sendBroadcast(Intent(BequantConstants.ACTION_EXCHANGE)
                                        .putExtra("from", item.from)
                                        .putExtra("to", item.to))
                    }
                }
            }
            MARKET_TITLE_ITEM -> {
                item as MarketTitleItem
                clearSortingOptions(holder.itemView)
                highlightSelectedSortingOption(item, holder.itemView)

                (holder.itemView as ViewGroup).children.filter { it is TextView && it.tag != null }
                        .forEach { child ->
                            child.setOnClickListener { v ->
                                val pos = v.tag.toString().toInt()
                                // define in what direction we want to sort
                                val calcDirection = !(item.sortDirections[pos] ?: false)
                                callback(pos, calcDirection)
                                item.sortBy = pos
                                item.sortDirections[pos] = calcDirection
                                clearSortingOptions(holder.itemView)
                                highlightSelectedSortingOption(item, holder.itemView)
                            }
                        }
            }
        }
    }

    private fun clearSortingOptions(itemView: View) {
        // default colors
        (itemView as ViewGroup).children.filter { it is TextView }.forEach {
            (it as TextView).setTextColor(itemView.resources.getColor(R.color.bequant_gray_6))
        }
        // hide sorting arrows
        itemView.children.filter { it.tag != null && it.tag.toString().contains("_arrow") }
                .forEach { it.visibility = View.INVISIBLE }
    }

    private fun highlightSelectedSortingOption(item: MarketTitleItem, itemView: View) {
        itemView.findViewWithTag<TextView>("${item.sortBy}")
                .setTextColor(itemView.resources.getColor(R.color.white))
        itemView.findViewWithTag<ImageView>("${item.sortBy}_arrow").apply {
            visibility = View.VISIBLE
            rotation = if (item.sortDirections[item.sortBy]!!) 0.0f else 180.0f
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).viewType

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemBequantMarketBinding.bind(itemView)
    }

    class DiffCallback : DiffUtil.ItemCallback<AdapterItem>() {
        override fun areItemsTheSame(oldItem: AdapterItem, newItem: AdapterItem): Boolean =
                oldItem == newItem


        override fun areContentsTheSame(oldItem: AdapterItem, newItem: AdapterItem): Boolean =
                when (oldItem.viewType) {
                    MARKET_ITEM -> {
                        oldItem as MarketItem
                        newItem as MarketItem
                        equalsValuesBy(oldItem, newItem, {it.volume}, {it.price}, {it.change},
                                {it.from}, {it.to}, {it.fiatPrice})
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