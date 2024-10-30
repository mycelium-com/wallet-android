package com.mycelium.wallet.activity.news.adapter.holder

import android.content.SharedPreferences
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.activity.modern.adapter.holder.NewsV2ListHolder
import com.mycelium.wallet.databinding.ItemAllNewsSearchBinding


class NewsSearchItemAllHolder(val preferences: SharedPreferences, itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    val binding = ItemAllNewsSearchBinding.bind(itemView)
    val category = binding.tvCategory as TextView
    val showAll = binding.viewMore
    val listHolder = NewsV2ListHolder(preferences, binding.list)
}