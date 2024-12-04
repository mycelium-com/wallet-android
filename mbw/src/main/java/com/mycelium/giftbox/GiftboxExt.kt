package com.mycelium.giftbox

import android.content.Intent
import android.content.res.Resources
import android.webkit.URLUtil
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import app.cash.sqldelight.ColumnAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.mycelium.giftbox.client.model.MCProductInfo
import com.mycelium.giftbox.model.Card
import com.mycelium.wallet.R
import java.math.BigDecimal
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

@BindingAdapter("image")
fun ImageView.loadImage(url: String?) {
    loadImage(url, null)
}

fun ImageView.loadImage(url: String?, options: RequestOptions?) {
    if (!url.isNullOrEmpty()) {
        val builder = Glide.with(context).load(url)
        options?.let {
            builder.apply(options)
        }
        builder.into(this)
    }
}

fun MCProductInfo.cardValues() =
    if (denominations?.isNotEmpty() == true && (denominations?.size ?: 100) < 6) {
        denominations?.joinToString {
            "${it.stripTrailingZeros().toPlainString()} ${this.currency}"
        }
    } else if (this.denominations?.isNotEmpty() == true && (this.denominations?.size ?: 100) >= 6) {
        "from ${denominations?.first()} ${currency} to ${denominations?.last()} ${currency}"
    } else {
        "from ${minFaceValue.stripTrailingZeros().toPlainString()} ${currency}" +
                if (maxFaceValue != BigDecimal.ZERO) {
                    " to ${maxFaceValue.stripTrailingZeros().toPlainString()} ${currency}"
                } else {
                    ""
                }
    }


fun Date.getDateString(resources: Resources): String =
        DateFormat.getDateInstance(DateFormat.LONG, resources.configuration.locale).format(this)

fun Date.getDateTimeString(resources: Resources): String =
        "${DateFormat.getDateInstance(DateFormat.LONG, resources.configuration.locale).format(this)} at " +
                DateFormat.getTimeInstance(DateFormat.SHORT, resources.configuration.locale).format(this)

fun TextView.setupDescription(description: String, more: Boolean, hasMore: (Boolean) -> Unit) {
    text = HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_LEGACY)
    if (layout != null) {
        hasMore(lineCount > 3)
        if (!more && lineCount > 3) {
            val endIndex = layout.getLineEnd(3) - 3
            if (0 < endIndex && endIndex < description.length - 3) {
                text = HtmlCompat.fromHtml("${description.subSequence(0, endIndex)}...", HtmlCompat.FROM_HTML_MODE_LEGACY)
            }
        }
    } else {
        postDelayed({ setupDescription(description, more, hasMore) }, 100)
    }
}

fun Card.shareText(resources: Resources): String {
    var text = resources.getString(R.string.share_gift_card_text, productName, amount, currencyCode)
    when {
        deliveryUrl.isNotEmpty() -> {
            text += "\nUrl: $deliveryUrl"
        }
        URLUtil.isValidUrl(code) -> {
            text += "\nUrl: $code"
        }
        code.isNotEmpty() -> {
            text += "\nCode: $code"
        }
    }
    if (pin.isNotEmpty()) {
        text += "\nPin: $pin"
    }
    return text
}

fun Fragment.shareGiftcard(card: Card) {
    startActivity(
            Intent.createChooser(
                    Intent(Intent.ACTION_SEND)
                            .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.gift_card_info))
                            .putExtra(Intent.EXTRA_TEXT, card.shareText(resources))
                            .setType("text/plain"), "share gift card"
            )
    )
}

val dateAdapter = object : ColumnAdapter<Date, String> {
    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ")

    override fun decode(databaseValue: String): Date = try {
        date.parse(databaseValue)
    } catch (e: ParseException) {
        DateFormat.getDateTimeInstance().parse(databaseValue)
    }

    override fun encode(value: Date): String = date.format(value)
}