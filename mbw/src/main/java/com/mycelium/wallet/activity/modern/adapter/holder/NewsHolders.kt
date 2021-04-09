package com.mycelium.wallet.activity.modern.adapter.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_mediaflow_news_category_btn.view.*


class NewsItemLoadingHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class NewsCategoryBtnHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val text = itemView.text
    val icon = itemView.icon
}

class NewsLoadingHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class NewsNoBookmarksHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class NewsTurnOff(itemView: View) : RecyclerView.ViewHolder(itemView)

class CurrencycomBannerHolder(itemView: View) : RecyclerView.ViewHolder(itemView)