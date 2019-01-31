package com.mycelium.wallet.activity.news

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.news.adapter.MoreNewsAdapter
import com.mycelium.wallet.activity.news.adapter.SliderAdapter
import com.mycelium.wallet.external.mediaflow.GetNewsTask
import com.mycelium.wallet.external.mediaflow.NewsConstants
import com.mycelium.wallet.external.mediaflow.database.NewsDatabase
import com.mycelium.wallet.external.mediaflow.model.Category
import com.mycelium.wallet.external.mediaflow.model.News
import kotlinx.android.synthetic.main.activity_news.*


class NewsActivity : AppCompatActivity() {
    lateinit var news: News
    var adapter: MoreNewsAdapter? = null
    private lateinit var preference: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)
        collapsing_toolbar.title = ""
        collapsing_toolbar.setStatusBarScrimColor(Color.parseColor("#1a1a1a"))
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        app_bar_layout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val scrollDelta = Math.abs(verticalOffset * 1f / appBarLayout.totalScrollRange)
            category.alpha = 1 - scrollDelta
            toolbar_shadow.visibility = if (scrollDelta == 1f) View.VISIBLE else View.GONE
        })
        news = intent.getSerializableExtra(NewsConstants.NEWS) as News
        NewsDatabase.read(news)
        content.setBackgroundColor(Color.TRANSPARENT)
        preference = getSharedPreferences(NewsConstants.NEWS_PREF, Context.MODE_PRIVATE)!!
        val categories = preference.getStringSet(NewsConstants.CATEGORY_FILTER, null)?.map { Category(it) }
                ?: listOf()
        val parsedContent = NewsUtils.parseNews(news.content)
        val contentText = parsedContent.news
                .replace("width=\".*?\"", "width=\"100%\"")
                .replace("width: .*?px", "width: 100%")
                .replace("height=\".*?\"", "")
        content.settings.defaultFontSize = 14;

        val webTextMarginHorizontal = resources.toWebViewPx(16f)
        val webTextMarginVertical = resources.toWebViewPx(24f)

        val html = getString(R.string.media_flow_html_template
                , resources.toWebViewPx(12f).toString()
                , webTextMarginVertical.toString()
                , webTextMarginHorizontal.toString()
                , resources.toWebViewPx(16f).toString()
                , resources.toWebViewPx(2f).toString()
                , resources.toWebViewPx(8f).toString()
                , contentText)
        content.loadDataWithBaseURL("https://blog.mycelium.com", html, "text/html", "UTF-8", null)
        tvTitle.text = news.title
        tvDate.text = NewsUtils.getDateAuthorString(this, news)

        val categoryText = if (news.categories.values.isNotEmpty()) news.categories.values.elementAt(0).name else ""
        category.text = categoryText
        category.setBackgroundResource(NewsUtils.getCategoryBackground(categoryText))

        scrollTop.setOnClickListener {
            scrollView.fullScroll(View.FOCUS_UP)
        }

        scrollView.viewTreeObserver.addOnScrollChangedListener {
            if (scrollView.scrollY > scrollView.height && scrollTop.visibility == View.GONE) {
                scrollTop.visibility = View.VISIBLE
            } else if (scrollView.scrollY <= scrollView.height && scrollTop.visibility == View.VISIBLE) {
                scrollTop.visibility = View.GONE
            }
        }
        val images = mutableListOf<String>()
        images.add(news.getFitImage(resources.displayMetrics.widthPixels))
        images.addAll(parsedContent.images)
        val imageAdapter = SliderAdapter(images)
        image_slider.adapter = imageAdapter
        pager_indicator.setupWithViewPager(image_slider)
        pager_indicator.visibility = if (images.size > 1) View.VISIBLE else View.GONE
        moreNewsList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        moreNewsList.isNestedScrollingEnabled = false;
        adapter = MoreNewsAdapter()
        moreNewsList.adapter = adapter
        moreNewsList.setHasFixedSize(false)
        val moreNewsTask = GetNewsTask(null, categories, 3) {
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
        adapter?.openClickListener = {
            finish()
            val intent = Intent(this@NewsActivity, NewsActivity::class.java)
            intent.putExtra(NewsConstants.NEWS, it)
            startActivity(intent)
        }
    }

    private fun Resources.toWebViewPx(dipValue: Float): Float {
        val metrics = this.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics) / metrics.density
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_share, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when {
            item?.itemId == R.id.share -> {
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND)
                        .putExtra(Intent.EXTRA_SUBJECT, news.title)
                        .putExtra(Intent.EXTRA_TEXT, news.link)
                        .setType("text/plain"), getString(R.string.share_news)))
                return true
            }
            item?.itemId == android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
