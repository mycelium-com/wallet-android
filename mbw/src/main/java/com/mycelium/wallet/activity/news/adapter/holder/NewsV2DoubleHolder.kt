package com.mycelium.wallet.activity.news.adapter.holder

import android.content.SharedPreferences
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.activity.modern.adapter.holder.NewsV2Holder
import kotlinx.android.synthetic.main.item_mediaflow_news_all.view.*


class NewsV2DoubleHolder(itemView: View, val preferences: SharedPreferences) : RecyclerView.ViewHolder(itemView) {
    val news1 = NewsV2Holder(itemView.news_1, preferences)
    val news2 = NewsV2Holder(itemView.news_2, preferences)
}