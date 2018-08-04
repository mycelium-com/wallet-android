package com.mycelium.wallet.activity.news

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.convertToCategory
import com.mycelium.wallet.activity.news.adapter.MoreNewsAdapter
import com.mycelium.wallet.external.mediaflow.GetNewsTask
import com.mycelium.wallet.external.mediaflow.NewsConstants
import com.mycelium.wallet.external.mediaflow.database.NewsDatabase
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

        app_bar_layout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val scrollDelta = Math.abs(verticalOffset * 1f / appBarLayout.totalScrollRange)
            category.alpha = 1 - scrollDelta
            toolbar_shadow.visibility = if (scrollDelta == 1f) View.VISIBLE else View.GONE
        }
        news = intent.getSerializableExtra("news") as News
        NewsDatabase.read(news)
        content.setBackgroundColor(Color.TRANSPARENT)
        preference = getSharedPreferences(NewsConstants.NEWS_PREF, Context.MODE_PRIVATE)!!
        val categories = preference.getStringSet("category_filter", null)?.toTypedArray()?.convertToCategory()
                ?: listOf()
        val contentText = news.content
                .replace("width=\".*?\"", "width=\"100%\"")
                .replace("width: .*?px", "width: 100%")
                .replace("height=\".*?\"", "")
        content.settings.javaScriptEnabled = true
        content.settings.loadsImagesAutomatically = true;
        content.settings.allowUniversalAccessFromFileURLs = true
        content.settings.defaultFontSize = 14;

        val webTextMarginHorizontal = resources.toWebViewPx(16f)
        val webTextMarginVertical = resources.toWebViewPx(24f)

        content.loadDataWithBaseURL("https://blog.mycelium.com"
                , "<html>"
                + "<head>"
                + "<script src='file:///android_asset/js/jquery.js'></script>"
                + "<link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/css/smart-slider-3.css\" media=\"all\">"
                + "<link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/css/smart-slider-3-addition.css\" media=\"all\">"
                + "<style type=\"text/css\">body{color: #fff; padding: 0px; margin:0px; line-height: 1.43;}"
                + " img{height: auto; width: 100%; max-width: 100%;}"
                + " .wp-caption { margin: 0; padding: 0; height: auto; width: 100%; max-width: 100%;}"
                + " .wp-caption-text, .wp-caption-dd {"
                + " clear: both; font-size: 75%;  font-weight: 400; font-style: italic;"
                + " text-align: center; color: #e7e7e7; width: 100%; margin: 0; margin-top: ${resources.toWebViewPx(12f)}}"
                + " a {text-decoration: none; color: #e7e7e7; }"
                + " p, section { margin-top: ${webTextMarginVertical}px; margin-left: ${webTextMarginHorizontal}px; "
                + " margin-bottom: ${webTextMarginVertical}px; margin-right: ${webTextMarginHorizontal}px;}"
                + " blockquote {margin-left: ${resources.toWebViewPx(16f)}px; font-family:Georgia,'Times New Roman',serif;"
                + " font-style:italic;border:solid #595959;border-width: 0 0 0 ${resources.toWebViewPx(2f)}px; color: #e7e7e7; }"
                + " blockquote p{ padding-top:${resources.toWebViewPx(12f)}px; padding-bottom: ${resources.toWebViewPx(12f)}px;}"
                + " li { margin-bottom: ${resources.toWebViewPx(8f)}px;}"
                + "</style>"
                + "<script type=\"text/javascript\">(function(){var N=this;N.N2_=N.N2_||{r:[],d:[]},N.N2R=N.N2R||function(){N.N2_.r.push(arguments)},N.N2D=N.N2D||function(){N.N2_.d.push(arguments)}}).call(window);if(!window.n2jQuery){window.n2jQuery={ready:function(cb){console.error('n2jQuery will be deprecated!');N2R(['\$'],cb)}}}window.nextend={localization:{},ready:function(cb){console.error('nextend.ready will be deprecated!');N2R('documentReady',function(\$){cb.call(window,\$)})}};</script>"
                + "<script type=\"text/javascript\" src=\"file:///android_asset/js/smart-slider-3-n2.js\"></script>"
                + "<script type=\"text/javascript\" src=\"file:///android_asset/js/smart-slider-3-nextend.js\"></script>"
                + "<script type=\"text/javascript\" src=\"file:///android_asset/js/smart-slider-3-fronend.js\"></script>"
                + "<script type=\"text/javascript\" src=\"file:///android_asset/js/smart-slider-3-simple-type.js\"></script>"
                + "<script type=\"text/javascript\" src=\"file:///android_asset/js/smart-slider-3-initialize.js\"></script>"
                + "</head>"
                + "<body>$contentText</body></html>"
                , "text/html", "UTF-8", null)
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
        Glide.with(image)
                .load(news.getFitImage(resources.displayMetrics.widthPixels))
                .apply(RequestOptions().centerCrop().error(R.drawable.news_default_image))
                .into(image)
        moreNewsList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        moreNewsList.isNestedScrollingEnabled = false;
        adapter = MoreNewsAdapter()
        moreNewsList.adapter = adapter
        moreNewsList.setHasFixedSize(false)
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

    private fun Resources.toWebViewPx(dipValue: Float): Float {
        val metrics = this.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics) / metrics.density
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
