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
import com.mycelium.giftbox.client.models.ProductInfo
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_giftbox_store.view.*


class StoresAdapter : ListAdapter<ProductInfo, RecyclerView.ViewHolder>(DiffCallback()) {

    var itemClickListener: ((ProductInfo) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            CardViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_store, parent, false))


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        Glide.with(holder.itemView.image)
                .load(item?.cardImageUrl)
                .into(holder.itemView.image)

        holder.itemView.title.text = item.name
        holder.itemView.description.text = item.categories?.joinToString(",")
        holder.itemView.additional.text = "from ${item.minimumValue} to ${item.maximumValue}"

        item.minimumValue.let {
            holder.itemView.discount.text = "-${item.minimumValue}%"
        } ?: kotlin.run {
            holder.itemView.discount.isVisible = false
        }

        holder.itemView.setOnClickListener {
            itemClickListener?.invoke(getItem(holder.adapterPosition))
        }
    }

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<ProductInfo>() {
        override fun areItemsTheSame(oldItem: ProductInfo, newItem: ProductInfo): Boolean =
                oldItem == newItem


        override fun areContentsTheSame(oldItem: ProductInfo, newItem: ProductInfo): Boolean =
                equalsValuesBy(oldItem, newItem,
                        { it.cardImageUrl }, { it.name }, { it.description })
    }
}