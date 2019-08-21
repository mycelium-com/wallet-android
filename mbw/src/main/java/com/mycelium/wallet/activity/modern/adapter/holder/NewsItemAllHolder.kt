package com.mycelium.wallet.activity.modern.adapter.holder

import android.content.SharedPreferences
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_mediaflow_news_all.view.*
import kotlinx.android.synthetic.main.item_mediaflow_news_category_btn.view.*
import kotlinx.android.synthetic.main.item_mediaflow_news_v2_big.view.*


class NewsItemAllHolder(val preferences: SharedPreferences, itemView: View) : RecyclerView.ViewHolder(itemView) {
    val news1 = NewsV2Holder(itemView.news_1, preferences)
    val news2 = NewsV2Holder(itemView.news_2, preferences)
    val bigHolder = NewsV2BigHolder(itemView.bigItem, preferences)
    val categoryHolder = NewsCategoryBtnHolder(itemView.buttonNewsCategory)
}