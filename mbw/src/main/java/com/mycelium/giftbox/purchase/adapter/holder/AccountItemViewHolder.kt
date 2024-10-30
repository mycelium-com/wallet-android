package com.mycelium.giftbox.purchase.adapter.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.databinding.ItemGiftboxSelectAccountBinding


class AccountItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = ItemGiftboxSelectAccountBinding.bind(itemView)
    val label = binding.label
    val coinType = binding.coinType
    val value2 = binding.value2
    val value = binding.value
}