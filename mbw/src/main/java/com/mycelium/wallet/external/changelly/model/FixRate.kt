package com.mycelium.wallet.external.changelly.model

import com.mycelium.wallet.external.changelly2.ExchangeFragment.Companion.CHANGELLY_TERM_OF_USER
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
    val amountFrom: BigDecimal?,
    val amountTo: BigDecimal?,
    val networkFee: BigDecimal?,
    val provider: String? = null,
    val termsOfUseLink: String? = CHANGELLY_TERM_OF_USER
)
