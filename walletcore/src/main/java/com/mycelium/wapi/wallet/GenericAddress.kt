package com.mycelium.wapi.wallet

import com.mycelium.wapi.wallet.coins.CryptoCurrency

import java.io.Serializable

interface GenericAddress : Serializable {
    val coinType: CryptoCurrency
    val id: Long

    fun getBytes():ByteArray
}
