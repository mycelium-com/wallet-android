package com.mycelium.bequant.remote.model

import com.fasterxml.jackson.annotation.JsonProperty

class TotpCreate(@JsonProperty("backup_password") val backupPassword: String,
                 val passcode: String)

class TotpCreateResponse(code: Int?,
                         message: String?,
                         @JsonProperty("backup_password") val backupPassword: String?,
                         @JsonProperty("otp_id") val otpId: Int?,
                         @JsonProperty("otp_link") val otpLink: String?) : BequantResponse(code, message)

class TotpActivate(@JsonProperty("otp_id") val otpId: Int,
                   val passcode: String)

class TotpConfirmResponse(val code: Int,
                          val message: String)

class TotpListResponse(val data: List<TotpListItem>)

class TotpListItem(val created: Int,
                   val name: String,
                   @JsonProperty("otp_id") val otpId: Int,
                   val status: String)
