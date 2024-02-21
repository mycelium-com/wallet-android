package com.mycelium.wallet.external.changelly.model

import java.math.BigDecimal


data class FixRateForAmount(val id: String,
                            val result: BigDecimal,
                            val networkFee: BigDecimal,
                            val from: String,
                            val to: String,
                            val amountFrom: BigDecimal,
                            val amountTo: BigDecimal,
                            val max: BigDecimal,
                            val maxFrom:BigDecimal,
                            val maxTo:BigDecimal,
                            val min:BigDecimal,
                            val minFrom:BigDecimal,
                            val minTo:BigDecimal)

