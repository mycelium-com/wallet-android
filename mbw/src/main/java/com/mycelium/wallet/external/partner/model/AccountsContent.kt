package com.mycelium.wallet.external.partner.model

import com.google.gson.annotations.SerializedName


data class AccountsContent(@SerializedName("banner-top") val bannersTop: List<MediaFlowBannerBannerTop>)

data class AccountsBannerTop(val imageUrl: String, val link: String) : CommonContent()
