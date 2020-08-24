package com.mycelium.wapi.wallet.fio

import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import java.math.BigInteger

class FioTransaction(val type: CryptoCurrency, val toAddress: String, val value: Value, val fee: BigInteger) : Transaction(type) {
    override fun getId(): ByteArray = ByteArray(0)

    override fun txBytes(): ByteArray = ByteArray(0)

    override fun getEstimatedTransactionSize(): Int = 0
}