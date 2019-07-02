package com.mycelium.wallet.activity.modern.adapter.holder

import android.content.Context
import android.content.SharedPreferences
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.adapter.NewsAdapter
import com.mycelium.wallet.activity.news.adapter.holder.NewsV2DoubleHolder
import com.mycelium.wallet.external.mediaflow.model.News
import kotlinx.android.synthetic.main.item_mediaflow_news_v2_list.view.*


class NewsV2ListHolder(val preferences: SharedPreferences, itemView: View) : RecyclerView.ViewHolder(itemView) {
    val adapter: NewsSmallAdapter = NewsSmallAdapter(itemView.context, preferences)
    var clickListener: ((News) -> Unit)? = null

    init {
        itemView.list.adapter = adapter
    }

    inner class NewsSmallAdapter(val context: Context, val preferences: SharedPreferences)
        : ListAdapter<News, RecyclerView.ViewHolder>(ItemListDiffCallback(context)) {

        private lateinit var layoutInflater: LayoutInflater

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)
            layoutInflater = LayoutInflater.from(recyclerView.context)
        }

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder {
            val holder = NewsV2DoubleHolder(layoutInflater.inflate(R.layout.item_mediaflow_news_v2_double, parent, false), preferences)
            val params = holder.itemView.layoutParams
            params.width = context.resources.getDimensionPixelSize(R.dimen.item_media_flow_width)
            holder.itemView.layoutParams = params
            return holder
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val newsSmallHolder = holder as NewsV2DoubleHolder
            if (position / 2 < super.getItemCount()) {
                newsSmallHolder.news1.bind(getItem(position / 2))
                newsSmallHolder.news1.openClickListener = {
                    clickListener?.invoke(it)
                }
                newsSmallHolder.news1.itemView.visibility = View.VISIBLE
            } else {
                newsSmallHolder.news1.itemView.visibility = View.GONE
            }
            if (position / 2 + 1 < super.getItemCount()) {
                newsSmallHolder.news2.bind(getItem(position / 2 + 1))
                newsSmallHolder.news2.openClickListener = {
                    clickListener?.invoke(it)
                }
                newsSmallHolder.news2.itemView.visibility = View.VISIBLE
            } else {
                newsSmallHolder.news2.itemView.visibility = View.GONE
            }

        }

        override fun getItemViewType(position: Int): Int {
            return NewsAdapter.TYPE_NEWS_V2
        }

        override fun getItemCount(): Int {
            return super.getItemCount() / 2
        }
    }

    class ItemListDiffCallback(val context: Context) : DiffUtil.ItemCallback<News>() {
        override fun areItemsTheSame(oldItem: News, newItem: News): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: News, newItem: News): Boolean {
            return oldItem == newItem
        }
    }
}