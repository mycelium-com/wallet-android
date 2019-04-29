package com.mycelium.wallet.activity.modern.adapter.holder

import android.content.SharedPreferences
import android.support.v7.widget.RecyclerView
import android.view.View
import kotlinx.android.synthetic.main.item_mediaflow_news_category_btn.view.*
import kotlinx.android.synthetic.main.item_mediaflow_news_v2_big.view.*
import kotlinx.android.synthetic.main.item_mediaflow_news_v2_list.view.*


class NewsItemAllHolder(val preferences: SharedPreferences, itemView: View) : RecyclerView.ViewHolder(itemView) {
    val listHolder = NewsV2ListHolder(preferences, itemView.list)
    val bigHolder = NewsV2BigHolder(itemView.bigItem, preferences)
    val categoryHolder = NewsCategoryBtnHolder(itemView.buttonNewsCategory)
}