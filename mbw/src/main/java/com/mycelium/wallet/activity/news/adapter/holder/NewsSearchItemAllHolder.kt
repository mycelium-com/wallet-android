package com.mycelium.wallet.activity.news.adapter.holder

import android.content.SharedPreferences
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.activity.modern.adapter.holder.NewsV2ListHolder
import kotlinx.android.synthetic.main.item_all_news_search.view.*
import kotlinx.android.synthetic.main.media_flow_tab_item.view.*


class NewsSearchItemAllHolder(val preferences: SharedPreferences, itemView: View) : RecyclerView.ViewHolder(itemView) {

    val category = itemView.category as TextView
    val showAll = itemView.view_more
    val listHolder = NewsV2ListHolder(preferences, itemView.list)
}