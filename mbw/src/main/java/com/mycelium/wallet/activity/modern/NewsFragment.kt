package com.mycelium.wallet.activity.modern

import android.content.*
import android.content.Context.MODE_PRIVATE
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.adapter.NewsAdapter
import com.mycelium.wallet.activity.news.NewsActivity
import com.mycelium.wallet.activity.news.NewsSearchActivity
import com.mycelium.wallet.activity.news.NewsUtils
import com.mycelium.wallet.external.mediaflow.GetCategoriesTask
import com.mycelium.wallet.external.mediaflow.GetNewsTask
import com.mycelium.wallet.external.mediaflow.NewsConstants
import com.mycelium.wallet.external.mediaflow.model.Category
import com.mycelium.wallet.external.mediaflow.model.News
import kotlinx.android.synthetic.main.fragment_news.*
import kotlinx.android.synthetic.main.media_flow_tab_item.view.*


class NewsFragment : Fragment() {

    private lateinit var adapter: NewsAdapter
    private lateinit var preference: SharedPreferences
    var searchActive = false

    var currentNews: News? = null

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NewsConstants.NEWS_UPDATE_ACTION && !searchActive) {
                startUpdate()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        preference = activity?.getSharedPreferences(NewsConstants.NEWS_PREF, MODE_PRIVATE)!!
        adapter = NewsAdapter(preference)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutManager = LinearLayoutManager(activity!!, LinearLayoutManager.VERTICAL, false)
        newsList.layoutManager = layoutManager
        newsList.adapter = adapter
        newsList.setHasFixedSize(false)
        adapter.openClickListener = {
            val intent = Intent(activity, NewsActivity::class.java)
            intent.putExtra(NewsConstants.NEWS, it)
            startActivity(intent)
        }
        adapter.categoryClickListener = {
            val tab = getTab(it, tabs)
            tab?.select()
        }
        tabs.addOnTabSelectedListener(object : TabLayout.BaseOnTabSelectedListener<TabLayout.Tab> {
            override fun onTabReselected(p0: TabLayout.Tab?) {
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                adapter.setCategory(tab.tag as Category)
            }
        })
        startUpdate()
    }

    override fun onResume() {
        super.onResume()
        startUpdate()
        LocalBroadcastManager.getInstance(activity!!).registerReceiver(updateReceiver, IntentFilter(NewsConstants.NEWS_UPDATE_ACTION))
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(activity!!).unregisterReceiver(updateReceiver)
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        if (currentNews == null) {
            inflater?.inflate(R.menu.news, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.action_search) {
            startActivity(Intent(activity, NewsSearchActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private var loading = false
    private fun startUpdate() {
        if (loading) {
            return
        }
        GetCategoriesTask {
            val list = mutableListOf(Category("All"))
            list.addAll(it)
            list.forEach { category ->
                if (getTab(category, tabs) == null) {
                    val view = layoutInflater.inflate(R.layout.media_flow_tab_item, tabs, false)
                    view.text.text = category.name
                    view.text.setTextColor(NewsUtils.getCategoryTextColor(category.name))
                    val tab = tabs.newTab().setCustomView(view)
                    tab.tag = category
                    tabs.addTab(tab)
                }
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        val taskListener: (List<News>) -> Unit = {
            loading = false
            if (it.isNotEmpty()) {
                adapter.setData(it)
            }
        }
        loading = true
        GetNewsTask(listener = taskListener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun getTab(category: Category, tabLayout: TabLayout): TabLayout.Tab? {
        for (i in 0..tabLayout.tabCount - 1) {
            if (tabLayout.getTabAt(i)?.tag == category) {
                return tabLayout.getTabAt(i)
            }
        }
        return null
    }
}
