package com.mycelium.wallet.external.mediaflow

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wallet.external.mediaflow.database.NewsDatabase
import com.mycelium.wallet.external.mediaflow.model.News
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaFlowSyncWorker(private val context: Context, workerParams: WorkerParameters)
    : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result  = withContext(Dispatchers.IO) {
        if (!SettingsPreference.mediaFlowEnabled) {
            return@withContext Result.success()
        }
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
                NewsSyncUtils.notifyAboutMediaFlowTopics(context,
                        topics.entries
                                .filter { it.value == NewsDatabase.SqlState.INSERTED }
                                .map { it.key })
            }
            preference.edit()
                    .putString(NewsConstants.MEDIA_FLOW_LOAD_STATE, NewsConstants.MEDIA_FLOW_DONE)
                    .apply()
            LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(NewsConstants.MEDIA_FLOW_DONE_ACTION))
        }
        return@withContext if (failed) Result.failure() else Result.success()
    }

    private fun loadTopics(after: String?, failed: () -> Unit): Map<News, NewsDatabase.SqlState>? {
        var result: Map<News, NewsDatabase.SqlState>? = null
        try {
            val categories = NewsFactory.service.categories().execute().body()
            val tags = NewsFactory.service.tags().execute().body()
            result = fetchAllPages { nextPage ->
                var totalPageCount = 1
                Pair(if (after != null && after.isNotEmpty()) {
                    NewsFactory.service.updatedPosts("${after}T00:00:00", nextPage)
                } else {
                    NewsFactory.service.posts(nextPage)
                }.execute().apply {
                    totalPageCount = this.headers()["X-WP-TotalPages"]?.toInt() ?: 1
                }.body()?.map {
                    it.categories = it.categoriesIds.map { categoryId -> categories?.find { it.id == categoryId } }
                    it.tags = it.tagsIds.map { tagId -> tags?.find { it.id == tagId } }
                    it.author = NewsFactory.service.user(it.authorId).execute().body()
                    if(it.featuredMediaId != null) {
                        it.image = NewsFactory.service.media(it.featuredMediaId).execute().body()?.sourceUrl
                    }
                    it
                }, totalPageCount)
            }.let { NewsDatabase.saveNews(it) }
        } catch (e: Exception) {
            failed.invoke()
            Log.e("NewsSyncReceiver", "update news call", e)
        }
        return result
    }

    private fun fetchAllPages(fetchPage: (Int) -> Pair<List<News>?, Int>) =
            mutableListOf<News>().apply {
                var i = 1
                do {
                    val pair = fetchPage.invoke(i++)
                    pair.first?.let { posts ->
                        addAll(posts)
                    }
                } while (i <= pair.second)
            }
}
