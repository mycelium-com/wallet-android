package com.mycelium.giftbox.cards.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.giftbox.client.models.Product
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_giftbox_store.view.*


class StoresAdapter : ListAdapter<Product, RecyclerView.ViewHolder>(DiffCallback()) {

    var itemClickListener: ((Product) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            CardViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_store, parent, false))


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        Glide.with(holder.itemView.image)
                .load(item?.card_image_url)
                .into(holder.itemView.image)

        holder.itemView.title.text = item.name
        holder.itemView.description.text = item.description
        holder.itemView.additional.text = "from ${item.minimum_value} to ${item.maximum_value}"

        item.minimum_value?.let {
            holder.itemView.discount.text = "-${item.minimum_value}%"
        } ?: kotlin.run {
            holder.itemView.discount.isVisible = false
        }

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
                        { it.card_image_url }, { it.name }, { it.description })
    }
}