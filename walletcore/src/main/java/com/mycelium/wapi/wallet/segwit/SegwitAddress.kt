package com.mycelium.wapi.wallet.segwit

import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.wapi.wallet.GenericAddress

import com.mycelium.wapi.wallet.coins.BitcoinMain
import com.mycelium.wapi.wallet.coins.BitcoinTest

class SegwitAddress(val address: com.mrd.bitlib.model.SegwitAddress): GenericAddress {

    override fun getCoinType() = if (address.isProdnet) BitcoinMain.get() else BitcoinTest.get()


    override fun getType(): AddressType? {
        return address.type
    }

    override fun getId(): Long {
        return 0
    }

    override fun getBip32Path(): HdKeyPath? {
        return address.bip32Path
    }

    override fun toString(): String {
        return address.toString()
    }
}