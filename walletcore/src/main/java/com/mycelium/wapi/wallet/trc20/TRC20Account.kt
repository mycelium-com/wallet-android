package com.mycelium.wapi.wallet.trc20

import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.SyncStatus
import com.mycelium.wapi.SyncStatusInfo
import com.mycelium.wapi.wallet.AccountListener
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.BroadcastResult
import com.mycelium.wapi.wallet.BroadcastResultType
import com.mycelium.wapi.wallet.Fee
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.SyncPausable
import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.TransactionData
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.exceptions.BuildTransactionException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsForFeeException
import com.mycelium.wapi.wallet.genericdb.TronAccountBacking
import com.mycelium.wapi.wallet.trc20.coins.TRC20Token
import com.mycelium.wapi.wallet.tron.AbstractTronAccount
import com.mycelium.wapi.wallet.tron.PendingDebit
import com.mycelium.wapi.wallet.tron.TronAccount
import com.mycelium.wapi.wallet.tron.TronAddress
import com.mycelium.wapi.wallet.tron.TronFeeEstimator
import com.mycelium.wapi.wallet.tron.TronKeys
import com.mycelium.wapi.wallet.tron.TronTransaction
import com.mycelium.wapi.wallet.tron.TronTransactionBuilder
import com.mycelium.wapi.wallet.tron.TronTransactionData
import com.mycelium.wapi.wallet.tron.remote.TronBlockchainService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import java.io.IOException
import java.math.BigInteger
import java.util.UUID
import java.util.logging.Level

/**
 * TRC20 token account living on the address of its parent [tronAcc], which pays the TRX fees
 * (bandwidth + energy) — the same parent/child shape as ERC20 over Ethereum.
 */
