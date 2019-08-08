package com.mycelium.wallet.activity.modern.adapter

import android.content.SharedPreferences
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.adapter.holder.*
import com.mycelium.wallet.activity.news.NewsUtils
import com.mycelium.wallet.external.mediaflow.model.Category
import com.mycelium.wallet.external.mediaflow.model.News


class NewsAdapter(val preferences: SharedPreferences)
    : ListAdapter<NewsAdapter.Entry, RecyclerView.ViewHolder>(ItemListDiffCallback()) {

    private lateinit var layoutInflater: LayoutInflater
    private val dataMap = mutableMapOf<Category, MutableSet<News>>()
    private var selectedCategory: Category = ALL

    var openClickListener: ((news: News) -> Unit)? = null
    var categoryClickListener: ((category: Category) -> Unit)? = null

    fun setData(data: List<News>) {
        dataMap.clear()
        addData(data)
    }

    fun addData(data: List<News>) {
        data.forEach { news ->
            val set = dataMap.getOrElse(news.categories.values.first()) {
                mutableSetOf()
            }
            set.remove(news) // remove old news from data set
            set.add(news)
            dataMap[news.getCategory()] = set
        }
        updateData()
    }

    private fun updateData() {
        val data = mutableListOf<Entry>()
        when {
            dataMap.isEmpty() -> {
                data.add(Entry(TYPE_NEWS_LOADING, null))
                data.add(Entry(TYPE_NEWS_LOADING, null))
            }
            selectedCategory == ALL -> dataMap.forEach { entry ->
                val sortedList = entry.value.toList().sortedByDescending { it.date }
                data.add(Entry(TYPE_NEWS_CATEGORY, sortedList[0]))
                data.add(Entry(TYPE_NEWS_BIG, sortedList[0]))
                if (sortedList.size > 1) {
                    data.add(Entry(TYPE_NEWS, sortedList[1]))
                }
                if (sortedList.size > 2) {
                    data.add(Entry(TYPE_NEWS, sortedList[2]))
                }
            }
            else -> dataMap[selectedCategory]?.forEachIndexed { index, news ->
                data.add(Entry(if (index == 0) TYPE_NEWS_BIG else TYPE_NEWS, news))
            }
        }
        data.add(Entry(TYPE_SPACE, null))
        submitList(data)
    }

    fun setCategory(category: Category) {
        this.selectedCategory = category
        updateData()
    }

    fun getCategory() = selectedCategory


    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        layoutInflater = LayoutInflater.from(recyclerView.context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SPACE -> SpaceViewHolder(layoutInflater.inflate(R.layout.item_mediaflow_space, parent, false))
            TYPE_NEWS_CATEGORY -> NewsCategoryBtnHolder(layoutInflater.inflate(R.layout.item_mediaflow_news_category_btn, parent, false))
            TYPE_NEWS_BIG -> NewsV2BigHolder(layoutInflater.inflate(R.layout.item_mediaflow_news_v2_big, parent, false), preferences)
            TYPE_NEWS -> NewsV2Holder(layoutInflater.inflate(R.layout.item_mediaflow_news_v2, parent, false), preferences)
            TYPE_NEWS_LOADING -> NewsLoadingHolder(layoutInflater.inflate(R.layout.item_mediaflow_loading, parent, false))
            TYPE_NEWS_ITEM_LOADING -> NewsItemLoadingHolder(layoutInflater.inflate(R.layout.item_mediaflow_item_loading, parent, false))
            else -> SpaceViewHolder(layoutInflater.inflate(R.layout.item_mediaflow_space, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (item.type) {
            TYPE_NEWS_LOADING -> {
                (holder.itemView as LinearLayout).startLayoutAnimation()
            }
            TYPE_NEWS_CATEGORY -> {
                val btnHolder = holder as NewsCategoryBtnHolder
                val category = item.news?.getCategory()!!
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
            TYPE_NEWS_BIG -> {
                val bigHolder = holder as NewsV2BigHolder
                val news = item.news
                bigHolder.bind(news!!)
                bigHolder.openClickListener = {
                    openClickListener?.invoke(it)
                }
            }
            TYPE_NEWS -> {
                val news = item.news
                val newsSmallHolder = holder as NewsV2Holder
                newsSmallHolder.bind(news!!)
                newsSmallHolder.openClickListener = {
                    openClickListener?.invoke(it)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).type

    data class Entry(val type: Int, val news: News?)

    class ItemListDiffCallback : DiffUtil.ItemCallback<Entry>() {
        override fun areItemsTheSame(oldItem: Entry, newItem: Entry): Boolean =
                oldItem.type == newItem.type && oldItem.news != null && newItem.news != null
                        && oldItem.news.id == newItem.news.id

        override fun areContentsTheSame(oldItem: Entry, newItem: Entry): Boolean =
                oldItem.type == newItem.type
                        && oldItem.news?.title == newItem.news?.title
                        && oldItem.news?.content == newItem.news?.content
    }

    companion object {
        const val PREF_FAVORITE = "favorite"

        const val TYPE_SPACE = 0
        const val TYPE_NEWS_LOADING = 1

        const val TYPE_NEWS_CATEGORY = 2
        const val TYPE_NEWS_BIG = 3
        const val TYPE_NEWS = 4

        const val TYPE_NEWS_ITEM_LOADING = 5

        val ALL = Category("All")
    }
}

fun News.getCategory(): Category = if (this.categories.values.isNotEmpty()) this.categories.values.first() else Category("Uncategorized")


