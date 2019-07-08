package com.mycelium.wallet.activity.news

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.widget.NestedScrollView
import android.support.v7.app.AppCompatActivity
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AbsoluteLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.NewsFragment
import com.mycelium.wallet.activity.modern.adapter.NewsAdapter
import com.mycelium.wallet.activity.news.adapter.SliderAdapter
import com.mycelium.wallet.external.mediaflow.NewsConstants
import com.mycelium.wallet.external.mediaflow.database.NewsDatabase
import com.mycelium.wallet.external.mediaflow.model.News
import kotlinx.android.synthetic.main.activity_news.*
import kotlinx.android.synthetic.main.media_flow_slider.view.*


class NewsActivity : AppCompatActivity() {
    lateinit var news: News
    private lateinit var preference: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)
        collapsing_toolbar.setStatusBarScrimColor(Color.parseColor("#1a1a1a"))
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        app_bar_layout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val scrollDelta = Math.abs(verticalOffset * 1f / appBarLayout.totalScrollRange)
            category.alpha = 1 - scrollDelta
            toolbar_shadow.visibility = if (scrollDelta == 1f) View.VISIBLE else View.GONE
            collapsing_toolbar.title = if (scrollDelta == 1f) news.title else ""
            llRoot.clipChildren = scrollDelta == 1f
            llRoot.clipToPadding = scrollDelta == 1f
        })
        news = intent.getSerializableExtra(NewsConstants.NEWS) as News
        NewsDatabase.markRead(news)
        content.setBackgroundColor(Color.TRANSPARENT)
        preference = getSharedPreferences(NewsConstants.NEWS_PREF, Context.MODE_PRIVATE)!!
        val parsedContent = NewsUtils.parseNews(news.content)
        val contentText = parsedContent.news
                .replace("width=\".*?\"", "width=\"100%\"")
                .replace("width: .*?px", "width: 100%")
                .replace("height=\".*?\"", "")
        content.settings.defaultFontSize = 14

        val html = getString(R.string.media_flow_html_template
                , resources.toWebViewPx(12f).toString()
                , resources.toWebViewPx(24f).toString()
                , resources.toWebViewPx(16f).toString()
                , resources.toWebViewPx(2f).toString()
                , resources.toWebViewPx(8f).toString()
                , contentText
                , resources.toWebViewPx(260f).toString())
        content.loadDataWithBaseURL("https://blog.mycelium.com", html, "text/html", "UTF-8", null)

        content.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                content.settings.javaScriptEnabled = true
                parsedContent.images.entries.forEach { entry ->
                    view?.evaluateJavascript("(function() {return document.getElementById('${entry.key}').offsetTop;})();") {
                        val top = it.toInt()
                        val injectView = LayoutInflater.from(this@NewsActivity).inflate(R.layout.media_flow_slider, view, false)
                        injectView.image_slider.adapter = SliderAdapter(entry.value)
                        injectView.pager_indicator.setupWithViewPager(injectView.image_slider)
                        injectView.pager_indicator.visibility = if (entry.value.size > 1) View.VISIBLE else View.GONE
                        view.addView(injectView, AbsoluteLayout.LayoutParams(AbsoluteLayout.LayoutParams.MATCH_PARENT
                                , resources.getDimensionPixelSize(R.dimen.media_slider_height)
                                , 0, resources.fromWebViewPx(top)))
                    }
                }
                content.settings.javaScriptEnabled = false
                val params = content.layoutParams
                content.measure(0, 0)
                params.height = content.measuredHeight
                content.layoutParams = params
            }

            @TargetApi(Build.VERSION_CODES.N)
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return handleUri(request?.url)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return handleUri(Uri.parse(url))
            }

            private fun handleUri(uri: Uri?): Boolean {
                if (uri != null) {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    return true
                }
                return false
            }
        }

        scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, oldScrollY ->
            val layoutParams = scrollBar.layoutParams
            val scrollHeight = scrollView.getChildAt(0).measuredHeight - scrollView.measuredHeight
            layoutParams.width = scrollView.measuredWidth * scrollY / scrollHeight
            scrollBar.layoutParams = layoutParams
        })

        tvTitle.text = news.title
        tvDate.text = NewsUtils.getDateString(this, news)
        author.text = news.author?.name

        val categoryText = if (news.categories.values.isNotEmpty()) news.categories.values.elementAt(0).name else ""
        category.text = categoryText
        Glide.with(image)
                .load(news.getFitImage(resources.displayMetrics.widthPixels))
                .apply(RequestOptions().centerCrop().error(R.drawable.news_default_image))
                .into(image)
        shareBtn2.setOnClickListener {
            share()
        }
        val fragment = supportFragmentManager.findFragmentById(R.id.otherNews) as NewsFragment
        fragment.currentNews = news
    }

    private fun share() {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_SUBJECT, news.title)
                .putExtra(Intent.EXTRA_TEXT, news.link)
                .setType("text/plain"), getString(R.string.share_news)))
    }

    private fun favorite() {
        preference.edit()
                .putBoolean(NewsAdapter.PREF_FAVORITE + news.id, !preference.getBoolean(NewsAdapter.PREF_FAVORITE + news.id, false))
                .apply()
        invalidateOptionsMenu()
    }

    fun Resources.toWebViewPx(dipValue: Float): Float {
        val metrics = this.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics) / metrics.density
    }

    fun Resources.fromWebViewPx(value: Int): Int {
        val metrics = this.displayMetrics
        return (value * metrics.density).toInt()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_share, menu)
        menu?.findItem(R.id.favorite)?.icon = resources.getDrawable(
                if (preference.getBoolean(NewsAdapter.PREF_FAVORITE + news.id, false)) R.drawable.ic_favorite
                else R.drawable.ic_not_favorite)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.share -> {
                share()
                return true
            }
            R.id.favorite -> {
                favorite()
                return true
            }
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
