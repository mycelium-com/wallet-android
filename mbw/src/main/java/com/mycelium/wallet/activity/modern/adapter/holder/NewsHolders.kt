package com.mycelium.wallet.activity.modern.adapter.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.databinding.ItemMediaflowBannerBinding
import com.mycelium.wallet.databinding.ItemMediaflowNewsCategoryBtnBinding
import com.mycelium.wallet.databinding.ItemMediaflowTurnOffBinding


class NewsItemLoadingHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class NewsCategoryBtnHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding =  ItemMediaflowNewsCategoryBtnBinding.bind(itemView)
    val text = binding.text
    val icon = binding.icon
}

class NewsLoadingHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class NewsNoBookmarksHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class NewsTurnOff(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = ItemMediaflowTurnOffBinding.bind(itemView)
}

class CurrencycomBannerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = ItemMediaflowBannerBinding.bind(itemView)
}