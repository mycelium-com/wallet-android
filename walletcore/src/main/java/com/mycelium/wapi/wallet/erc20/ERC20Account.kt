package com.mycelium.wapi.wallet.erc20

import com.mrd.bitlib.crypto.InMemoryPrivateKey
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
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.AbstractEthERC20Account
import com.mycelium.wapi.wallet.eth.CancelEstimate
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.EthAddress
import com.mycelium.wapi.wallet.eth.EthBlockchainService
import com.mycelium.wapi.wallet.eth.EthTransaction
import com.mycelium.wapi.wallet.eth.EthTransactionData
import com.mycelium.wapi.wallet.eth.EthTxStatus
import com.mycelium.wapi.wallet.eth.Tx
import com.mycelium.wapi.wallet.exceptions.BuildTransactionException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsException
import com.mycelium.wapi.wallet.exceptions.InsufficientFundsForFeeException
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.Hash
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.tx.Transfer
import org.web3j.utils.Numeric
import java.io.IOException
import java.math.BigInteger
import java.util.UUID
import java.util.logging.Level


class ERC20Account(val chainId: Long,
                   private val accountContext: ERC20AccountContext,
                   private val token: ERC20Token,
                   val ethAcc: EthAccount,
                   credentials: Credentials? = null,
                   backing: EthAccountBacking,
                   private val accountListener: AccountListener?,
                   blockchainService: EthBlockchainService,
                   address: EthAddress? = null) : AbstractEthERC20Account(accountContext.currency, credentials,
        backing, blockchainService, ERC20Account::class.simpleName, address), SyncPausable {

    override fun createTx(address: Address, amount: Value, fee: Fee, data: TransactionData?): Transaction {
        if (address !is EthAddress) {
            throw BuildTransactionException(Throwable("Not an Ethereum address"))
        }
        val ethTxData = (data as? EthTransactionData)
        val nonce = ethTxData?.nonce ?: accountContext.nonce
        val gasLimit = ethTxData?.gasLimit ?: BigInteger.valueOf(TOKEN_TRANSFER_GAS_LIMIT)
        val gasPrice = ethTxData?.suggestedGasPrice?.let {
            Value.valueOf(basedOnCoinType, it)
        } ?: (fee as FeePerKbFee).feePerKb
        val inputData = getInputData(address.toString(), amount.value)
        val estimatedGasUsed = ethTxData?.gasLimit?.toInt() ?: ((Transfer.GAS_LIMIT.toInt() + TOKEN_TRANSFER_GAS_LIMIT) / 2).toInt()

        if (calculateMaxSpendableAmount(gasPrice, null, ethTxData) < amount) {
            throw InsufficientFundsException(Throwable("Insufficient funds"))
        }
        if (gasLimit < Transfer.GAS_LIMIT) {
            throw BuildTransactionException(Throwable("Gas limit must be at least ${Transfer.GAS_LIMIT}"))
        }
        if (gasLimit > EthAccount.MAX_TX_GAS_LIMIT) {
            throw BuildTransactionException(Throwable("Gas limit must not exceed ${EthAccount.MAX_TX_GAS_LIMIT}"))
        }
        // gas paid in the token (paymaster): the ETH account is not touched
        if (ethTxData?.tokenFee == null && ethAcc.accountBalance.spendable.value < gasPrice.value * gasLimit) {
            throw InsufficientFundsForFeeException(Throwable("Insufficient funds on eth account to pay for fee"))
        }

        // Gas is paid in ETH, so the rollout flag lives on the ETH coin
        // (basedOnCoinType). When 1559-enabled with a recoverable tip, send the
        // token transfer as a type-2 tx; otherwise legacy.
        if (ethTxData?.suggestedGasPrice != null) {
            checkGasPriceAgainstBaseFee(basedOnCoinType, gasPrice.value)
        }
        val tip = eip1559Tip(basedOnCoinType, gasPrice.value, ethTxData?.maxPriorityFeePerGas)
        return if (tip != null) {
            EthTransaction(basedOnCoinType, address.toString(), Value.zeroValue(basedOnCoinType),
                gasPrice.value, nonce, gasLimit, inputData, estimatedGasUsed, amount,
                maxFeePerGas = gasPrice.value, maxPriorityFeePerGas = tip)
        } else {
            EthTransaction(basedOnCoinType, address.toString(), Value.zeroValue(basedOnCoinType),
                gasPrice.value, nonce, gasLimit, inputData, estimatedGasUsed, amount)
        }
    }

    private fun getInputData(address: String, value: BigInteger): String {
        val function = org.web3j.abi.datatypes.Function(
                StandardToken.FUNC_TRANSFER,
                listOf(org.web3j.abi.datatypes.Address(address),
                        Uint256(value)), emptyList())
        return FunctionEncoder.encode(function)
    }

    override fun signTx(request: Transaction, keyCipher: KeyCipher) {
        val ethTx = request as EthTransaction
        val rawTransaction = if (ethTx.isEip1559) {
            // type-2: chainId is embedded in the envelope, gasPrice is ignored.
            // `to` is the token contract; inputData carries the transfer() call.
            RawTransaction.createTransaction(chainId, ethTx.nonce, ethTx.gasLimit, token.contractAddress,
                    ethTx.ethValue.value, ethTx.inputData,
                    ethTx.maxPriorityFeePerGas, ethTx.maxFeePerGas)
        } else {
            RawTransaction.createTransaction(ethTx.nonce, ethTx.gasPrice, ethTx.gasLimit,
                    token.contractAddress, ethTx.ethValue.value, ethTx.inputData)
        }
        val signedMessage = if (ethTx.isEip1559) {
            TransactionEncoder.signMessage(rawTransaction, credentials)
        } else {
            TransactionEncoder.signMessage(rawTransaction, chainId, credentials)
        }
        val hexValue = Numeric.toHexString(signedMessage)
        request.apply {
            signedHex = hexValue
            // keccak256 over the actual signed bytes — same hash the network computes.
            txHash = Hash.sha3(signedMessage)
            txBinary = TransactionEncoder.encode(rawTransaction)!!
        }
    }

    override fun broadcastTx(tx: Transaction): BroadcastResult {
        try {
            val result = blockchainService.sendTransaction((tx as EthTransaction).signedHex!!)
            if (!result.success) {
                return BroadcastResult(result.message, BroadcastResultType.REJECT_INVALID_TX_PARAMS)
            }
            val newTxid = "0x" + HexUtils.toHex(tx.txHash)
            // type-2 → store the cap (maxFeePerGas); legacy → the gas price.
            val price = tx.effectiveGasPrice
            backing.putTransaction(-1, System.currentTimeMillis() / 1000, newTxid,
                    tx.signedHex!!, receivingAddress.addressString, tx.toAddress,
                    Value.valueOf(basedOnCoinType, tx.tokenValue!!.value), Value.valueOf(basedOnCoinType, price * tx.gasLimit), 0,
                    tx.nonce, price, true, null, true, tx.gasLimit, tx.estimatedGasUsed.toBigInteger())
            markPendingReplacedByNonce(tx.nonce, newTxid)
            bumpNonceAfterBroadcast(tx.nonce)
            // ERC20 shares an on-chain address with its parent EthAccount, so
            // bump there too to prevent the next ETH or token send from reusing
            // this nonce before the next sync, and mirror the REPLACED marking
            // since the nonce is shared across both accounts' backings.
            ethAcc.markPendingReplacedByNonce(tx.nonce, newTxid)
            ethAcc.bumpNonceAfterBroadcast(tx.nonce)
            // Show the outgoing tokens as pending right away instead of after the
            // next periodic sync, and refresh the parent too — the gas for this
            // transfer is now committed ETH.
            updateBalanceCache()
            ethAcc.updateBalanceCache()
            return BroadcastResult(BroadcastResultType.SUCCESS)
        } catch (e: Exception) {
            return when (e) {
                is IOException -> BroadcastResult(BroadcastResultType.NO_SERVER_CONNECTION)
                else -> {
                    logger.log(Level.SEVERE, "Error sending ERC-20 transaction: ${e.localizedMessage}")
                    BroadcastResult(e.localizedMessage, BroadcastResultType.REJECT_INVALID_TX_PARAMS)
                }
            }
        }
    }

    /**
     * Writes a transfer that was sent as a UserOperation (gas paid in the token) and has already
     * mined. Without it the balance stays at the pre-send value until blockbook indexes the
     * bundle, and a second send right after — "Max" above all — is priced against tokens that
     * are gone. [debited] is what left the account in this token: the amount plus the fee.
     * The next sync overwrites the row with blockbook's view of the same tx (same net value).
     */
    fun recordTokenPaidTransfer(txid: String, blockNumber: Long, recipient: String, debited: BigInteger) {
        backing.putTransaction(blockNumber.toInt(), System.currentTimeMillis() / 1000, txid, "",
                receivingAddress.addressString, recipient, Value.valueOf(basedOnCoinType, debited),
                // the gas was paid in ETH by the bundler, not by this account
                Value.zeroValue(basedOnCoinType), 1, BigInteger.ZERO, BigInteger.ZERO, true, null, true)
        updateBalanceCache()
    }

    override fun ethFeeSpendable(): Value = ethAcc.accountBalance.spendable

    override suspend fun cancelTransaction(
        record: com.mycelium.wapi.wallet.EthTransactionSummary,
        estimate: CancelEstimate,
    ): BroadcastResult {
        // Cancel of an ERC20 send is still an ETH self-transfer at the same
        // nonce, so the actual signing/broadcast lives on the parent
        // EthAccount. We additionally mirror the CANCELED status into our
        // own backing — the per-token sync only sees token transfers, so the
        // generic replacement detection won't otherwise pick it up here.
        val result = ethAcc.cancelTransaction(record, estimate)
        if (result.resultType == BroadcastResultType.SUCCESS) {
            backing.markTransactionStatus("0x" + HexUtils.toHex(record.id), EthTxStatus.CANCELED)
            bumpNonceAfterBroadcast(estimate.nonce)
            updateBalanceCache()
        }
        return result
    }

    override val coinType: ERC20Token
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
        val syncTx = syncTransactions()
        updateBalanceCache()
        return syncTx
    }

    override fun getNonce() = accountContext.nonce

    override fun setNonce(nonce: BigInteger) {
        accountContext.nonce = nonce
    }

    override fun setBlockChainHeight(height: Int) {
        accountContext.blockHeight = height
    }

    override fun getBlockChainHeight() = accountContext.blockHeight

    override val isArchived: Boolean
        get() = accountContext.archived

    override val isActive: Boolean
        get() = !isArchived

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
        // Backing is empty again, so the next sync has to walk the whole
        // address history before incremental mode can be trusted.
        accountContext.everFullySynced = false
    }

    override fun isVisible() = true

    override fun isDerivedFromInternalMasterseed() = false

    override val id: UUID
        get() = accountContext.uuid

    override fun broadcastOutgoingTransactions() = true

    /** The whole token balance, less the fee when the fee is paid in the token too. */
    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value, destinationAddress: EthAddress?, txData: TransactionData?): Value {
        val tokenFee = (txData as? EthTransactionData)?.tokenFee ?: return accountBalance.spendable
        return Value.valueOf(coinType, (accountBalance.spendable.value - tokenFee).max(BigInteger.ZERO))
    }

    override val syncTotalRetrievedTransactions = 0 // TODO implement after full transaction history implementation

    override val typicalEstimatedTransactionSize = TOKEN_TRANSFER_GAS_LIMIT.toInt()

    override fun getPrivateKey(cipher: KeyCipher): InMemoryPrivateKey {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateBalanceCache(): Boolean {
        val pendingReceiving = getPendingReceiving()
        val pendingSending = getPendingSending()
        val newBalance = Balance(Value.valueOf(basedOnCoinType, getConfirmed() - pendingSending),
                Value.valueOf(basedOnCoinType, pendingReceiving), Value.valueOf(basedOnCoinType, pendingSending), Value.zeroValue(basedOnCoinType))
        if (newBalance != accountContext.balance) {
            accountContext.balance = newBalance
            accountListener?.balanceUpdated(this)
            return true
        }
        return false
    }

    override fun signMessage(message: String, address: Address?): String {
        TODO("Not yet implemented")
    }

    private fun getConfirmed(): BigInteger = getTransactionSummaries(0, Int.MAX_VALUE)
            .filter { it.confirmations > 0 }
            .map { it.transferred.value }
            .fold(BigInteger.ZERO, BigInteger::add)

    private fun getPendingReceiving(): BigInteger = backing.getUnconfirmedTransactions(receivingAddress.addressString)
            .filter {
                // Only live pending counts toward balance. REPLACED / CANCELED
                // / DROPPED rows still have confirmations=0 but no funds will
                // arrive, so they must not inflate the pending balance.
                it.status == EthTxStatus.PENDING
                        && !it.sender.addressString.equals(receiveAddress.addressString, true)
                        && it.receiver.addressString.equals(receiveAddress.addressString, true)
            }
            .map { it.value.value }
            .fold(BigInteger.ZERO, BigInteger::add)

    private fun getPendingSending(): BigInteger = backing.getUnconfirmedTransactions(receivingAddress.addressString)
            .filter {
                it.status == EthTxStatus.PENDING
                        && it.sender.addressString.equals(receiveAddress.addressString, true)
                        && !it.receiver.addressString.equals(receiveAddress.addressString, true)
            }
            .map { it.value.value }
            .fold(BigInteger.ZERO, BigInteger::add)

    /**
     * Settle local PENDING rows whose transaction has actually mined but carries
     * no token transfer for us — the shape a reverted `transfer()` takes, since a
     * failed call emits no Transfer event and blockbook then returns an empty
     * `tokenTransfers` array.
     *
     * Such a row can never leave PENDING on its own: the token-filtered sync loop
     * skips it, and reconcileLocalPending() refuses to drop it because the tx is
     * demonstrably alive on-chain. It would sit in [getPendingSending] forever,
     * permanently deducting its amount from the displayed balance. Write the row
     * through with the real confirmations and on-chain status; the amount stops
     * counting because a failed token tx transfers nothing.
     */
    private fun settleRevertedTransfers(allRemoteTransactions: List<Tx>) {
        val livePending = getLivePendingTransactions()
        if (livePending.isEmpty()) return
        val minedByTxid = allRemoteTransactions
            .filter { it.confirmations.signum() > 0 }
            .associateBy { it.txid }
        livePending.forEach { local ->
            val txid = "0x" + HexUtils.toHex(local.id)
            val mined = minedByTxid[txid] ?: return@forEach
            if (mined.getTokenTransfer(token.contractAddress, receivingAddress.addressString) != null) return@forEach
            val gasForFee = mined.gasUsed?.takeIf { it.signum() > 0 } ?: mined.gasLimit
            backing.putTransaction(mined.blockHeight.toInt(), mined.blockTime, mined.txid, "",
                    local.sender.addressString, local.receiver.addressString, local.value,
                    Value.valueOf(basedOnCoinType, mined.gasPrice * gasForFee),
                    mined.confirmations.toInt(), mined.nonce, mined.gasPrice, true, null,
                    mined.success, mined.gasLimit, mined.gasUsed)
        }
    }

    private suspend fun syncTransactions():Boolean {
        try {
            // Fetch the full unfiltered address history once, cache it for
            // the parent getNewNonce() to reuse (avoids paginating twice),
            // then filter to this token's transfers for our local backing.
            //
            // Whether to sweep is OUR OWN decision — the parent's flag says
            // nothing about whether this token's transfers have been fetched.
            // Keying off the parent left a token account added to an
            // already-synced ETH account (or archived and re-activated) in
            // incremental mode from the very first sync: pagination stopped on
            // page 1 and the token's transfers, which sit further back in the
            // address history, were never fetched — a permanent zero balance.
            //
            // The stop set still comes from the parent, and that is deliberate:
            // the request paginates the ENTIRE address history, which the parent
            // stores in full while we keep only this token's subset. Matching
            // against our own sparse txids would almost never hit, so we could
            // never stop early. Once our own sweep has completed we hold every
            // transfer of ours that is older than the newest page, which is what
            // makes the parent's stop set safe to trust.
            val fullSweep = !accountContext.everFullySynced
            val knownConfirmed = if (fullSweep) emptySet() else ethAcc.confirmedTxidsSnapshot()
            val allRemoteTransactions = withContext(Dispatchers.IO) {
                blockchainService.getTransactions(receivingAddress.addressString, knownConfirmedTxids = knownConfirmed)
            }
            cacheSyncTxs(allRemoteTransactions)
            val remoteTransactions = allRemoteTransactions.filter { it.getTokenTransfer(token.contractAddress, receivingAddress.addressString) != null }
            //TODO convert backing.putTransaction to backing.putTransactions
            remoteTransactions.forEach { tx ->
                tx.getTokenTransfer(token.contractAddress, receivingAddress.addressString)?.also { tokenTransfer ->
                    val gasForFee = tx.gasUsed?.takeIf { it.signum() > 0 } ?: tx.gasLimit
                    backing.putTransaction(tx.blockHeight.toInt(), tx.blockTime, tx.txid, "", tokenTransfer.from,
                            tokenTransfer.to, Value.valueOf(basedOnCoinType, tokenTransfer.value),
                            Value.valueOf(basedOnCoinType, tx.gasPrice * gasForFee),
                            tx.confirmations.toInt(), tx.nonce, tx.gasPrice, true, null, tx.success, tx.gasLimit, tx.gasUsed)
                }
            }
            if (fullSweep) {
                // Only after the sweep and the writes above succeeded. An
                // IOException mid-pagination throws out of getTransactions()
                // before we get here, so the next sync retries the full sweep.
                accountContext.everFullySynced = true
            }
            settleRevertedTransfers(allRemoteTransactions)
            val remoteTransactionsIds = remoteTransactions.map { it.txid }.toSet()

            // Blockbook's contract-filtered response excludes pending contract
            // calls (no tokenTransfers yet). Also, the address?details=txs
            // endpoint silently drops pending txs entirely. Walk the `txids`
            // array from the default address endpoint (which DOES include
            // pending at the top) and fetch any unknown txid individually.
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
                        val tokenTransfer = tx.getTokenTransfer(token.contractAddress, receivingAddress.addressString)
                            ?: tx.getPendingTokenTransfer(token.contractAddress, receivingAddress.addressString)
                        tokenTransfer?.also { t ->
                            val gasForFee = tx.gasUsed?.takeIf { it.signum() > 0 } ?: tx.gasLimit
                            backing.putTransaction(tx.blockHeight.toInt(), tx.blockTime, tx.txid, "", t.from,
                                    t.to, Value.valueOf(basedOnCoinType, t.value),
                                    Value.valueOf(basedOnCoinType, tx.gasPrice * gasForFee),
                                    tx.confirmations.toInt(), tx.nonce, tx.gasPrice, true, null, tx.success, tx.gasLimit, tx.gasUsed)
                        }
                    } catch (_: IOException) { /* skip, try next sync */ }
                }

            // Mark stale local pending txs as REPLACED / CANCELED / DROPPED
            // instead of deleting. The helper queries the full ETH-level
            // address history to find a confirmed replacement with the same
            // from+nonce, then falls back to per-tx endpoint to confirm
            // disappearance before marking DROPPED.
            val knownAlive = remoteTransactionsIds + candidateTxids.toSet()
            reconcileLocalPending(knownAliveTxids = knownAlive)
            return true
        } catch (e: IOException) {
            lastSyncInfo = SyncStatusInfo(SyncStatus.ERROR)
            logger.log(Level.SEVERE, "ERC20Account Error retrieving ETH/ERC-20 transaction history: ${e.javaClass} ${e.localizedMessage}")
            return false
        }
    }

    // the following two wrappers are needed because we can't store balance in db with ERC20 coin type
    // we only can store balance in db with coinType within cryptocurrencies list
    // (refer to com.mycelium.wapi.wallet.coins.COIN for the list)
    // but on different activities we need balance with ERC20 coin type, therefore we need to convert
    // coinType -> basedOnCoinType (which is ETH for ERC20 and it's in the cryptocurrencies list)
    // all the time we going to store it in db, and basedOnCoinType -> coinType when we read from db
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
        const val TOKEN_TRANSFER_GAS_LIMIT = 90_000L
        val AVG_TOKEN_TRANSFER_GAS = (Transfer.GAS_LIMIT.toLong() + TOKEN_TRANSFER_GAS_LIMIT) / 2
        private const val MAX_PENDING_SCAN = 20
    }
}
