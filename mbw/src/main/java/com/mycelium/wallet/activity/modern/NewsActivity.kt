package com.mycelium.wallet.activity.modern

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import com.mycelium.wallet.R
import com.mycelium.wallet.external.news.model.News
import kotlinx.android.synthetic.main.activity_news.*

class NewsActivity : AppCompatActivity() {
    lateinit var news: News

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)
        setSupportActionBar(toolbar)
        news = intent.getSerializableExtra("news") as News
        content.text = Html.fromHtml(news.content)
        tvTitle.text = news.title
        tvDate.text = DateUtils.getRelativeTimeSpanString(news.date.time)
        category.text = if (news.categories.values.isNotEmpty()) news.categories.values.elementAt(0).name else null
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_share, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.share) {
            val s = Intent(Intent.ACTION_SEND)
            s.type = "text/plain"
            s.putExtra(Intent.EXTRA_SUBJECT, news.title)
            s.putExtra(Intent.EXTRA_TEXT, news.link)
            startActivity(Intent.createChooser(s, "Share News"))
            return true
        } else if (item?.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
