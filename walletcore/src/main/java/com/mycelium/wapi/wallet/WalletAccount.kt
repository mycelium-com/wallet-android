package com.mycelium.wapi.wallet

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.exceptions.*
import java.util.*

interface WalletAccount<A : Address> : SyncPausable {
    fun setAllowZeroConfSpending(b: Boolean)

    @Throws(BuildTransactionException::class, InsufficientFundsException::class, OutputTooSmallException::class)
    fun createTx(address: Address, amount: Value, fee: Fee, data: TransactionData?): Transaction

    @Throws(BuildTransactionException::class, InsufficientFundsException::class, OutputTooSmallException::class)
    fun createTx(outputs: List<Pair<Address, Value>>, fee: Fee, data: TransactionData?): Transaction

    @Throws(InvalidKeyCipher::class)
    fun signTx(request: Transaction, keyCipher: KeyCipher)

    @Throws(TransactionBroadcastException::class)
    fun broadcastTx(tx: Transaction): BroadcastResult

    /**
     * Get current receive address
     */
    val receiveAddress: A?
    val coinType: CryptoCurrency

    /**
     * Some assets could base on another assets. For example Colu protocol is implemented
     * on top of BTC. By this reason, fee should be specified in BTC, not in Colu tokens
     * By default, based on coin type returns same as getCoinType
     * @return coin type based on
     */
    val basedOnCoinType: CryptoCurrency
    val accountBalance: Balance

    /**
     * Determine whether an address is one of our own addresses
     *
     * @param address the address to check
     * @return true iff this address is one of our own
     */
    fun isMineAddress(address: Address?): Boolean
    fun isExchangeable(): Boolean
    fun getTx(transactionId: ByteArray): Transaction?
    fun getTxSummary(transactionId: ByteArray): TransactionSummary?

    /**
     * @return transactions in reversed order (last added goes first)
     */
    fun getTransactionSummaries(offset: Int, limit: Int): List<TransactionSummary>

    /**
     * Get the transaction history of this account since the stated timestamp in milliseconds
     * @param receivingSince only include tx younger than this
     */
    fun getTransactionsSince(receivingSince: Long): List<TransactionSummary>
    fun getUnspentOutputViewModels(): List<OutputViewModel>
    var label: String
    fun isSpendingUnconfirmed(tx: Transaction): Boolean
    fun hasHadActivity(): Boolean

    /**
     * Synchronize this account
     *
     *
     * This method should only be called from the wallet manager
     *
     * @param mode synchronization parameter
     * @return false if synchronization failed due to failed blockchain
     * connection
     */
    suspend fun synchronize(mode: SyncMode?): Boolean

    /**
     * Get the block chain height as it were last time this account was
     * synchronized;
     *
     * @return the block chain height as it were last time this account was
     * synchronized;
     */
    fun getBlockChainHeight(): Int

    /**
     * Can this account be used for spending, or is it read-only?
     */
    fun canSpend(): Boolean

    /**
     * Can this account be used for signing messages?
     */
    fun canSign(): Boolean

    fun signMessage(message: String, address: Address?): String

    /**
     * Get is account sync in progress
     */
    fun isSyncing(): Boolean

    /**
     * Is this account archived?
     *
     *
     * An archived account is not tracked, and cannot be used until it has been
     * activated.
     */
    val isArchived: Boolean

    /**
     * Is this account active?
     *
     *
     * An account is active if it is not archived
     * It is tracked and can be used.
     */
    val isActive: Boolean

    /**
     * Archive the account.
     *
     *
     * An archived account is no longer tracked or monitored, and you cannot get
     * the current balance or transaction history from it. An end user would
     * archive an account to reduce network latency, storage, and CPU
     * requirements. This is in particular important for HD accounts, which
     * monitor an ever increasing set of addresses.
     *
     *
     * An account that has been archived can always be unarchived without loss of
     * funds. When unarchiving the account needs to be synchronized.
     *
     *
     * This method has no effect if the account is archived already.
     */
    fun archiveAccount()

    /**
     * Activate an account.
     *
     *
     * This puts an account into the active state. Only active accounts are
     * monitored and can be used. When activating an account that was archived is
     * needs to be synchronized before it can be used.
     *
     *
     * This method has no effect if the account is already active.
     */
    fun activateAccount()

    /**
     * In order to rescan an account.
     *
     *
     * This causes the locally cached data to be dropped.
     * BalanceSatoshis and transaction history will get deleted.
     * Data will be re-created upon next synchronize.
     */
    fun dropCachedData()

    /**
     * Is the account visible in UI
     */
    fun isVisible(): Boolean

    /**
     * Returns true, if this account is based on the internal masterseed.
     */
    fun isDerivedFromInternalMasterseed(): Boolean

    /**
     * Returns account id
     */
    val id: UUID
    fun broadcastOutgoingTransactions(): Boolean
    fun removeAllQueuedTransactions()

    /**
     * Determine the maximum spendable amount you can send in a transaction
     * Destination address can be null
     */
    fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value, destinationAddress: A?, txData: TransactionData?): Value

    /**
     * Returns the number of retrieved transactions during synchronization
     */
    val syncTotalRetrievedTransactions: Int
    val typicalEstimatedTransactionSize: Int

    /**
     * Returns the private key used by the account to sign transactions
     */
    @Throws(InvalidKeyCipher::class)
    fun getPrivateKey(cipher: KeyCipher): InMemoryPrivateKey?
    val dummyAddress: A
    fun getDummyAddress(subType: String): A
    val dependentAccounts: List<WalletAccount<*>>

    /**
     * Queue a transaction for broadcasting.
     *
     *
     * The transaction is broadcast on next synchronization.
     *
     * @param transaction     an transaction
     */
    fun queueTransaction(transaction: Transaction)
}