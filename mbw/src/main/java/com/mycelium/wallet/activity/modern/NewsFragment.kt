package com.mycelium.wallet.activity.modern

import android.content.*
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Context.MODE_PRIVATE
import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.adapter.NewsAdapter
import com.mycelium.wallet.activity.news.NewsActivity
import com.mycelium.wallet.activity.news.adapter.NewsSearchAdapter
import com.mycelium.wallet.activity.news.adapter.PaginationScrollListener
import com.mycelium.wallet.external.mediaflow.GetAllNewsTask
import com.mycelium.wallet.external.mediaflow.GetCategoriesTask
import com.mycelium.wallet.external.mediaflow.GetNewsTask
import com.mycelium.wallet.external.mediaflow.NewsConstants
import com.mycelium.wallet.external.mediaflow.model.Category
import com.mycelium.wallet.external.mediaflow.model.News
import kotlinx.android.synthetic.main.fragment_news.*
import kotlinx.android.synthetic.main.media_flow_tab_item.view.*


class NewsFragment : Fragment() {

    private lateinit var adapter: NewsAdapter
    private lateinit var adapterSearch: NewsSearchAdapter
    private lateinit var preference: SharedPreferences
    var searchActive = false

    var currentNews: News? = null
    private var loading = false
    private var isLastPage = false

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NewsConstants.NEWS_UPDATE_ACTION && !searchActive) {
                loadItems()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        preference = requireActivity().getSharedPreferences(NewsConstants.NEWS_PREF, MODE_PRIVATE)
        adapter = NewsAdapter(preference)
        adapterSearch = NewsSearchAdapter(preference)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        newsList.setHasFixedSize(false)
        newsList.addOnScrollListener(object : PaginationScrollListener(newsList.layoutManager as LinearLayoutManager) {
            override fun loadMoreItems() {
                if (!searchActive) {
                    loadItems(adapter.itemCount)
                }
            }

            override fun isLastPage() = isLastPage

            override fun isLoading() = loading
        })
        val newsClick: (News) -> Unit = {
            val intent = Intent(activity, NewsActivity::class.java)
            intent.putExtra(NewsConstants.NEWS, it)
            startActivity(intent)
        }
        adapter.openClickListener = newsClick
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
                loadItems()
            }
        })
        adapterSearch.openClickListener = newsClick
        search_close.setOnClickListener {
            if (search_input.text.isEmpty()) {
                searchActive = false
                activity?.invalidateOptionsMenu()
                updateUI()
                val inputMethodManager = activity?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(search_input.applicationWindowToken, 0);
            } else {
                search_input.text = null
            }
        }
        search_input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(search: CharSequence?, p1: Int, p2: Int, p3: Int) {
                startUpdateSearch(search_input.text.toString())
            }
        })
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        loadItems()
        LocalBroadcastManager.getInstance(activity!!).registerReceiver(updateReceiver, IntentFilter(NewsConstants.NEWS_UPDATE_ACTION))
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(activity!!).unregisterReceiver(updateReceiver)
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (currentNews == null) {
            inflater.inflate(R.menu.news, menu)
            menu.findItem(R.id.action_favorite)?.let {
                updateFavoriteMenu(it)
            }
        }
        menu.findItem(R.id.action_search)?.isVisible = !searchActive
        menu.findItem(R.id.action_favorite)?.isVisible = !searchActive
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_search) {
            searchActive = true
            activity?.invalidateOptionsMenu()
            updateUI()
            val inputMethodManager = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(search_input, 0)
            return true
        } else if (item.itemId == R.id.action_favorite) {
            preference.edit()
                    .putBoolean(NewsConstants.FAVORITE, preference.getBoolean(NewsConstants.FAVORITE, false).not())
                    .apply()
            updateFavoriteMenu(item)
            loadItems()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateUI() {
        if (searchActive) {
            newsList.adapter = adapterSearch
            tabs.visibility = View.GONE
            discover.visibility = View.VISIBLE
            search.visibility = View.VISIBLE
            startUpdateSearch()
        } else {
            newsList.adapter = adapter
            tabs.visibility = View.VISIBLE
            discover.visibility = View.GONE
            search.visibility = View.GONE
            loadItems()
        }
    }

    private fun updateFavoriteMenu(item: MenuItem) {
        item.icon = resources.getDrawable(if (preference.getBoolean(NewsConstants.FAVORITE, false)) R.drawable.ic_favorite else R.drawable.ic_not_favorite)
    }

    private fun loadItems(offset: Int = 0) {
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
                    val tab = tabs.newTab().setCustomView(view)
                    tab.tag = category
                    tabs.addTab(tab)
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        val taskListener: (List<News>, Long) -> Unit = { pageData, count ->
            loading = false
            val list = if (preference.getBoolean(NewsConstants.FAVORITE, false)) {
                pageData.filter { news -> preference.getBoolean(NewsAdapter.PREF_FAVORITE + news.id, false) }
            } else {
                pageData
            }
            if (offset == 0) {
                adapter.setData(list)
            } else {
                adapter.addData(list)
            }
            isLastPage = offset + PaginationScrollListener.PAGE_SIZE >= count
        }
        loading = true
        if (adapter.getCategory() == NewsAdapter.ALL) {
            GetAllNewsTask(taskListener)
        } else {
            GetNewsTask(null, listOf(adapter.getCategory()), offset, PaginationScrollListener.PAGE_SIZE, taskListener)
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun startUpdateSearch(search: String? = null) {
        val taskListener: (List<News>, Long) -> Unit = { data, count ->
            if (search == null || search.isEmpty()) {
                adapterSearch.setData(data)
                adapterSearch.setSearchData(null)
            } else {
                adapterSearch.setSearchData(data)
            }
        }

        GetNewsTask(search, listOf(), listener = taskListener)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    private fun getTab(category: Category, tabLayout: TabLayout): TabLayout.Tab? {
        for (i in 0 until tabLayout.tabCount) {
            if (tabLayout.getTabAt(i)?.tag == category) {
                return tabLayout.getTabAt(i)
            }
        }
        return null
    }
}
