package com.mycelium.wallet.activity.settings

import android.content.Context
import android.content.SharedPreferences
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.WalletConfiguration
import java.util.*

object SettingsPreference {
    private val FIO_ENABLE = "fio_enable"
    private val sharedPreferences: SharedPreferences = WalletApplication.getInstance().getSharedPreferences("settings", Context.MODE_PRIVATE)

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
        FIO_ENABLE -> Date().before(getSharedDate(WalletConfiguration.PREFS_FIO_END_DATE,
                date(2019, Calendar.NOVEMBER, 1, 0, 0, "Europe/Paris")))
        else -> false
    }

    private fun getSharedDate(key: String, defaultDate: Date): Date =
            Date(sharedPreferences.getLong(key, defaultDate.time))

    private fun date(year: Int, month: Int, day: Int, hour: Int, minute: Int, timezone: String) = Calendar.getInstance().apply {
        timeZone = TimeZone.getTimeZone(timezone)
        set(year, month, day, hour, minute)
    }.time
}
