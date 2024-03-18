package com.mycelium.wallet.external.changelly.model

import java.io.Serializable
import java.math.BigDecimal


class ChangellyTransactionOffer : Serializable {
    @JvmField
    var id: String? = null
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

    var trackUrl: String? = null
    var type: String? = null
    var refundAddress: String? = null
    var refundExtraId: String? = null
}