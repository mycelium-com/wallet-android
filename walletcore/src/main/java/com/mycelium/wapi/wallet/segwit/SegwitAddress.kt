package com.mycelium.wapi.wallet.segwit

import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.BitcoinMain
import com.mycelium.wapi.wallet.coins.BitcoinTest

class SegwitAddress(val address: com.mrd.bitlib.model.SegwitAddress): BtcAddress {
    override val bip32Path = address.bip32Path
    override val type = address.type
    override val coinType = if (address.isProdnet) BitcoinMain.get() else BitcoinTest.get()
    override val id = 0L

    override fun toString(): String {
        return address.toString()
    }
}