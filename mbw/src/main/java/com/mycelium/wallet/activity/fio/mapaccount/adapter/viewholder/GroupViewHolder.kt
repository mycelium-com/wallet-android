package com.mycelium.wallet.activity.fio.mapaccount.adapter.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.databinding.ItemFioAccountMappingGroupBinding


class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = ItemFioAccountMappingGroupBinding.bind(itemView)
    val title = binding.title
    val expandIcon = binding.expandIcon
    val balance = binding.balance
}