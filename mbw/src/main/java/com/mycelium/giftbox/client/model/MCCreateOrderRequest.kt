package com.mycelium.giftbox.client.model


import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

/**
 * Parameters to create new order
 */

//"user_id": "123456",
//"store_identifier": "airlinegift-us",
//"face_value": 1.0,
//"payment_currency": "BTC",
//"fiat_currency": "USD",
data class MCCreateOrderRequest(
    @JsonProperty("user_id")
    var userId: String,
    @JsonProperty("store_identifier")
    var storeIdentifier: String,
    @JsonProperty("face_value")
    var faceValue: BigDecimal,
    @JsonProperty("payment_currency")
    var paymentCurrency: String,
    @JsonProperty("fiat_currency")
    var fiatCurrency: String
)

