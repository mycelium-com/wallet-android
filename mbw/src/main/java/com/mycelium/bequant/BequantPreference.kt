package com.mycelium.bequant

import android.content.Context
import com.mycelium.wallet.WalletApplication


object BequantPreference {
    val preference by lazy { WalletApplication.getInstance().getSharedPreferences("bequant_main", Context.MODE_PRIVATE) }

    fun setAccessToken(accessToken: String) {
        preference.edit().putString(Constants.ACCESS_TOKEN_KEY, accessToken).apply()
    }

    fun getAccessToken() = preference.getString(Constants.ACCESS_TOKEN_KEY, null) ?: ""

    fun setSession(session: String) {
        preference.edit().putString(Constants.SESSION_KEY, session).apply()
    }

    fun getSession() = preference.getString(Constants.SESSION_KEY, null) ?: ""
}