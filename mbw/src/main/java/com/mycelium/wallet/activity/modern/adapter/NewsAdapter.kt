package com.mycelium.wallet.activity.modern.adapter

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.adapter.holder.LinksViewHolder
import com.mycelium.wallet.activity.modern.adapter.holder.NewsViewHolder
import com.mycelium.wallet.activity.modern.adapter.holder.SpaceViewHolder
import com.mycelium.wallet.activity.news.NewsUtils
import com.mycelium.wallet.activity.news.getFitImage
import com.mycelium.wallet.external.mediaflow.model.News
import kotlinx.android.synthetic.main.item_news.view.*


class NewsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var data = mutableListOf<News>()
    private var nativeData = mutableListOf<News>()
    var shareClickListener: ((news: News) -> Unit)? = null
    var openClickListener: ((news: News) -> Unit)? = null

    var searchMode = false
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    fun setData(data: List<News>) {
        this.data.clear()
        this.data.addAll(data)
        this.nativeData.clear()
        this.nativeData.addAll(data)
        notifyDataSetChanged()
    }

    fun addData(data: List<News>) {
        this.data.addAll(data)
        this.nativeData.addAll(data)
        notifyDataSetChanged()
    }

    private lateinit var layoutInflater: LayoutInflater

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        layoutInflater = LayoutInflater.from(recyclerView.context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_NEWS) {
            return NewsViewHolder(layoutInflater.inflate(R.layout.item_news, parent, false))
        } else if (viewType == TYPE_LINKS) {
            return LinksViewHolder(layoutInflater.inflate(R.layout.item_links, parent, false))
        } else {
            return SpaceViewHolder(layoutInflater.inflate(R.layout.item_mediaflow_space, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_NEWS) {
            val newsViewHolder = holder as NewsViewHolder
            val news = data[if (searchMode) position else position - 1]
            newsViewHolder.title.text = news.title
            val category = if (news.categories.values.isNotEmpty()) news.categories.values.elementAt(0).name else ""
            newsViewHolder.itemView.category.text = category
            newsViewHolder.itemView.category.setBackgroundResource(NewsUtils.getCategoryBackground(category))
            newsViewHolder.description.post {
                newsViewHolder.description.text = Html.fromHtml(news.excerpt)
            }
            newsViewHolder.date.text = NewsUtils.getDateAuthorString(newsViewHolder.date.context, news)
            newsViewHolder.itemView.read_check.visibility = if (news.isRead) View.VISIBLE else View.GONE
            val requestOptions = RequestOptions()
                    .transforms(CenterCrop(), RoundedCorners(newsViewHolder.itemView.image.resources.getDimensionPixelSize(R.dimen.media_flow_round_corner)))
            Glide.with(newsViewHolder.itemView.image)
                    .load(news.getFitImage(newsViewHolder.itemView.image.resources.displayMetrics.widthPixels))
                    .error(Glide.with(newsViewHolder.itemView.image)
                            .load(R.drawable.news_default_image)
                            .apply(requestOptions))
                    .apply(requestOptions)
                    .into(newsViewHolder.itemView.image)
            newsViewHolder.share.setOnClickListener {
                shareClickListener?.invoke(news)
            }
            newsViewHolder.itemView.setOnClickListener {
                openClickListener?.invoke(news)
            }
        } else if (getItemViewType(position) == TYPE_LINKS) {
            val linksViewHolder = holder as LinksViewHolder
            linksViewHolder.telegram.setOnClickListener {
                try {
                    it.context.startActivity(Intent(ACTION_VIEW, Uri.parse("tg://resolve?domain=MyceliumWallet")))
                } catch (e: Exception) {
                    it.context.startActivity(Intent(ACTION_VIEW, Uri.parse("https://t.me/MyceliumWallet")))
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return if (searchMode) {
            data.size
        } else {
            data.size + 1
        } + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0 && !searchMode) {
            TYPE_LINKS
        } else if (position == itemCount - 1) {
            TYPE_SPACE
        } else {
            TYPE_NEWS
        }
    }

    companion object {
        const val TYPE_SPACE = 0
        const val TYPE_NEWS = 1
        const val TYPE_LINKS = 2
    }
}

