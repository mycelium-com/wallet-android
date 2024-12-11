package com.mycelium.giftbox.client.model

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class MCPrice(
    @JsonProperty("exchange_rate")
    val rate: BigDecimal,
    @JsonProperty("amount_in_btc")
    val amount: BigDecimal
) : Parcelable
