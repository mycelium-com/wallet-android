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
        news = intent.getSerializableExtra("news") as News
        NewsDatabase.read(news)
        content.setBackgroundColor(Color.TRANSPARENT)
        preference = getSharedPreferences(NewsConstants.NEWS_PREF, Context.MODE_PRIVATE)!!
        val categories = preference.getStringSet("category_filter", null)?.toTypedArray()?.convertToCategory()
                ?: listOf()
        val contentText = news.content.replace("width: ", "")
        content.settings.javaScriptEnabled = true
        content.settings.loadsImagesAutomatically = true;
        content.settings.allowUniversalAccessFromFileURLs = true
        content.settings.defaultFontSize = 14;

        val webTextMarginHorizontal = resources.toWebViewPx(16f)
        val webTextMarginVertical = resources.toWebViewPx(24f)
        content.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.e("MyApplication", consoleMessage?.message() + " -- From line "
                        + consoleMessage?.lineNumber() + " of "
                        + consoleMessage?.sourceId());
                return super.onConsoleMessage(consoleMessage)
            }
        }
        content.loadDataWithBaseURL("https://blog.mycelium.com"
                , "<html>"
                + "<head>"
                + "<script src='file:///android_asset/js/jquery.js'></script>"
                + "<style type=\"text/css\">body{color: #fff; padding: 0px; margin:0px}"
                + " img{display: inline; height: auto; max-width: 100%;}"
                + " .wp-caption-text, .wp-caption-dd {"
                + " clear: both; font-size: 75%;  font-weight: 400; font-style: italic;"
                + " text-align: center; color: #e7e7e7; width: 100%; margin: 0; margin-top: ${resources.toWebViewPx(12f)}}"
                + " a {text-decoration: none; color: #e7e7e7; }"
                + " p { margin-top: ${webTextMarginVertical}px; margin-left: ${webTextMarginHorizontal}px; "
                + "  margin-bottom: ${webTextMarginVertical}px; margin-right: ${webTextMarginHorizontal}px;}"
                + " blockquote {margin-left: ${resources.toWebViewPx(16f)}px; font-family:Georgia,'Times New Roman',serif;"
                + " font-style:italic;border:solid #595959;border-width: 0 0 0 ${resources.toWebViewPx(2f)}px; color: #e7e7e7; }" +
                " blockquote p{ padding-top:${resources.toWebViewPx(12f)}px; padding-bottom: ${resources.toWebViewPx(12f)}px;}"
                + "</style></head>"
                + "<body>$contentText"
                + "<link rel='stylesheet' href='file:///android_asset/css/slideshow.css' type='text/css' media='all' />"
                + "<script type='text/javascript'>"
                + " var jetpackSlideshowSettings = {\"spinner\":\"https:\\/\\/s2.wp.com\\/wp-content\\/mu-plugins\\/shortcodes\\/img\\/slideshow-loader.gif\",\"speed\":\"4000\",\"blog_id\":\"147147619\",\"blog_subdomain\":\"blogmyceliumcom\",\"user_id\":\"0\"};"
                + "</script>"
                + "<script type='text/javascript'>"
                + "var jetpackCarouselStrings = {\"widths\":[370,700,1000,1200,1400,2000],\"is_logged_in\":\"\",\"lang\":\"en\",\"ajaxurl\":\"https:\\/\\/blog.mycelium.com\\/wp-admin\\/admin-ajax.php\",\"nonce\":\"755b1c32e6\",\"display_exif\":\"1\",\"display_geo\":\"1\",\"single_image_gallery\":\"1\",\"single_image_gallery_media_file\":\"\",\"background_color\":\"black\",\"comment\":\"Comment\",\"post_comment\":\"Post Comment\",\"write_comment\":\"Write a Comment...\",\"loading_comments\":\"Loading Comments...\",\"download_original\":\"View full size <span class=\\\"photo-size\\\">{0}<span class=\\\"photo-size-times\\\">\\u00d7<\\/span>{1}<\\/span>\",\"no_comment_text\":\"Please be sure to submit some text with your comment.\",\"no_comment_email\":\"Please provide an email address to comment.\",\"no_comment_author\":\"Please provide your name to comment.\",\"comment_post_error\":\"Sorry, but there was an error posting your comment. Please try again later.\",\"comment_approved\":\"Your comment was approved.\",\"comment_unapproved\":\"Your comment is in moderation.\",\"camera\":\"Camera\",\"aperture\":\"Aperture\",\"shutter_speed\":\"Shutter Speed\",\"focal_length\":\"Focal Length\",\"copyright\":\"Copyright\",\"comment_registration\":\"0\",\"require_name_email\":\"1\",\"login_url\":\"https:\\/\\/blogmyceliumcom.wordpress.com\\/wp-login.php?redirect_to=https%3A%2F%2Fblog.mycelium.com%2F2018%2F07%2F19%2Fhow-do-i-create-another-account%2F\",\"blog_id\":\"147147619\",\"meta_data\":[\"camera\",\"aperture\",\"shutter_speed\",\"focal_length\",\"copyright\"],\"local_comments_commenting_as\":\"<fieldset><label for=\\\"email\\\">Email (Required)<\\/label> <input type=\\\"text\\\" name=\\\"email\\\" class=\\\"jp-carousel-comment-form-field jp-carousel-comment-form-text-field\\\" id=\\\"jp-carousel-comment-form-email-field\\\" \\/><\\/fieldset><fieldset><label for=\\\"author\\\">Name (Required)<\\/label> <input type=\\\"text\\\" name=\\\"author\\\" class=\\\"jp-carousel-comment-form-field jp-carousel-comment-form-text-field\\\" id=\\\"jp-carousel-comment-form-author-field\\\" \\/><\\/fieldset><fieldset><label for=\\\"url\\\">Website<\\/label> <input type=\\\"text\\\" name=\\\"url\\\" class=\\\"jp-carousel-comment-form-field jp-carousel-comment-form-text-field\\\" id=\\\"jp-carousel-comment-form-url-field\\\" \\/><\\/fieldset>\",\"reblog\":\"Reblog\",\"reblogged\":\"Reblogged\",\"reblog_add_thoughts\":\"Add your thoughts here... (optional)\",\"reblogging\":\"Reblogging...\",\"post_reblog\":\"Post Reblog\",\"stats_query_args\":\"blog=147147619&v=wpcom&tz=3&user_id=0&subd=blogmyceliumcom\",\"is_public\":\"0\",\"reblog_enabled\":\"\"};"
                + "</script>"
                + "<script src='file:///android_asset/js/slideshow.js'></script>"
                + "</body></html>"
                , "text/html", "UTF-8", null)
        tvTitle.text = news.title
        tvDate.text = NewsUtils.getDateAuthorString(this, news)

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
        Glide.with(image)
                .load(news.image)
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
