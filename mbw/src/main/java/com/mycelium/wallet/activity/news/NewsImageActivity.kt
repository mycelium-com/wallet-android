package com.mycelium.wallet.activity.news

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.activity_news_image.*


class NewsImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_image)
        val url = intent.getStringExtra("url")
        supportActionBar?.title = ""
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)
        supportActionBar?.setHomeButtonEnabled(true);
        supportActionBar?.setDisplayHomeAsUpEnabled(true);
        Glide.with(ivImage)
                .load(url)
                .apply(RequestOptions()
                        .placeholder(R.drawable.mediaflow_default_picture)
                        .error(R.drawable.mediaflow_default_picture))
                .into(ivImage)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}