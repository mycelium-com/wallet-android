package com.mycelium.wallet.activity.news.adapter.holder

import android.content.SharedPreferences
import android.support.v7.widget.RecyclerView
import android.view.View
import com.mycelium.wallet.activity.modern.adapter.holder.NewsV2ListHolder
import kotlinx.android.synthetic.main.item_all_news_search.view.*


class NewsSearchItemAllHolder(val preferences: SharedPreferences, itemView: View) : RecyclerView.ViewHolder(itemView) {

    val category = itemView.category
    val showAll = itemView.showAll
    val listHolder = NewsV2ListHolder(preferences, itemView.list)
}