package com.mycelium.wallet.activity.news

import android.content.Context
import android.content.res.Resources
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

fun News.getFitImage(width:Int): String {
    var result = this.image
    val regexp = Regex("fit=([0-9]*?)%2C([0-9]*?)&")
    val matchResult = regexp.find(this.image)
    if (matchResult != null) {
        val serverWidth = matchResult.groupValues[1].toInt()
        val serverHeight = matchResult.groupValues[2].toInt()
        if (serverWidth > width) {
            val height = width * serverHeight / serverWidth
            result = this.image.replace(regexp, "fit=$width%2C$height&")
        }
    }
    return result
}