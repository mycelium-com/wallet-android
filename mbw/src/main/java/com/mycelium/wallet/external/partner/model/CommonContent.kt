package com.mycelium.wallet.external.partner.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*


abstract class CommonContent(@SerializedName("parentId") val parentId: String? = null,
                             @SerializedName("start-date") val startDate: Date? = null,
                             @SerializedName("end-date") val endDate: Date? = null,
                             @SerializedName("isEnabled") val isEnabled: Boolean? = true) : Serializable {
    fun isActive() = isEnabled != false &&
            Date().let { startDate?.before(it) != false && endDate?.after(it) != false }
}