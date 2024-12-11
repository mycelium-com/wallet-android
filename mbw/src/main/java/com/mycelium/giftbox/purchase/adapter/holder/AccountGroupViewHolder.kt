package com.mycelium.giftbox.purchase.adapter.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.databinding.ItemGiftboxSelectAccountGroupBinding


class AccountGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = ItemGiftboxSelectAccountGroupBinding.bind(itemView)
    val chevron = binding.chevron
    val label = binding.label
    val count = binding.count
    val value = binding.value
}