package com.mycelium.wallet.activity.export.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mrd.bitlib.crypto.BipSss
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ItemShareBinding

class SharesAdapter : ListAdapter<BipSss.Share, RecyclerView.ViewHolder>(ShareDiffCallback()) {

    var shareListener: ((BipSss.Share) -> Unit)? = null
    var itemListener: ((BipSss.Share) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            EMPTY_TYPE -> EmptyViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_space, parent, false)
                    .apply {
                        layoutParams.height = resources.getDimensionPixelSize(R.dimen.size_x12)
                    }
            )

            ITEM_TYPE -> ShareViewHolder(
                ItemShareBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )

            else -> TODO("not implemented")
        }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            EMPTY_TYPE -> {}
            ITEM_TYPE -> {
                val item = getItem(position)
                (holder as ShareViewHolder).bind(item)
                holder.binding.shareQr.setOnClickListener {
                    shareListener?.invoke(item)
                }
                holder.binding.tvShare.setOnClickListener {
                    itemListener?.invoke(item)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            EMPTY -> EMPTY_TYPE
            else -> ITEM_TYPE
        }

    class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class ShareViewHolder(val binding: ItemShareBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(share: BipSss.Share) {
            binding.tvLabel.text = binding.tvLabel.context.getString(
                R.string.part_of, share.shareNumber
            )
            val shareString = share.toString()
            binding.tvShare.text = shareString
            binding.qrImageView.setQrCode(shareString)
            binding.shareQr.setOnClickListener {

            }
        }
    }

    class ShareDiffCallback : DiffUtil.ItemCallback<BipSss.Share>() {
        override fun areItemsTheSame(oldItem: BipSss.Share, newItem: BipSss.Share): Boolean =
            oldItem != EMPTY && oldItem == newItem

        override fun areContentsTheSame(oldItem: BipSss.Share, newItem: BipSss.Share): Boolean =
            equalsValuesBy(oldItem, newItem, { it.shareNumber }, { it.threshold }, { it.shareData })
    }

    companion object {
        val EMPTY = BipSss.Share(0, ByteArray(0), 0, 0, 0, ByteArray(0), 0)
        private const val ITEM_TYPE = 0
        private const val EMPTY_TYPE = 1
    }

}