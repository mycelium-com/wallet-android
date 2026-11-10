package com.mycelium.wapi.wallet.tron

import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.SyncStatus
import com.mycelium.wapi.SyncStatusInfo
import com.mycelium.wapi.wallet.AccountListener
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.BroadcastResult
import com.mycelium.wapi.wallet.BroadcastResultType
import com.mycelium.wapi.wallet.Fee
import com.google.common.base.Optional
import com.mrd.bitlib.crypto.BipDerivationType
import com.mycelium.wapi.wallet.ExportableAccount
import com.mycelium.wapi.wallet.KeyCipher
import org.web3j.utils.Numeric
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.SyncPausable
import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.TransactionData
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.coins.Value.Companion.max
import com.mycelium.wapi.wallet.coins.Value.Companion.valueOf
import com.mycelium.wapi.wallet.eth.toUUID
import com.mycelium.wapi.wallet.exceptions.BuildTransactionException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsForFeeException
import com.mycelium.wapi.wallet.genericdb.TronAccountBacking
import com.mycelium.wapi.wallet.tron.remote.TronBlockchainService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import java.io.IOException
import java.math.BigInteger
import java.util.UUID
import java.util.logging.Level

class TronAccount(
    private val accountContext: TronAccountContext,
    credentials: Credentials? = null,
    backing: TronAccountBacking,
    private val accountListener: AccountListener?,
    blockchainService: TronBlockchainService,
    address: TronAddress? = null,
) : AbstractTronAccount(accountContext.currency, credentials, backing, blockchainService,
    TronAccount::class.simpleName, address), SyncPausable, ExportableAccount {

    val enabledTokens: List<String>
        get() = accountContext.enabledTokens ?: listOf()

    val accountIndex: Int
        get() = accountContext.accountIndex

    fun updateEnabledTokens() {
        accountContext.updateEnabledTokens()
    }

    /** TRX (sun) frozen for staking, refreshed with each balance sync. Informational. */
    @Volatile
    var stakedSun: BigInteger = BigInteger.ZERO
        private set

    override fun hasHadActivity(): Boolean =
        accountBalance.spendable.isPositive() || super.hasHadActivity()

    @Throws(InsufficientFundsException::class, BuildTransactionException::class)
    override fun createTx(toAddress: Address, value: Value, fee: Fee, data: TransactionData?): Transaction {
        val tronTxData = data as? TronTransactionData
        val bandwidth = tronTxData?.bandwidth ?: BigInteger.valueOf(typicalEstimatedTransactionSize.toLong())
        val energy = tronTxData?.energy ?: BigInteger.ZERO
        val feeLimit = tronTxData?.feeLimit ?: (fee as FeePerKbFee).feePerKb.value
        val estimatedFee = tronTxData?.estimatedFee ?: feeLimit

        if (feeLimit < BigInteger.ZERO) {
            throw BuildTransactionException(Throwable("Fee limit should be positive or zero"))
        }
        if (value.value <= BigInteger.ZERO) {
            throw BuildTransactionException(Throwable("Value should be positive"))
        }
        if (toAddress !is TronAddress) {
            throw BuildTransactionException(Throwable("Not a TRON address"))
        }
        if (value > calculateMaxSpendableAmount(valueOf(coinType, feeLimit), null, tronTxData)) {
            // the amount alone fits, only amount + fee limit does not
            if (value <= accountBalance.spendable) {
                throw InsufficientFundsForFeeException(Throwable("Insufficient funds to pay for fee"))
            }
            throw InsufficientFundsException(Throwable("Insufficient funds to send $value with fee limit $feeLimit sun"))
        }
        return TronTransaction(coinType, toAddress.addressString, value, feeLimit, bandwidth, energy, estimatedFee)
    }

    /** Builds `raw_data` against a fresh TAPOS reference and signs it. Requires network access for the block ref. */
    override fun signTx(request: Transaction, keyCipher: KeyCipher) {
        val tx = request as TronTransaction
        val keyPair = credentials?.ecKeyPair ?: throw IllegalStateException("Watch-only account cannot sign")
        val blockRef = ensureBlockRef()
        val time = TronTransactionBuilder.txTime(tx.expirationMs ?: TronTransactionBuilder.DEFAULT_EXPIRATION_MS)
        val raw = TronTransactionBuilder.buildTransfer(
            blockRef,
            receivingAddress.getBytes(),
            TronAddress(coinType, tx.toAddress).getBytes(),
            tx.tronValue.valueAsLong,
            time,
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
        val txid = tronTx.txHash?.let { HexUtils.toHex(it) }
            ?: return BroadcastResult("No transaction hash", BroadcastResultType.REJECT_INVALID_TX_PARAMS)
        val result = broadcastSigned(signed, txid)
        // UNKNOWN_OUTCOME: the tx may be on the network — record it as pending anyway so its TRX
        // stays reserved (no double send); sync drops it if the node never saw it.
        if (result.resultType != BroadcastResultType.SUCCESS && result.resultType != BroadcastResultType.UNKNOWN_OUTCOME) {
            return result
        }
        rememberBalanceBeforeBroadcast(txid)
        backing.putTransaction(
            blockNumber = -1,
            timestamp = System.currentTimeMillis() / 1000,
            txid = txid,
            raw = HexUtils.toHex(signed),
            from = receivingAddress.addressString,
            to = tronTx.toAddress,
            value = tronTx.tronValue,
            fee = valueOf(coinType, tronTx.estimatedFee),
            feeLimit = tronTx.feeLimit,
            bandwidth = tronTx.bandwidth,
            energy = tronTx.energy,
        )
        // Reflect the outgoing pending tx in the balance right away so the spent TRX
        // cannot be re-spent before the next sync
        updateBalanceCache()
        return result
    }

    override val coinType
        get() = accountContext.currency

    override val basedOnCoinType
        get() = coinType

    override val accountBalance
        get() = accountContext.balance

    override suspend fun doSynchronization(mode: SyncMode?): Boolean {
        // History first: the balance reserves locally known outgoing txs the node's balance does
        // not reflect yet, so the ones that have meanwhile landed have to be marked confirmed
        // before that reservation is computed — otherwise they come off the balance twice.
        val historyOk = mode?.ignoreTransactionHistory == true || syncTransactions()
        val balanceOk = withContext(Dispatchers.IO) { updateBalanceCache() }
        return balanceOk && historyOk
    }

    override fun updateBalanceCache(): Boolean {
        val balResponse = try {
            blockchainService.getBalance(receivingAddress.addressString)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Couldn't update TRX balance: ${e.javaClass} ${e.localizedMessage}. Using cached value.")
            return false
        }
        // Reserve locally broadcast, not-yet-confirmed outgoing txs (value + fee): the node's
        // balance does not reflect them yet, and without this the UI would happily double-spend
        // the same TRX until the next sync.
        val pendingSending = unreflectedPending(balResponse.confirmed, getUnconfirmedTransactions()
            .filter { it.sender.addressString == receivingAddress.addressString }
            .map { PendingDebit(HexUtils.toHex(it.id), it.value.value, it.value.value + (it.fee?.value ?: BigInteger.ZERO)) })
        val confirmed = (balResponse.confirmed - pendingSending).max(BigInteger.ZERO)
        stakedSun = balResponse.stakedSun
        val newBalance = Balance(valueOf(coinType, confirmed), Value.zeroValue(coinType),
            valueOf(coinType, pendingSending), Value.zeroValue(coinType))
        if (newBalance != accountContext.balance) {
            accountContext.balance = newBalance
            accountListener?.balanceUpdated(this)
        }
        return true
    }

    private suspend fun syncTransactions(): Boolean {
        try {
            // Incremental fetch: only txs newer than the last confirmed one (with an overlap
            // window); the first sync walks the (bounded) full history via pagination
            val maxConfirmedTs = backing.getMaxConfirmedTimestamp()
            val isInitialSync = maxConfirmedTs == 0L
            val minTimestamp = if (isInitialSync) 0L else maxOf(0L, maxConfirmedTs * 1000 - SYNC_OVERLAP_MS)
            val remoteTransactions = withContext(Dispatchers.IO) {
                blockchainService.getTransactions(receivingAddress.addressString, minTimestamp, fetchAll = isInitialSync)
            }
            backing.putTransactions(remoteTransactions, BigInteger.valueOf(typicalEstimatedTransactionSize.toLong()))
            resolvePendingTransactions(remoteTransactions.map { it.txid })
            return true
        } catch (e: IOException) {
            lastSyncInfo = SyncStatusInfo(SyncStatus.ERROR)
            logger.log(Level.SEVERE, "TronAccount: Error retrieving TRON transaction history: ${e.javaClass} ${e.localizedMessage}")
            return false
        }
    }

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
        accountContext.balance = Balance.getZeroBalance(coinType)
    }

    override fun isDerivedFromInternalMasterseed() = credentials != null

    /** Address as public data; the raw private key as 64 hex chars (TronLink-importable) as private data. */
    override fun getExportData(cipher: KeyCipher): ExportableAccount.Data =
        ExportableAccount.Data(
            Optional.fromNullable(credentials?.ecKeyPair?.privateKey?.let { Numeric.toHexStringNoPrefixZeroPadded(it, 64) }),
            mapOf(BipDerivationType.BIP44 to receivingAddress.addressString))

    override val id: UUID
        get() = credentials?.ecKeyPair?.toUUID() ?: UUID.nameUUIDFromBytes(receivingAddress.addressString.toByteArray())

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value, destinationAddress: TronAddress?, txData: TransactionData?): Value {
        val tronTxData = txData as? TronTransactionData
        val reserve = (tronTxData?.feeLimit ?: tronTxData?.estimatedFee)?.let { valueOf(coinType, it) } ?: minerFeePerKilobyte
        val spendable = accountBalance.spendable - reserve
        return max(spendable, Value.zeroValue(coinType))
    }

    override var label: String
        get() = accountContext.accountName
        set(value) {
            accountContext.accountName = value
        }

    override fun getBlockChainHeight() = accountContext.blockHeight

    override fun setBlockChainHeight(height: Int) {
        accountContext.blockHeight = height
    }

    override val isArchived
        get() = accountContext.archived

    override val typicalEstimatedTransactionSize = TronTransaction.TRX_TRANSFER_BANDWIDTH
}
