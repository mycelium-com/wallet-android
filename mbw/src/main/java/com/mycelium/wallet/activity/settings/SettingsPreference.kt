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
        get() = isFioActive(FIO_ENABLE)

    private fun isFioActive(id: String) = when (id) {
        FIO_ENABLE -> Date().before(getFioEndDate())
        else -> false
    }

    private fun getFioEndDate(): Date =
            Date(sharedPreferences.getLong(WalletConfiguration.PREFS_FIO_END_DATE, Calendar.getInstance().apply {
                timeZone = TimeZone.getTimeZone("Europe/Paris")
                set(2019, Calendar.NOVEMBER, 1, 0, 0)
            }.timeInMillis))
}
