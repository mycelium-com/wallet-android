package com.mycelium.wapi.wallet.genericdb

import com.mrd.bitlib.util.HexUtils
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.EthTransactionSummary
import com.mycelium.wapi.wallet.InputViewModel
import com.mycelium.wapi.wallet.OutputViewModel
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.EthAddress
import com.mycelium.wapi.wallet.eth.EthTxStatus
import com.mycelium.wapi.wallet.eth.Tx
import org.web3j.tx.Transfer
import java.math.BigInteger
import java.util.*

class EthAccountBacking(val walletDB: WalletDB, private val uuid: UUID, private val currency: CryptoCurrency, private val token: ERC20Token? = null) {
    private val ethQueries = walletDB.ethAccountBackingQueries
    private val queries = walletDB.accountBackingQueries
    private val contractCreationAddress = EthAddress(currency, "0x0000000000000000000000000000000000000000")

    fun getTransactionSummaries(offset: Long, limit: Long, ownerAddress: String): List<TransactionSummary> =
            ethQueries.selectTransactionSummaries(uuid, limit, offset, mapper = { txid: String,
                                                                                  currency: CryptoCurrency,
                                                                                  blockNumber: Int,
                                                                                  timestamp: Long,
                                                                                  value: Value,
                                                                                  fee: Value,
                                                                                  confirmations: Int,
                                                                                  from: String,
                                                                                  to: String,
                                                                                  nonce: BigInteger?,
                                                                                  gasLimit: BigInteger,
                                                                                  gasUsed: BigInteger,
                                                                                  gasPrice: BigInteger?,
                                                                                  success: Boolean,
                                                                                  internalValue: Value?,
                                                                                  hasTokenTransfers: Boolean,
                                                                                  status: String ->
                createTransactionSummary(ownerAddress, txid, currency, blockNumber, timestamp,
                        value, fee, confirmations, from, to, nonce, gasLimit, gasUsed, gasPrice, hasTokenTransfers, internalValue, success, EthTxStatus.parse(status))
            }).executeAsList()

    /**
     * @param timestampParameter time in seconds
     */
    fun getTransactionSummariesSince(timestampParameter: Long, ownerAddress: String): List<TransactionSummary> =
            ethQueries.selectTransactionSummariesSince(uuid, timestampParameter, mapper = { txid: String,
                                                                                            currency: CryptoCurrency,
                                                                                            blockNumber: Int,
                                                                                            timestamp: Long,
                                                                                            value: Value,
                                                                                            fee: Value,
                                                                                            confirmations: Int,
                                                                                            from: String,
                                                                                            to: String,
                                                                                            nonce: BigInteger?,
                                                                                            gasLimit: BigInteger,
                                                                                            gasUsed: BigInteger,
                                                                                            gasPrice: BigInteger?,
                                                                                            success: Boolean,
                                                                                            internalValue: Value?,
                                                                                            hasTokenTransfers: Boolean,
                                                                                            status: String ->
                createTransactionSummary(ownerAddress, txid, currency, blockNumber, timestamp,
                        value, fee, confirmations, from, to, nonce, gasLimit, gasUsed, gasPrice, hasTokenTransfers, internalValue, success, EthTxStatus.parse(status))
            }).executeAsList()

    fun getTransactionSummary(txidParameter: String, ownerAddress: String): TransactionSummary? =
            ethQueries.selectTransactionSummaryById(uuid, txidParameter, mapper = { txid: String,
                                                                                    currency: CryptoCurrency,
                                                                                    blockNumber: Int,
                                                                                    timestamp: Long,
                                                                                    value: Value,
                                                                                    fee: Value,
                                                                                    confirmations: Int,
                                                                                    from: String,
                                                                                    to: String,
                                                                                    nonce: BigInteger?,
                                                                                    gasLimit: BigInteger,
                                                                                    gasUsed: BigInteger,
                                                                                    gasPrice: BigInteger?,
                                                                                    success: Boolean,
                                                                                    internalValue: Value?,
                                                                                    hasTokenTransfers: Boolean,
                                                                                    status: String ->
                createTransactionSummary(ownerAddress, txid, currency, blockNumber, timestamp,
                        value, fee, confirmations, from, to, nonce, gasLimit, gasUsed, gasPrice, hasTokenTransfers, internalValue, success, EthTxStatus.parse(status))
            }).executeAsOneOrNull()

