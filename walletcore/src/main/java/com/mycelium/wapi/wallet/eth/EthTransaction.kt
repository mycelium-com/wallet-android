package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import org.web3j.tx.Transfer
import java.math.BigInteger


class EthTransaction(type: CryptoCurrency, val toAddress: String, val ethValue: Value, val gasPrice: BigInteger,
                     val nonce: BigInteger, val gasLimit: BigInteger, val inputData: String,
                     val estimatedGasUsed: Int = Transfer.GAS_LIMIT.toInt(), val tokenValue: Value? = null,
                     // EIP-1559 (type-2) fee fields. When both are set the tx is
                     // signed as type-2 with `gasPrice` ignored; otherwise it
                     // falls back to the legacy (type-0) `gasPrice` path.
                     val maxFeePerGas: BigInteger? = null,
                     val maxPriorityFeePerGas: BigInteger? = null) : Transaction(type) {
    var signedHex: String? = null
    var txHash: ByteArray? = null
    var txBinary: ByteArray? = null
    override fun getId() = txHash

    val isEip1559: Boolean
        get() = maxFeePerGas != null && maxPriorityFeePerGas != null

    /** Effective per-gas price used for fee accounting: the cap for type-2, else the legacy gas price. */
    val effectiveGasPrice: BigInteger
        get() = maxFeePerGas ?: gasPrice

    override fun txBytes() = txBinary
    override fun totalFee(): Value = Value.valueOf(type, effectiveGasPrice.times(estimatedTransactionSize.toBigInteger()))

    override fun getEstimatedTransactionSize() = gasLimit.toInt()
}
