package com.mycelium.wapi.wallet

import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.EthAddress
import java.math.BigInteger

class EthTransactionSummary(val sender: EthAddress, val receiver: EthAddress, val nonce: BigInteger?,
                            val value: Value, val internalValue: Value?, val gasLimit: BigInteger, val gasUsed: BigInteger,
                            val hasTokenTransfers: Boolean,
                            type: CryptoCurrency, id: ByteArray, hash: ByteArray,
                            transferred: Value, timestamp: Long, height: Int, confirmations: Int,
                            isQueuedOutgoing: Boolean, inputs: List<InputViewModel>,
                            outputs: List<OutputViewModel>,
                            destinationAddresses: List<Address>,
                            risk: ConfirmationRiskProfileLocal?, rawSize: Int, fee: Value?)
    : TransactionSummary(type, id, hash, transferred, timestamp, height, confirmations,
        isQueuedOutgoing, inputs, outputs, destinationAddresses, risk, rawSize, fee)