    /**
     * Returns rows where blockchain `confirmations = 0`. Includes terminal
     * states such as REPLACED/DROPPED — callers that only want live mempool
     * txs should additionally filter by `status == PENDING`.
     */
    fun getUnconfirmedTransactions(ownerAddress: String): List<EthTransactionSummary> =
            ethQueries.selectUnconfirmedTransactions(uuid, mapper = { txid: String,
                                                                      currency: CryptoCurrency,
                                                                      blockNumber: Int,
                                                                      timestamp: Long,
                                                                      value: Value,
                                                                      fee: Value,
                                                                      confirmations: Int,
                                                                      from: String,
                                                                      to: String,
                                                                      nonce: BigInteger?,
                                                                      gasLimit: BigInteger,
                                                                      gasUsed: BigInteger,
                                                                      gasPrice: BigInteger?,
                                                                      hasTokenTransfers: Boolean,
                                                                      status: String ->
                createTransactionSummary(ownerAddress, txid, currency, blockNumber, timestamp,
                        value, fee, confirmations, from, to, nonce, gasLimit, gasUsed, gasPrice, hasTokenTransfers,
                        status = EthTxStatus.parse(status))
            }).executeAsList()


    fun putTransaction(blockNumber: Int, timestamp: Long, txid: String, raw: String, from: String, to: String?, value: Value,
                       fee: Value, confirmations: Int, nonce: BigInteger, gasPrice: BigInteger, hasTokenTransfers: Boolean = false,
                       internalValue: Value? = null, success: Boolean = true,
                       gasLimit: BigInteger = Transfer.GAS_LIMIT, gasUsed: BigInteger? = null,
                       status: EthTxStatus? = null) {
        // Default status derives from on-chain facts. If the caller didn't
        // pass an explicit status, preserve any sticky terminal status that's
        // already in the row (CANCELED / REPLACED / DROPPED) — these are
        // local-truth markers and the next sync's INSERT OR REPLACE must not
        // resurrect a tx that the user has already finalized.
        val derivedStatus = when {
            confirmations > 0 && success -> EthTxStatus.CONFIRMED
            confirmations > 0 && !success -> EthTxStatus.FAILED
            else -> EthTxStatus.PENDING
        }
        val existingStatus = ethQueries.selectStatus(uuid, txid).executeAsOneOrNull()
            ?.let { EthTxStatus.parse(it) }
        val effectiveStatus = status
            ?: existingStatus?.takeIf { it in STICKY_STATUSES }
            ?: derivedStatus
        queries.insertTransaction(txid, uuid, currency, if (blockNumber == -1) Int.MAX_VALUE else blockNumber, timestamp, raw, value, fee, confirmations)
        ethQueries.insertTransaction(txid, uuid, from, to ?: contractCreationAddress.addressString,
            nonce, gasLimit, gasPrice, success, internalValue, hasTokenTransfers, effectiveStatus.name)
        if (gasUsed != null) {
            updateGasUsed(txid, gasUsed, fee)
        }
    }

    private companion object {
        // Local-only terminal statuses set via markTransactionStatus that
        // must survive a putTransaction upsert at the next sync.
        val STICKY_STATUSES = setOf(EthTxStatus.CANCELED, EthTxStatus.REPLACED, EthTxStatus.DROPPED)
    }

    fun putTransactions(remoteTransactions: List<Tx>, coinType: CryptoCurrency, typicalEstimatedTransactionSize: BigInteger) {
        walletDB.transaction {
            remoteTransactions.forEach { tx ->
                // For pending txs, blockbook returns gasUsed=0 (not null), so the
                // elvis operator on the original code computed fee = gasPrice * 0
                // = 0. Use gasLimit as the upper-bound estimate until the tx mines
                // and a real gasUsed is available.
                val gasForFee = tx.gasUsed?.takeIf { it.signum() > 0 } ?: tx.gasLimit
                putTransaction(tx.blockHeight.toInt(), tx.blockTime, tx.txid, "", tx.from, tx.to,
                        Value.valueOf(coinType, tx.value),
                        Value.valueOf(coinType, tx.gasPrice * gasForFee),
                        tx.confirmations.toInt(), tx.nonce, tx.gasPrice, tx.tokenTransfers.isNotEmpty(),
                        Value.valueOf(coinType, tx.internalValue ?: BigInteger.ZERO),
                        tx.success, tx.gasLimit, tx.gasUsed)
            }
        }
    }

    fun deleteAllAccountTransactions() {
        queries.deleteAllAccountTransactions(uuid)
    }

    fun deleteTransaction(txid: String) {
        queries.deleteTransaction(uuid, txid)
    }

    /**
     * Update only the local lifecycle status without touching any on-chain
     * fields. Used to mark txs REPLACED / CANCELED / DROPPED without losing
     * the historical record.
     */
    fun markTransactionStatus(txid: String, status: EthTxStatus) {
        ethQueries.updateStatus(status.name, uuid, txid)
    }

    private fun updateGasUsed(txid: String, gasUsed: BigInteger, fee: Value) {
        ethQueries.updateGasUsed(gasUsed, uuid, txid)
        queries.updateFee(fee, uuid, txid)
    }

