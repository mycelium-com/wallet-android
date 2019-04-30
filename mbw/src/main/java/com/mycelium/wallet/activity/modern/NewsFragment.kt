package com.mycelium.wallet.activity.modern

import android.content.*
import android.content.Context.MODE_PRIVATE
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.SearchView
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
        newsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            var scrollY = 0
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                scrollY += dy
                if (layoutManager.findLastVisibleItemPosition() > adapter.itemCount - 5 && !adapter.searchMode) {
                    startUpdate(null, adapter.itemCount - 2)
                }
            }
        })
        adapter.openClickListener = {
            val intent = Intent(activity, NewsActivity::class.java)
            intent.putExtra("news", it)
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

//        val searchItem = menu?.findItem(R.id.action_search)
//        val searchView = searchItem?.actionView as SearchView
//        searchView.maxWidth = Integer.MAX_VALUE;
//        searchView.setOnSearchClickListener {
//            searchActive = true
//        }
//        searchView.setOnCloseListener {
//            searchActive = false
//            false
//        }
//        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(s: String): Boolean {
//                startUpdate(s)
//                return true
//            }
//
//            override fun onQueryTextChange(s: String): Boolean {
//                startUpdate(s)
//                return true
//            }
//        })
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.action_search) {
            startActivity(Intent(activity, NewsSearchActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private var loading = false
    private fun startUpdate(search: String? = null, offset: Int = 0) {
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
            adapter.searchMode = search != null && search.isNotEmpty()
            if (it.isNotEmpty()) {
                if (offset == 0) {
                    adapter.setData(it)
                } else {
//                    adapter.addData(it)
                }
            }
        }
        val task = if (search != null) {
            GetNewsTask(search, listOf(), listener = taskListener)
        } else {
            GetNewsTask(search, listOf(), 30, offset, taskListener)
        }

        loading = true
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
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
