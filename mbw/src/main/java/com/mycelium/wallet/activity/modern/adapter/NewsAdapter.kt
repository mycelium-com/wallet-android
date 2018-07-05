package com.mycelium.wallet.activity.modern.adapter

import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.adapter.holder.LinksViewHolder
import com.mycelium.wallet.activity.modern.adapter.holder.NewsViewHolder
import com.mycelium.wallet.external.news.model.News
import kotlinx.android.synthetic.main.item_news.view.*
import java.util.*


class NewsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var data: List<News> = ArrayList()
    private var nativeData: List<News> = ArrayList()
    var shareClickListener: ((news: News) -> Unit)? = null
    var openClickListener: ((news: News) -> Unit)? = null

    fun setData(data: List<News>) {
        this.data = data
        this.nativeData = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_NEWS) {
            return NewsViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false))
        } else /*if (viewType == TYPE_LINKS)*/ {
            return LinksViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_links, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_NEWS) {
            val newsViewHolder = holder as NewsViewHolder
            val news = data[position - 1]
            newsViewHolder.title.text = news.title
            newsViewHolder.itemView.category.text = if (news.categories.values.isNotEmpty()) news.categories.values.elementAt(0).name else null
            newsViewHolder.description.text = Html.fromHtml(news.content)
            newsViewHolder.date.text = "${DateUtils.getRelativeTimeSpanString(news.date.time)} ${newsViewHolder.date.resources.getString(R.string.bullet)} ${news.author.name}"
            newsViewHolder.share.setOnClickListener {
                shareClickListener?.invoke(news)
            }
            newsViewHolder.itemView.setOnClickListener {
                openClickListener?.invoke(news)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            TYPE_LINKS
        } else {
            TYPE_NEWS
        }
    }

    companion object {
        val TYPE_NEWS = 1
        val TYPE_LINKS = 2

        private val images = HashMap<String, Drawable>()
    }
}