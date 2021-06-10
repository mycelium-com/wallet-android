package com.mycelium.giftbox.cards.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.giftbox.client.models.Order
import com.mycelium.giftbox.client.models.Status
import com.mycelium.giftbox.getDateString
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_giftbox_purchaced.view.*


class PurchasedAdapter : ListAdapter<Order, RecyclerView.ViewHolder>(DiffCallback()) {

    var itemClickListener: ((Order) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            CardViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_purchaced, parent, false))


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.title.text = item.productName
        holder.itemView.description.text = item.amount
        holder.itemView.additional.text = when (item.status) {
            Status.pROCESSING -> {
                holder.itemView.additionalLabel.visibility = View.GONE
                holder.itemView.additional.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_history, 0, 0, 0)
                "Processing"
            }
            Status.eRROR -> {
                holder.itemView.additionalLabel.visibility = View.GONE
                holder.itemView.additional.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_history, 0, 0, 0)
                "Failed"
            }
            else -> {
                holder.itemView.additionalLabel.visibility = View.VISIBLE
                holder.itemView.additional.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                item.timestamp?.getDateString(holder.itemView.resources)
            }
        }
        Glide.with(holder.itemView.image)
                .load(item.productImg)
                .into(holder.itemView.image)
        holder.itemView.setOnClickListener {
            itemClickListener?.invoke(getItem(holder.adapterPosition))
        }
    }

    override fun getItemViewType(position: Int): Int =
            when (getItem(position)) {
                LOADING_ITEM -> TYPE_LOADING
                else -> TYPE_CARD
            }

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean =
                oldItem == newItem


        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean =
                equalsValuesBy(oldItem, newItem,
                        { it.productImg }, { it.productName }, { it.amount }, { it.timestamp })
    }

    companion object {
        const val TYPE_CARD = 0
        const val TYPE_LOADING = 1

        val LOADING_ITEM = Order()
    }
}