package com.mycelium.bequant.remote.model

import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.mrd.bitlib.util.HashUtils
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.Constants
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

    val db = Firebase.database.getReference(Constants.DB_COLLECTION)
            .child(Constants.DB_DOCUMENT_USERS)

    fun track() {
        val hashedEmail = HashUtils.doubleSha256(BequantPreference.getEmail().toByteArray()).toHex()
        db.push().setValue(UserInfo(hashedEmail,
                        this,
                        if (this == KYC_STATUS_CHANGE) BequantPreference.getKYCStatus() else null));
    }
}