package com.mycelium.wallet.activity.news.adapter.holder

import android.content.SharedPreferences
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.activity.modern.adapter.holder.NewsV2Holder
import com.mycelium.wallet.databinding.ItemMediaflowNewsV2DoubleBinding


class NewsV2DoubleHolder(itemView: View, val preferences: SharedPreferences) : RecyclerView.ViewHolder(itemView) {
    val binding = ItemMediaflowNewsV2DoubleBinding.bind(itemView)
    val news1 = NewsV2Holder(binding.news1.root, preferences)
    val news2 = NewsV2Holder(binding.news2.root, preferences)
}