class TRC20Account(
    private val accountContext: TRC20AccountContext,
    private val token: TRC20Token,
    val tronAcc: TronAccount,
    credentials: Credentials? = null,
    backing: TronAccountBacking,
    private val accountListener: AccountListener?,
    blockchainService: TronBlockchainService,
    address: TronAddress? = null,
) : AbstractTronAccount(accountContext.currency, credentials, backing, blockchainService,
    TRC20Account::class.simpleName, address), SyncPausable {

    val contractAddress: String get() = token.contractAddress

    override fun createTx(address: Address, amount: Value, fee: Fee, data: TransactionData?): Transaction {
        val tronTxData = data as? TronTransactionData
        val bandwidth = tronTxData?.bandwidth ?: BigInteger.valueOf(TOKEN_TRANSFER_BANDWIDTH)
        val energy = tronTxData?.energy ?: BigInteger.valueOf(TOKEN_TRANSFER_ENERGY)
        // Callers that don't run the fee estimator (exchange, gift cards, …) pass the generic
        // per-coin fee, which is far below what a contract call may burn. A too-low fee_limit
        // makes the transfer fail on-chain with OUT_OF_ENERGY while burning the whole limit,
        // so never go below the conservative minimum in that case.
        val feeLimit = tronTxData?.feeLimit
            ?: (fee as FeePerKbFee).feePerKb.value.max(BigInteger.valueOf(TronFeeEstimator.MIN_TRC20_FEE_LIMIT))
        val estimatedFee = tronTxData?.estimatedFee ?: (fee as FeePerKbFee).feePerKb.value

        if (address !is TronAddress) {
            throw BuildTransactionException(Throwable("Not a TRON address"))
        }
        if (amount.value <= BigInteger.ZERO) {
            throw BuildTransactionException(Throwable("Value should be positive"))
        }
        if (feeLimit <= BigInteger.ZERO) {
            throw BuildTransactionException(Throwable("Fee limit must be positive for TRC20 transfers"))
        }
        if (calculateMaxSpendableAmount(Value.valueOf(basedOnCoinType, feeLimit), null, null) < amount) {
            throw InsufficientFundsException(Throwable("Insufficient funds"))
        }
        if (tronTxData?.checkFeeBalance != false && tronAcc.accountBalance.spendable.value < feeLimit) {
            throw InsufficientFundsForFeeException(Throwable("Insufficient TRX on the parent account to pay the network fee"))
        }
        return TronTransaction(basedOnCoinType, address.addressString, Value.zeroValue(basedOnCoinType),
            feeLimit, bandwidth, energy, estimatedFee, TOKEN_TRANSFER_BANDWIDTH.toInt(), amount,
            expirationMs = tronTxData?.expirationMs)
    }

    /**
     * Plain TRC20 transfer for the energy service flow: the partner sponsors energy, so the TRX
     * balance of the parent account is not checked and the fee shown is zero TRX. [expirationMs]
     * bounds how long the partner can hold the signed tx before broadcasting.
     */
    fun createEnergyServiceTransferTx(toAddress: TronAddress, amount: Value, feeLimit: BigInteger, expirationMs: Long): TronTransaction =
        createTx(toAddress, amount, FeePerKbFee(Value.zeroValue(basedOnCoinType)),
            TronTransactionData(feeLimit = feeLimit, estimatedFee = BigInteger.ZERO, checkFeeBalance = false,
                expirationMs = expirationMs)) as TronTransaction

    override fun signTx(request: Transaction, keyCipher: KeyCipher) {
        val tx = request as TronTransaction
        val keyPair = credentials?.ecKeyPair ?: throw IllegalStateException("Watch-only account cannot sign")
        val blockRef = ensureBlockRef()
        val callData = TronTransactionBuilder.trc20TransferData(
            TronAddress(basedOnCoinType, tx.toAddress), tx.tokenValue!!.value
        )
        val time = TronTransactionBuilder.txTime(tx.expirationMs ?: TronTransactionBuilder.DEFAULT_EXPIRATION_MS)
        val raw = TronTransactionBuilder.buildTriggerSmartContract(
            blockRef,
            receivingAddress.getBytes(),
            TronAddress(basedOnCoinType, token.contractAddress).getBytes(),
            callData,
            tx.feeLimit.toLong(),
            time = time,
        )
        val signature = TronKeys.sign(raw.txId, keyPair)
        tx.rawData = raw.rawData
        tx.txHash = raw.txId
        tx.signature = signature
        tx.signedBytes = TronTransactionBuilder.encodeSigned(raw.rawData, signature)
        tx.signingOutput = TronTransactionBuilder.encodeSigningOutput(raw, signature, blockRef)
        tx.signedExpirationMs = time.expiration
        tx.isSigned = true
    }

    override fun broadcastTx(tx: Transaction): BroadcastResult {
        val tronTx = tx as TronTransaction
        val signed = tronTx.signedBytes ?: return BroadcastResult("Transaction is not signed", BroadcastResultType.REJECT_INVALID_TX_PARAMS)
        try {
            val result = blockchainService.broadcast(signed)
            if (!result.success) {
                return BroadcastResult(result.message, BroadcastResultType.REJECT_INVALID_TX_PARAMS)
            }
            val txid = tronTx.txHash?.let { HexUtils.toHex(it) } ?: result.txid
                ?: return BroadcastResult("No transaction hash", BroadcastResultType.REJECT_INVALID_TX_PARAMS)
            rememberBalanceBeforeBroadcast(txid)
            backing.putTransaction(
                blockNumber = -1,
                timestamp = System.currentTimeMillis() / 1000,
                txid = txid,
                raw = HexUtils.toHex(signed),
                from = receivingAddress.addressString,
                to = tronTx.toAddress,
                value = tronTx.tokenValue!!,
                fee = Value.valueOf(basedOnCoinType, tronTx.estimatedFee),
                feeLimit = tronTx.feeLimit,
                hasTokenTransfers = true,
                bandwidth = tronTx.bandwidth,
                energy = tronTx.energy,
                tronTxType = com.mycelium.wapi.wallet.tron.TronTxType.TRC20_TRANSFER,
            )
            // Reflect the outgoing pending transfer in the balance right away so the spent
            // tokens cannot be re-spent before the next sync
            updateBalanceCache()
            return BroadcastResult(BroadcastResultType.SUCCESS)
        } catch (e: IOException) {
            return BroadcastResult(BroadcastResultType.NO_SERVER_CONNECTION)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error sending TRC-20 transaction: ${e.localizedMessage}")
            return BroadcastResult(e.localizedMessage, BroadcastResultType.REJECT_INVALID_TX_PARAMS)
        }
    }

    override val coinType: TRC20Token
        get() = token

    override val basedOnCoinType
        get() = accountContext.currency

    override val accountBalance: Balance
        get() = readBalance()

    override var label: String
        get() = accountContext.accountName
        set(value) {
            accountContext.accountName = value
        }

    override suspend fun doSynchronization(mode: SyncMode?): Boolean {
        // History first: the balance reserves locally known outgoing txs the node's balance does
        // not reflect yet, so the ones that have meanwhile landed have to be marked confirmed
        // before that reservation is computed — otherwise they come off the balance twice.
        val historyOk = mode?.ignoreTransactionHistory == true || syncTransactions()
        val balanceOk = withContext(Dispatchers.IO) { updateBalanceCache() }
        return balanceOk && historyOk
    }

    override fun setBlockChainHeight(height: Int) {
        accountContext.blockHeight = height
    }

    override fun getBlockChainHeight() = accountContext.blockHeight

    override val isArchived: Boolean
        get() = accountContext.archived

    override fun archiveAccount() {
        accountContext.archived = true
        dropCachedData()
    }

    override fun activateAccount() {
        accountContext.archived = false
        dropCachedData()
    }

    override fun dropCachedData() {
        clearBacking()
        saveBalance(Balance.getZeroBalance(coinType))
    }

    override fun isDerivedFromInternalMasterseed() = false

    override val id: UUID
        get() = accountContext.uuid

    /**
     * The whole token balance, minus the energy-service fee when that mode is active: there the
     * fee is paid in the token and the partner broadcasts it before the transfer, so a "max" of
     * the full balance would leave nothing for the fee and fail the `amount + fee <= spendable`
     * check (see [com.mycelium.wapi.wallet.tron.energy.EnergyServiceManager.execute]).
     *
     * The fee is the one the send screen is showing: it holds a single quote per recipient, so
     * this max and that fee always add up to the same balance.
     */
    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value, destinationAddress: TronAddress?, txData: TransactionData?): Value {
        val tokenFee = (txData as? TronTransactionData)?.tokenFee ?: return accountBalance.spendable
        return Value.valueOf(coinType, (accountBalance.spendable.value - tokenFee).max(BigInteger.ZERO))
    }

    override val typicalEstimatedTransactionSize = TOKEN_TRANSFER_BANDWIDTH.toInt()

    /** Fetches `balanceOf()` and reserves locally broadcast, not-yet-confirmed outgoing transfers. */
    override fun updateBalanceCache(): Boolean {
        val onChain = blockchainService.getTrc20Balance(receivingAddress.addressString, token.contractAddress)
        if (onChain == null) {
            logger.log(Level.WARNING, "Couldn't update TRC20 balance for ${token.symbol}. Using cached value.")
            return false
        }
        val pendingSending = unreflectedPending(onChain, getPendingTransfers())
        val confirmed = (onChain - pendingSending).max(BigInteger.ZERO)
        val newBalance = Balance(Value.valueOf(coinType, confirmed), Value.zeroValue(coinType),
            Value.valueOf(coinType, pendingSending), Value.zeroValue(coinType))
        if (newBalance != readBalance()) {
            saveBalance(newBalance)
            accountListener?.balanceUpdated(this)
        }
        return true
    }

    private fun getPendingTransfers() = getUnconfirmedTransactions()
        .filter { it.sender.addressString == receivingAddress.addressString && it.receiver.addressString != receivingAddress.addressString }
        .map { PendingDebit(HexUtils.toHex(it.id), it.value.value, it.value.value) }

    private suspend fun syncTransactions(): Boolean {
        try {
            val maxConfirmedTs = backing.getMaxConfirmedTimestamp()
            val isInitialSync = maxConfirmedTs == 0L
            val minTimestamp = if (isInitialSync) 0L else maxOf(0L, maxConfirmedTs * 1000 - SYNC_OVERLAP_MS)
            val transfers = withContext(Dispatchers.IO) {
                blockchainService.getTrc20Transfers(receivingAddress.addressString, token.contractAddress, minTimestamp, fetchAll = isInitialSync)
            }
            // The trc20 endpoint carries no block number: resolve it (and the fee) per tx with
            // bounded concurrency; txs already stored with a block skip the lookup entirely.
            val known = backing.getConfirmedTxBlockNumbers()
            // energy-service transfers are stored mined but with the typical energy: fetch their receipt once
            val placeholderEnergy = backing.getConfirmedTxidsWithEnergy(BigInteger.valueOf(TOKEN_TRANSFER_ENERGY))
            val blockInfo = HashMap<String, Pair<Long, Long>>()
            // txid → (energy_usage_total, net_usage) from the receipt: what the tx really consumed
            val resourceInfo = HashMap<String, Pair<Long, Long>>()
            val semaphore = Semaphore(TX_INFO_MAX_CONCURRENCY)
            coroutineScope {
                transfers.map { tx ->
                    async(Dispatchers.IO) {
                        known[tx.txid]?.let { block ->
                            if (tx.txid in placeholderEnergy) {
                                try {
                                    backing.updateResources(tx.txid, blockchainService.getTransactionInfo(tx.txid))
                                } catch (e: IOException) {
                                    logger.log(Level.WARNING, "getTransactionInfo failed for ${tx.txid}: ${e.message}")
                                }
                            }
                            return@async tx.txid to (block to -1L)
                        }
                        semaphore.withPermit {
                            try {
                                val info = blockchainService.getTransactionInfo(tx.txid)
                                synchronized(resourceInfo) { resourceInfo[tx.txid] = info.energyUsageTotal to info.netUsage }
                                tx.txid to (info.blockNumber to info.fee)
                            } catch (e: IOException) {
                                logger.log(Level.WARNING, "getTransactionInfo failed for ${tx.txid}: ${e.message}")
                                null
                            }
                        }
                    }
                }.awaitAll().filterNotNull().forEach { (txid, info) ->
                    // -1 fee = keep whatever is stored (row already known); the INSERT OR REPLACE
                    // below is skipped for those by not listing them at all.
                    if (info.second >= 0) blockInfo[txid] = info
                }
            }
            backing.putTrc20Transactions(transfers, blockInfo, BigInteger.valueOf(TOKEN_TRANSFER_BANDWIDTH), BigInteger.valueOf(TOKEN_TRANSFER_ENERGY), resourceInfo)
            // rows older than the incremental window are not in `transfers`: repair a few of them per sync
            (placeholderEnergy - transfers.map { it.txid }.toSet()).take(PLACEHOLDER_REPAIRS_PER_SYNC).forEach { txid ->
                try {
                    backing.updateResources(txid, withContext(Dispatchers.IO) { blockchainService.getTransactionInfo(txid) })
                } catch (e: IOException) {
                    logger.log(Level.WARNING, "getTransactionInfo failed for $txid: ${e.message}")
                }
            }
            resolvePendingTransactions(transfers.map { it.txid })
            return true
        } catch (e: IOException) {
            lastSyncInfo = SyncStatusInfo(SyncStatus.ERROR)
            logger.log(Level.SEVERE, "TRC20Account: Error retrieving TRC-20 transaction history: ${e.javaClass} ${e.localizedMessage}")
            return false
        }
    }

    /**
     * Records an energy-service transfer the partner broadcast for us: the main token transfer
     * and, when known, the fee payment to the partner's wallet, linked to the main tx so history
     * can label it.
     *
     * Both rows carry a zero TRX fee: this account burns no TRX (the partner supplies the energy)
     * and the fee it does pay is the token transfer of the second row — counting it on the main
     * tx as well would show it twice in the history.
     */
    fun saveEnergyServiceTransaction(txHash: String, recipient: String, amount: BigInteger, feeAmount: BigInteger,
                                     feeTxHash: String?, feeWallet: String?) {
        val now = System.currentTimeMillis() / 1000
        backing.putTransaction(
            blockNumber = blockOfBroadcastTx(txHash), timestamp = now, txid = txHash, raw = "",
            from = receivingAddress.addressString, to = recipient,
            value = Value.valueOf(coinType, amount),
            fee = Value.zeroValue(basedOnCoinType),
            feeLimit = BigInteger.ZERO, hasTokenTransfers = true,
            bandwidth = BigInteger.valueOf(TOKEN_TRANSFER_BANDWIDTH), energy = BigInteger.valueOf(TOKEN_TRANSFER_ENERGY),
            tronTxType = com.mycelium.wapi.wallet.tron.TronTxType.TRC20_TRANSFER,
        )
        if (feeTxHash != null && feeWallet != null) {
            backing.putTransaction(
                blockNumber = blockOfBroadcastTx(feeTxHash), timestamp = now, txid = feeTxHash, raw = "",
                from = receivingAddress.addressString, to = feeWallet,
                value = Value.valueOf(coinType, feeAmount),
                fee = Value.zeroValue(basedOnCoinType),
                feeLimit = BigInteger.ZERO, hasTokenTransfers = true,
                bandwidth = BigInteger.valueOf(TOKEN_TRANSFER_BANDWIDTH), energy = BigInteger.valueOf(TOKEN_TRANSFER_ENERGY),
                tronTxType = com.mycelium.wapi.wallet.tron.TronTxType.TRC20_TRANSFER,
            )
            backing.insertFeeLink(feeTxHash, txHash)
        }
        updateBalanceCache()
        accountListener?.balanceUpdated(this)
    }

    /** True when [txId] is included in a block. Throws when the network cannot answer. */
    @Throws(IOException::class)
    fun isTransactionConfirmed(txId: String): Boolean = blockchainService.getTransactionInfo(txId).isConfirmed

    /**
     * Block [txid] is already in, or -1 while it is not in one — which is what marks a transaction
     * pending, and a pending outgoing transaction is held back from the balance on top of what the
     * node reports. The energy service partner broadcasts before it reports the order done, so its
     * transfers are often mined by the time they are stored: counted as pending they would come
     * off the balance a second time, and the account would read as empty until the next sync.
     */
    private fun blockOfBroadcastTx(txid: String): Int = try {
        blockchainService.getTransactionInfo(txid).blockNumber.toInt().takeIf { it > 0 } ?: -1
    } catch (e: Exception) {
        -1
    }

    // Balances are persisted with the TRX coin type: only coins registered in COINS can be
    // stored, and tokens are runtime objects (same rule as ERC20). Convert on the way in/out.
    private fun saveBalance(balance: Balance) {
        accountContext.balance = Balance(Value.valueOf(basedOnCoinType, balance.confirmed.value),
            Value.valueOf(basedOnCoinType, balance.pendingReceiving.value),
            Value.valueOf(basedOnCoinType, balance.pendingSending.value),
            Value.valueOf(basedOnCoinType, 0))
    }

    private fun readBalance(): Balance {
        val balance = accountContext.balance
        return Balance(Value.valueOf(coinType, balance.confirmed.value),
            Value.valueOf(coinType, balance.pendingReceiving.value),
            Value.valueOf(coinType, balance.pendingSending.value),
            Value.valueOf(coinType, 0))
    }

    companion object {
        const val TOKEN_TRANSFER_BANDWIDTH = 345L
        /** Typical TRC20 transfer energy when the recipient already holds the token. */
        const val TOKEN_TRANSFER_ENERGY = 65_000L
        private const val PLACEHOLDER_REPAIRS_PER_SYNC = 20
        private const val TX_INFO_MAX_CONCURRENCY = 3
    }
}
