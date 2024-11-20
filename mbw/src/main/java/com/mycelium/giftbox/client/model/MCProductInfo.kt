package com.mycelium.giftbox.client.model


import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonProperty
import com.mycelium.wallet.R
import com.mycelium.wallet.WalletApplication
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

// "identifier": "b-and-m",
//"name": "B&M",
//"currency": "GBP",
//"countries_served": ["GB"],
//"min_face_value": 10.0,
//"max_face_value": 1000.0,
//"stock_status": "not supported",
//"logo_url": "https://assets.tillo.dev/images/brands/logos/180x180/b-and-m.jpg",
//"gift_card_url": "https://assets.tillo.dev/images/brands/vouchers/330x214/b-and-m.jpg"

@Parcelize
data class MCProductInfo(
    @JsonProperty("identifier")
    var id: String? = null,

    @JsonProperty("name")
    var name: String? = null,

    @JsonProperty("description")
    var description: String? = null,

    @JsonProperty("currency")
    var currency: String? = null,

    @JsonProperty("countries_served")
    var countries: List<String>? = null,

    @JsonProperty("categories")
    var categories: String? = null,

    @JsonProperty("min_face_value")
    var minFaceValue: BigDecimal = BigDecimal.ZERO,

    @JsonProperty("max_face_value")
    var maxFaceValue: BigDecimal = BigDecimal.TEN.pow(10),

    @JsonProperty("denominations")
    var denominations: List<BigDecimal>? = null,

    @JsonProperty("stock_status")
    var stockStatus: String? = null,

    @JsonProperty("logo_url")
    var logoUrl: String? = null,

    @JsonProperty("gift_card_url")
    var cardImageUrl: String? = null,

    @JsonProperty("expiry")
    var expiryData:String? = null

) : Parcelable

fun MCProductInfo.getCardValue(): String =
//    if (denominationType == ProductInfo.DenominationType.fixed && availableDenominations?.size ?: 100 < 6) {
//        availableDenominations!!.joinToString {
//            "${it.stripTrailingZeros().toPlainString()} $currencyCode"
//        }
//    } else {
        WalletApplication.getInstance().getString(
            R.string.from_s_to_s,
                "${minFaceValue?.stripTrailingZeros()?.toPlainString()} $currency",
                "${maxFaceValue?.stripTrailingZeros()?.toPlainString()} $currency")
//    }
