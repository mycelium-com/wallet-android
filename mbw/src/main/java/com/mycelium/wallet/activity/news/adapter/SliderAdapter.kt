package com.mycelium.wallet.activity.news.adapter

import android.support.v4.view.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.media_flow_slider_item.view.*


class SliderAdapter(val data: List<String>) : PagerAdapter() {
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(container.context).inflate(R.layout.media_flow_slider_item, container, false)
        Glide.with(view.image)
                .load(data[position])
                .apply(RequestOptions().centerCrop().error(R.drawable.news_default_image))
                .into(view.image)
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun getCount(): Int {
        return data.size
    }

}