    private fun createTransactionSummary(ownerAddress: String,
                                         txid: String,
                                         currency: CryptoCurrency,
                                         blockNumber: Int,
                                         timestamp: Long,
                                         value: Value,
                                         fee: Value,
                                         confirmations: Int,
                                         from: String,
                                         to: String,
                                         nonce: BigInteger?,
                                         gasLimit: BigInteger,
                                         gasUsed: BigInteger,
                                         gasPrice: BigInteger?,
                                         hasTokenTransfers: Boolean,
                                         internalValue: Value? = null,
                                         success: Boolean = true,
                                         status: EthTxStatus = EthTxStatus.PENDING): EthTransactionSummary {
        val convertedValue = if (token != null) transformValueFromDb(token, value) else value
        val inputs = listOf(InputViewModel(EthAddress(currency, from), value, false))
        // "to" address may be empty if we have a contract funding transaction
        val outputs = if (to.isEmpty()) {
            listOf()
        } else {
            listOf(OutputViewModel(EthAddress(currency, to), convertedValue, false))
        }
        val destAddresses = listOf(if (to.isEmpty()) contractCreationAddress else EthAddress(currency, to))
        // Treat REPLACED/CANCELED/DROPPED as "no funds moved on this tx"
        // for the purpose of the displayed transferred amount. The on-chain
        // value/fee of the *replacement* tx will be reflected by its own row.
        val effectiveSuccess = success && status != EthTxStatus.REPLACED && status != EthTxStatus.CANCELED && status != EthTxStatus.DROPPED
        val transferred = if (token != null) {
            if (status == EthTxStatus.REPLACED || status == EthTxStatus.CANCELED || status == EthTxStatus.DROPPED)
                Value.zeroValue(token)
            else getTokenTransferred(ownerAddress, from, to, convertedValue)
        } else getEthTransferred(ownerAddress, from, to, convertedValue, fee, effectiveSuccess, internalValue, hasTokenTransfers, status)
        return EthTransactionSummary(EthAddress(currency, from), EthAddress(currency, to), nonce,
                convertedValue, internalValue, gasLimit, gasUsed, gasPrice ?: (fee.value / gasUsed), hasTokenTransfers, status, currency, HexUtils.toBytes(txid.substring(2)),
                HexUtils.toBytes(txid.substring(2)), transferred, timestamp, if (blockNumber == Int.MAX_VALUE) -1 else blockNumber,
                confirmations, false, inputs, outputs,
                destAddresses, null, Transfer.GAS_LIMIT.toInt(), fee)
    }

    private fun transformValueFromDb(token: ERC20Token, value: Value): Value = Value.valueOf(token, value.value)

    private fun getTokenTransferred(ownerAddress: String, from: String, to: String, value: Value): Value {
        return if (from.equals(ownerAddress, true) && !to.equals(ownerAddress, true)) {
            // outgoing
            -value
        } else if (!from.equals(ownerAddress, true) && to.equals(ownerAddress, true)) {
            // incoming
            value
        } else if (from.equals(ownerAddress, true) && to.equals(ownerAddress, true)) {
            // self
            Value.zeroValue(token!!)
        } else {
            // transaction doesn't relate to us in any way. should not happen
            throw IllegalStateException("Transaction that wasn't sent to us or from us detected.")
        }
    }

    private fun getEthTransferred(ownerAddress: String, from: String, to: String, value: Value,
                                  fee: Value, success: Boolean, internalValue: Value?,
                                  hasTokenTransfers: Boolean, status: EthTxStatus): Value {
        // For txs that never settled on-chain from our side (REPLACED /
        // CANCELED / DROPPED), neither the value nor the fee actually left
        // the account. Show zero so balances and transferred match.
        if (status == EthTxStatus.REPLACED || status == EthTxStatus.CANCELED || status == EthTxStatus.DROPPED) {
            return Value.zeroValue(currency)
        }
        // Outgoing 0-value ETH tx from the user — necessarily a contract call
        // (or 0-ETH self-noop). Either way, the only ETH-side impact is the
        // miner fee. Don't trust `internalValue` here: some blockbook
        // responses for pending/failed contract calls surface token-related
        // amounts there, which would otherwise flip the tx to appear as
        // incoming. We can't gate on hasTokenTransfers because pending contract
        // calls don't yet have the tokenTransfers array populated by blockbook.
        if (value.isZero() && from.equals(ownerAddress, true) && !to.equals(ownerAddress, true)) {
            return -fee
        }
        return if (from.equals(ownerAddress, true) && !to.equals(ownerAddress, true)) {
            // outgoing
            if (success) -value - fee + (internalValue ?: Value.zeroValue(currency)) else -fee
        } else if (!from.equals(ownerAddress, true) && to.equals(ownerAddress, true)) {
            // incoming
            if (success) value else Value.zeroValue(currency)
        } else if (from.equals(ownerAddress, true) && to.equals(ownerAddress, true)) {
            // self
            -fee
        } else {
            // this can happen if the contract call that led to the funds transfer to user's account
            // was initiated by a foreign account (not the user's)
            internalValue ?: Value.zeroValue(currency)
        }
    }
}
