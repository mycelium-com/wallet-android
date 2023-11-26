package com.mycelium.wallet.external.changelly.model

import java.math.BigDecimal


data class FixRateForAmount(val id: String,
                            val result: BigDecimal,
                            val from: String,
                            val to: String,
                            val amountFrom: BigDecimal,
                            val amountTo: BigDecimal)

