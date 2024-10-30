package com.mycelium.wallet.activity.modern

import android.content.*
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Context.MODE_PRIVATE
import android.os.AsyncTask
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.adapter.NewsAdapter
import com.mycelium.wallet.activity.modern.adapter.isFavorite
import com.mycelium.wallet.activity.news.NewsActivity
import com.mycelium.wallet.activity.news.NewsUtils
import com.mycelium.wallet.activity.news.adapter.NewsSearchAdapter
import com.mycelium.wallet.activity.news.adapter.PaginationScrollListener
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wallet.databinding.FragmentNewsBinding
import com.mycelium.wallet.databinding.MediaFlowTabItemBinding
import com.mycelium.wallet.event.PageSelectedEvent
import com.mycelium.wallet.external.mediaflow.*
import com.mycelium.wallet.external.mediaflow.model.Category
import com.mycelium.wallet.external.mediaflow.model.News
import com.mycelium.wallet.external.partner.startContentLink
import com.mycelium.wallet.randomOrNull
import com.squareup.otto.Subscribe


class NewsFragment : Fragment() {

    private lateinit var adapter: NewsAdapter
    private lateinit var adapterSearch: NewsSearchAdapter
    private lateinit var preference: SharedPreferences
    var searchActive = false

    var currentNews: News? = null
    private var loading = false
    private var isLastPage = false
    var binding: FragmentNewsBinding? = null
    var newsClick: (News) -> Unit = {
        startActivity(Intent(activity, NewsActivity::class.java)
                .putExtra(NewsConstants.NEWS, it))
    }

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == NewsConstants.MEDIA_FLOW_UPDATE_ACTION && !searchActive) {
                loadItems()
            }
        }
    }

    private val failReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            adapter.state = NewsAdapter.State.FAIL
            updateUI()
        }
    }

    private val startLoadReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            adapter.state = NewsAdapter.State.LOADING
            updateUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        preference = requireActivity().getSharedPreferences(NewsConstants.NEWS_PREF, MODE_PRIVATE)
        adapter = NewsAdapter(preference)
        adapterSearch = NewsSearchAdapter(preference)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            FragmentNewsBinding.inflate(inflater, container, false)
                .apply {
                    binding = this
                }
                .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.newsList?.setHasFixedSize(false)
        binding?.newsList?.addOnScrollListener(object : PaginationScrollListener(binding!!.newsList.layoutManager as LinearLayoutManager) {
            override fun loadMoreItems() {
                if (!searchActive) {
                    loadItems(adapter.itemCount)
                }
            }

            override fun isLastPage() = isLastPage

            override fun isLoading() = loading
        })
        adapter.categoryClickListener = {
            val tab = getTab(it, binding?.tabs!!)
            tab?.select()
        }
        adapter.turnOffListener = {
            requireActivity().finish()
            startActivity(Intent(requireContext(), ModernMain::class.java))
        }
        adapter.bannerClickListener = {
            startContentLink(it?.link)
        }
        binding?.tabs?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) {
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                adapter.setCategory(tab.tag as Category)
                loadItems()
            }
        })
        binding?.searchClose?.setOnClickListener {
            if (binding?.searchInput?.text?.isEmpty() == true) {
                searchActive = false
                activity?.invalidateOptionsMenu()
                updateUI()
                val inputMethodManager = activity?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding?.searchInput?.applicationWindowToken, 0)
            } else {
                binding?.searchInput?.text = null
            }
        }
        binding?.searchInput?.doOnTextChanged { text, start, count, after ->
            startUpdateSearch(binding?.searchInput?.text.toString())
        }
        binding?.retry?.setOnClickListener {
            WorkManager.getInstance(requireContext())
                    .enqueue(OneTimeWorkRequest.Builder(MediaFlowSyncWorker::class.java).build())
        }
        binding?.mediaFlowLoading?.text = getString(R.string.loading_media_flow_feed_please_wait, "")
        updateUI()
    }

    private fun initTopBanner() {
        if (currentNews == null) {
            SettingsPreference.getMediaFlowContent()?.bannersTop
                    ?.filter { it.isActive() && preference.getBoolean(it.parentId, true)
                            && SettingsPreference.isContentEnabled(it.parentId)
                }?.randomOrNull()?.let { banner ->
                    binding?.layoutTopBanner?.topBanner?.visibility = VISIBLE
                    Glide.with(binding?.layoutTopBanner?.bannerImage!!)
                        .load(banner.imageUrl)
                        .into(binding?.layoutTopBanner?.bannerImage!!)
                    binding?.layoutTopBanner?.topBanner?.setOnClickListener {
                        startContentLink(banner.link)
                    }
                    binding?.layoutTopBanner?.bannerClose?.setOnClickListener {
                        binding?.layoutTopBanner?.topBanner?.visibility = GONE
                        preference.edit().putBoolean(banner.parentId, false).apply()
                    }
                }
        } else {
            binding?.layoutTopBanner?.topBanner?.visibility = GONE
            adapter.showBanner = false
        }
    }

    @Subscribe
    internal fun pageSelectedEvent(event: PageSelectedEvent): Unit {
        if (event.tag == "tab_news") {
            initTopBanner()
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.openClickListener = newsClick
        adapterSearch.openClickListener = newsClick
        loadItems()
        initTopBanner()
        LocalBroadcastManager.getInstance(requireContext()).run {
            registerReceiver(updateReceiver, IntentFilter(NewsConstants.MEDIA_FLOW_UPDATE_ACTION))
            registerReceiver(failReceiver, IntentFilter(NewsConstants.MEDIA_FLOW_FAIL_ACTION))
            registerReceiver(startLoadReceiver, IntentFilter(NewsConstants.MEDIA_FLOW_START_LOAD_ACTION))
        }
        MbwManager.getEventBus().register(this)
    }

    override fun onPause() {
        MbwManager.getEventBus().unregister(this)
        LocalBroadcastManager.getInstance(requireContext()).run {
            unregisterReceiver(updateReceiver)
            unregisterReceiver(failReceiver)
            unregisterReceiver(startLoadReceiver)
        }
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
            inputMethodManager.showSoftInput(binding?.searchInput, 0)
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

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun updateUI() {
        if (searchActive) {
            binding?.newsList?.adapter = adapterSearch
            binding?.tabs?.visibility = GONE
            binding?.discover?.visibility = VISIBLE
            binding?.search?.visibility = VISIBLE
            binding?.unableToLoad?.visibility = GONE
            binding?.mediaFlowLoading?.visibility = GONE
            startUpdateSearch()
        } else {
            binding?.newsList?.adapter = adapter
            binding?.tabs?.visibility = VISIBLE
            binding?.discover?.visibility = GONE
            binding?.search?.visibility = GONE
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
            //fix possible crash when page was hidden and task the "get categories" returns the result(in this case views are null) 
            if (!isAdded) {
                return@GetCategoriesTask
            }
            if (it.isNotEmpty()) {
                val list = mutableListOf(Category("All"))
                list.addAll(it)
                NewsUtils.sort(list).forEach { category ->
                    if (getTab(category, binding?.tabs!!) == null) {
                        val itemBinding =
                            MediaFlowTabItemBinding.inflate(layoutInflater, binding?.tabs!!, false)
                        itemBinding.text.text = category.name
                        val tab = binding?.tabs!!.newTab().setCustomView(itemBinding.root)
                        tab.tag = category
                        binding?.tabs!!.addTab(tab)
                    }
                }
                cleanTabs(list, binding?.tabs!!)
                binding?.unableToLoad?.visibility = GONE
                binding?.mediaFlowLoading?.visibility = GONE
            } else {
                when (preference.getString(NewsConstants.MEDIA_FLOW_LOAD_STATE, NewsConstants.MEDIA_FLOW_LOADING)) {
                    NewsConstants.MEDIA_FLOW_FAIL -> {
                        adapter.state = NewsAdapter.State.FAIL
                        binding?.unableToLoad?.visibility = VISIBLE
                        binding?.mediaFlowLoading?.visibility = GONE
                    }
                    NewsConstants.MEDIA_FLOW_LOADING -> {
                        adapter.state = NewsAdapter.State.LOADING
                        binding?.unableToLoad?.visibility = GONE
                        binding?.mediaFlowLoading?.visibility = VISIBLE
                        binding?.mediaFlowLoading?.postOnAnimationDelayed(object : Runnable {
                            var tick = 0
                            override fun run() {
                                binding?.mediaFlowLoading?.text = getString(R.string.loading_media_flow_feed_please_wait,
                                        when (tick++ % 3) {
                                            0 -> ".  "
                                            1 -> ".. "
                                            else -> "..."
                                        })
                                binding?.mediaFlowLoading?.postOnAnimationDelayed(this, 1000)
                            }
                        }, 1000)
                    }
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        val taskListener: (List<News>, Long) -> Unit = { pageData, count ->
            loading = false
            adapter.isFavorite = preference.getBoolean(NewsConstants.FAVORITE, false)
            val list = if (adapter.isFavorite) {
                pageData.filter { news -> news.isFavorite(preference) }
            } else {
                pageData
            }.filter { news -> news.id != currentNews?.id }
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

    private fun cleanTabs(list: MutableList<Category>, tabLayout: TabLayout) {
        for (i in tabLayout.tabCount - 1 downTo 0) {
            if (!list.contains(tabLayout.getTabAt(i)?.tag)) {
                tabLayout.removeTab(tabLayout.getTabAt(i)!!)
            }
        }
    }
}
