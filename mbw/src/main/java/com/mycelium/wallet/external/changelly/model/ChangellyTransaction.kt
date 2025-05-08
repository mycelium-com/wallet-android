package com.mycelium.wallet.external.changelly.model

import java.io.Serializable
import java.math.BigDecimal
import java.util.Date

class ChangellyTransaction(
    val id: String,
    val status: String,
    val moneySent: String,
    val amountExpectedFrom: BigDecimal? = null,
    val amountExpectedTo: BigDecimal? = null,
    val networkFee: BigDecimal? = null,
    val currencyFrom: String,
    val moneyReceived: String,
    val currencyTo: String,
    val payoutAddress: String,
    val createdAt: Date,
) : Serializable
