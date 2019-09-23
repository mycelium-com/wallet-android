package com.mycelium.wallet.activity.settings

import android.content.Context
import android.content.SharedPreferences
import com.mycelium.wallet.PartnerInfo
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.WalletConfiguration
import com.mycelium.wallet.external.mediaflow.model.Category
import java.util.*

object SettingsPreference {
    private val NEWS_NOTIFICATION_ENABLE = "news_notification_enable"
    private val FIO_ENABLE = "fio_enable"
    private val sharedPreferences: SharedPreferences = WalletApplication.getInstance().getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val oldDate = date(1950, Calendar.JANUARY, 1, 0, 0, "Europe/Paris")

    var fioEnabled
        get() = sharedPreferences.getBoolean(FIO_ENABLE, true) && fioActive
        set(enable) {
            sharedPreferences.edit()
                    .putBoolean(FIO_ENABLE, enable)
                    .apply()
        }

    val fioActive
        get() = isActive(FIO_ENABLE)

    private fun isActive(id: String) = when (id) {
        FIO_ENABLE -> PartnerInfo(getSharedDate(WalletConfiguration.PREFS_FIO_START_DATE),
                getSharedDate(WalletConfiguration.PREFS_FIO_END_DATE))
        else -> PartnerInfo(oldDate, oldDate)
    }.isActive()

    private fun getSharedDate(key: String, defaultDate: Date = oldDate): Date =
            Date(sharedPreferences.getLong(key, defaultDate.time))

    private fun date(year: Int, month: Int, day: Int, hour: Int, minute: Int, timezone: String) = Calendar.getInstance().apply {
        timeZone = TimeZone.getTimeZone(timezone)
        set(year, month, day, hour, minute)
    }.time


    private fun PartnerInfo.isActive() = Date().after(startDate) && Date().before(endDate)

    var mediaFLowNotificationEnabled
        get() = sharedPreferences.getBoolean(NEWS_NOTIFICATION_ENABLE, true)
        set(value) = sharedPreferences.edit().putBoolean(NEWS_NOTIFICATION_ENABLE, value).apply()

    fun setMediaFlowCategoryNotificationEnabled(category: Category, enable: Boolean) {
        sharedPreferences.edit().putBoolean(NEWS_NOTIFICATION_ENABLE + category.name, enable).apply()
    }

    fun getMediaFlowCategoryNotificationEnabled(category: Category) =
            sharedPreferences.getBoolean(NEWS_NOTIFICATION_ENABLE + category.name, true)
}
