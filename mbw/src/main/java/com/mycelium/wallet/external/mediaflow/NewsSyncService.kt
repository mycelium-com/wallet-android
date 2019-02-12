package com.mycelium.wallet.external.mediaflow

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.text.format.DateUtils
import android.util.Log
import android.widget.RemoteViews
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.StartupActivity
import com.mycelium.wallet.activity.news.NewsUtils
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wallet.external.mediaflow.database.NewsDatabase
import com.mycelium.wallet.external.mediaflow.model.News
import com.squareup.otto.Bus
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

const val mediaFlowNotificationId = 100000
const val mediaFlowNotificationGroup = "Media Flow"

class NewsSyncService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        val preference = getSharedPreferences(NewsConstants.NEWS_PREF, Context.MODE_PRIVATE)!!
        val lastUpdateTime = preference.getString(NewsConstants.UPDATE_TIME, null);
        val updateTime = SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(Date())
        NewsUpdate(MbwManager.getEventBus(), lastUpdateTime) {
            preference.edit()
                    .putString(NewsConstants.UPDATE_TIME, updateTime)
                    .apply()
            if (it != null && it.isNotEmpty()) {
                LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(NewsConstants.NEWS_UPDATE_ACTION))
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            if (SettingsPreference.getInstance().isNewsNotificationEnabled) {
                val newTopics = arrayListOf<News>()
                it?.entries?.forEach {
                    if (it.value == NewsDatabase.SqlState.INSERTED) {
                        newTopics.add(it.key)
                    }
                }


                val builder = NotificationCompat.Builder(this, NewsConstants.NEWS)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setAutoCancel(true)
                if (newTopics.size == 1) {
                    val news = newTopics[0]

                    val remoteViews = RemoteViews(packageName, R.layout.layout_news_notification)
                    remoteViews.setTextViewText(R.id.title, news.title)
                    remoteViews.setTextViewText(R.id.category, news.categories.values.elementAt(0).name)
                    remoteViews.setTextViewText(R.id.date, DateUtils.getRelativeTimeSpanString(this, news.date.time))

                    val activityIntent = Intent(this, StartupActivity::class.java)
                    activityIntent.action = NewsUtils.MEDIA_FLOW_ACTION
                    activityIntent.putExtra(NewsConstants.NEWS, news)
                    val pIntent = PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_CANCEL_CURRENT)

                    builder.setContent(remoteViews)
                            .setContentIntent(pIntent)
                    builder.setGroup(mediaFlowNotificationGroup)
                    notificationManager?.notify(mediaFlowNotificationId, builder.build())
                } else if (newTopics.size > 1) {
                    val title = resources.getQuantityString(R.plurals.media_flow_new_topic, newTopics.size, newTopics.size)
                    builder.setContentTitle(title)
                            .setGroupSummary(true)
                    val inboxStyle = NotificationCompat.InboxStyle()
                            .setBigContentTitle(title)
                    newTopics.forEach {
                        inboxStyle.addLine(it.title)
                    }
                    builder.setStyle(inboxStyle)

                    val activityIntent = Intent(this, StartupActivity::class.java)
                    activityIntent.action = NewsUtils.MEDIA_FLOW_ACTION
                    val pIntent = PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_CANCEL_CURRENT)
                    builder.setContentIntent(pIntent)
                            .setGroup(mediaFlowNotificationGroup)
                    notificationManager?.notify(mediaFlowNotificationId, builder.build())
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
            val news = if (after != null && after.isNotEmpty()) {
                val res = mutableListOf<News>()
                NewsFactory.getService().updatedPosts(after).execute().body()?.posts?.let {
                    res.addAll(it)
                }
                res
            } else {
                NewsFactory.getService().posts().execute().body()?.posts
            }
            result = news?.let { NewsDatabase.saveNews(it) }
        } catch (e: Exception) {
            Log.e("NewsSyncReceiver", "update news call", e)
        }
        return result
    }

    override fun onPostExecute(result: Map<News, NewsDatabase.SqlState>?) {
        super.onPostExecute(result)
        listener?.invoke(result)
    }
}
