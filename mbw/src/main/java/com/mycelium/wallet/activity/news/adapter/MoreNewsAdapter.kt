package com.mycelium.wallet.activity.news.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.news.NewsUtils
import com.mycelium.wallet.activity.news.getFitImage
import com.mycelium.wallet.activity.util.NewsMoreRoundedCorners
import com.mycelium.wallet.external.mediaflow.model.News
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
        val news = data!![position]
        holder.itemView.title.text = news.title
        val category = if (news.categories.values.isNotEmpty()) news.categories.values.elementAt(0).name else ""
        holder.itemView.category.text = category
        holder.itemView.category.setBackgroundResource(NewsUtils.getCategoryBackground(category))
        holder.itemView.date.text = NewsUtils.getDateAuthorString(holder.itemView.context, news)
        val requestOptions = RequestOptions()
                .transforms(CenterCrop(), NewsMoreRoundedCorners(holder.itemView.image.resources.getDimensionPixelSize(R.dimen.media_flow_round_corner)))
        Glide.with(holder.itemView.image)
                .load(news.getFitImage(holder.itemView.resources.getDimensionPixelSize(R.dimen.mediaflow_more_image_size)))
                .error(Glide.with(holder.itemView.image)
                        .load(R.drawable.news_default_image)
                        .apply(requestOptions))
                .apply(requestOptions)
                .into(holder.itemView.image)
        holder.itemView.setOnClickListener {
            openClickListener?.invoke(news)
        }
    }

    override fun getItemCount(): Int {
        return data?.size ?: 0
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

}
