package com.mycelium.giftbox.client.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonProperty
import com.mycelium.giftbox.client.models.Status
import kotlinx.parcelize.Parcelize

//"order_id": "12345",
//"status": "completed",
//"card_url": "https://example.com/card/12345",
//"card_code": "ABCDEF123456"
@Parcelize
data class MCOrderStatusResponse(
    @JsonProperty("order_id")
    override var orderId: String,
    @JsonProperty("status")
    override var status: Status,
    @JsonProperty("card_url")
    var cardUrl: String? = null,
    @JsonProperty("card_code")
    var cardCode: String? = null,
    @JsonProperty("card_pin")
    var cardPin: String? = null
) : MCOrderCommon, Parcelable


interface MCOrderCommon {
    val orderId: String
    var status: Status
}