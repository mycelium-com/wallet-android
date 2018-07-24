package com.mycelium.wallet.activity.news

import android.content.Context
import android.text.format.DateUtils
import com.mycelium.wallet.R
import com.mycelium.wallet.external.mediaflow.model.News


object NewsUtils {
    fun getCategoryBackground(category: String) = when (category) {
        "News" -> R.drawable.background_news_category_news
        "Announcements" -> R.drawable.background_news_category_announcements
        "Must read" -> R.drawable.background_news_category_must_read
        "Jobs & Partnership" -> R.drawable.background_news_category_job
        else -> R.drawable.background_news_category
    }

    const val myceliumAuthor = "myceliumholding"
    const val MEDIA_FLOW_ACTION: String = "media_flow"

    fun getDateAuthorString(context: Context, news: News): String {
        return "${DateUtils.getRelativeTimeSpanString(context, news.date.time)}" +
                if (news.author.name != NewsUtils.myceliumAuthor) {
                    "${context.getString(R.string.bullet)} ${news.author.name}"
                } else ""
    }
}