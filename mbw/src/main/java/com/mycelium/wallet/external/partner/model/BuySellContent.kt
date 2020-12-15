package com.mycelium.wallet.external.partner.model

import com.google.gson.annotations.SerializedName


data class BuySellContent(@SerializedName("list-item") val listItem: List<BuySellItem>)


data class BuySellItem(val title: String,
                       val description: String,
                       val iconUrl: String,
                       val link: String) : CommonContent()