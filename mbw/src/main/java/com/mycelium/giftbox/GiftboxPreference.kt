package com.mycelium.giftbox

import android.content.Context
import android.content.SharedPreferences
import com.mycelium.giftbox.client.models.Order
import com.mycelium.wallet.WalletApplication


object GiftboxPreference {
    private val preference: SharedPreferences by lazy { WalletApplication.getInstance().getSharedPreferences("giftbox_main", Context.MODE_PRIVATE) }

    fun redeem(order: Order) {
        val redeemSet = preference.getStringSet("redeemed_set", setOf())!!.toMutableSet()
        redeemSet.add(order.clientOrderId)
        preference.edit().putStringSet("redeemed_set", redeemSet).apply()
    }

    fun isRedeemed(order: Order) =
            preference.getStringSet("redeemed_set", setOf())!!.contains(order.clientOrderId)

    fun setGroupOpen(group: String, flag: Boolean) {
        preference.edit().putBoolean(group, flag).apply()
    }

    fun isGroupOpen(group: String): Boolean =
            preference.getBoolean(group, true)
}