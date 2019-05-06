package com.mycelium.wapi.wallet

import com.mrd.bitlib.UnsignedTransaction
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value

abstract class BitcoinBasedSendRequest<T : GenericTransaction> protected constructor(type: CryptoCurrency, fee: Value?) : SendRequest<T>(type, fee) {

    var unsignedTx: UnsignedTransaction? = null

}
