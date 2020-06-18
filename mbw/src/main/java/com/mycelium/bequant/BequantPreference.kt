package com.mycelium.bequant

import android.content.Context
import com.mycelium.wallet.Utils
import com.mycelium.wallet.WalletApplication
import com.mycelium.wapi.wallet.coins.Value


object BequantPreference {
    val preference by lazy { WalletApplication.getInstance().getSharedPreferences("bequant_main", Context.MODE_PRIVATE) }

    fun setPhone(phone: String) {
        preference.edit().putString(Constants.PHONE_KEY, phone).apply()
    }

    fun getPhone() = preference.getString(Constants.PHONE_KEY, null) ?: ""

    fun setEmail(email: String) {
        preference.edit().putString(Constants.EMAIL_KEY, email).apply()
    }

    fun getEmail() = preference.getString(Constants.EMAIL_KEY, null) ?: ""

    fun setAccessToken(accessToken: String) {
        preference.edit().putString(Constants.ACCESS_TOKEN_KEY, accessToken).apply()
    }

    fun getAccessToken() = preference.getString(Constants.ACCESS_TOKEN_KEY, null) ?: ""

    fun setSession(session: String) {
        preference.edit().putString(Constants.SESSION_KEY, session).apply()
    }

    fun getSession() = preference.getString(Constants.SESSION_KEY, null) ?: ""

    // TODO maybe should be linked to private/public key (/api-key)
    fun isLogged(): Boolean = getPrivateKey().isNotEmpty()

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

    fun setMockCastodialBalance(value: Value) {
        preference.edit().putString("balance", value.valueAsBigDecimal.toString()).apply()
    }

    fun getMockCastodialBalance() = Value.parse(Utils.getBtcCoinType(),
            preference.getString("balance", "0") ?: "0")
}
