package com.mycelium.wallet.activity.modern

import android.content.DialogInterface
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.*
import butterknife.ButterKnife
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.adapter.NewsAdapter
import com.mycelium.wallet.external.news.GetNewsTask
import com.mycelium.wallet.external.news.NewsFactory
import com.mycelium.wallet.external.news.database.NewsDatabase
import com.mycelium.wallet.external.news.model.Category
import kotlinx.android.synthetic.main.fragment_news.*


class NewsFragment : Fragment() {

    private var adapter: NewsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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

        val task = GetNewsTask(null, listOf())
        task.listener = {
            adapter?.setData(it)
        }
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        object : AsyncTask<Void, Void, Unit>() {
            override fun doInBackground(vararg voids: Void) {
                try {
                    val news = NewsFactory.getService().posts().posts
                    NewsDatabase.saveNews(news)
                } catch (e: Exception) {
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.news, menu)

        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView
        searchView.setOnSearchClickListener {

        }
        searchView.setOnCloseListener {
            false
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                GetNewsTask(s, listOf()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                return true
            }

            override fun onQueryTextChange(s: String): Boolean {
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.action_filter) {
            activity?.let {
                val filter = mutableSetOf<Int>()
                var categories = mutableListOf<Category>()
                NewsDatabase.getCategories().forEach {  }
                AlertDialog.Builder(it, R.style.MyceliumModern_Dialog)
                        .setTitle("Select flows")
                        .setMultiChoiceItems(arrayOf("News", "Must Read"), BooleanArray(2), DialogInterface.OnMultiChoiceClickListener { dialogInterface, which, isChecked ->
                            if (isChecked) {
                                filter.add(which)
                            } else {
                                filter.remove(which)
                            }
                        })
                        .setNegativeButton(R.string.button_cancel, null)
                        .setPositiveButton(R.string.button_ok, DialogInterface.OnClickListener { dialogInterface, i ->

                        })
                        .create()
                        .show()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
