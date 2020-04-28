package com.mycelium.bequant.remote.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable


class Auth(val email: String,
           val password: String,
           @JsonInclude(JsonInclude.Include.NON_NULL)
           @JsonProperty("otp_code")
           var otpCode: String? = null,
           @JsonInclude(JsonInclude.Include.NON_NULL)
           @JsonProperty("otp_backup_password")
           var otp_backup_password: String? = null) : Serializable

class AuthResponse(val issues: String?,
                   val session: String?,
                   @JsonProperty("access_token") val accessToken: String?) {
}

class ApiKeyResponse(code: Int?,
                     message: String?,
                     @JsonProperty("private_key")
                     val privateKey: String?,
                     @JsonProperty("public_key")
                     val publicKey: String?) : BequantResponse(code, message) {
}