package com.mycelium.wallet.activity.modern.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.wallet.activity.modern.adapter.UnspentOutputAdapter.UnspentOutputViewHolder
import com.mycelium.wapi.wallet.OutputViewModel
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.ItemUnspentOutputBinding

class UnspentOutputAdapter : ListAdapter<OutputViewModel, UnspentOutputViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnspentOutputViewHolder =
        UnspentOutputViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_unspent_output, parent, false)
        )

    override fun onBindViewHolder(holder: UnspentOutputViewHolder, position: Int) {
        holder.binding.value.text = getItem(position).value.toStringWithUnit()
        holder.binding.address.address = getItem(position).address
    }

    class UnspentOutputViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemUnspentOutputBinding.bind(view)

        init {
            binding.address.setChopLength(100)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<OutputViewModel>() {
        override fun areItemsTheSame(
            oldItem: OutputViewModel,
            newItem: OutputViewModel
        ): Boolean = oldItem.address == newItem.address

        override fun areContentsTheSame(
            oldItem: OutputViewModel,
            newItem: OutputViewModel
        ): Boolean =
            equalsValuesBy(oldItem, newItem, { it.address }, { it.value })

    }
}