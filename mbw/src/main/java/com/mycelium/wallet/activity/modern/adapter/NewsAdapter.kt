package com.mycelium.wallet.activity.modern.adapter

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.adapter.holder.*
import com.mycelium.wallet.activity.news.NewsUtils
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wallet.external.mediaflow.model.Category
import com.mycelium.wallet.external.mediaflow.model.News
import com.mycelium.wallet.external.partner.model.MediaFlowBannerInList


class NewsAdapter(val preferences: SharedPreferences)
    : ListAdapter<NewsAdapter.Entry, RecyclerView.ViewHolder>(ItemListDiffCallback()) {

    enum class State {
        DEFAULT, LOADING, FAIL
    }

    private lateinit var layoutInflater: LayoutInflater
    private val dataMap = mutableMapOf<Category, MutableSet<News>>()
    private var selectedCategory: Category = ALL

    var openClickListener: ((news: News) -> Unit)? = null
    var categoryClickListener: ((category: Category) -> Unit)? = null
    var turnOffListener: (() -> Unit)? = null
    var state = State.DEFAULT
    var isFavorite = false
    var bannerClickListener: ((MediaFlowBannerInList?) -> Unit)? = null
    var showBanner = true

    fun setData(data: List<News>) {
        dataMap.clear()
        addData(data)
    }

    fun addData(data: List<News>) {
        data.forEach { news ->
            val set = dataMap.getOrElse(news.categories.first()) {
                mutableSetOf()
            }
            set.remove(news) // allow potentially changed news to be updated
            set.add(news)
            dataMap[news.getCategory()] = set
        }
        updateData()
    }

    private fun updateData() {
        val data = mutableListOf<Entry>()
        if (!preferences.getBoolean(PREF_KEEP_MF, false)) {
            data.add(Entry(TYPE_TURN_OFF))
        }
        when {
            dataMap.isEmpty() -> {
                when (state) {
                    State.LOADING -> {
                        data.add(Entry(TYPE_NEWS_LOADING))
                        data.add(Entry(TYPE_NEWS_LOADING))
                    }
                    State.FAIL -> data.add(Entry(TYPE_EMPTY))
                    else -> {
                        data.add(Entry(if (isFavorite) TYPE_NEWS_NO_BOOKMARKS else TYPE_EMPTY))
                    }
                }
            }
            selectedCategory == ALL -> {
                val banners = SettingsPreference.getMediaFlowContent()?.bannersInList?.filter {
                    showBanner && it.isActive() && SettingsPreference.isContentEnabled(it.parentId)
                } ?: listOf()
                NewsUtils.sort(dataMap.keys.toMutableList()).forEachIndexed { index, category ->
                val sortedList = dataMap[category]?.toList()?.sortedByDescending { it.date }
                        ?: listOf()
                    banners.firstOrNull { it.index == index }?.let {
                        data.add(Entry(TYPE_BIG_BANNER, null, it))
                }
                    sortedList.firstOrNull()?.also { primaryNews ->
                        data.add(Entry(TYPE_NEWS_CATEGORY, primaryNews))
                        data.add(Entry(TYPE_NEWS_BIG, primaryNews, null, primaryNews.isFavorite(preferences)))
                }
                    for (i in 1..3) {
                        sortedList.getOrNull(i)?.also { secondaryNews ->
                            data.add(Entry(TYPE_NEWS, secondaryNews, null, secondaryNews.isFavorite(preferences)))
                        }
                    }
                }
            }
            else -> dataMap[selectedCategory]?.forEachIndexed { index, news ->
                data.add(Entry(if (index == 0) TYPE_NEWS_BIG else TYPE_NEWS, news, null, news.isFavorite(preferences)))
            }
        }
        data.add(Entry(TYPE_SPACE, null))
        submitList(data)
    }

    fun setCategory(category: Category) {
        selectedCategory = category
        updateData()
    }

    fun getCategory() = selectedCategory


    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        layoutInflater = LayoutInflater.from(recyclerView.context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
        TYPE_TURN_OFF -> NewsTurnOff(layoutInflater.inflate(R.layout.item_mediaflow_turn_off, parent, false))
        TYPE_SPACE -> SpaceViewHolder(layoutInflater.inflate(R.layout.item_list_space, parent, false))
        TYPE_NEWS_CATEGORY -> NewsCategoryBtnHolder(layoutInflater.inflate(R.layout.item_mediaflow_news_category_btn, parent, false))
        TYPE_NEWS_BIG -> NewsV2BigHolder(layoutInflater.inflate(R.layout.item_mediaflow_news_v2_big, parent, false), preferences)
        TYPE_NEWS -> NewsV2Holder(layoutInflater.inflate(R.layout.item_mediaflow_news_v2, parent, false), preferences)
        TYPE_NEWS_LOADING -> NewsLoadingHolder(layoutInflater.inflate(R.layout.item_mediaflow_loading, parent, false))
        TYPE_NEWS_ITEM_LOADING -> NewsItemLoadingHolder(layoutInflater.inflate(R.layout.item_mediaflow_item_loading, parent, false))
        TYPE_NEWS_NO_BOOKMARKS -> NewsNoBookmarksHolder(layoutInflater.inflate(R.layout.item_mediaflow_no_bookmarks, parent, false))
        TYPE_BIG_BANNER -> CurrencycomBannerHolder(layoutInflater.inflate(R.layout.item_mediaflow_banner, parent, false))
        else -> SpaceViewHolder(layoutInflater.inflate(R.layout.item_list_space, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (item.type) {
            TYPE_TURN_OFF -> {
                holder as NewsTurnOff
                holder.binding.yesButton.setOnClickListener {
                    preferences.edit().putBoolean(PREF_KEEP_MF, true).apply()
                    updateData()
                }
                holder.binding.noButton.setOnClickListener {
                    AlertDialog.Builder(holder.itemView.context, R.style.MyceliumModern_Dialog_BlueButtons)
                            .setTitle(R.string.you_about_turn_off_mf)
                            .setMessage(R.string.you_about_turn_off_mf_text)
                            .setNegativeButton(R.string.turn_off) { _, p ->
                                SettingsPreference.mediaFlowEnabled = false
                                turnOffListener?.invoke()
                            }
                            .setPositiveButton(R.string.button_cancel, null)
                            .create()
                            .show()
                }
            }
            TYPE_NEWS_LOADING -> {
                (holder.itemView as LinearLayout).startLayoutAnimation()
            }
            TYPE_NEWS_CATEGORY -> {
                val btnHolder = holder as NewsCategoryBtnHolder
                val category = item.news?.getCategory()!!
                btnHolder.text.text = category.name
                if (NewsUtils.getCategoryIcon(category.name) != 0) {
                    btnHolder.icon.setImageResource(NewsUtils.getCategoryIcon(category.name))
                    btnHolder.icon.visibility = VISIBLE
                } else {
                    btnHolder.icon.visibility = GONE
                }
                btnHolder.itemView.setOnClickListener {
                    categoryClickListener?.invoke(category)
                }
            }
            TYPE_NEWS_BIG -> {
                val bigHolder = holder as NewsV2BigHolder
                val news = item.news!!
                bigHolder.bind(news)
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
            TYPE_BIG_BANNER -> {
                holder as CurrencycomBannerHolder
                Glide.with(holder.binding.image)
                        .load(item.banner?.imageUrl)
                        .apply(RequestOptions()
                                .placeholder(R.drawable.mediaflow_default_picture)
                                .error(R.drawable.mediaflow_default_picture))
                        .into(holder.binding.image)
                holder.itemView.setOnClickListener {
                    bannerClickListener?.invoke(getItem(holder.adapterPosition).banner)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).type

    data class Entry(val type: Int,
                     val news: News? = null,
                     val banner: MediaFlowBannerInList? = null,
                     val favorite: Boolean = false)

    class ItemListDiffCallback : DiffUtil.ItemCallback<Entry>() {
        override fun areItemsTheSame(oldItem: Entry, newItem: Entry): Boolean =
                oldItem.type == newItem.type
                        && oldItem.news?.id == newItem.news?.id
                        && oldItem.banner?.imageUrl == newItem.banner?.imageUrl

        override fun areContentsTheSame(oldItem: Entry, newItem: Entry): Boolean =
                when (oldItem.type) {
                    TYPE_NEWS, TYPE_NEWS_BIG -> {
                        oldItem.news?.title?.rendered == newItem.news?.title?.rendered
                                && oldItem.news?.content?.rendered == newItem.news?.content?.rendered
                        && oldItem.favorite == newItem.favorite
                                && oldItem.news?.image == newItem.news?.image
                    }
                    TYPE_BIG_BANNER -> {
                        oldItem.banner?.index == newItem.banner?.index
                                && oldItem.banner?.imageUrl == newItem.banner?.imageUrl
                    }
                    else -> oldItem == newItem
                }
    }

    companion object {
        const val PREF_FAVORITE = "favorite"
        const val PREF_KEEP_MF = "keep_media_flow"

        const val TYPE_SPACE = 0
        const val TYPE_NEWS_LOADING = 1

        const val TYPE_NEWS_CATEGORY = 2
        const val TYPE_NEWS_BIG = 3
        const val TYPE_NEWS = 4

        const val TYPE_NEWS_ITEM_LOADING = 5
        const val TYPE_NEWS_NO_BOOKMARKS = 6
        const val TYPE_EMPTY = 7

        const val TYPE_TURN_OFF = 8

        const val TYPE_BIG_BANNER = 9

        val ALL = Category("All")
    }
}

fun News.isFavorite(preferences: SharedPreferences) = preferences.getBoolean(NewsAdapter.PREF_FAVORITE + id, false)

fun News.getCategory(): Category = if (this.categories.isNotEmpty()) this.categories.first() else Category("Uncategorized")


