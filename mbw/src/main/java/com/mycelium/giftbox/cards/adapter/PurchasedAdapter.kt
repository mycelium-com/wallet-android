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
import com.mycelium.giftbox.client.models.Order
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_giftbox_purchaced.view.*
import java.text.DateFormat
import java.util.*


class PurchasedAdapter : ListAdapter<Order, RecyclerView.ViewHolder>(DiffCallback()) {

    var itemClickListener: ((Order) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            CardViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_purchaced, parent, false))


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.title.text = item.productName
        holder.itemView.description.text = item.amount
        holder.itemView.additional.text = getDateString(holder.itemView.resources, item.timestamp!!)
        Glide.with(holder.itemView.image)
                .load(item.productImg)
                .into(holder.itemView.image)
        holder.itemView.setOnClickListener {
            itemClickListener?.invoke(getItem(holder.adapterPosition))
        }
    }

    private fun getDateString(resources: Resources, date: Date): String =
            DateFormat.getDateInstance(DateFormat.LONG, resources.configuration.locale).format(date)

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean =
                oldItem == newItem


        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean =
                equalsValuesBy(oldItem, newItem,
                        { it.productImg }, { it.productName }, { it.amount }, { it.timestamp })
    }
}