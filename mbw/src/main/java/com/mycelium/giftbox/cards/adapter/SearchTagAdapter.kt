package com.mycelium.giftbox.cards.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ItemGiftBoxTagBinding


class SearchTagAdapter : ListAdapter<String, RecyclerView.ViewHolder>(DiffCallback()) {

    var clickListener: ((String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_gift_box_tag, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolder
        holder.binding.text.text = getItem(position)
        holder.itemView.setOnClickListener {
            clickListener?.invoke(getItem(holder.adapterPosition))
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemGiftBoxTagBinding.bind(itemView)
    }

    class DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem


        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem
    }

}