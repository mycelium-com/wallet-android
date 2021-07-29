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
import com.mrd.bitlib.model.AddressType
import com.mycelium.giftbox.model.Card
import com.mycelium.wallet.R
import com.squareup.sqldelight.ColumnAdapter
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
                            .putExtra(Intent.EXTRA_SUBJECT, "Gift Card information")
                            .putExtra(Intent.EXTRA_TEXT, card.shareText(resources))
                            .setType("text/plain"), "share gift card"
            )
    )
}

val dateAdapter = object : ColumnAdapter<Date, String> {
    override fun decode(databaseValue: String): Date = DateFormat.getDateTimeInstance().parse(databaseValue)

    override fun encode(value: Date): String = DateFormat.getDateTimeInstance().format(value)
}