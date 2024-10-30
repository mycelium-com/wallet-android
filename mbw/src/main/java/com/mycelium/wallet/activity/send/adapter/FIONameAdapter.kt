package com.mycelium.wallet.activity.send.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.FioAddressItemBinding


class FIONameAdapter : ListAdapter<String, RecyclerView.ViewHolder>(DiffCallback()) {

    var clickListener: ((String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                HEADER -> HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.fio_address_item_header, parent, false))
                else -> ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.fio_address_item, parent, false))
            }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                holder.binding.text.text = getItem(position)
                holder.itemView.setOnClickListener {
                    clickListener?.invoke(getItem(holder.adapterPosition))
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
            if (getItem(position) == HEADER_ITEM) HEADER else ITEM

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = FioAddressItemBinding.bind(itemView)
    }
    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem
    }

    companion object {
        const val HEADER_ITEM = "FIO NAMES"
        const val HEADER = 0
        const val ITEM = 1
    }
}