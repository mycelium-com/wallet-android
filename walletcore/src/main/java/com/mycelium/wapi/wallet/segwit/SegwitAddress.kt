package com.mycelium.wapi.wallet.segwit

import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency

class SegwitAddress(override val coinType: CryptoCurrency,
                    override val address: com.mrd.bitlib.model.SegwitAddress): BtcAddress {
    override val bip32Path = address.bip32Path
    override val type = address.type
    override val id = 0L

    override fun toString(): String {
        return address.toString()
    }

    override fun getBytes(): ByteArray = address.allAddressBytes
}