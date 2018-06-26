package com.mycelium.wallet.activity.modern

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.ButterKnife
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.adapter.NewsAdapter
import com.mycelium.wallet.external.news.NewsFactory
import com.mycelium.wallet.external.news.model.News
import kotlinx.android.synthetic.main.fragment_news.*
import java.io.IOException


class NewsFragment : Fragment() {

    private var adapter: NewsAdapter? = null
    private var sharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = activity?.getSharedPreferences("news", Context.MODE_PRIVATE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ButterKnife.bind(this, view)

        newsList.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        adapter = NewsAdapter()
        newsList.adapter = adapter
        adapter?.shareClickListener = { news ->
            val s = Intent(Intent.ACTION_SEND)
            s.type = "text/plain"
            s.putExtra(Intent.EXTRA_SUBJECT, news.title)
            s.putExtra(Intent.EXTRA_TEXT, news.link)
            startActivity(Intent.createChooser(s, "Share News"))
        }
        adapter?.openClickListener = {
            val intent = Intent(activity, NewsActivity::class.java)
            intent.putExtra("news", it)
            startActivity(intent)
        }
        val savedNews = sharedPreferences?.getString("news", null)
        if (savedNews != null) {
            try {
                adapter?.setData(NewsFactory.objectMapper.readValue<Any>(savedNews, object : TypeReference<List<News>>() {

                }) as List<News>)
            } catch (e: IOException) {
                Log.e("NewsFragment", "", e)
            }

        }
        object : AsyncTask<Void, Void, List<News>>() {
            override fun doInBackground(vararg voids: Void): List<News> {
                return NewsFactory.getService().posts().posts
            }

            override fun onPostExecute(news: List<News>) {
                super.onPostExecute(news)
                adapter?.setData(news)
                try {
                    val s = NewsFactory.objectMapper.writeValueAsString(news)
                    sharedPreferences?.edit()?.putString("news", s)?.apply()
                } catch (e: JsonProcessingException) {
                    e.printStackTrace()
                }

            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}
