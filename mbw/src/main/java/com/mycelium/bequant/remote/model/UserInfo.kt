package com.mycelium.bequant.remote.model

import com.mrd.bitlib.util.HashUtils
import com.mycelium.bequant.BequantPreference
import java.util.*


data class UserInfo(val userId: String,
                    val event: BequantUserEvent,
                    val kycStatus: KYCStatus? = null,
                    val timeStamp: String = Date().time.toString())

enum class BequantUserEvent {
    REGISTRATION_COMPLETED,
    SIGNIN,
    EMAIL_CONFIRMED,
    TWO_FACTOR_SETUP_DONE,
    KYC_STATUS_CHANGE;

    fun sendInfo(info: UserInfo) {
        // Here the data should be sent
    }

    fun track() {
        val hashedEmail = HashUtils.doubleSha256(BequantPreference.getEmail().toByteArray()).toHex()
        sendInfo(UserInfo(hashedEmail,
                        this,
                        if (this == KYC_STATUS_CHANGE) BequantPreference.getKYCStatus() else null));
    }
}