package com.mycelium.giftbox.cards.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.giftbox.model.Card
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_giftbox_store.view.*


class StoresAdapter : ListAdapter<Card, RecyclerView.ViewHolder>(DiffCallback()) {

    var itemClickListener: ((Card) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            CardViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_store, parent, false))


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.title.text = item.company
        holder.itemView.description.text = item.description
        holder.itemView.discount.text = "-${item.discount}%"
        holder.itemView.setOnClickListener {
            itemClickListener?.invoke(getItem(holder.adapterPosition))
        }
    }

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<Card>() {
        override fun areItemsTheSame(oldItem: Card, newItem: Card): Boolean =
                oldItem.company == newItem.company


        override fun areContentsTheSame(oldItem: Card, newItem: Card): Boolean =
                equalsValuesBy(oldItem, newItem,
                        { it.image }, { it.company }, { it.description }, { it.discount })
    }
}