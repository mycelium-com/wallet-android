package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import org.web3j.tx.Transfer
import java.math.BigInteger


class EthTransaction(type: CryptoCurrency, val toAddress: String, val ethValue: Value, val gasPrice: BigInteger,
                     val nonce: BigInteger, val gasLimit: BigInteger, val inputData: String,
                     val estimatedGasUsed: Int = Transfer.GAS_LIMIT.toInt(), val tokenValue: Value? = null) : Transaction(type) {
    var signedHex: String? = null
    var txHash: ByteArray? = null
    var txBinary: ByteArray? = null
    override fun getId() = txHash

    override fun txBytes() = txBinary
    override fun totalFee(): Value = Value.valueOf(type, gasPrice.times(estimatedTransactionSize.toBigInteger()) )

    override fun getEstimatedTransactionSize() = gasLimit.toInt()
}
