package com.mycelium.wallet.activity.news

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.convertToCategory
import com.mycelium.wallet.activity.news.adapter.MoreNewsAdapter
import com.mycelium.wallet.external.news.GetNewsTask
import com.mycelium.wallet.external.news.NewsConstants
import com.mycelium.wallet.external.news.model.News
import kotlinx.android.synthetic.main.activity_news.*

class NewsActivity : AppCompatActivity() {
    lateinit var news: News
    var adapter: MoreNewsAdapter? = null
    private lateinit var preference: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        news = intent.getSerializableExtra("news") as News
        content.setBackgroundColor(Color.TRANSPARENT);
        preference = getSharedPreferences(NewsConstants.NEWS_PREF, Context.MODE_PRIVATE)!!
        val categories = preference.getStringSet("category_filter", null)?.toTypedArray()?.convertToCategory()
                ?: listOf()

        content.loadDataWithBaseURL("https://blog.mycelium.com"
                , "<html>"
                + "<head><style type=\"text/css\">body{color: #fff; padding: 0px; margin:0px}" +
                " img{display: inline; height: auto; max-width: 100%;}</style></head>"
                + "<body>${news.content}</body></html>"
                , "text/html", "UTF-8", null)
        tvTitle.text = news.title
        tvDate.text = DateUtils.getRelativeTimeSpanString(this, news.date.time)

        val categoryText = if (news.categories.values.isNotEmpty()) news.categories.values.elementAt(0).name else ""
        category.text = categoryText
        category.setBackgroundResource(NewsUtils.getCategoryBackground(categoryText))

        scrollTop.setOnClickListener {
            scrollView.smoothScrollTo(0, 0)
        }

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            if (scrollView.scrollY > scrollView.height && scrollTop.visibility == View.GONE) {
                scrollTop.visibility = View.VISIBLE
            } else if (scrollView.scrollY <= scrollView.height && scrollTop.visibility == View.VISIBLE) {
                scrollTop.visibility = View.GONE
            }
        }

        moreNewsList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        moreNewsList.isNestedScrollingEnabled = false;
        adapter = MoreNewsAdapter()
        moreNewsList.adapter = adapter
        val moreNewsTask = GetNewsTask(null, categories, 3)
        moreNewsTask.listener =
                {
                    val list = it.toMutableList()
                    list.remove(news)
                    if (list.isEmpty()) {
                        moreNewsList.visibility = View.GONE
                        moreDivider.visibility = View.GONE
                        moreText.visibility = View.GONE
                    } else {
                        adapter?.data = list
                    }
                }
        moreNewsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        adapter?.openClickListener =
                {
                    finish()
                    val intent = Intent(this@NewsActivity, NewsActivity::class.java)
                    intent.putExtra(NewsConstants.NEWS, it)
                    startActivity(intent)
                }
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
