package com.mycelium.bequant.remote.model

import java.io.Serializable


class Register(val email: String,
               val password: String) : Serializable {
}

class RegisterResponse(val code: Int,
                       val message: String) {
}