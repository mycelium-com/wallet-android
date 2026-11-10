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
import com.google.common.base.Optional
import com.mrd.bitlib.crypto.BipDerivationType
import com.mycelium.wapi.wallet.ExportableAccount
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
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsForFeeException
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
        backing, blockchainService, EthAccount::class.simpleName, address), SyncPausable, ExportableAccount {

    val enabledTokens: List<String>
        get() = accountContext.enabledTokens ?: listOf()

    val accountIndex: Int
        get() = accountContext.accountIndex

    /**
     * Whether this account has walked its whole address history at least once.
     * ERC20 children no longer decide their own sweep from this — they track it
     * per token — but they still borrow the parent's confirmed-txid stop set,
     * which is only meaningful once the parent itself is fully swept.
     */
    val everFullySynced: Boolean
        get() = accountContext.everFullySynced

    fun updateEnabledTokens() {
        accountContext.updateEnabledTokens()
    }
    override fun hasHadActivity(): Boolean =
            accountBalance.spendable.isPositive() || accountContext.nonce > BigInteger.ZERO

    @Throws(InsufficientFundsException::class, BuildTransactionException::class)
    override fun createTx(toAddress: Address, value: Value, fee: Fee, data: TransactionData?): Transaction {
        val ethTxData = data as? EthTransactionData
        val nonce = ethTxData?.nonce ?: accountContext.nonce
        val inputData = ethTxData?.inputData ?: ""
        val gasLimit = ethTxData?.gasLimit ?: defaultGasLimit(inputData)
        val gasPrice = ethTxData?.suggestedGasPrice?.let { Value.valueOf(coinType, it) } ?: (fee as FeePerKbFee).feePerKb

        if (toAddress !is EthAddress) {
            throw BuildTransactionException(Throwable("Not an Ethereum address"))
        }
        if (gasPrice.value <= BigInteger.ZERO) {
            throw BuildTransactionException(Throwable("Gas price should be positive and non-zero"))
        }
        if (value.value < BigInteger.ZERO) {
            throw BuildTransactionException(Throwable("Value should be positive"))
        }
        if (gasLimit < Transfer.GAS_LIMIT) {
            throw BuildTransactionException(Throwable("Gas limit must be at least ${Transfer.GAS_LIMIT}"))
        }
        if (gasLimit > MAX_TX_GAS_LIMIT) {
            throw BuildTransactionException(Throwable("Gas limit must not exceed $MAX_TX_GAS_LIMIT"))
        }
        // max spendable is clamped at zero, so a zero-value tx would pass with a fee above the balance
        if (gasPrice.value * gasLimit > accountBalance.spendable.value) {
            throw InsufficientFundsForFeeException(Throwable("Insufficient funds to pay for fee"))
        }
        if (value > calculateMaxSpendableAmount(gasPrice, null, ethTxData)) {
            // the amount alone fits, only amount + fee does not
            if (value <= accountBalance.spendable) {
                throw InsufficientFundsForFeeException(Throwable("Insufficient funds to pay for fee"))
            }
            throw InsufficientFundsException(Throwable("Insufficient funds to send " + Convert.fromWei(value.value.toBigDecimal(), Convert.Unit.ETHER) +
                    " ether with gas price " + Convert.fromWei(gasPrice.valueAsBigDecimal, Convert.Unit.GWEI) + " gwei"))
        }
        if (ethTxData?.suggestedGasPrice != null) {
            checkGasPriceAgainstBaseFee(coinType, gasPrice.value)
        }
        // EIP-1559: treat the chosen gas price as the maxFeePerGas cap and pair
        // it with the matching tip (explicit, or recovered from the snapshot).
        // If no tip is available, fall back to a legacy (type-0) tx.
        val tip = eip1559Tip(coinType, gasPrice.value, ethTxData?.maxPriorityFeePerGas)
        return if (tip != null) {
            EthTransaction(coinType, toAddress.toString(), value, gasPrice.value, nonce, gasLimit, inputData,
                    maxFeePerGas = gasPrice.value, maxPriorityFeePerGas = tip)
        } else {
            EthTransaction(coinType, toAddress.toString(), value, gasPrice.value, nonce, gasLimit, inputData)
        }
    }

    override fun signTx(request: Transaction, keyCipher: KeyCipher) {
        request as EthTransaction
        val rawTransaction = if (request.isEip1559) {
            // type-2: chainId is embedded in the envelope, gasPrice is ignored.
            RawTransaction.createTransaction(chainId, request.nonce, request.gasLimit, request.toAddress,
                    request.ethValue.value, request.inputData,
                    request.maxPriorityFeePerGas, request.maxFeePerGas)
        } else {
            RawTransaction.createTransaction(request.nonce, request.gasPrice, request.gasLimit,
                    request.toAddress, request.ethValue.value, request.inputData)
        }
        val signedMessage = if (request.isEip1559) {
            TransactionEncoder.signMessage(rawTransaction, credentials)
        } else {
            TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
        }
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
        tx as EthTransaction
        val signedHex = tx.signedHex
        val hash = tx.txHash
        if (signedHex == null || hash == null) {
            return BroadcastResult("Transaction is not signed", BroadcastResultType.REJECT_INVALID_TX_PARAMS)
        }
        val newTxid = "0x" + HexUtils.toHex(hash)
        // A lost response is checked against the node and re-sent as the same bytes;
        // UNKNOWN_OUTCOME means the tx may be on the network.
        val result = sendSignedTx(signedHex, newTxid)
        val accepted = result.resultType == BroadcastResultType.SUCCESS
        if (!accepted && result.resultType != BroadcastResultType.UNKNOWN_OUTCOME) {
            return result
        }
        // Stored as PENDING in both cases: an UNKNOWN_OUTCOME tx keeps its amount + gas reserved
        // and shows in the history; reconcileLocalPending() marks it DROPPED after
        // DROP_GRACE_SECONDS if no endpoint ever sees it, or REPLACED once another tx mines at its nonce.
        // For type-2 txs the persisted/displayed price is the cap
        // (maxFeePerGas); effectiveGasPrice resolves to gasPrice for legacy.
        // Once the tx mines, sync overwrites this with the real on-chain
        // effective price + gasUsed.
        val price = tx.effectiveGasPrice
        backing.putTransaction(-1, System.currentTimeMillis() / 1000, newTxid,
                signedHex, receivingAddress.addressString, tx.toAddress, tx.ethValue,
                valueOf(coinType, price * tx.gasLimit), 0, tx.nonce, price, gasLimit = tx.gasLimit)
        if (accepted) {
            markPendingReplacedByNonce(tx.nonce, newTxid)
            bumpNonceAfterBroadcast(tx.nonce)
        }
        // UNKNOWN_OUTCOME deliberately leaves the nonce alone: a retry then reuses this nonce,
        // so it and the lost tx are mutually exclusive — at most one can ever mine (if the lost
        // one is in the mempool the node refuses the retry as an underpriced replacement).
        // The next sync's getNewNonce() moves past it once the node reports it pending.
        // Reflect the outgoing amount immediately. Without this the screen
        // keeps showing the pre-send balance as spendable until the next
        // periodic sync, roughly a minute later.
        updateBalanceCache()
        return result
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
        val rawTransaction = if (estimate.isEip1559) {
            // type-2 replacement: bump both cap and tip (see estimateCancelTx).
            // chainId is embedded in the envelope.
            RawTransaction.createTransaction(
                chainId,
                estimate.nonce,
                estimate.gasLimit,
                receivingAddress.addressString,
                BigInteger.ZERO,
                "",
                estimate.newMaxPriorityFeePerGas,
                estimate.newMaxFeePerGas,
            )
        } else {
            RawTransaction.createTransaction(
                estimate.nonce,
                estimate.newGasPrice,
                estimate.gasLimit,
                receivingAddress.addressString,
                BigInteger.ZERO,
                "",
            )
        }
        val signedBytes = if (estimate.isEip1559) {
            TransactionEncoder.signMessage(rawTransaction, signingCredentials)
        } else {
            TransactionEncoder.signMessage(rawTransaction, chainId, signingCredentials)
        }
        val signedHex = Numeric.toHexString(signedBytes)
        val cancelTxHash = Hash.sha3(signedBytes)

        val cancelTxid = "0x" + HexUtils.toHex(cancelTxHash)
        return try {
            // Same lost-response recovery as broadcastTx. An UNKNOWN_OUTCOME cancel is returned as
            // is and nothing is recorded: it spends the original's nonce, so whatever happened at
            // most one of the two mines and sync resolves which.
            val result = withContext(Dispatchers.IO) { sendSignedTx(signedHex, cancelTxid) }
            if (result.resultType != BroadcastResultType.SUCCESS) {
                return result
            }
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

        // Pending comes from our own PENDING rows, not from the server. Blockbook
        // does not return an `unconfirmedBalance` field for Ethereum at all — the
        // key is simply absent from address?details=basic, in the middle of a
        // mempool window as much as outside one — so the old code deserialized it
        // to a default zero every time and no ETH account could ever show a
        // pending amount. An outgoing send therefore stayed fully spendable on
        // screen until it mined, and the user could go spend the same coins again.
        val pendingReceiving = pendingReceivingWei()
        val pendingSending = pendingSendingWei()

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

    /** Address as public data; the raw 32-byte private key (0x-prefixed hex, MetaMask-importable) as private data. */
    override fun getExportData(cipher: KeyCipher): ExportableAccount.Data =
        ExportableAccount.Data(
            Optional.fromNullable(credentials?.ecKeyPair?.privateKey?.let { Numeric.toHexStringWithPrefixZeroPadded(it, 64) }),
            mapOf(BipDerivationType.BIP44 to receivingAddress.addressString))

    /**
     * ETH leaving the account in still-unconfirmed transactions: the amount plus
     * the gas budgeted for it. Self-transfers move no value, so only their fee
     * counts. Only live PENDING rows are included — REPLACED / CANCELED / DROPPED
     * ones are unconfirmed too but will never settle.
     */
    private fun pendingSendingWei(): BigInteger {
        val own = receivingAddress.addressString
        return getLivePendingTransactions()
            .filter { it.sender.addressString.equals(own, true) }
            .fold(BigInteger.ZERO) { acc, tx ->
                val movesValue = !tx.receiver.addressString.equals(own, true)
                val fee = tx.fee?.value ?: BigInteger.ZERO
                acc + fee + (if (movesValue) tx.value.value else BigInteger.ZERO)
            }
    }

    /** ETH arriving in still-unconfirmed transactions. The sender pays their own gas. */
    private fun pendingReceivingWei(): BigInteger {
        val own = receivingAddress.addressString
        return getLivePendingTransactions()
            .filter {
                !it.sender.addressString.equals(own, true)
                        && it.receiver.addressString.equals(own, true)
            }
            .fold(BigInteger.ZERO) { acc, tx -> acc + tx.value.value }
    }

    private suspend fun syncTransactions(): Boolean {
        try {
            // Incremental sync: once the account has finished at least one
            // full sweep, blockbook's newest-first pagination can stop as
            // soon as it hits a confirmed tx we already store. The first
            // sweep (or any after archive→unarchive) does a full walk.
            val fullSweep = !accountContext.everFullySynced
            val knownConfirmed = if (fullSweep) emptySet() else backing.getConfirmedTxids()
            val remoteTransactions = withContext(Dispatchers.IO) {
                blockchainService.getTransactions(receivingAddress.addressString, knownConfirmedTxids = knownConfirmed)
            }
            cacheSyncTxs(remoteTransactions)
            backing.putTransactions(remoteTransactions, coinType, typicalEstimatedTransactionSize.toBigInteger())
            if (fullSweep) {
                // Mark only after the sweep + write succeed. An IOException
                // mid-pagination throws out of getTransactions() before we
                // reach this line, leaving the flag false so the next sync
                // re-attempts the full sweep.
                accountContext.everFullySynced = true
            }
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

        /** EIP-7825 (Fusaka) per-transaction gas limit cap: nodes reject anything above it. */
        @JvmField
        val MAX_TX_GAS_LIMIT: BigInteger = BigInteger.valueOf(16_777_216)

        /**
         * Gas limit when the user did not set one. Calldata is charged per token (zero byte = 1, non-zero = 4):
         * 4 gas per token of intrinsic cost, but since Pectra (EIP-7623) a tx must also cover the floor of
         * 10 gas per token — the floor is what a data-only transfer pays, and a lower limit is rejected
         * ("IntrinsicGas"). Execution cost of a contract call is not estimated; set the limit manually for that.
         */
        @JvmStatic
        fun defaultGasLimit(inputData: String?): BigInteger {
            val bytes = inputData?.takeIf { it.isNotBlank() }?.let {
                try {
                    Numeric.hexStringToByteArray(it)
                } catch (e: Exception) {
                    null
                }
            } ?: return Transfer.GAS_LIMIT
            val tokens = bytes.sumOf { if (it.toInt() == 0) 1L else 4L }
            return Transfer.GAS_LIMIT + BigInteger.valueOf(tokens * CALLDATA_FLOOR_GAS_PER_TOKEN)
        }

        private const val CALLDATA_FLOOR_GAS_PER_TOKEN = 10L
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
        // Backing is now empty — force the next sync to do a full sweep
        // before re-enabling incremental, otherwise we'd stop on page 1
        // with no local history to compare against.
        accountContext.everFullySynced = false
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
        val gl = (txData as? EthTransactionData)?.let { it.gasLimit ?: defaultGasLimit(it.inputData) }
            ?: typicalEstimatedTransactionSize.toBigInteger()
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
