package com.mycelium.bequant.remote.model

import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.Constants
import java.util.*


data class UserInfo(val email: String,
                    val status: UserStatus,
                    val kycStatus: KYCStatus,
                    val updateDate: Date = Date())

enum class UserStatus {
    SINGUP_START,
    SIGNIN,
    SINGUP,
    TWO_FACTOR,
    KYC_STATUS_CHANGED;

    val db = Firebase.database.getReference(Constants.DB_COLLECTION)
            .child(Constants.DB_DOCUMENT_USERS)

    fun track() {
        db.child(BequantPreference.getEmail().replace('.', '_'))
                .setValue(UserInfo(BequantPreference.getEmail(),
                        this,
                        BequantPreference.getKYCStatus()))
    }
}