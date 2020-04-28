package com.mycelium.bequant.remote.model


class BequantBalance(val currency: String,
                     val available: String,
                     val reserved: String)

class DepositAddress(val address: String,
                     val paymentId: Long?,
                     val publicKey: String?)