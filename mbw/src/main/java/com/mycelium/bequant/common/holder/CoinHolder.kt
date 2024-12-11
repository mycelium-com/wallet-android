package com.mycelium.bequant.common.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.databinding.ItemBequantAccountBinding
import com.mycelium.wallet.databinding.ItemBequantCoinExpandedBinding
import com.mycelium.wallet.databinding.ItemBequantSearchBinding


class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = ItemBequantCoinExpandedBinding.bind(itemView)
}

class ItemAccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = ItemBequantAccountBinding.bind(itemView)
}

class SearchHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = ItemBequantSearchBinding.bind(itemView)
}

class SpaceHolder(itemView: View) : RecyclerView.ViewHolder(itemView)