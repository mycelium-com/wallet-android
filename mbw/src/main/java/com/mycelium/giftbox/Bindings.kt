package com.mycelium.giftbox

import android.content.res.Resources
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
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
    text = androidx.core.text.HtmlCompat.fromHtml(description, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY)
    if (!more && layout != null) {
        val endIndex = layout.getLineEnd(3) - 3
        if (0 < endIndex && endIndex < description.length - 3) {
            text = androidx.core.text.HtmlCompat.fromHtml("${description.subSequence(0, endIndex)}...", androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }
}