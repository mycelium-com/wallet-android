package com.mycelium.bequant

import android.content.Context
import com.mycelium.wallet.WalletApplication


object BequantPreference {
    val preference by lazy { WalletApplication.getInstance().getSharedPreferences("bequant_main", Context.MODE_PRIVATE) }

    fun setSession(session: String) {
        preference.edit().putString("session", session).apply()
    }

    fun getSession() = preference.getString("session", null) ?: ""
}