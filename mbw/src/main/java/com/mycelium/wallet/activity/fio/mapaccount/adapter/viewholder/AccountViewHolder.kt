package com.mycelium.wallet.activity.fio.mapaccount.adapter.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.databinding.ItemFioNameDetailsAccountBinding


class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = ItemFioNameDetailsAccountBinding.bind(itemView)
    val icon = binding.icon
    val label = binding.title
    val type = binding.subtitle
    val balance = binding.balance
}