package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import org.web3j.tx.Transfer
import java.math.BigInteger


class EthTransaction(val type: CryptoCurrency, val toAddress: String, val value: Value, val gasPrice: BigInteger,
                     val nonce: BigInteger, val gasLimit: BigInteger, val inputData: String) : Transaction(type) {
    var signedHex: String? = null
    var txHash: ByteArray? = null
    var txBinary: ByteArray? = null
    override fun getId() = txHash

    override fun txBytes() = txBinary

    // This only true for pure ETH transaction, without contracts.
    override fun getEstimatedTransactionSize() = Transfer.GAS_LIMIT.toInt()
}
