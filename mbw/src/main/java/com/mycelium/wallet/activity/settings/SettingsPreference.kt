package com.mycelium.wallet.activity.settings

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.mycelium.wallet.Constants
import com.mycelium.wallet.PartnerInfo
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.WalletConfiguration
import com.mycelium.wallet.external.partner.model.*
import java.util.*

object SettingsPreference {
    private val NEWS_NOTIFICATION_ENABLE = "news_notification_enable"
    private val MEDIA_FLOW_ENABLE = "media_flow_enable"
    private val FIO_ENABLE = "fio_enable"
    private val PARTNER_KEY = "partner-info"
    private val MEDIAFLOW_KEY = "mediaflow"
    private val MAIN_MENU_KEY = "mainmenu"
    private val BUY_SELL_KEY = "buysell"
    private val BALANCE_KEY = "balance"
    private val PARTNER_ENABLED = "partner-enabled"
    private val sharedPreferences: SharedPreferences = WalletApplication.getInstance().getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE)
    private val oldDate = date(1950, Calendar.JANUARY, 1, 0, 0, "Europe/Paris")

    @JvmStatic
    var fioEnabled
        get() = sharedPreferences.getBoolean(FIO_ENABLE, true) && fioActive
        set(enable) {
            sharedPreferences.edit()
                    .putBoolean(FIO_ENABLE, enable)
                    .apply()
        }

    @JvmStatic
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

    @JvmStatic
    var mediaFlowEnabled
        get() = sharedPreferences.getBoolean(MEDIA_FLOW_ENABLE, true)
        set(value) = sharedPreferences.edit().putBoolean(MEDIA_FLOW_ENABLE, value).apply()


    fun getPartnersHeaderTitle(): String? = getPartnersLocalized()?.title

    fun getPartnersHeaderText(): String? = getPartnersLocalized()?.text

    fun getPartners(): List<Partner>? = getPartnersLocalized()?.partners

    private fun getPartnersLocalized(): PartnersLocalized? =
            Gson().fromJson(sharedPreferences.getString(
                    if (sharedPreferences.contains("partners-${getLanguage()}")) "partners-${getLanguage()}" else "partners-en", ""), PartnersLocalized::class.java)

    @JvmStatic
    fun getMediaFlowContent(): MediaFlowContent? =
            Gson().fromJson(sharedPreferences.getString(
                    if (sharedPreferences.contains("${MEDIAFLOW_KEY}-${getLanguage()}")) "${MEDIAFLOW_KEY}-${getLanguage()}" else "${MEDIAFLOW_KEY}-en", ""), MediaFlowContent::class.java)

    @JvmStatic
    fun getMainMenuContent(): MainMenuContent? =
            Gson().fromJson(sharedPreferences.getString(
                    if (sharedPreferences.contains("${MAIN_MENU_KEY}-${getLanguage()}")) "${MAIN_MENU_KEY}-${getLanguage()}" else "${MAIN_MENU_KEY}-en", ""), MainMenuContent::class.java)

    @JvmStatic
    fun getBalanceContent(): BalanceContent? =
            Gson().fromJson(sharedPreferences.getString(
                    if (sharedPreferences.contains("${BALANCE_KEY}-${getLanguage()}")) "${BALANCE_KEY}-${getLanguage()}" else "${BALANCE_KEY}-en", ""), BalanceContent::class.java)

    @JvmStatic
    fun getBuySellContent(): BuySellContent? =
            Gson().fromJson(sharedPreferences.getString(
                    if (sharedPreferences.contains("${BUY_SELL_KEY}-${getLanguage()}")) "${BUY_SELL_KEY}-${getLanguage()}" else "${BUY_SELL_KEY}-en", ""), BuySellContent::class.java)

    @JvmStatic
    fun getLanguage(): String? = sharedPreferences.getString(Constants.LANGUAGE_SETTING, Locale.getDefault().language)

    @JvmStatic
    fun getPartnerInfos(): List<PartnerInfo> = mutableListOf<PartnerInfo>().apply {
        sharedPreferences.all.filter { it.key.startsWith("${PARTNER_KEY}-") }.forEach {
            add(Gson().fromJson(it.value.toString(), PartnerInfo::class.java))
        }
    }

    private fun getPartnerInfo(id: String): PartnerInfo? =
            Gson().fromJson(sharedPreferences.getString("${PARTNER_KEY}-$id", ""), PartnerInfo::class.java)

    @JvmStatic
    fun isEnabled(partnerInfoId: String): Boolean = sharedPreferences.getBoolean("${PARTNER_ENABLED}-${partnerInfoId}", true)

    fun setEnabled(partnerInfoId: String, enable: Boolean) {
        sharedPreferences.edit().putBoolean("${PARTNER_ENABLED}-${partnerInfoId}", enable).apply()
    }

    @JvmStatic
    fun isContentEnabled(id: String): Boolean = getPartnerInfo(id).let { it?.isEnabled ?: true && it?.isActive() ?: true } && isEnabled(id)
}
