package com.mycelium.wallet.activity.settings

import android.content.Context
import android.content.SharedPreferences
import com.mycelium.wallet.WalletApplication
import java.util.*

object SettingsPreference {
    private val FIO_ENABLE = "fio_enable"
    private val sharedPreferences: SharedPreferences = WalletApplication.getInstance().getSharedPreferences("settings", Context.MODE_PRIVATE)

    var fioEnabled
        get() = sharedPreferences.getBoolean(FIO_ENABLE, true) && isActive(FIO_ENABLE)
        set(enable) {
            sharedPreferences.edit()
                    .putBoolean(FIO_ENABLE, enable)
                    .apply()
        }

    val fioActive
        get() = isActive(FIO_ENABLE)

    private fun isActive(id: String) = when (id) {
        FIO_ENABLE -> isBefore(2019, Calendar.NOVEMBER, 1, 0, 0, "Europe/Paris")
        else -> false
    }

    private fun isBefore(year: Int, month: Int, day: Int, hour: Int, minute: Int, timezone: String): Boolean {
        val calendar = Calendar.getInstance()
        calendar.timeZone = TimeZone.getTimeZone(timezone)
        calendar.set(year, month, day, hour, minute)
        return Date().before(calendar.time)
    }
}
