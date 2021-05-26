package com.mycelium.giftbox

import android.content.res.Resources
import android.widget.ImageView
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