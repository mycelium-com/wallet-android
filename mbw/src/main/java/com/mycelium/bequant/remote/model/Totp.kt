package com.mycelium.bequant.remote.model

import com.fasterxml.jackson.annotation.JsonProperty

class TotpConfirmResponse(val code: Int,
                          val message: String)

class TotpListResponse(val data: List<TotpListItem>)

class TotpListItem(val created: Int,
                   val name: String,
                   @JsonProperty("otp_id") val otpId: Int)
