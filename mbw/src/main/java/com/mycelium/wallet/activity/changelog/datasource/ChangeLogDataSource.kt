package com.mycelium.wallet.activity.changelog.datasource

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.edit
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.changelog.ChangeLogUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

internal class ChangeLogDataSource(
    context: Context
) {
    private val resources by lazyOf(context.resources)
    private val sharedPreferences by lazy {
        val preferencesName = "${context.packageName}_preferences"
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    /**
     * Last version code read from [SharedPreferences] or [NO_VERSION].
     */
    val lastVersionCode get() = sharedPreferences.getInt(VERSION_KEY, NO_VERSION)

    /**
     * Version code of the current installation.
     */
    val currentVersionCode get() = BuildConfig.VERSION_CODE

    /**
     * Version name of the current installation.
     */
    val currentVersionName get() = BuildConfig.VERSION_NAME

    suspend fun saveCurrentVersion() = withContext(Dispatchers.IO) {
        sharedPreferences.edit { putInt(VERSION_KEY, currentVersionCode) }
    }

    suspend fun hasNewReleaseChangeLog() = withContext(Dispatchers.IO) {
        val isNewVersion = lastVersionCode < currentVersionCode
        var hasNewVersionChangeLog = false
        if (isNewVersion) {
            val xmlParser = createChangeLogParser()
            try {
                var eventType = xmlParser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT && !hasNewVersionChangeLog) {
                    if (eventType == XmlPullParser.START_TAG && xmlParser.name == ReleaseTag.NAME.tag) {
                        val versionCode = xmlParser
                            .getAttributeValue(null, ReleaseTag.ATTRIBUTE_VERSION_CODE.tag)
                            .toInt()
                        hasNewVersionChangeLog = versionCode == currentVersionCode
                    }
                    eventType = xmlParser.next()
                }
            } catch (e: NumberFormatException) {
                Log.e(LOG_TAG, e.message, e)
            } catch (e: XmlPullParserException) {
                Log.e(LOG_TAG, e.message, e)
            } catch (e: IOException) {
                Log.e(LOG_TAG, e.message, e)
            }
        }
        return@withContext isNewVersion && hasNewVersionChangeLog
    }

    suspend fun readChangeLog(): List<ChangeLogUiModel> = withContext(Dispatchers.IO) {
        val xmlParser = createChangeLogParser()
        val changeLog = mutableListOf<ChangeLogUiModel>()

        try {
            var latestRelease = true
            var eventType = xmlParser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xmlParser.name == ReleaseTag.NAME.tag) {
                    changeLog.addAll(parseReleaseTag(xmlParser, latestRelease))
                    latestRelease = false
                }
                eventType = xmlParser.next()
            }
        } catch (e: XmlPullParserException) {
            Log.e(LOG_TAG, e.message, e)
        } catch (e: IOException) {
            Log.e(LOG_TAG, e.message, e)
        }

        return@withContext changeLog
    }

    private fun createChangeLogParser() = resources.getXml(R.xml.changelog_master)

    private fun parseReleaseTag(xml: XmlPullParser, latestRelease: Boolean): List<ChangeLogUiModel> {
        val version = xml.getAttributeValue(null, ReleaseTag.ATTRIBUTE_VERSION.tag)
        val versionCode = try {
            val versionCodeStr = xml.getAttributeValue(null, ReleaseTag.ATTRIBUTE_VERSION_CODE.tag)
            versionCodeStr.toInt()
        } catch (e: NumberFormatException) {
            NO_VERSION
        }
        var eventType = xml.eventType
        val changes: MutableList<String> = ArrayList()
        while (eventType != XmlPullParser.END_TAG || xml.name == ReleaseTag.CHANGE.tag) {
            if (eventType == XmlPullParser.START_TAG && xml.name == ReleaseTag.CHANGE.tag) {
                xml.next()
                changes.add(xml.text)
            }
            eventType = xml.next()
        }
        val release =
            if (latestRelease) ChangeLogUiModel.LatestRelease(versionCode, version)
            else ChangeLogUiModel.Release(versionCode, version)
        return listOf(release) + changes.map { ChangeLogUiModel.Change(it) }
    }

    enum class ReleaseTag(val tag: String) {
        NAME("release"),
        CHANGE("change"),
        ATTRIBUTE_VERSION("version"),
        ATTRIBUTE_VERSION_CODE("versioncode")
    }

    private companion object {

        const val LOG_TAG = "ChangeLog"

        /**
         * This is the key used when storing the version code in [SharedPreferences].
         */
        const val VERSION_KEY = "ckChangeLog_last_version_code"

        /**
         * Constant that used when no version code is available.
         */
        const val NO_VERSION = -1
    }
}