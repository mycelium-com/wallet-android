package com.mycelium.bequant.remote.model

import com.fasterxml.jackson.annotation.JsonProperty


class Auth(val email: String,
           val password: String,
           val otpCode: String,
           val othBackupPassword: String) {
}

class AuthResponse(val issues: String?,
                   val session: String?,
                   @JsonProperty("access_token") val accessToken: String?) {
}

class ApiKeyResponse(code: Int?,
                     message: String?,
                     @JsonProperty("private_key") privateKey: String?,
                     @JsonProperty("public_key") publicKey: String?) : BequantResponse(code, message) {
}