package com.mycelium.wallet.external.mediaflow

import android.os.AsyncTask
import com.mycelium.wallet.external.mediaflow.database.NewsDatabase
import com.mycelium.wallet.external.mediaflow.model.Category


class GetCategoriesTask(val listener: ((List<Category>) -> Unit)) : AsyncTask<Void, Void, List<Category>>() {
    override fun doInBackground(vararg p0: Void?): List<Category> {
        return NewsDatabase.getCategories()
    }

    override fun onPostExecute(result: List<Category>) {
        super.onPostExecute(result)
        listener.invoke(result)
    }
}