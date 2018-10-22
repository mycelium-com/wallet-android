package com.mycelium.wapi.wallet

import com.mrd.bitlib.model.AddressType
import com.mycelium.wapi.wallet.coins.CryptoCurrency

import java.io.Serializable

interface GenericAddress : Serializable {
    val coinType: CryptoCurrency
    val type: AddressType
    val id: Long
}
