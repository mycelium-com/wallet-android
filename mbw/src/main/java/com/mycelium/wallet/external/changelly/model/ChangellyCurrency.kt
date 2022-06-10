package com.mycelium.wallet.external.changelly.model


class ChangellyCurrency(val currency: String? = null,
                        val ticker: String,
                        val enabled: Boolean = false,
                        val fixRateEnabled: Boolean = false)