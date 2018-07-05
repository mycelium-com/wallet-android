package com.mycelium.wallet.external.news

import android.os.AsyncTask
import com.mycelium.wallet.external.news.database.NewsDatabase
import com.mycelium.wallet.external.news.model.Category
import com.mycelium.wallet.external.news.model.News


class GetNewsTask(val search: String?, val categories: List<Category>) : AsyncTask<Void, Void, List<News>>() {

    var listener: ((List<News>) -> Unit)? = null

    override fun doInBackground(vararg p0: Void?): List<News> {
        return NewsDatabase.getNews(search, categories)
    }

    override fun onPostExecute(result: List<News>) {
        super.onPostExecute(result)
        listener?.invoke(result)
    }
}