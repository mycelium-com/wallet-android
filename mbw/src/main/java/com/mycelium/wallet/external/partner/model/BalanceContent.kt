package com.mycelium.wallet.external.partner.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class BalanceContent(@SerializedName("buy-sell-buttons") val buttons: List<BuySellButton>)

data class BuySellButton(val name: String?,
                         val iconUrl: String?,
                         val link: String?,
                         val parentId: String?,
                         val index:Int?,
                         val isEnabled: Boolean = true) : Serializable