package com.mycelium.wallet.activity.export.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mrd.bitlib.crypto.BipSss
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ItemShareBinding

class SharesAdapter :
    ListAdapter<BipSss.Share, SharesAdapter.ShareViewHolder>(ShareDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShareViewHolder {
        val binding = ItemShareBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShareViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShareViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ShareViewHolder(private val binding: ItemShareBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(share: BipSss.Share) {
            binding.tvLabel.text = binding.tvLabel.context.getString(
                R.string.part_of, share.shareNumber, share.threshold
            )
            val shareString = share.toString()
            binding.tvShare.text = shareString
            binding.qrImageView.setQrCode(shareString)
        }
    }

    class ShareDiffCallback : DiffUtil.ItemCallback<BipSss.Share>() {
        override fun areItemsTheSame(oldItem: BipSss.Share, newItem: BipSss.Share): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: BipSss.Share, newItem: BipSss.Share): Boolean =
            equalsValuesBy(oldItem, newItem, { it.shareNumber }, { it.threshold }, { it.shareData })
    }
}