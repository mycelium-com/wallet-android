package com.mycelium.wallet.external.mediaflow

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.StartupActivity
import com.mycelium.wallet.activity.news.NewsUtils
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wallet.external.mediaflow.database.NewsDatabase
import com.mycelium.wallet.external.mediaflow.model.News
import com.mycelium.wallet.external.mediaflow.model.NewsContainer
import com.squareup.otto.Bus
import java.text.SimpleDateFormat
import java.util.*

const val mediaFlowNotificationId = 34563487
const val mediaFlowNotificationGroup = "Media Flow"

class NewsSyncService : Service() {

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        val preference = getSharedPreferences(NewsConstants.NEWS_PREF, Context.MODE_PRIVATE)!!
        val lastUpdateTime = preference.getString(NewsConstants.UPDATE_TIME, null)
        val updateTime = SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(Date())
        NewsUpdate(MbwManager.getEventBus(), lastUpdateTime) {
            if (it == null) {
                return@NewsUpdate
            }
            preference.edit()
                    .putString(NewsConstants.UPDATE_TIME, updateTime)
                    .apply()
            if (it.isNotEmpty()) {
                LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(NewsConstants.NEWS_UPDATE_ACTION))
            }

            if (SettingsPreference.newsNotificationEnabled
                    && lastUpdateTime != null // not show for init load
            ) {
                val newTopics = arrayListOf<News>()
                it.entries.forEach {
                    if (it.value == NewsDatabase.SqlState.INSERTED) {
                        newTopics.add(it.key)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val name = getString(R.string.media_flow_notification_title)
                    val importance = NotificationManager.IMPORTANCE_DEFAULT
                    val channel = NotificationChannel(NewsConstants.NEWS, name, importance).apply {
                        description = name
                    }
                    // Register the channel with the system
                    val notificationManager: NotificationManager =
                            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.createNotificationChannel(channel)
                }

                val builder = NotificationCompat.Builder(this, NewsConstants.NEWS)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentTitle(getString(R.string.media_flow_notification_title))

                if (newTopics.size == 1) {
                    val news = newTopics[0]
                    builder.setContentText(news.title)
                    builder.setTicker(news.title)

                    val remoteViews = RemoteViews(packageName, R.layout.layout_news_notification)
                    remoteViews.setTextViewText(R.id.title, news.title)

                    val activityIntent = Intent(this, StartupActivity::class.java)
                    activityIntent.action = NewsUtils.MEDIA_FLOW_ACTION
                    activityIntent.putExtra(NewsConstants.NEWS, news)
                    val pIntent = PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_CANCEL_CURRENT)

                    builder.setContent(remoteViews)
                            .setContentIntent(pIntent)
                    builder.setGroup(mediaFlowNotificationGroup)
                    NotificationManagerCompat.from(this).notify(mediaFlowNotificationId, builder.build())
                } else if (newTopics.size > 1) {
                    builder.setGroupSummary(true)
                    val inboxStyle = NotificationCompat.InboxStyle()
                            .setBigContentTitle(getString(R.string.media_flow_notification_title))
                    newTopics.forEach {
                        inboxStyle.addLine(it.title)
                    }
                    builder.setStyle(inboxStyle)

                    val activityIntent = Intent(this, StartupActivity::class.java)
                    activityIntent.action = NewsUtils.MEDIA_FLOW_ACTION
                    val pIntent = PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_CANCEL_CURRENT)
                    builder.setContentIntent(pIntent)
                            .setGroup(mediaFlowNotificationGroup)
                    NotificationManagerCompat.from(this).notify(mediaFlowNotificationId, builder.build())
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}

class NewsUpdate(val bus: Bus, val after: String?, val listener: ((Map<News, NewsDatabase.SqlState>?) -> Unit)?)
    : AsyncTask<Void, Void, Map<News, NewsDatabase.SqlState>>() {
    override fun doInBackground(vararg p0: Void?): Map<News, NewsDatabase.SqlState>? {
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

    override fun onPostExecute(result: Map<News, NewsDatabase.SqlState>?) {
        super.onPostExecute(result)
        listener?.invoke(result)
    }
}
