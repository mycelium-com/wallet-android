package com.mycelium.wallet.activity.modern.adapter

import android.content.SharedPreferences
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.adapter.holder.*
import com.mycelium.wallet.activity.news.NewsUtils
import com.mycelium.wallet.external.mediaflow.model.Category
import com.mycelium.wallet.external.mediaflow.model.News


class NewsAdapter(val preferences: SharedPreferences) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var dataMap = mutableMapOf<Category, MutableList<News>>()
    private var category: Category = ALL

    var openClickListener: ((news: News) -> Unit)? = null
    var categoryClickListener: ((category: Category) -> Unit)? = null

    fun setData(data: List<News>) {
        dataMap.clear()
        data.forEach { news ->
            val list = dataMap.getOrElse(news.categories.values.first()) {
                mutableListOf()
            }
            list.add(news)
            dataMap[news.getCategory()] = list
        }
        notifyDataSetChanged()
    }

    fun setCategory(category: Category) {
        this.category = category
        notifyDataSetChanged()
    }

    private lateinit var layoutInflater: LayoutInflater

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        layoutInflater = LayoutInflater.from(recyclerView.context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SPACE -> SpaceViewHolder(layoutInflater.inflate(R.layout.item_mediaflow_space, parent, false))
            TYPE_NEWS_ITEM_ALL -> NewsItemAllHolder(preferences, layoutInflater.inflate(R.layout.item_mediaflow_news_all, parent, false))
            TYPE_NEWS_V2_BIG -> NewsV2BigHolder(layoutInflater.inflate(R.layout.item_mediaflow_news_v2_big, parent, false), preferences)
            TYPE_NEWS_V2 -> NewsV2Holder(layoutInflater.inflate(R.layout.item_mediaflow_news_v2, parent, false), preferences)
            TYPE_NEWS_LOADING -> NewsLoadingHolder(layoutInflater.inflate(R.layout.item_mediaflow_loading, parent, false))
            else -> SpaceViewHolder(layoutInflater.inflate(R.layout.item_mediaflow_space, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when {
            getItemViewType(position) == TYPE_NEWS_V2_BIG -> {
                val bigHolder = holder as NewsV2BigHolder
                val news = dataMap[category]!![position]
                bigHolder.bind(news)
                bigHolder.openClickListener = {
                    openClickListener?.invoke(it)
                }
            }
            getItemViewType(position) == TYPE_NEWS_V2 -> {
                val news = dataMap[category]!![position]
                val newsSmallHolder = holder as NewsV2Holder
                newsSmallHolder.bind(news)
                newsSmallHolder.openClickListener = {
                    openClickListener?.invoke(it)
                }
            }
            getItemViewType(position) == TYPE_NEWS_ITEM_ALL -> {
                val allHolder = holder as NewsItemAllHolder

                val category = dataMap.entries.elementAt(position).key
                val list = dataMap.entries.elementAt(position).value

                allHolder.bigHolder.bind(list[0])
                allHolder.bigHolder.openClickListener = {
                    openClickListener?.invoke(it)
                }

                val news1Holder = holder.news1
                if (list.size > 1) {
                    news1Holder.bind(list[1])
                    news1Holder.itemView.visibility = View.VISIBLE
                    news1Holder.openClickListener = {
                        openClickListener?.invoke(list[1])
                    }
                } else {
                    news1Holder.itemView.visibility = View.GONE
                }
                val news2Holder = holder.news2
                if (list.size > 2) {
                    news2Holder.bind(list.get(2))
                    news2Holder.itemView.visibility = View.VISIBLE
                    news2Holder.openClickListener = {
                        openClickListener?.invoke(list[2])
                    }
                } else {
                    news2Holder.itemView.visibility = View.GONE
                }

                val btnHolder = allHolder.categoryHolder
                btnHolder.text.text = category.name
                if (NewsUtils.getCategoryIcon(category.name) != 0) {
                    btnHolder.icon.setImageResource(NewsUtils.getCategoryIcon(category.name))
                    btnHolder.icon.visibility = View.VISIBLE
                } else {
                    btnHolder.icon.visibility = View.GONE
                }
                btnHolder.itemView.setOnClickListener {
                    categoryClickListener?.invoke(category)
                }
            }
        }
    }


    override fun getItemCount(): Int {
        if (dataMap.isEmpty()) return 2
        return (if (category == ALL) dataMap.size
        else dataMap[category]?.size ?: 0) + 1
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            dataMap.isEmpty() -> TYPE_NEWS_LOADING
            position == itemCount - 1 -> TYPE_SPACE
            category == ALL -> TYPE_NEWS_ITEM_ALL
            position == 0 -> TYPE_NEWS_V2_BIG
            else -> TYPE_NEWS_V2
        }
    }

    companion object {
        const val PREF_FAVORITE = "favorite"

        const val TYPE_SPACE = 0

        const val TYPE_NEWS_V2_BIG = 1
        const val TYPE_NEWS_V2 = 2

        const val TYPE_NEWS_ITEM_ALL = 3

        const val TYPE_NEWS_LOADING = 4

        val ALL = Category("All")
    }
}

fun News.getCategory(): Category = if (this.categories.values.isNotEmpty()) this.categories.values.first() else Category("Uncategorized")


