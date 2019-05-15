package com.mycelium.wapi.wallet

import com.mrd.bitlib.UnsignedTransaction
import com.mrd.bitlib.model.Transaction
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value

abstract class BitcoinBasedGenericTransaction protected constructor(type: CryptoCurrency, feePerKb: Value?) : GenericTransaction(type) {
    override fun txBytes(): ByteArray? {
        return tx?.toBytes()
    }

    override fun getId(): Sha256Hash? {
        return tx?.id
    }
    var tx: Transaction? = null
    var unsignedTx: UnsignedTransaction? = null

}
