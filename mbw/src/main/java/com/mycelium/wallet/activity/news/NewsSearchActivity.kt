package com.mycelium.wallet.activity.news

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.news.adapter.NewsSearchAdapter
import com.mycelium.wallet.external.mediaflow.GetNewsTask
import com.mycelium.wallet.external.mediaflow.NewsConstants
import com.mycelium.wallet.external.mediaflow.model.News
import kotlinx.android.synthetic.main.activity_news_search.*


class NewsSearchActivity : AppCompatActivity() {

    private lateinit var adapter: NewsSearchAdapter
    private lateinit var preference: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_search)
        preference = getSharedPreferences(NewsConstants.NEWS_PREF, Context.MODE_PRIVATE)!!
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)
        adapter = NewsSearchAdapter(preference)
        newsList.adapter = adapter
        adapter.openClickListener = {
            val intent = Intent(this, NewsActivity::class.java)
            intent.putExtra(NewsConstants.NEWS, it)
            startActivity(intent)
        }
        startUpdate()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.news_search, menu)
        (menu?.findItem(R.id.action_search)?.actionView as SearchView?)?.let { searchView ->
            searchView.queryHint = getString(R.string.search_topics_and_articles)
            searchView.setOnCloseListener {
                adapter.setSearchData(null)
                false
            }
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(s: String): Boolean {
                    startUpdate(s)
                    return true
                }

                override fun onQueryTextChange(s: String): Boolean {
                    startUpdate(s)
                    return true
                }
            })
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startUpdate(search: String? = null) {
        val taskListener: (List<News>) -> Unit = {
            if (search == null) {
                adapter.setData(it)
            } else {
                adapter.setSearchData(it)
            }
        }

        GetNewsTask(search, listOf(), listener = taskListener)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}