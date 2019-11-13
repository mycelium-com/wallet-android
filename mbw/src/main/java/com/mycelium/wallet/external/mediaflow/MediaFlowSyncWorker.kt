package com.mycelium.wallet.external.mediaflow

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wallet.external.mediaflow.database.NewsDatabase
import com.mycelium.wallet.external.mediaflow.model.News
import com.mycelium.wallet.external.mediaflow.model.NewsContainer
import java.text.SimpleDateFormat
import java.util.*


class MediaFlowSyncWorker(val context: Context, workerParams: WorkerParameters)
    : Worker(context, workerParams) {
    override fun doWork(): Result {
        val preference = context.getSharedPreferences(NewsConstants.NEWS_PREF, Context.MODE_PRIVATE)!!
        val lastUpdateTime = preference.getString(NewsConstants.UPDATE_TIME, null)
        val updateTime = SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(Date())
        preference.edit()
                .putString(NewsConstants.MEDIA_FLOW_LOAD_STATE, NewsConstants.MEDIA_FLOW_LOADING)
                .apply()
        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(NewsConstants.MEDIA_FLOW_START_LOAD_ACTION))

        var failed = false
        val topics = loadTopics(lastUpdateTime) {
            failed = true
            preference.edit()
                    .putString(NewsConstants.MEDIA_FLOW_LOAD_STATE, NewsConstants.MEDIA_FLOW_FAIL)
                    .apply()
            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(NewsConstants.MEDIA_FLOW_FAIL_ACTION))
        }

        if (topics != null) {
            preference.edit()
                    .putString(NewsConstants.UPDATE_TIME, updateTime)
                    .apply()
            if (topics.isNotEmpty()) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(NewsConstants.MEDIA_FLOW_UPDATE_ACTION))
            }

            if (SettingsPreference.mediaFLowNotificationEnabled
                    && lastUpdateTime != null // not show for init load
            ) {
                val newTopics = arrayListOf<News>()
                topics.entries.forEach {
                    if (it.value == NewsDatabase.SqlState.INSERTED) {
                        newTopics.add(it.key)
                    }
                }
                NewsSyncUtils.notifyAboutMediaFlowTopics(context, newTopics)
            }
            preference.edit()
                    .putString(NewsConstants.MEDIA_FLOW_LOAD_STATE, NewsConstants.MEDIA_FLOW_DONE)
                    .apply()
            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(NewsConstants.MEDIA_FLOW_DONE_ACTION))
        }
        return if (failed) Result.failure() else Result.success()
    }

    private fun loadTopics(after: String?, failed: () -> Unit): Map<News, NewsDatabase.SqlState>? {
        var result: Map<News, NewsDatabase.SqlState>? = null
        try {
            result = fetchAllPages { nextPage ->
                if (after != null && after.isNotEmpty()) {
                    NewsFactory.service.updatedPosts(after, nextPage).execute().body()
                } else {
                    NewsFactory.service.posts(nextPage).execute().body()
                }
            }.let { NewsDatabase.saveNews(it) }
        } catch (e: Exception) {
            failed.invoke()
            Log.e("NewsSyncReceiver", "update news call", e)
        }
        return result
    }

    private fun fetchAllPages(fetchPage: (String?) -> NewsContainer?) =
            mutableListOf<News>().apply {
                var nextPage: String? = ""
                do {
                    val newsContainer = fetchPage.invoke(nextPage)
                    newsContainer?.posts?.let { posts ->
                        addAll(posts)
                    }
                    nextPage = newsContainer?.meta?.nextPage
                } while (nextPage?.isNotEmpty() == true)
            }
}