package com.mycelium.wallet.external.mediaflow

import android.os.AsyncTask
import com.mycelium.wallet.external.mediaflow.database.NewsDatabase
import com.mycelium.wallet.external.mediaflow.model.Category
import com.mycelium.wallet.external.mediaflow.model.News


class GetNewsTask(val search: String?, val categories: List<Category>,
                  val limit: Int = -1, val offset: Int = 0,
                  var listener: (List<News>) -> Unit) : AsyncTask<Void, Void, List<News>>() {

    override fun doInBackground(vararg p0: Void?): List<News> {
        return NewsDatabase.getNews(search, categories, limit, offset)
    }

    override fun onPostExecute(result: List<News>) {
        super.onPostExecute(result)
        listener.invoke(result)
    }
}