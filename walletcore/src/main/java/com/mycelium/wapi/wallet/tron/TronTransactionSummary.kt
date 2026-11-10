package com.mycelium.wapi.wallet.tron

import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.ConfirmationRiskProfileLocal
import com.mycelium.wapi.wallet.InputViewModel
import com.mycelium.wapi.wallet.OutputViewModel
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import java.math.BigInteger

class TronTransactionSummary(
    val sender: TronAddress,
    val receiver: TronAddress,
    val value: Value,
    val bandwidth: BigInteger,
    val energy: BigInteger,
    val feeLimit: BigInteger,
    val hasTokenTransfers: Boolean,
    val success: Boolean,
    /** TRON operation classification (transfer, stake, vote, contract call, …). */
    val tronTxType: TronTxType,
    type: CryptoCurrency,
    id: ByteArray,
    hash: ByteArray,
    transferred: Value,
    timestamp: Long,
    height: Int,
    confirmations: Int,
    isQueuedOutgoing: Boolean,
    inputs: List<InputViewModel>,
    outputs: List<OutputViewModel>,
    destinationAddresses: List<Address>,
    risk: ConfirmationRiskProfileLocal?,
    rawSize: Int,
    fee: Value?
) : TransactionSummary(
    type, id, hash, transferred, timestamp, height, confirmations,
    isQueuedOutgoing, inputs, outputs, destinationAddresses, risk, rawSize, fee
) {
    /** True when this transaction is an energy-service fee payment (token sent to the partner's fee wallet). */
    var isEnergyServiceFee: Boolean = false

    /** The txid of the main transfer this fee payment accompanies. */
    var parentTxId: String? = null
}
