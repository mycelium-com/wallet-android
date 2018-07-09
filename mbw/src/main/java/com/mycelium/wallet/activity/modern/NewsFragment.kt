package com.mycelium.wallet.activity.modern

import android.content.*
import android.content.Context.MODE_PRIVATE
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.*
import butterknife.ButterKnife
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.adapter.NewsAdapter
import com.mycelium.wallet.external.news.GetNewsTask
import com.mycelium.wallet.external.news.NewsConstants
import com.mycelium.wallet.external.news.NewsConstants.CATEGORY_FILTER
import com.mycelium.wallet.external.news.database.NewsDatabase
import com.mycelium.wallet.external.news.model.Category
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

        newsList.layoutManager = LinearLayoutManager(activity!!, LinearLayoutManager.VERTICAL, false)
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
            R.id.action_filter -> activity?.let {
                val categories = mutableListOf<String>()
                NewsDatabase.getCategories().forEach {
                    categories.add(it.name)
                }
                val checked = BooleanArray(categories.size)
                preference.getStringSet(CATEGORY_FILTER, null)?.let {
                    it.forEach {
                        val index = categories.indexOf(it)
                        if (index != -1) {
                            checked[index] = true
                        }
                    }
                }
                AlertDialog.Builder(it, R.style.MyceliumModern_Dialog)
                        .setTitle(getString(R.string.select_flows))
                        .setMultiChoiceItems(categories.toTypedArray(), checked, { _, which, isChecked ->
                            checked[which] = isChecked
                        })
                        .setNegativeButton(R.string.button_cancel, null)
                        .setPositiveButton(R.string.button_ok, { dialogInterface, i ->
                            val selected = mutableSetOf<String>()
                            checked.forEachIndexed { index, b ->
                                if (b) {
                                    selected.add(categories[index])
                                }
                            }
                            preference.edit().putStringSet(CATEGORY_FILTER, selected).apply()
                            startUpdate()
                        })
                        .create()
                        .show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startUpdate(search: String? = null) {
        val categories = preference.getStringSet(CATEGORY_FILTER, null)?.toTypedArray()?.convertToCategory()
                ?: listOf()
        val task = GetNewsTask(search, categories)
        task.listener = {
            adapter?.searchMode = search != null && search.isNotEmpty()
            adapter?.setData(it)
        }
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}

fun Array<String>.convertToCategory(): List<Category> {
    val result = mutableListOf<Category>()
    this.forEach {
        result.add(Category(it))
    }
    return result
}