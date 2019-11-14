package com.mycelium.wallet.external.partner.model

import com.google.gson.annotations.SerializedName


data class PartnersLocalized(@SerializedName("header-title") val title: String,
                             @SerializedName("header-text") val text: String,
                             @SerializedName("partners") val partners: List<Partner>)
