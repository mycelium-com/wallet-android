package com.mycelium.wallet.external.partner.model

import com.google.gson.annotations.SerializedName


data class BuySellContent(@SerializedName("list-item") val listItem: List<BuySellItem>,
                          @SerializedName("bankcard") val exchangeList: List<BuySellBackCardItem> )


data class BuySellItem(val title: String,
                       val description: String,
                       val iconUrl: String,
                       val link: String) : CommonContent()

data class BuySellBackCardItem(val title: String,
                               val description: String,
                               val iconUrl: String,
                               val counties: List<String>,
                               val link: String) : CommonContent()