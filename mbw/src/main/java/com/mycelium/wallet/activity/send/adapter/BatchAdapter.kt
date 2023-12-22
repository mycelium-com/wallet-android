package com.mycelium.wallet.activity.send.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.ItemSendCoinsBatchBinding
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.coins.Value

data class BatchItem(
    val index: Int, val label: String, val address: Address?,
    val crypto: Value?, val fiat: Value?
)

class BatchAdapter : ListAdapter<BatchItem, RecyclerView.ViewHolder>(ItemDiffCallback()) {
    var clipboardListener: ((BatchItem) -> Unit)? = null
    var contactListener: ((BatchItem) -> Unit)? = null
    var qrScanListener: ((BatchItem) -> Unit)? = null
    var amountListener: ((BatchItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_send_coins_batch, parent, false)
        )

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ViewHolder
        val item = getItem(position)
        holder.binding.label.text = item.label
        holder.binding.address.text = item.address?.toString()
        holder.binding.cryptoAmount.text = item.crypto?.toStringWithUnit()
        holder.binding.fiatAmount.text = item.fiat?.toStringWithUnit()

        holder.binding.clipboard.setOnClickListener {
            clipboardListener?.invoke(item)
        }
        holder.binding.contacts.setOnClickListener {
            contactListener?.invoke(item)
        }
        holder.binding.qrCode.setOnClickListener {
            qrScanListener?.invoke(item)
        }
        holder.binding.cryptoAmount.setOnClickListener {
            amountListener?.invoke(item)
        }
        holder.binding.fiatAmount.setOnClickListener {
            amountListener?.invoke(item)
        }
    }

    class ViewHolder(val itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemSendCoinsBatchBinding.bind(itemView)
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<BatchItem>() {
        override fun areItemsTheSame(oldItem: BatchItem, newItem: BatchItem): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: BatchItem, newItem: BatchItem): Boolean =
            oldItem == newItem
    }
}