package com.mycelium.wallet.activity.modern.adapter

import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.adapter.holder.LinksViewHolder
import com.mycelium.wallet.activity.modern.adapter.holder.NewsViewHolder
import com.mycelium.wallet.external.news.model.News
import kotlinx.android.synthetic.main.item_news.view.*
import java.util.*


class NewsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var data: List<News> = ArrayList()
    var shareClickListener: ((news: News) -> Unit)? = null
    var openClickListener: ((news: News) -> Unit)? = null

    fun setData(data: List<News>) {
        this.data = data
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
            newsViewHolder.itemView.category.text = news.categories.values.elementAt(0).name
            newsViewHolder.description.text = Html.fromHtml(news.content, Html.ImageGetter { s ->
                if (!images.containsKey(s)) {
                    Glide.with(newsViewHolder.description.context)
                            .load(s)
                            .into(object : SimpleTarget<Drawable>() {
                                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                    val viewWidth = newsViewHolder.description.width
                                    if (resource.intrinsicWidth < viewWidth) {
                                        resource.setBounds(0, 0, resource.intrinsicWidth, resource.intrinsicHeight)
                                    } else {
                                        resource.setBounds(0, 0, viewWidth, viewWidth * resource.intrinsicHeight / resource.intrinsicWidth)
                                    }
                                    images[s] = resource
                                    notifyItemChanged(holder.adapterPosition)
                                }
                            })
                }
                images[s]
            }, null)
            newsViewHolder.date.text = DateUtils.getRelativeTimeSpanString(news.date.time)
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