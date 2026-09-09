package com.mycelium.wapi.wallet.genericdb

import com.mrd.bitlib.util.HexUtils
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.InputViewModel
import com.mycelium.wapi.wallet.OutputViewModel
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.tron.TronAddress
import com.mycelium.wapi.wallet.tron.TronTransactionSummary
import com.mycelium.wapi.wallet.tron.TronTxType
import com.mycelium.wapi.wallet.tron.remote.TronTrc20Transfer
import com.mycelium.wapi.wallet.tron.remote.TronTx
import java.math.BigInteger
import java.util.UUID

/**
 * Transaction storage for TRON and TRC20 accounts. [currency] is the currency the account
 * displays values in (TRX for [com.mycelium.wapi.wallet.tron.TronAccount], the token for
 * TRC20 accounts); fees are always in TRX ([feeCurrency]).
 *
 * @param chainHeightProvider current chain height, used to compute confirmations at read time
 */
class TronAccountBacking(
    val walletDB: WalletDB,
    private val uuid: UUID,
    private val currency: CryptoCurrency,
    private val feeCurrency: CryptoCurrency,
    private val chainHeightProvider: () -> Int,
) {
    private val tronQueries = walletDB.tronAccountBackingQueries
    private val queries = walletDB.accountBackingQueries

    init {
        // Rows whose value or fee is tagged with a TRC20 token (see repairValueTags in the .sq)
        // cannot be decoded, and one of them fails every read of the history. Repairing on open
        // rather than only in a migration covers rows added after the migration ran.
        tronQueries.repairValueTags(uuid)
    }

    fun getTransactionSummaries(offset: Long, limit: Long, ownerAddress: String): List<TronTransactionSummary> =
        tronQueries.selectTransactionSummaries(uuid, limit, offset, mapper = { txid: String,
                                                                                _: CryptoCurrency,
                                                                                blockNumber: Int,
                                                                                timestamp: Long,
                                                                                value: Value,
                                                                                fee: Value,
                                                                                _: Int,
                                                                                from: String,
                                                                                to: String,
                                                                                bandwidth: BigInteger,
                                                                                energy: BigInteger,
                                                                                feeLimit: BigInteger,
                                                                                success: Boolean,
                                                                                hasTokenTransfers: Boolean,
                                                                                tronTxType: String ->
            createTransactionSummary(ownerAddress, txid, blockNumber, timestamp, value, fee, from, to,
                bandwidth, energy, feeLimit, success, hasTokenTransfers, TronTxType.fromName(tronTxType))
        }).executeAsList().readable().applyFeeLinks()

    /** @param timestampParameter time in seconds */
    fun getTransactionSummariesSince(timestampParameter: Long, ownerAddress: String): List<TronTransactionSummary> =
        tronQueries.selectTransactionSummariesSince(uuid, timestampParameter, mapper = { txid: String,
                                                                                          _: CryptoCurrency,
                                                                                          blockNumber: Int,
                                                                                          timestamp: Long,
                                                                                          value: Value,
                                                                                          fee: Value,
                                                                                          _: Int,
                                                                                          from: String,
                                                                                          to: String,
                                                                                          bandwidth: BigInteger,
                                                                                          energy: BigInteger,
                                                                                          feeLimit: BigInteger,
                                                                                          success: Boolean,
                                                                                          hasTokenTransfers: Boolean,
                                                                                          tronTxType: String ->
            createTransactionSummary(ownerAddress, txid, blockNumber, timestamp, value, fee, from, to,
                bandwidth, energy, feeLimit, success, hasTokenTransfers, TronTxType.fromName(tronTxType))
        }).executeAsList().readable().applyFeeLinks()

    fun getTransactionSummary(txidParameter: String, ownerAddress: String): TronTransactionSummary? =
        tronQueries.selectTransactionSummaryById(uuid, txidParameter, mapper = { txid: String,
                                                                                  _: CryptoCurrency,
                                                                                  blockNumber: Int,
                                                                                  timestamp: Long,
                                                                                  value: Value,
                                                                                  fee: Value,
                                                                                  _: Int,
                                                                                  from: String,
                                                                                  to: String,
                                                                                  bandwidth: BigInteger,
                                                                                  energy: BigInteger,
                                                                                  feeLimit: BigInteger,
                                                                                  success: Boolean,
                                                                                  hasTokenTransfers: Boolean,
                                                                                  tronTxType: String ->
            createTransactionSummary(ownerAddress, txid, blockNumber, timestamp, value, fee, from, to,
                bandwidth, energy, feeLimit, success, hasTokenTransfers, TronTxType.fromName(tronTxType))
        }).executeAsOneOrNull()?.takeIf { it.id.isNotEmpty() }?.also { listOf(it).applyFeeLinks() }

    /** Locally broadcast or remotely seen txs that are not yet included in a block. */
    fun getUnconfirmedTransactions(ownerAddress: String): List<TronTransactionSummary> =
        tronQueries.selectUnconfirmedTransactions(uuid, mapper = { txid: String,
                                                                    _: CryptoCurrency,
                                                                    blockNumber: Int,
                                                                    timestamp: Long,
                                                                    value: Value,
                                                                    fee: Value,
                                                                    _: Int,
                                                                    from: String,
                                                                    to: String,
                                                                    bandwidth: BigInteger,
                                                                    energy: BigInteger,
                                                                    feeLimit: BigInteger,
                                                                    success: Boolean,
                                                                    hasTokenTransfers: Boolean,
                                                                    tronTxType: String ->
            createTransactionSummary(ownerAddress, txid, blockNumber, timestamp, value, fee, from, to,
                bandwidth, energy, feeLimit, success, hasTokenTransfers, TronTxType.fromName(tronTxType))
        }).executeAsList().readable().applyFeeLinks()

    /**
     * Not every txid is one we produced: the energy service partner reports the hashes it
     * broadcast for us and those are stored as they arrive. A value that isn't hex would throw
     * on every read of the history — that is, on every sync — so it decodes to nothing here and
     * [readable] drops the row, instead of one bad hash taking the account down with it.
     */
    private fun decodeTxid(txid: String): ByteArray = try {
        HexUtils.toBytes(txid)
    } catch (e: RuntimeException) {
        ByteArray(0)
    }

    private fun List<TronTransactionSummary>.readable() = filter { it.id.isNotEmpty() }

    /** Marks fee payments of the energy service so the history can render them as such. */
    private fun List<TronTransactionSummary>.applyFeeLinks(): List<TronTransactionSummary> {
        if (isEmpty()) return this
        val feeMap = tronQueries.selectFeeLinks(uuid).executeAsList().associate { it.feeTxId to it.mainTxId }
        if (feeMap.isEmpty()) return this
        forEach { summary ->
            feeMap[summary.idHex]?.let { mainTxId ->
                summary.isEnergyServiceFee = true
                summary.parentTxId = mainTxId
            }
        }
        return this
    }

    fun insertFeeLink(feeTxId: String, mainTxId: String) {
        tronQueries.insertFeeLink(feeTxId, mainTxId, uuid)
    }

    /**
     * @param blockNumber -1 for a locally broadcast tx (stored as Int.MAX_VALUE so it sorts on top)
     * @param timestamp seconds
     */
    fun putTransaction(
        blockNumber: Int, timestamp: Long, txid: String, raw: String, from: String, to: String, value: Value,
        fee: Value, feeLimit: BigInteger, hasTokenTransfers: Boolean = false, success: Boolean = true,
        bandwidth: BigInteger = BigInteger.valueOf(268), energy: BigInteger = BigInteger.ZERO,
        tronTxType: TronTxType = TronTxType.TRX_TRANSFER,
    ) {
        val confirmations = if (blockNumber > 0) 1 else 0
        // Values are persisted with the TRX coin type: the DB value adapter can only resolve
        // coins registered in COINS, and TRC20 tokens are runtime objects (same rule as ERC20).
        // createTransactionSummary re-tags them with the account's display currency. The fee goes
        // through the same adapter, so it is re-tagged here too — a token-typed fee would be
        // written as its runtime name and throw on every later read of the history.
        queries.insertTransaction(txid, uuid, feeCurrency, if (blockNumber == -1) Int.MAX_VALUE else blockNumber,
            timestamp, raw, Value.valueOf(feeCurrency, value.value), Value.valueOf(feeCurrency, fee.value),
            confirmations)
        tronQueries.insertTransaction(txid, uuid, from, to, bandwidth, energy, feeLimit, success,
            hasTokenTransfers, tronTxType.name)
    }

    /** Stores TRX-level history for a [com.mycelium.wapi.wallet.tron.TronAccount]. */
    fun putTransactions(remoteTransactions: List<TronTx>, typicalBandwidth: BigInteger) {
        walletDB.transaction {
            remoteTransactions
                // TRC10 transfers are deliberately kept out of history: the wallet doesn't
                // support TRC10 assets and such incoming txs are almost always scam/airdrop noise.
                .filter { it.contractType != TronTxType.TRC10_CONTRACT_TYPE }
                .forEach { tx ->
                    putTransaction(
                        blockNumber = tx.blockNumber.toInt(),
                        timestamp = tx.blockTimestampSec,
                        txid = tx.txid,
                        raw = "",
                        from = tx.from,
                        to = tx.to,
                        value = Value.valueOf(currency, tx.value),
                        fee = Value.valueOf(feeCurrency, tx.fee),
                        feeLimit = BigInteger.ZERO,
                        hasTokenTransfers = tx.contractType == "TriggerSmartContract",
                        success = tx.success,
                        bandwidth = if (tx.netUsage > 0) BigInteger.valueOf(tx.netUsage) else typicalBandwidth,
                        energy = BigInteger.valueOf(tx.energyUsage),
                        tronTxType = TronTxType.fromContractType(tx.contractType),
                    )
                }
        }
    }

    /**
     * Stores TRC20 transfers. [blockInfo] maps txid → (blockNumber, fee in sun); transfers
     * without an entry are skipped (their block is unknown, storing them would degrade an
     * existing confirmed row back to pending via INSERT OR REPLACE).
     */
    fun putTrc20Transactions(
        transfers: List<TronTrc20Transfer>,
        blockInfo: Map<String, Pair<Long, Long>>,
        typicalBandwidth: BigInteger,
        typicalEnergy: BigInteger,
    ) {
        walletDB.transaction {
            transfers.forEach { tx ->
                val (blockNumber, feeSun) = blockInfo[tx.txid] ?: return@forEach
                putTransaction(
                    blockNumber = blockNumber.toInt(),
                    timestamp = tx.blockTimestampMs / 1000,
                    txid = tx.txid,
                    raw = "",
                    from = tx.from,
                    to = tx.to,
                    value = Value.valueOf(currency, tx.value),
                    fee = Value.valueOf(feeCurrency, feeSun),
                    feeLimit = BigInteger.ZERO,
                    hasTokenTransfers = true,
                    success = true,
                    bandwidth = typicalBandwidth,
                    energy = typicalEnergy,
                    tronTxType = TronTxType.TRC20_TRANSFER,
                )
            }
        }
    }

    fun deleteAllAccountTransactions() {
        queries.deleteAllAccountTransactions(uuid)
    }

    fun deleteTransaction(txid: String) {
        queries.deleteTransaction(uuid, txid)
    }

    /** txid -> blockNumber for locally stored txs already included in a block. */
    fun getConfirmedTxBlockNumbers(): Map<String, Long> =
        tronQueries.selectConfirmedTxBlocks(uuid).executeAsList()
            .associate { it.txid to it.blockNumber.toLong() }

    /** Newest confirmed tx timestamp in seconds; 0 when there is no confirmed history. */
    fun getMaxConfirmedTimestamp(): Long =
        tronQueries.selectMaxConfirmedTimestamp(uuid).executeAsOne().toLong()

    /** Marks a previously pending tx as included in [blockNumber]. */
    fun confirmTransaction(txid: String, blockNumber: Int, timestampSec: Long? = null, fee: Value? = null) {
        walletDB.transaction {
            queries.updateTransaction(blockNumber, 1, uuid, txid)
            timestampSec?.let { queries.updateBlockNumberAndTimestamp(blockNumber, it, uuid, txid) }
            fee?.let { queries.updateFee(it, uuid, txid) }
        }
    }

    /**
     * A locally broadcast tx that got mined but failed on-chain. The row was inserted
     * optimistically with success = true; confirming it as-is would present a burnt fee as
     * a completed transfer. [fee] is the fee actually charged, when known.
     */
    fun failTransaction(txid: String, blockNumber: Int, fee: Value? = null) {
        walletDB.transaction {
            queries.updateTransaction(blockNumber, 1, uuid, txid)
            tronQueries.updateSuccess(false, uuid, txid)
            fee?.let { queries.updateFee(it, uuid, txid) }
        }
    }

    private fun createTransactionSummary(
        ownerAddress: String,
        txid: String,
        blockNumber: Int,
        timestamp: Long,
        value: Value,
        fee: Value,
        from: String,
        to: String,
        bandwidth: BigInteger,
        energy: BigInteger,
        feeLimit: BigInteger,
        success: Boolean,
        hasTokenTransfers: Boolean,
        tronTxType: TronTxType,
    ): TronTransactionSummary {
        // Values were persisted with this account's display currency (token for TRC20 accounts,
        // TRX otherwise) — same rule as ERC20: re-tag defensively in case the coin object differs.
        val displayValue = Value.valueOf(currency, value.value)
        val fromAddress = TronAddress(currency, from)
        val toAddress = TronAddress(currency, to.ifEmpty { from })
        val inputs = listOf(InputViewModel(fromAddress, displayValue, false))
        val outputs = if (to.isEmpty()) listOf() else listOf(OutputViewModel(toAddress, displayValue, false))
        val destAddresses = listOf(toAddress)
        val transferred = getTronTransferred(ownerAddress, from, to, displayValue, fee, success, tronTxType)
        val height = if (blockNumber == Int.MAX_VALUE) -1 else blockNumber
        val confirmations = if (height > 0) maxOf(chainHeightProvider() - height + 1, 1) else 0
        val idBytes = decodeTxid(txid)
        return TronTransactionSummary(
            fromAddress, toAddress, displayValue, bandwidth, energy, feeLimit, hasTokenTransfers, success,
            tronTxType, currency, idBytes, idBytes, transferred, timestamp, height, confirmations,
            false, inputs, outputs, destAddresses, null, bandwidth.toInt(), fee
        )
    }

    private fun getTronTransferred(
        ownerAddress: String, from: String, to: String, value: Value,
        fee: Value, success: Boolean, tronTxType: TronTxType,
    ): Value {
        val feeInDisplayCurrency = fee.type == currency
        val feeOnly = if (feeInDisplayCurrency) -fee else Value.zeroValue(currency)
        // Non-transfer operations (staking, delegation, voting, reward claims, contract calls)
        // don't move funds to a counterparty. Staking/delegation move TRX into (out of) a locked
        // position, so surface the principal with a direction sign; every other op costs the fee.
        // They only affect the account that owns (signed) the operation — as the mere receiver
        // of a resource (un)delegation nothing moves for us.
        if (!tronTxType.isTransfer) {
            if (!from.equals(ownerAddress, true)) return Value.zeroValue(currency)
            return when (tronTxType) {
                TronTxType.STAKE, TronTxType.DELEGATE_RESOURCE, TronTxType.CONTRACT_CALL ->
                    if (value.moreThanZero()) -value + feeOnly else feeOnly
                TronTxType.UNSTAKE, TronTxType.UNDELEGATE_RESOURCE,
                TronTxType.WITHDRAW_UNFROZEN, TronTxType.CLAIM_REWARDS ->
                    if (value.moreThanZero()) value else feeOnly
                else -> feeOnly
            }
        }
        val isFrom = from.equals(ownerAddress, true)
        val isTo = to.equals(ownerAddress, true)
        return when {
            isFrom && !isTo -> // outgoing
                if (feeInDisplayCurrency) {
                    if (success) -value - fee else -fee
                } else {
                    // Token transfer: the fee is paid in TRX and shown on the TRX account, so a
                    // failed one moved nothing at all in this account's currency.
                    if (success) -value else Value.zeroValue(currency)
                }
            !isFrom && isTo -> if (success) value else Value.zeroValue(currency) // incoming
            isFrom && isTo -> feeOnly // self
            else -> Value.zeroValue(currency)
        }
    }
}
