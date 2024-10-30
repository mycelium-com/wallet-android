package com.mycelium.wallet.activity.fio.mapaccount.adapter.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.databinding.ItemFioAccountMappingSubGroupBinding


class SubGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = ItemFioAccountMappingSubGroupBinding.bind(itemView)
    val title = binding.text
}