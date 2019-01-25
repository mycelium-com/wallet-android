package com.mycelium.wallet.activity.modern

import android.content.*
import android.content.Context.MODE_PRIVATE
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.view.*
import butterknife.ButterKnife
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.adapter.MediaFlowFilterAdapter
import com.mycelium.wallet.activity.modern.adapter.NewsAdapter
import com.mycelium.wallet.activity.news.NewsActivity
import com.mycelium.wallet.external.mediaflow.GetNewsTask
import com.mycelium.wallet.external.mediaflow.NewsConstants
import com.mycelium.wallet.external.mediaflow.NewsConstants.CATEGORY_FILTER
import com.mycelium.wallet.external.mediaflow.database.NewsDatabase
import com.mycelium.wallet.external.mediaflow.model.Category
import com.mycelium.wallet.external.mediaflow.model.News
import kotlinx.android.synthetic.main.dialog_mediaflow_filter.view.*
import kotlinx.android.synthetic.main.fragment_news.*


class NewsFragment : Fragment() {

    private val adapter: NewsAdapter = NewsAdapter()
    private lateinit var preference: SharedPreferences
    var searchActive = false

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
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ButterKnife.bind(this, view)
        val layoutManager = LinearLayoutManager(activity!!, LinearLayoutManager.VERTICAL, false)
        newsList.layoutManager = layoutManager
        newsList.adapter = adapter
        newsList.setHasFixedSize(false)
        newsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            var scrollY = 0;
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                scrollY += dy
                if (scrollY > recyclerView.height && scrollTop.visibility == View.GONE) {
                    scrollTop.visibility = View.VISIBLE
                } else if (scrollY <= recyclerView.height && scrollTop.visibility == View.VISIBLE) {
                    scrollTop.visibility = View.GONE
                }
                if (layoutManager.findLastVisibleItemPosition() > adapter.itemCount - 5 && !adapter.searchMode) {
                    startUpdate(null, adapter.itemCount - 2)
                }
            }
        })
        adapter.shareClickListener = { news ->
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_SUBJECT, news.title)
                    .putExtra(Intent.EXTRA_TEXT, news.link)
                    .setType("text/plain"), getString(R.string.share_news)))
        }
        adapter.openClickListener = {
            val intent = Intent(activity, NewsActivity::class.java)
            intent.putExtra("news", it)
            startActivity(intent)
        }
        scrollTop.setOnClickListener {
            newsList.smoothScrollToPosition(0)
        }
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
        inflater?.inflate(R.menu.news, menu)

        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView
        searchView.maxWidth = Integer.MAX_VALUE;
        searchView.setOnSearchClickListener {
            searchActive = true
        }
        searchView.setOnCloseListener {
            searchActive = false
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

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_filter ->
                ShowFilterTask { result ->
                    activity?.let { context ->
                        val view = LayoutInflater.from(context).inflate(R.layout.dialog_mediaflow_filter, null)
                        view.title.text = getString(R.string.select_flows)
                        view.list.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                        val filterAdapter = MediaFlowFilterAdapter(result)
                        filterAdapter.checked.addAll(preference.getStringSet(CATEGORY_FILTER, setOf())!!)
                        view.list.adapter = filterAdapter
                        val dialog = AlertDialog.Builder(context, R.style.MyceliumSettings_Dialog_Small)
                                .setView(view)
                                .create()

                        dialog.show()
                        filterAdapter.checkListener = {
                            view.btOk.isEnabled = filterAdapter.checked.size > 0
                        }
                        view.btCancel.setOnClickListener {
                            dialog.dismiss()
                        }
                        view.btOk.setOnClickListener {
                            dialog.dismiss()
                            preference.edit().putStringSet(CATEGORY_FILTER, filterAdapter.checked).apply()
                            startUpdate()
                        }
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
        return super.onOptionsItemSelected(item)
    }

    private var loading = false
    private fun startUpdate(search: String? = null, offset: Int = 0) {
        if (loading) {
            return
        }
        val categories = preference.getStringSet(CATEGORY_FILTER, null)?.map { Category(it) } ?: listOf()
        val taskListener: (List<News>) -> Unit = {
            loading = false
            adapter.searchMode = search != null && search.isNotEmpty()
            if (it.isNotEmpty()) {
                if (offset == 0) {
                    adapter.setData(it)
                } else {
                    adapter.addData(it)
                }
            }
        }
        val task = if (search != null) {
            GetNewsTask(search, categories, listener = taskListener)
        } else {
            GetNewsTask(search, categories, 30, offset, taskListener)
        }

        loading = true
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    class ShowFilterTask(val listener: ((List<Category>) -> Unit)) : AsyncTask<Void, Void, List<Category>>() {
        override fun doInBackground(vararg args: Void?): List<Category> {
            return NewsDatabase.getCategories()
        }

        override fun onPostExecute(result: List<Category>?) {
            super.onPostExecute(result)
            result?.let { listener.invoke(it) }
        }
    }
}
