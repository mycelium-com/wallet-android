package com.mycelium.giftbox.cards.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.giftbox.client.models.Product
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_giftbox_purchaced.view.*


class PurchasedAdapter : ListAdapter<Product, RecyclerView.ViewHolder>(DiffCallback()) {

    var itemClickListener: ((Product) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            CardViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_purchaced, parent, false))


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.title.text = item.name
        holder.itemView.description.text = "Gift card amount:"
        holder.itemView.additional.text = "Date:"
        holder.itemView.setOnClickListener {
            itemClickListener?.invoke(getItem(holder.adapterPosition))
        }
    }

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean =
                oldItem == newItem


        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean =
                equalsValuesBy(oldItem, newItem,
                        { it.card_image_url }, { it.name }, { it.description } )
    }
}