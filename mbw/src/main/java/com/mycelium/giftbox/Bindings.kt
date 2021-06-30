package com.mycelium.giftbox

import android.content.Intent
import android.content.res.Resources
import android.webkit.URLUtil
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.mycelium.giftbox.client.models.Order
import com.mycelium.wallet.R
import java.text.DateFormat
import java.util.*

@BindingAdapter("image")
fun ImageView.loadImage(url: String?) {
    if (!url.isNullOrEmpty()) {
        Glide.with(context).load(url)
                .into(this)
    }
}

fun Date.getDateString(resources: Resources): String =
        DateFormat.getDateInstance(DateFormat.LONG, resources.configuration.locale).format(this)

fun Date.getDateTimeString(resources: Resources): String =
        "${DateFormat.getDateInstance(DateFormat.LONG, resources.configuration.locale).format(this)} at " +
                DateFormat.getTimeInstance(DateFormat.SHORT, resources.configuration.locale).format(this)

fun TextView.setupDescription(description: String, more: Boolean): Unit {
    text = HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_LEGACY)
    if (!more && layout != null) {
        val endIndex = layout.getLineEnd(3) - 3
        if (0 < endIndex && endIndex < description.length - 3) {
            text = HtmlCompat.fromHtml("${description.subSequence(0, endIndex)}...", HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }
}

fun Order.shareText(resources: Resources): String {
    var text = resources.getString(R.string.share_gift_card_text, productName, amount, currencyCode)
    val code = items?.firstOrNull()
    when {
        code?.deliveryUrl?.isNotEmpty() == true -> {
            text += "\nUrl: " + code.deliveryUrl
        }
        URLUtil.isValidUrl(code?.code) -> {
            text += "\nUrl: " + code?.code
        }
        code?.code?.isNotEmpty() == true -> {
            text += "\nCode: " + code.code
        }
    }
    if (code?.pin?.isNotEmpty() == true) {
        text += "\nPin: " + code.pin
    }
    return text
}

fun Fragment.shareGiftcard(order: Order) {
    startActivity(
            Intent.createChooser(
                    Intent(Intent.ACTION_SEND)
                            .putExtra(Intent.EXTRA_SUBJECT, "Gift Card information")
                            .putExtra(Intent.EXTRA_TEXT, order.shareText(resources))
                            .setType("text/plain"), "share gift card"
            )
    )
}