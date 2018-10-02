package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.model.Transaction
import com.mycelium.wapi.wallet.SendRequest
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value


class ColuSendRequest(type: CryptoCurrency?, val destination: BtcAddress, val amount: Value)
    : SendRequest<ColuTransaction>(type) {
    var txHex: String? = null

    fun setTransaction(tx: Transaction) {
        this.tx = ColuTransaction(tx.id, this.type, Value.zeroValue(type), Value.zeroValue(type)
                , 0, tx, 0, false
                , listOf(), listOf())
    }
}