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
import kotlinx.android.synthetic.main.dialog_meida_flow_filter.view.*
import kotlinx.android.synthetic.main.fragment_news.*


class NewsFragment : Fragment() {

    private var adapter: NewsAdapter? = null
    private lateinit var preference: SharedPreferences
    var searchActive = false

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            if (p1?.action == NewsConstants.NEWS_UPDATE_ACTION && !searchActive) {
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
        adapter = NewsAdapter()
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
                if (adapter != null && layoutManager.findLastVisibleItemPosition() > adapter!!.itemCount - 5 && !adapter!!.searchMode) {
                    startUpdate(null, adapter!!.itemCount - 2)
                }
            }
        })
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
                ShowFilter({ result ->
                    activity?.let { context ->
                        val view = LayoutInflater.from(context).inflate(R.layout.dialog_meida_flow_filter, null)
                        view.title.text = getString(R.string.select_flows)
                        view.list.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                        val adapter = MediaFlowFilterAdapter(result)
                        adapter.checked.addAll(preference.getStringSet(CATEGORY_FILTER, setOf()))
                        view.list.adapter = adapter
                        val dialog = AlertDialog.Builder(context, R.style.MyceliumSettings_Dialog_Small)
                                .setView(view)
                                .create()

                        dialog.show()
                        adapter.checkListener = {
                            view.btOk.isEnabled = adapter.checked.size > 0
                        }
                        view.btCancel.setOnClickListener {
                            dialog.dismiss()
                        }
                        view.btOk.setOnClickListener {
                            dialog.dismiss()
                            preference.edit().putStringSet(CATEGORY_FILTER, adapter.checked).apply()
                            startUpdate()
                        }
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
        return super.onOptionsItemSelected(item)
    }
    private var loading = false
    private fun startUpdate(search: String? = null, offset: Int = 0) {
        if(loading) {
            return
        }
        val categories = preference.getStringSet(CATEGORY_FILTER, null)?.toTypedArray()?.convertToCategory()
                ?: listOf()
        val task = if (search != null) {
            GetNewsTask(search, categories)
        } else {
            GetNewsTask(search, categories, 30, offset)
        }

        task.listener = {
            loading = false
            adapter?.searchMode = search != null && search.isNotEmpty()
            if(it.isNotEmpty()) {
                if (offset == 0) {
                    adapter?.setData(it)
                } else {
                    adapter?.addData(it)
                }
            }
        }
        loading = true
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    class ShowFilter(val listener: ((List<Category>) -> Unit)) : AsyncTask<Void, Void, List<Category>>() {
        override fun doInBackground(vararg p0: Void?): List<Category> {
            return NewsDatabase.getCategories()
        }

        override fun onPostExecute(result: List<Category>?) {
            super.onPostExecute(result)
            result?.let { listener.invoke(it) }
        }
    }
}

fun Array<String>.convertToCategory(): List<Category> {
    val result = mutableListOf<Category>()
    this.forEach {
        result.add(Category(it))
    }
    return result
}