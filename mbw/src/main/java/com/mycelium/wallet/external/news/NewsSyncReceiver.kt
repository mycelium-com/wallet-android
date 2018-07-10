package com.mycelium.wallet.external.news

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.text.format.DateUtils
import android.util.Log
import android.widget.RemoteViews
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.news.NewsActivity
import com.mycelium.wallet.activity.settings.SettingsPreference
import com.mycelium.wallet.external.news.NewsConstants.NEWS_UPDATE_ACTION
import com.mycelium.wallet.external.news.NewsConstants.UPDATE_TIME
import com.mycelium.wallet.external.news.database.NewsDatabase
import com.mycelium.wallet.external.news.model.News
import com.squareup.otto.Bus
import java.text.SimpleDateFormat
import java.util.*


class NewsSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val preference = context.getSharedPreferences(NewsConstants.NEWS_PREF, Context.MODE_PRIVATE)!!
        val lastUpdateTime = preference.getString(UPDATE_TIME, null);
        NewsUpdate(MbwManager.getInstance(context).eventBus, lastUpdateTime, {
            val formattedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.UK).format(Date())
            preference.edit()
                    .putString(UPDATE_TIME, formattedDate)
                    .apply()
            if (it != null && it.isNotEmpty()) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(NEWS_UPDATE_ACTION))
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            if (SettingsPreference.getInstance().isNewsNotificationEnabled) {
                it?.entries?.forEach {
                    if (it.value == NewsDatabase.SqlState.INSERTED) {
                        val intent = Intent(context, NewsActivity::class.java)
                        val news = it.key
                        intent.putExtra(NewsConstants.NEWS, news)
                        val pIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

                        val remoteViews = RemoteViews(context.packageName, R.layout.layout_news_notification)
                        remoteViews.setTextViewText(R.id.title, news.title)
                        remoteViews.setTextViewText(R.id.category, news.categories.values.elementAt(0).name)
                        remoteViews.setTextViewText(R.id.date, DateUtils.getRelativeTimeSpanString(context, news.date.time))
                        val builder = NotificationCompat.Builder(context, NewsConstants.NEWS)
                                .setContent(remoteViews)
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setAutoCancel(true)
                                .setContentIntent(pIntent)
                        notificationManager?.notify(it.key.id + 100000, builder.build())
                    }
                }
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}

class NewsUpdate(val bus: Bus, val after: String?, val listener: ((Map<News, NewsDatabase.SqlState>?) -> Unit)?)
    : AsyncTask<Void, Void, Map<News, NewsDatabase.SqlState>>() {
    override fun doInBackground(vararg p0: Void?): Map<News, NewsDatabase.SqlState>? {
        var result: Map<News, NewsDatabase.SqlState>? = null
        try {
            val news = if (after != null && after.isNotEmpty()) {
                NewsFactory.getService().posts(after).execute().body()?.posts
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
