package com.mycelium.bequant.remote.model


class BequantBalance(val currency: String,
                     var available: String,
                     val reserved: String)

class DepositAddress(val address: String,
                     val paymentId: Long?,
                     val publicKey: String?)


class Currency(var id: String = "",
               var fullName: String = "",
               var crypto: Boolean = true)

class Ticker(val symbol: String,
                     val last: Double?,
                     val open: Double?,
                     val volume: Double)
