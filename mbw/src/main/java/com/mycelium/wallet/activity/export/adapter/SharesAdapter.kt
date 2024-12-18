package com.mycelium.wallet.activity.export.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.databinding.ItemShareBinding

class SharesAdapter : ListAdapter<String, SharesAdapter.ShareViewHolder>(ShareDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShareViewHolder {
        val binding = ItemShareBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShareViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShareViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ShareViewHolder(private val binding: ItemShareBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(share: String) {
            binding.tvShare.text = share
            binding.qrImageView.setQrCode(share)
        }
    }

    class ShareDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
            oldItem == newItem
    }
}