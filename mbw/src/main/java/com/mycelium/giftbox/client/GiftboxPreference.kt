package com.mycelium.giftbox.client

import android.content.Context
import android.content.SharedPreferences
import com.mycelium.wallet.WalletApplication


object GiftboxPreference {
    private val preference: SharedPreferences by lazy {
        WalletApplication.getInstance().getSharedPreferences("giftbox_main", Context.MODE_PRIVATE)
    }

//    fun setAccessToken(accessToken: String) {
//        preference.edit().putString(Constants.ACCESS_TOKEN_KEY, accessToken).apply()
//    }
//
//    fun getAccessToken() = preference.getString(Constants.ACCESS_TOKEN_KEY, Constants.API_KEY) ?: ""

}
