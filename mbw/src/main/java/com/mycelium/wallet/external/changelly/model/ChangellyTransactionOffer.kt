package com.mycelium.wallet.external.changelly.model

import java.io.Serializable
import java.math.BigDecimal


class ChangellyTransactionOffer : Serializable {
    @JvmField
    var id: String? = null
    var apiExtraFee = 0.0
    var changellyFee = 0.0
    @JvmField
    var payinExtraId: String? = null
    var status: String? = null
    @JvmField
    var currencyFrom: String? = null
    var currencyTo: String? = null
    @JvmField
    var amountTo:BigDecimal = BigDecimal.ZERO
    @JvmField
    var payinAddress: String? = null
    var payoutAddress: String? = null
    var payoutExtraId: String? = null
    var createdAt: String? = null

    val amountExpectedFrom:BigDecimal = BigDecimal.ZERO
}