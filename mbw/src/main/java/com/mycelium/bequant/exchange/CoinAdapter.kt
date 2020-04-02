package com.mycelium.bequant.exchange

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.bequant.receive.model.CoinListItem
import com.mycelium.wallet.R


class CoinAdapter : ListAdapter<CoinListItem, RecyclerView.ViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                TYPE_SEARCH -> {
                    SearchHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_search, parent, false))
                }
                TYPE_ITEM -> {
                    ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_coin, parent, false))
                }
                else -> {
                    SpaceHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_space, parent, false))
                }
            }

    override fun onBindViewHolder(parent: RecyclerView.ViewHolder, position: Int) {
    }

    class DiffCallback : DiffUtil.ItemCallback<CoinListItem>() {
        override fun areItemsTheSame(p0: CoinListItem, p1: CoinListItem): Boolean =
                p0.type == p1.type
                        && p0.coin == p1.coin

        override fun areContentsTheSame(p0: CoinListItem, p1: CoinListItem): Boolean =
                p0.coin?.symbol == p1.coin?.symbol
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class SearchHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class SpaceHolder(itemView: View) : RecyclerView.ViewHolder(itemView)


    companion object {
        const val TYPE_SEARCH = 0
        const val TYPE_SPACE = 1
        const val TYPE_ITEM = 2
    }
}