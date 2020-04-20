package com.mycelium.bequant.remote.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable


class Register(val email: String,
               val password: String) : Serializable

class RegisterResponse(code: Int, message: String) : BequantResponse(code, message)

class Email(val email: String)

class PasswordSet(@JsonProperty("new_password") val newPassword: String,
                  val token: String)

class PasswordUpdate(@JsonProperty("old_password") val oldPassword: String,
                     @JsonProperty("new_password") val newPassword: String)