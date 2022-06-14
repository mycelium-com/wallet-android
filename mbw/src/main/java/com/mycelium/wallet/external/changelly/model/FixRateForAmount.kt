package com.mycelium.wallet.external.changelly.model


data class FixRateForAmount(val id: String,
                            val rate: Double,
                            val from: String,
                            val to: String,
                            val amountFrom: Double,
                            val amountTo: Double)

