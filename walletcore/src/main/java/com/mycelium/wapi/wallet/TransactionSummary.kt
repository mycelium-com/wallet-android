package com.mycelium.wapi.wallet

import com.google.common.base.Optional
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.eth.EthAccount
import java.io.Serializable

open class TransactionSummary(var type: CryptoCurrency,
                              var id: ByteArray, protected var hash: ByteArray,
                              var transferred: Value,
                              var timestamp: Long,
                              var height: Int,
                              var confirmations: Int,
                              var isQueuedOutgoing: Boolean,
                              var inputs: List<InputViewModel>,
                              var outputs: List<OutputViewModel>,
                              var destinationAddresses: List<Address>,
                              risk: ConfirmationRiskProfileLocal?,
                              rawSize: Int, fee: Value?) : Serializable,
    Comparable<TransactionSummary> {
    var rawSize: Int
        protected set

    @Transient
    var confirmationRiskProfile: Optional<ConfirmationRiskProfileLocal> =
        Optional.fromNullable(risk)
    var fee: Value?
        protected set
    val isIncoming: Boolean
        get() = transferred.moreOrEqualThanZero()
    val idHex: String
        get() = HexUtils.toHex(id)

    init {
        this.rawSize = rawSize
        this.fee = fee
    }

//    fun getType(): AssetInfo {
//        return type
//    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val other = o as TransactionSummary
        return id.contentEquals(other.id)
    }

    override fun toString(): String {
        return idHex
    }

    fun canCancel(): Boolean {
        return isQueuedOutgoing
    }

    override fun compareTo(other: TransactionSummary): Int {
        // TODO: Fix block heights! Currently the block heights are calculated as latest block height - confirmations + 1 but as it's not atomically collecting all the data, we run off by one frequently for transactions that get synced during a block being discovered.

        // Blockchains core property is that they determine the sorting of transactions.
        // In Bitcoin, timestamps of transactions are not required to be increasing, so the
        // block height is what has to be sorted by for transactions from different blocks.
        // if (other.getHeight() != getHeight()) {
        //     return other.getHeight() - getHeight();
        // }

        // If no block height is available (alt coins?), we do sort by timestamp.
        return if (other.timestamp != timestamp) {
            (other.timestamp - timestamp).toInt()
        } else {
            // Transactions are sorted within a block, too. Here we don't have that sequence number
            // handy but to ensure stable sorting, we have to sort by something robust.
            idHex.compareTo(other.idHex)
        }
    }
}

fun TransactionSummary.isMinerFeeTx(account: WalletAccount<*>): Boolean =
    (this as? EthTransactionSummary)?.run {
        account is EthAccount && !isIncoming
                && value.isZero() && hasTokenTransfers
    } ?: false