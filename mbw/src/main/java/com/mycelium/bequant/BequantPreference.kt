package com.mycelium.bequant

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.mycelium.bequant.remote.model.KYCRequest
import com.mycelium.bequant.remote.model.KYCStatus
import com.mycelium.wallet.Utils
import com.mycelium.wallet.WalletApplication
import com.mycelium.wapi.wallet.coins.Value
import java.util.*


object BequantPreference {
    private val preference: SharedPreferences by lazy { WalletApplication.getInstance().getSharedPreferences("bequant_main", Context.MODE_PRIVATE) }

    fun setPhone(phone: String) {
        preference.edit().putString(Constants.PHONE_KEY, phone).apply()
    }

    fun getPhone() = preference.getString(Constants.PHONE_KEY, null) ?: ""

    fun setEmail(email: String) {
        preference.edit().putString(Constants.EMAIL_KEY, email).apply()
    }

    fun getEmail() = preference.getString(Constants.EMAIL_KEY, null) ?: ""

    fun getKYCToken() = preference.getString(Constants.KYC_UUID_KEY, null) ?: ""

    fun setKYCToken(uuid: String) {
        preference.edit().putString(Constants.KYC_UUID_KEY, uuid).apply()
    }

    fun setAccessToken(accessToken: String) {
        preference.edit().putString(Constants.ACCESS_TOKEN_KEY, accessToken).apply()
    }

    fun getAccessToken() = preference.getString(Constants.ACCESS_TOKEN_KEY, null) ?: ""

    fun setSession(session: String) {
        preference.edit().putString(Constants.SESSION_KEY, session).apply()
    }

    fun getSession() = preference.getString(Constants.SESSION_KEY, null) ?: ""

    @JvmStatic
    fun isLogged(): Boolean = getSession().isNotEmpty()

    fun isDemo(): Boolean = getAccessToken().isEmpty()

    fun isIntroShown() = preference.getBoolean(Constants.INTRO_KEY, false)

    fun setIntroShown() = preference.edit().putBoolean(Constants.INTRO_KEY, true).apply()

    fun clear() {
        preference.edit().clear().apply()
    }

    fun setApiKeys(privateKey: String?, publicKey: String?) {
        preference.edit()
                .putString(Constants.PRIVATE_KEY, privateKey)
                .putString(Constants.PUBLIC_KEY, publicKey)
                .apply()
    }

    fun getPublicKey(): String = preference.getString(Constants.PUBLIC_KEY, null) ?: ""

    fun getPrivateKey(): String = preference.getString(Constants.PRIVATE_KEY, null) ?: ""

    fun hasKeys(): Boolean = getPrivateKey().isNotEmpty()

    fun hideZeroBalance() = preference.getBoolean(Constants.HIDE_ZERO_BALANCE_KEY, false)

    fun setHideZeroBalance(checked: Boolean) {
        preference.edit().putBoolean(Constants.HIDE_ZERO_BALANCE_KEY, checked).apply()
    }

    fun setLastKnownBalance(value: Value) {
        preference.edit().putString("balance", value.valueAsBigDecimal.toString()).apply()
    }

    fun getLastKnownBalance() = Value.parse(Utils.getBtcCoinType(),
            preference.getString("balance", "0") ?: "0")

    fun getKYCRequest() =
            Gson().fromJson(preference.getString(Constants.KYC_REQUEST_KEY, null), KYCRequest::class.java)
                    ?: KYCRequest()

    fun setKYCRequest(request: KYCRequest) {
        preference.edit().putString(Constants.KYC_REQUEST_KEY, Gson().toJson(request)).apply()
    }

    fun getKYCStatus(): KYCStatus = KYCStatus.valueOf(preference.getString("kyc_status", "NONE")
            ?: "NONE")

    fun setKYCStatus(status: KYCStatus) {
        preference.edit().putString(Constants.KYC_STATUS_KEY, status.toString()).apply()
    }

    fun getKYCStatusMessage() = preference.getString(Constants.KYC_STATUS_MESSAGE_KEY, "")

    fun setKYCStatusMessage(message: String) {
        preference.edit().putString(Constants.KYC_STATUS_MESSAGE_KEY, message).apply()
    }

    fun setKYCSubmitDate(date: Date) {
        preference.edit().putLong("kyc_submit_date", date.time).apply()
    }

    fun getKYCSubmitDate(): Date = Date(preference.getLong("kyc_submit_date", 0))
}
