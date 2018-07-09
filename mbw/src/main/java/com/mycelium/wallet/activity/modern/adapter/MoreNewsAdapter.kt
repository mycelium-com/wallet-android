package com.mycelium.wallet.activity.modern.adapter

import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mycelium.wallet.R
import com.mycelium.wallet.external.news.model.News
import kotlinx.android.synthetic.main.item_more_news.view.*

class MoreNewsAdapter : RecyclerView.Adapter<MoreNewsAdapter.ViewHolder>() {
    var data: List<News>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var openClickListener: ((news: News) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_more_news, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val news = data?.get(position)!!
        holder.itemView.title?.text = news.title
        holder.itemView.category.text = if (news.categories.values.isNotEmpty()) news.categories.values.elementAt(0).name else null
        holder.itemView.date?.text = "${DateUtils.getRelativeTimeSpanString(news.date.time)} ${holder.itemView.date.resources.getString(R.string.bullet)} ${news.author.name}"
        holder.itemView.setOnClickListener {
            openClickListener?.invoke(news)
        }
    }

    override fun getItemCount(): Int {
        return data?.size ?: 0
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

}
