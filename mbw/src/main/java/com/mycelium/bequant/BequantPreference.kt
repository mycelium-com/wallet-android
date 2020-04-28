package com.mycelium.bequant

import android.content.Context
import com.mycelium.wallet.WalletApplication


object BequantPreference {
    val preference by lazy { WalletApplication.getInstance().getSharedPreferences("bequant_main", Context.MODE_PRIVATE) }

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

    // TODO mybe should be linked to private/public key (/api-key)
    fun isLogged(): Boolean = getSession().isNotEmpty()


    fun clear() {
        preference.edit().clear().apply()
    }

    fun setApiKeys(privateKey: String?, publicKey: String?) {
        preference.edit()
                .putString(Constants.PRIVATE_KEY, privateKey)
                .putString(Constants.PUBLIC_KEY, publicKey)
                .apply()
    }
}