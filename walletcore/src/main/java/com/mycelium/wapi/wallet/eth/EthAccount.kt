package com.mycelium.wapi.wallet.eth

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.util.BitUtils
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
import com.mycelium.wapi.wallet.coins.Value.Companion.max
import com.mycelium.wapi.wallet.coins.Value.Companion.valueOf
import com.mycelium.wapi.wallet.exceptions.BuildTransactionException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsException
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.Sign
import org.web3j.crypto.TransactionEncoder
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.io.IOException
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.logging.Level

class EthAccount(private val chainId: Long,
                 private val accountContext: EthAccountContext,
                 credentials: Credentials? = null,
                 backing: EthAccountBacking,
                 private val accountListener: AccountListener?,
                 blockchainService: EthBlockchainService,
                 address: EthAddress? = null) : AbstractEthERC20Account(accountContext.currency, credentials,
        backing, blockchainService, EthAccount::class.simpleName, address), SyncPausable {

    val enabledTokens: List<String>
        get() = accountContext.enabledTokens ?: listOf()

    val accountIndex: Int
        get() = accountContext.accountIndex

    fun updateEnabledTokens() {
        accountContext.updateEnabledTokens()
    }
    override fun hasHadActivity(): Boolean =
            accountBalance.spendable.isPositive() || accountContext.nonce > BigInteger.ZERO

    @Throws(InsufficientFundsException::class, BuildTransactionException::class)
    override fun createTx(toAddress: Address, value: Value, fee: Fee, data: TransactionData?): Transaction {
        val ethTxData = data as? EthTransactionData
        val nonce = ethTxData?.nonce ?: accountContext.nonce
        val gasLimit = ethTxData?.gasLimit ?: BigInteger.valueOf(typicalEstimatedTransactionSize.toLong())
        val inputData = ethTxData?.inputData ?: ""
        val gasPrice = ethTxData?.suggestedGasPrice?.let { Value.valueOf(coinType, it) } ?: (fee as FeePerKbFee).feePerKb

        if (gasPrice.value <= BigInteger.ZERO) {
            throw BuildTransactionException(Throwable("Gas price should be positive and non-zero"))
        }
        if (value.value < BigInteger.ZERO) {
            throw BuildTransactionException(Throwable("Value should be positive"))
        }
        if (gasLimit < Transfer.GAS_LIMIT) {
            throw BuildTransactionException(Throwable("Gas limit must be at least ${Transfer.GAS_LIMIT}"))
        }
        if (value > calculateMaxSpendableAmount(gasPrice, null, ethTxData)) {
            throw InsufficientFundsException(Throwable("Insufficient funds to send " + Convert.fromWei(value.value.toBigDecimal(), Convert.Unit.ETHER) +
                    " ether with gas price " + Convert.fromWei(gasPrice.valueAsBigDecimal, Convert.Unit.GWEI) + " gwei"))
        }
        return EthTransaction(coinType, toAddress.toString(), value, gasPrice.value, nonce, gasLimit, inputData)
    }

    override fun signTx(request: Transaction, keyCipher: KeyCipher) {
        val rawTransaction = (request as EthTransaction).run {
            RawTransaction.createTransaction(nonce, gasPrice, gasLimit, toAddress, ethValue.value,
                    inputData)
        }
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
        val hexValue = Numeric.toHexString(signedMessage)
        request.apply {
            signedHex = hexValue
            // The on-chain txid is keccak256 over the actually-broadcast bytes.
            // web3j's TransactionUtils.generateTransactionHash without chainId
            // recomputes signMessage in legacy (pre-EIP-155) form, which produces
            // a different hash than what the network indexes — the next sync then
            // pulls the real hash and we end up with two rows for the same tx.
            txHash = Hash.sha3(signedMessage)
            txBinary = TransactionEncoder.encode(rawTransaction)!!
        }
    }

    override fun signMessage(message: String, address: Address?): String {
        val msgBytes = message.toByteArray(StandardCharsets.UTF_8)
        val sig = Sign.signPrefixedMessage(msgBytes, credentials!!.ecKeyPair)
        return "${Numeric.toHexString(sig.r)}${Numeric.toHexString(sig.s).substring(2)}${HexUtils.toHex(sig.v)}"
    }

    override fun broadcastTx(tx: Transaction): BroadcastResult {
        try {
            val result = blockchainService.sendTransaction((tx as EthTransaction).signedHex!!)
            if (!result.success) {
                return BroadcastResult(result.message, BroadcastResultType.REJECT_INVALID_TX_PARAMS)
            }
            backing.putTransaction(-1, System.currentTimeMillis() / 1000, "0x" + HexUtils.toHex(tx.txHash),
                    tx.signedHex!!, receivingAddress.addressString, tx.toAddress, tx.ethValue,
                    valueOf(coinType, tx.gasPrice * tx.gasLimit), 0, tx.nonce, tx.gasPrice, gasLimit = tx.gasLimit)
            bumpNonceAfterBroadcast(tx.nonce)
        } catch (e: IOException) {
            return BroadcastResult(BroadcastResultType.NO_SERVER_CONNECTION)
        }
        return BroadcastResult(BroadcastResultType.SUCCESS)
    }

    override fun ethFeeSpendable(): Value = accountBalance.spendable

    override suspend fun cancelTransaction(
        record: com.mycelium.wapi.wallet.EthTransactionSummary,
        estimate: CancelEstimate,
    ): BroadcastResult {
        val signingCredentials = credentials
            ?: return BroadcastResult("Account has no credentials", BroadcastResultType.REJECT_INVALID_TX_PARAMS)

        // Cancel pattern: 0-value self-transfer at the same nonce. If the
        // sending account has an EIP-7702 delegate set, the delegate code
        // runs with empty calldata when `to == self`. We over-provision
        // gasLimit so a benign fallback() can complete; with only the 21000
        // intrinsic budget the delegate would out-of-gas immediately.
        // Unused gas is refunded; if the delegate actively reverts, the
        // larger budget burns more — we accept this tradeoff because the
        // alternative (sending to a different address) carries permanent
        // foot-gun risk if a future code change lets a non-zero value slip
        // through.
        val rawTransaction = RawTransaction.createTransaction(
            estimate.nonce,
            estimate.newGasPrice,
            estimate.gasLimit,
            receivingAddress.addressString,
            BigInteger.ZERO,
            "",
        )
        val signedBytes = TransactionEncoder.signMessage(rawTransaction, chainId, signingCredentials)
        val signedHex = Numeric.toHexString(signedBytes)
        val cancelTxHash = Hash.sha3(signedBytes)

        return try {
            val result = withContext(Dispatchers.IO) { blockchainService.sendTransaction(signedHex) }
            if (!result.success) {
                return BroadcastResult(result.message, BroadcastResultType.REJECT_INVALID_TX_PARAMS)
            }
            val cancelTxid = "0x" + HexUtils.toHex(cancelTxHash)
            backing.putTransaction(
                -1,
                System.currentTimeMillis() / 1000,
                cancelTxid,
                signedHex,
                receivingAddress.addressString,
                receivingAddress.addressString,
                Value.zeroValue(coinType),
                valueOf(coinType, estimate.newGasPrice * estimate.gasLimit),
                0,
                estimate.nonce,
                estimate.newGasPrice,
                gasLimit = estimate.gasLimit,
            )
            // Mark the original as CANCELED locally so the UI updates immediately.
            // The next sync's reconcileLocalPending() will reconfirm this once
            // the cancel mines.
            backing.markTransactionStatus("0x" + HexUtils.toHex(record.id), EthTxStatus.CANCELED)
            bumpNonceAfterBroadcast(estimate.nonce)
            updateBalanceCache()
            BroadcastResult(BroadcastResultType.SUCCESS)
        } catch (e: IOException) {
            BroadcastResult(BroadcastResultType.NO_SERVER_CONNECTION)
        }
    }

    override val coinType
        get() = accountContext.currency

    override val basedOnCoinType
        get() = coinType

    override val accountBalance
        get() = accountContext.balance

    override fun getNonce() = accountContext.nonce

    override fun setNonce(nonce: BigInteger) {
        accountContext.nonce = nonce
    }

    override suspend fun doSynchronization(mode: SyncMode?): Boolean {
        val syncTx = syncTransactions()
        updateBalanceCache()
        return syncTx
    }

    override fun updateBalanceCache(): Boolean {
        val balResponse: BalanceResponse?
        try {
            balResponse = blockchainService.getBalance(receivingAddress.addressString)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Couldn't update eth balance:  ${e.javaClass} ${e.localizedMessage}. Using cached value.")
            return false
        }

        var pendingReceiving = BigInteger.ZERO
        var pendingSending = BigInteger.ZERO
        if (balResponse.unconfirmed <= BigInteger.ZERO) {
            pendingSending = balResponse.unconfirmed.negate()
        } else {
            pendingReceiving = balResponse.unconfirmed
        }

        val newBalance = Balance(valueOf(coinType, balResponse.confirmed - pendingSending),
                valueOf(coinType, pendingReceiving), valueOf(coinType, pendingSending), Value.zeroValue(coinType))
        if (newBalance != accountContext.balance) {
            accountContext.balance = newBalance
            accountListener?.balanceUpdated(this)
            return true
        }
        return false
    }

    override fun canSign() = credentials != null

    private suspend fun syncTransactions(): Boolean {
        try {
            val remoteTransactions = withContext(Dispatchers.IO) { blockchainService.getTransactions(receivingAddress.addressString) }
            backing.putTransactions(remoteTransactions, coinType, typicalEstimatedTransactionSize.toBigInteger())
            val remoteTransactionsIds = remoteTransactions.map { it.txid }.toSet()

            // Blockbook's address?details=txs endpoint silently drops pending
            // txs at times. Walk the txids endpoint (which DOES include
            // pending at the top) and fetch any unknown txid via the per-tx
            // endpoint so we don't lose visibility of in-flight sends.
            val candidateTxids = try {
                withContext(Dispatchers.IO) { blockchainService.getAddressTxids(receivingAddress.addressString) }
            } catch (_: IOException) { emptyList() }
            val locallyKnownTxids = getUnconfirmedTransactions()
                .map { "0x" + HexUtils.toHex(it.id) }
                .toSet()
            candidateTxids.asSequence()
                .filter { it !in remoteTransactionsIds && it !in locallyKnownTxids }
                .take(MAX_PENDING_SCAN)
                .forEach { txid ->
                    try {
                        val tx = withContext(Dispatchers.IO) { blockchainService.getTransaction(txid) }
                        if (tx.confirmations.signum() != 0) return@forEach
                        backing.putTransactions(listOf(tx), coinType, typicalEstimatedTransactionSize.toBigInteger())
                    } catch (_: IOException) { /* skip, try next sync */ }
                }

            // Replace the old "delete after 150s if missing" logic. We now
            // keep the row and mark it REPLACED / CANCELED / DROPPED
            // depending on what the address history reveals. Pass the
            // already-fetched address tx list so the helper doesn't refetch.
            val knownAlive = remoteTransactionsIds + candidateTxids
            reconcileLocalPending(remoteTransactions, knownAlive)
            return true
        } catch (e: IOException) {
            lastSyncInfo = SyncStatusInfo(SyncStatus.ERROR)
            logger.log(Level.SEVERE, "EthAccount: Error retrieving ETH/ERC-20 transaction history: ${e.javaClass} ${e.localizedMessage}")
            return false
        }
    }

    companion object {
        private const val MAX_PENDING_SCAN = 20
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

    override fun isVisible() = true

    override fun isDerivedFromInternalMasterseed() = true

    override val id: UUID
        get() = credentials?.ecKeyPair?.toUUID()
            ?: UUID.nameUUIDFromBytes(receivingAddress.getBytes())

    override fun broadcastOutgoingTransactions() = true

    override fun calculateMaxSpendableAmount(gasPrice: Value, ign: EthAddress?, txData: TransactionData?): Value {
        val gp =
            (txData as? EthTransactionData)?.suggestedGasPrice?.let { Value.valueOf(coinType, it) } ?: gasPrice
        val gl = (txData as? EthTransactionData)?.gasLimit ?: typicalEstimatedTransactionSize.toBigInteger()
        val spendable = accountBalance.spendable - gp * gl
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

    override val syncTotalRetrievedTransactions: Int = 0 // TODO implement after full transaction history implementation

    override val typicalEstimatedTransactionSize = Transfer.GAS_LIMIT.toInt()

    override fun getPrivateKey(cipher: KeyCipher): InMemoryPrivateKey {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}


fun ECKeyPair.toUUID(): UUID = UUID(
        BitUtils.uint64ToLong(publicKey.toByteArray(), 8),
        BitUtils.uint64ToLong(publicKey.toByteArray(), 16))
