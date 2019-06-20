package com.mycelium.wallet.activity.news.adapter

import android.content.SharedPreferences
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.adapter.getCategory
import com.mycelium.wallet.activity.modern.adapter.holder.NewsV2Holder
import com.mycelium.wallet.activity.modern.adapter.holder.SpaceViewHolder
import com.mycelium.wallet.activity.news.NewsUtils
import com.mycelium.wallet.activity.news.adapter.holder.NewsSearchHeaderHolder
import com.mycelium.wallet.activity.news.adapter.holder.NewsSearchItemAllHolder
import com.mycelium.wallet.external.mediaflow.model.Category
import com.mycelium.wallet.external.mediaflow.model.News


class NewsSearchAdapter(val preferences: SharedPreferences) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var dataMap = mutableMapOf<Category, MutableList<News>>()
    var searchDataMap: List<News>? = null
    var openClickListener: ((news: News) -> Unit)? = null

    private lateinit var layoutInflater: LayoutInflater

    fun setData(data: List<News>) {
        dataMap.clear()
        data.forEach { news ->
            val list = dataMap.getOrElse(news.categories.values.elementAt(0)) {
                mutableListOf()
            }
            list.add(news)
            dataMap[news.getCategory()] = list
        }
        notifyDataSetChanged()
    }

    fun setSearchData(data: List<News>?) {
        searchDataMap = data
        notifyDataSetChanged()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        layoutInflater = LayoutInflater.from(recyclerView.context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> NewsSearchHeaderHolder(layoutInflater.inflate(R.layout.item_search_header, parent, false))
            TYPE_NEWS_ITEM_ALL -> NewsSearchItemAllHolder(preferences, layoutInflater.inflate(R.layout.item_all_news_search, parent, false))
            TYPE_NEWS_V2 -> NewsV2Holder(layoutInflater.inflate(R.layout.item_mediaflow_news_v2, parent, false), preferences)
            else -> SpaceViewHolder(layoutInflater.inflate(R.layout.item_mediaflow_space, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_NEWS_ITEM_ALL) {
            val dataCategory = dataMap.entries.elementAt(position - 1).key
            val list = dataMap.entries.elementAt(position - 1).value

            val listHolder = holder as NewsSearchItemAllHolder
            listHolder.category.text = dataCategory.name
            listHolder.category.setTextColor(NewsUtils.getCategoryTextColor(dataCategory.name))
            listHolder.showAll.setOnClickListener {
                searchDataMap = list
                notifyDataSetChanged()
            }
            listHolder.listHolder.adapter.submitList(list.subList(1, list.size))
            listHolder.listHolder.clickListener = {
                openClickListener?.invoke(it)
            }
        } else if (getItemViewType(position) == TYPE_NEWS_V2) {
            val v2Holder = holder as NewsV2Holder
            searchDataMap?.let {
                val news = it[position]
                v2Holder.bind(news)
                v2Holder.openClickListener = {
                    openClickListener?.invoke(it)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return searchDataMap?.size ?: (dataMap.size + 1)
    }

    override fun getItemViewType(position: Int): Int {
        return if (searchDataMap == null) {
            if (position == 0) HEADER else TYPE_NEWS_ITEM_ALL
        } else TYPE_NEWS_V2
    }

    companion object {
        const val HEADER = 1
        const val TYPE_NEWS_V2 = 2
        const val TYPE_NEWS_ITEM_ALL = 3
    }
}