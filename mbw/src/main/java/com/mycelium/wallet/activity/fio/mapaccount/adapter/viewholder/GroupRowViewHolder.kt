package com.mycelium.wallet.activity.fio.mapaccount.adapter.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.databinding.ItemFioGroupRowBinding


class GroupRowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = ItemFioGroupRowBinding.bind(itemView)
    val title = binding.title
    val expandIcon = binding.expandIcon
}