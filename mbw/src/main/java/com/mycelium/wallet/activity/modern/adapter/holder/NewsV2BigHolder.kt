package com.mycelium.wallet.activity.modern.adapter.holder

import android.content.SharedPreferences
import android.text.Html
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.adapter.isFavorite
import com.mycelium.wallet.activity.news.NewsUtils
import com.mycelium.wallet.activity.news.getFitImage
import com.mycelium.wallet.databinding.ItemMediaflowNewsV2BigBinding
import com.mycelium.wallet.external.mediaflow.model.News


class NewsV2BigHolder(itemView: View, val preferences: SharedPreferences) : RecyclerView.ViewHolder(itemView) {
    var openClickListener: ((News) -> Unit)? = null
    val binding = ItemMediaflowNewsV2BigBinding.bind(itemView)
    fun bind(news: News) {
        binding.title.text = Html.fromHtml(news.title.rendered)
        binding.date.text = NewsUtils.getDateString(binding.date.context, news)
        binding.tvAuthor.text = news.author?.name

        binding.favoriteButton.visibility = if (news.isFavorite(preferences)) VISIBLE else GONE

        val requestOptions = RequestOptions()
                .transforms(CenterCrop(), RoundedCorners(binding.ivImage.resources.getDimensionPixelSize(R.dimen.media_flow_round_corner)))
        Glide.with(binding.ivImage)
                .load(news.getFitImage(binding.ivImage.resources.displayMetrics.widthPixels))
                .error(Glide.with(binding.ivImage)
                        .load(R.drawable.mediaflow_default_picture)
                        .apply(requestOptions))
                .apply(requestOptions)
                .into(binding.ivImage)
        itemView.setOnClickListener {
            openClickListener?.invoke(news)
        }
    }
}