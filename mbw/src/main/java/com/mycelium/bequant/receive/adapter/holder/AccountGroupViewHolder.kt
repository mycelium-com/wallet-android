package com.mycelium.bequant.receive.adapter.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.databinding.ItemBequantSelectAccountBinding


class AccountGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = ItemBequantSelectAccountBinding.bind(itemView)
    val label = binding.label
    val value = binding.value
}