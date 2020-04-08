package com.mycelium.bequant.remote.model


class Auth(val email: String,
           val password: String,
           val otpCode: String,
           val othBackupPassword: String) {
}