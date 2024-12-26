package com.mycelium.giftbox.client.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonProperty
import com.mycelium.giftbox.client.models.Status
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.util.Date

//"order_id": "12345",
//"status": "pending",
//"payment_data": {
//"payment_address": "mrfarzXwapEMQKENi7JVWpWST46S73caBQ",
//"payment_amount": 1.49e-05,
//"payment_currency": "BTC",
//"order_id": "12345"
//},
//"card_url": null,
//"card_code": null
@Parcelize
data class MCOrderResponse(
    @JsonProperty("order_id")
    override var orderId: String,
    @JsonProperty("status")
    override var status: Status,

    @JsonProperty("card_url")
    var cardUrl: String? = null,
    @JsonProperty("card_code")
    var cardCode: String? = null,
    @JsonProperty("card_pin")
    var cardPin: String? = null,

    @JsonProperty("brand_info")
    var product: MCProductInfo? = null,

    @JsonProperty("order_date")
    var createdDate: Date? = null,
    @JsonProperty("fiat_amount")
    var faceValue: BigDecimal? = null,

    @JsonProperty("payment_address")
    var paymentAddress: String? = null,
    @JsonProperty("payment_amount")
    var paymentAmount: BigDecimal? = null,
    @JsonProperty("payment_currency")
    var paymentCurrency: String? = null,

    @JsonProperty("expiration_time")
    val expireTime: Int,

    var quantity: BigDecimal = BigDecimal.ONE
) : MCOrderCommon, Parcelable