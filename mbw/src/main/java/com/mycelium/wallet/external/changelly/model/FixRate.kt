package com.mycelium.wallet.external.changelly.model

import java.math.BigDecimal

data class FixRate(
    val id: String,
    val result: BigDecimal,
    val from: String,
    val to: String,
    val maxFrom: BigDecimal,
    val maxTo: BigDecimal,
    val minFrom: BigDecimal,
    val minTo: BigDecimal,
    val amountFrom: BigDecimal,
    val amountTo: BigDecimal,
)
