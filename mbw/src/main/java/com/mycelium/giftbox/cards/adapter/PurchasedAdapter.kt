package com.mycelium.giftbox.cards.adapter

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.giftbox.client.models.Item
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_giftbox_purchaced.view.*
import java.text.DateFormat
import java.util.*


class PurchasedAdapter : ListAdapter<Item, RecyclerView.ViewHolder>(DiffCallback()) {

    var itemClickListener: ((Item) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            CardViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_purchaced, parent, false))


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.title.text = item.product_name
        holder.itemView.description.text = item.amount
        holder.itemView.additional.text = getDateString(holder.itemView.resources, item.timestamp!!)
        Glide.with(holder.itemView.image)
                .load(item.product_img)
                .into(holder.itemView.image)
        holder.itemView.setOnClickListener {
            itemClickListener?.invoke(getItem(holder.adapterPosition))
        }
    }

    private fun getDateString(resources: Resources, date: Date): String =
            DateFormat.getDateInstance(DateFormat.LONG, resources.configuration.locale).format(date)

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean =
                oldItem == newItem


        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean =
                equalsValuesBy(oldItem, newItem,
                        { it.product_img }, { it.product_name }, { it.amount }, { it.timestamp })
    }
}