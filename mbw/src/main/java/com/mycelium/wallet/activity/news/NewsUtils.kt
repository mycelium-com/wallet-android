package com.mycelium.wallet.activity.news

import com.mycelium.wallet.R


object NewsUtils {
    fun getCategoryBackground(category: String) = when (category) {
        "News" -> R.drawable.background_news_category_news
        "Announcements" -> R.drawable.background_news_category_announcements
        "Must read" -> R.drawable.background_news_category_must_read
        "Jobs & Partnership" -> R.drawable.background_news_category_job
        else -> R.drawable.background_news_category
    }

    const val myceliumAuthor = "myceliumholding"
}