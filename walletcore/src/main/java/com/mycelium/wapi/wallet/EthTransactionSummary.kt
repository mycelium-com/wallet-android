package com.mycelium.wapi.wallet

import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.EthAddress
import java.math.BigInteger

// TODO change val gasLimit: Long, val gasUsed: Long to BigInteger
// TODO make nonce default 0 in 1.sqm and in EthAccountBacking.sq
class EthTransactionSummary(val sender: EthAddress, val receiver: EthAddress, val nonce: BigInteger,
                            val value: Value, val gasLimit: Long, val gasUsed: Long,
                            type: CryptoCurrency?, id: ByteArray?, hash: ByteArray?,
                            transferred: Value?, timestamp: Long, height: Int, confirmations: Int,
                            isQueuedOutgoing: Boolean, inputs: List<GenericInputViewModel>?,
                            outputs: List<GenericOutputViewModel>?,
                            destinationAddresses: List<GenericAddress>?,
                            risk: ConfirmationRiskProfileLocal?, rawSize: Int, fee: Value?)
    : GenericTransactionSummary(type, id, hash, transferred, timestamp, height, confirmations,
        isQueuedOutgoing, inputs, outputs, destinationAddresses, risk, rawSize, fee)