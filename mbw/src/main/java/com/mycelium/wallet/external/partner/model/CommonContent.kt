package com.mycelium.wallet.external.partner.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*


abstract class CommonContent(@SerializedName("start-date") val startDate: Date? = null,
                             @SerializedName("end-date") val endDate: Date? = null,
                             @SerializedName("isEnabled") val isEnabled: Boolean? = true) : Serializable {
    fun isActive() = isEnabled ?: true &&
            Date().let { startDate?.before(it) ?: true && endDate?.after(it) ?: true }
}