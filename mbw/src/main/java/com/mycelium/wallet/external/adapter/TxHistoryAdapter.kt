package com.mycelium.wallet.external.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.databinding.ItemChangelly2HistoryBinding


class TxHistoryAdapter : ListAdapter<String, RecyclerView.ViewHolder>(DiffCallback()) {
    var clickListener: ((String) -> Unit)? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolder(ItemChangelly2HistoryBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val h = holder as ViewHolder
        h.binding.text.text = getItem(position)
        h.binding.root.setOnClickListener {
            clickListener?.invoke(getItem(holder.absoluteAdapterPosition))
        }
    }

    class ViewHolder(val binding: ItemChangelly2HistoryBinding) : RecyclerView.ViewHolder(binding.root)

    class DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem

    }
}