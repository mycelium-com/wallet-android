package com.mycelium.wallet.external.changelly.model

import java.io.Serializable


class ChangellyTransaction(val id: String,
                           val status: String,
                           val moneySent: String,
                           val currencyFrom: String,
                           val moneyReceived: String,
                           val currencyTo: String,
                           val createdAt:Long) : Serializable