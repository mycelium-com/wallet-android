package com.mycelium.wallet.activity.settings

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import com.mycelium.bequant.BequantConstants
import com.mycelium.bequant.BequantPreference
import com.mycelium.wallet.Constants
import com.mycelium.wallet.PartnerInfo
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.external.partner.model.*
import java.util.*

object SettingsPreference {
    private const val NEWS_NOTIFICATION_ENABLE = "news_notification_enable"
    private const val MEDIA_FLOW_ENABLE = "media_flow_enable"
    private const val FIO_ENABLE = "fio_enable"
    private const val PARTNER_KEY = "partner-info"
    private const val MEDIAFLOW_KEY = "mediaflow"
    private const val ACCOUNTS_KEY = "accounts"
    private const val MAIN_MENU_KEY = "mainmenu"
    private const val BUY_SELL_KEY = "buysell"
    private const val BALANCE_KEY = "balance"
    private const val PARTNER_ENABLED = "partner-enabled"
    private const val EXCHANGE_CONFIRMATION_ENABLE = "exchange_confiramation_enable"
    private val sharedPreferences: SharedPreferences = WalletApplication.getInstance().getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE)
    private val gson by lazy {
        GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create()
    }

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
        get() = isContentEnabled("fio-presale")

    private fun date(year: Int, month: Int, day: Int, hour: Int, minute: Int, timezone: String) = Calendar.getInstance().apply {
        timeZone = TimeZone.getTimeZone(timezone)
        set(year, month, day, hour, minute)
    }.time

    var mediaFLowNotificationEnabled
        get() = sharedPreferences.getBoolean(NEWS_NOTIFICATION_ENABLE, true)
        set(value) = sharedPreferences.edit().putBoolean(NEWS_NOTIFICATION_ENABLE, value).apply()

    @JvmStatic
    var mediaFlowEnabled
        get() = sharedPreferences.getBoolean(MEDIA_FLOW_ENABLE, true)
        set(value) = sharedPreferences.edit().putBoolean(MEDIA_FLOW_ENABLE, value).apply()

    @JvmStatic
    var exchangeConfirmationEnabled
        get() = sharedPreferences.getBoolean(EXCHANGE_CONFIRMATION_ENABLE, false)
        set(value) = sharedPreferences.edit().putBoolean(EXCHANGE_CONFIRMATION_ENABLE, value).apply()


    fun getPartnersHeaderTitle(): String? = getPartnersLocalized()?.title

    fun getPartnersHeaderText(): String? = getPartnersLocalized()?.text

    fun getPartners(): List<Partner>? = getPartnersLocalized()?.partners

    private fun SharedPreferences.getLocalizedString(key: String) = getString(
            if (contains("$key-${getLanguage()}")) "$key-${getLanguage()}" else "$key-en", "")!!

    private fun <E> load(key: String, clazz: Class<E>): E? = gson.fromJson(sharedPreferences.getLocalizedString(key), clazz)

    private fun getPartnersLocalized() = load("partners", PartnersLocalized::class.java)

    @JvmStatic
    fun getMediaFlowContent() = load(MEDIAFLOW_KEY, MediaFlowContent::class.java)

    @JvmStatic
    fun getAccountsContent() = load(ACCOUNTS_KEY, AccountsContent::class.java)

    @JvmStatic
    fun getMainMenuContent() = load(MAIN_MENU_KEY, MainMenuContent::class.java)

    @JvmStatic
    fun getBalanceContent() = load(BALANCE_KEY, BalanceContent::class.java)

    @JvmStatic
    fun getBuySellContent() = load(BUY_SELL_KEY, BuySellContent::class.java)

    @JvmStatic
    fun getLanguage(): String? = sharedPreferences.getString(Constants.LANGUAGE_SETTING, Locale.getDefault().language)
    @JvmStatic
    fun getPartnerInfos(): List<PartnerInfo> = mutableListOf<PartnerInfo>().apply {
        sharedPreferences.all.filter { it.key.startsWith("${PARTNER_KEY}-") }.forEach {
            add(gson.fromJson(it.value.toString(), PartnerInfo::class.java))
        }
    }

    private fun getPartnerInfo(id: String): PartnerInfo? {
        val string = sharedPreferences.getString("${PARTNER_KEY}-$id", null) ?: return null
        return gson.fromJson(string, PartnerInfo::class.java)
    }

    @JvmStatic
    fun isEnabled(partnerInfoId: String): Boolean = sharedPreferences.getBoolean(
        "${PARTNER_ENABLED}-${partnerInfoId}",
        if (partnerInfoId == BequantConstants.PARTNER_ID) BequantPreference.isLogged() else true
    )

    fun setEnabled(partnerInfoId: String, enable: Boolean) {
        sharedPreferences.edit().putBoolean("${PARTNER_ENABLED}-${partnerInfoId}", enable).apply()
    }

    @JvmStatic
    fun isContentEnabled(id: String?): Boolean = id == null ||
            getPartnerInfo(id)?.isActive() ?: true && isEnabled(id)
}
