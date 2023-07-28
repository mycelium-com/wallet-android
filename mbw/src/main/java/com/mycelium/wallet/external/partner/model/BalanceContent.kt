package com.mycelium.wallet.external.partner.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class BalanceContent(@SerializedName("buy-sell-buttons") val buttons: List<BuySellButton>,
                          @SerializedName("buy-sell-quads") val quads: List<BuySellButton>)

data class BuySellButton(val name: String?,
                         val iconUrl: String?,
                         val link: String?,
                         val index: Int?) : CommonContent(), Serializable