package com.mycelium.wallet.external.vip.model

import com.google.gson.annotations.SerializedName

data class ActivateVipRequest(
    @field:SerializedName("vip_code")
    val vipCode: String
)
