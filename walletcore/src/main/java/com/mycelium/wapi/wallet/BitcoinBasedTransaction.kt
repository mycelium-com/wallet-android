package com.mycelium.wapi.wallet

import com.mrd.bitlib.UnsignedTransaction
import com.mrd.bitlib.model.BitcoinTransaction
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value

abstract class BitcoinBasedTransaction protected constructor(type: CryptoCurrency, feePerKb: Value?) : Transaction(type) {
    override fun txBytes(): ByteArray? {
        return tx?.toBytes()
    }

    override fun getId(): ByteArray? {
        return tx?.id!!.bytes
    }
    var tx: BitcoinTransaction? = null
    var unsignedTx: UnsignedTransaction? = null

}
