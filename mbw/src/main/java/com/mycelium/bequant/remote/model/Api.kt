package com.mycelium.bequant.remote.model


class BequantBalance(val currency: String,
                     var available: String,
                     val reserved: String)


class Ticker(val symbol: String,
                     val last: Double?,
                     val open: Double?,
                     val volume: Double)
