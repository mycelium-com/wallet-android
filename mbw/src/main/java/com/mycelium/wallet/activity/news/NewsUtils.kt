package com.mycelium.wallet.activity.news

import android.content.Context
import android.text.format.DateUtils
import com.mycelium.wallet.R
import com.mycelium.wallet.external.mediaflow.model.Category
import com.mycelium.wallet.external.mediaflow.model.News


object NewsUtils {

    const val MEDIA_FLOW_ACTION: String = "media_flow"
    const val NEWS_CATEGORY = "News"

    // Priorities will be sorted in ascending order given these values
    private val categoryPriorities = mapOf("All" to 0, NEWS_CATEGORY to 1, "Micro OTC" to 2,
            "Wallet Features" to 3, "Knowledge Center" to 4)

    fun getCategoryIcon(category: String) = when (category) {
        "News" -> R.drawable.ic_earth
        "Micro OTC" -> R.drawable.ic_micro_otc
        "Knowledge Center" -> R.drawable.ic_education
        else -> R.drawable.ic_mediaflow_category_default_icon
    }

    fun sort(categories: MutableList<Category>): List<Category> =
            categories.sortedWith(compareBy<Category, Int?>(nullsLast<Int>(), { categoryPriorities[it.name] }).then(compareBy { it.name }))

    fun getDateString(context: Context, news: News): String {
        return DateUtils.getRelativeTimeSpanString(news.date.time, System.currentTimeMillis(),
                0L, DateUtils.FORMAT_ABBREV_MONTH.or(DateUtils.FORMAT_ABBREV_TIME)).toString()
    }

    data class ParsedData(val news: String, val images: Map<String, List<String>>)

    fun parseNews(data: String): ParsedData {
        val resultImage = mutableMapOf<String, List<String>>()
        var sliderIndex = 1
        val result = Regex("<div class=\"n2-section-smartslider.*?\".*?>.*?alt=\"Slider\" /></div></div>",
                RegexOption.DOT_MATCHES_ALL)
                .replace(data) {
                    val imageList = mutableListOf<String>()
                    it.groups[0]?.let { matchGroup ->
                        val res = Regex("<img .*? src=\"(.*?)\"").findAll(matchGroup.value)
                        res.forEach { matchResult ->
                            matchResult.groups[1]?.let { matchGroup ->
                                var image = matchGroup.value
                                if (!image.startsWith("data:")) {
                                    if (image.startsWith("//")) {
                                        image = "https:$image"
                                    }
                                    imageList.add(image)
                                }
                            }
                        }
                    }
                    val sliderId = "slider_${sliderIndex++}"
                    if (imageList.isNotEmpty()) {
                        resultImage[sliderId] = imageList
                    }
                    "<div class=\"slider\" id=\"$sliderId\"></div>"
                }
        return ParsedData(result, resultImage)
    }
}

fun News.getFitImage(width: Int): String {
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