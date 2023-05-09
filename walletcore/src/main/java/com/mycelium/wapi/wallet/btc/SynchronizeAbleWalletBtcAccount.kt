package com.mycelium.wapi.wallet.btc

import com.google.common.collect.ImmutableMap
import com.mrd.bitlib.StandardTransactionBuilder.*
import com.mrd.bitlib.UnsignedTransaction
import com.mrd.bitlib.model.BitcoinTransaction
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.OutputList
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.SyncStatus
import com.mycelium.wapi.SyncStatusInfo
import com.mycelium.wapi.model.BalanceSatoshis
import com.mycelium.wapi.model.TransactionOutputSummary
import com.mycelium.wapi.model.TransactionSummary
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance
import java.util.*

abstract class SynchronizeAbleWalletBtcAccount : SyncPausableAccount(), WalletBtcAccount {
    private val _lastSync = HashMap<SyncMode.Mode, Date>(SyncMode.Mode.values().size)

    @Volatile
    private var isSyncing = false

    override var label: String = "Unknown"

    /**
     * Checks if the account needs to be synchronized, according to the provided SyncMode
     *
     * @param syncMode the requested sync mode
     * @return true if sync is needed
     */
    private fun needsSynchronization(syncMode: SyncMode?): Boolean {
        if (syncMode!!.ignoreSyncInterval) {
            return true
        }

        // check how long ago the last sync for this mode
        return Date().time - (_lastSync[syncMode.mode]?.time ?: 0L) > getSyncInterval(syncMode)!!
    }

    /**
     * Returns the normal sync interval for this mode
     * if synchronize() is called faster than this interval (and ignoreSyncInterval=false), the sync is disregarded
     *
     * @param syncMode the Mode to get the interval for
     * @return the interval in milliseconds
     */
    private fun getSyncInterval(syncMode: SyncMode?): Int? {
        return MIN_SYNC_INTERVAL[syncMode!!.mode]
    }

    /**
     * Synchronize this account
     *
     *
     * This method should only be called from the wallet manager
     *
     * @param mode set synchronization parameters
     * @return false if synchronization failed due to failed blockchain
     * connection
     */
    override suspend fun synchronize(mode: SyncMode?): Boolean {
        val fixMode = mode ?: SyncMode.NORMAL
        return if (needsSynchronization(fixMode)) {
            isSyncing = true
            try {
                val synced = doSynchronization(fixMode)
                // if sync went well, remember current time for this sync mode
                if (synced) {
                    _lastSync[fixMode!!.mode] = Date()
                    lastSyncInfo = SyncStatusInfo(SyncStatus.SUCCESS)
                }
                synced
            } finally {
                isSyncing = false
            }
        } else {
            true
        }
    }

    override fun isSyncing(): Boolean {
        return isSyncing && maySync
    }

    override fun isVisible(): Boolean {
        return true
    }

    /**
     * Do the necessary steps to synchronize this account.
     * This function has to be implemented for the individual accounts and will only be called, if it is
     * needed (according to various timeouts, etc)
     *
     * @param mode SyncMode
     * @return true if sync was successful
     */
    abstract suspend fun doSynchronization(mode: SyncMode): Boolean
    abstract override fun broadcastOutgoingTransactions(): Boolean
    abstract override fun broadcastTransaction(transaction: BitcoinTransaction): BroadcastResult
    @Throws(InvalidKeyCipher::class)
    abstract override fun signTransaction(unsigned: UnsignedTransaction, cipher: KeyCipher): BitcoinTransaction
    abstract override fun deleteTransaction(transactionId: Sha256Hash): Boolean
    abstract override fun cancelQueuedTransaction(transaction: Sha256Hash): Boolean
    abstract override fun getNetwork(): NetworkParameters
    abstract override fun calculateMaxSpendableAmount(minerFeeToUse: Value, destinationAddress: BtcAddress?, txData: TransactionData?): Value
    @Throws(BtcOutputTooSmallException::class, InsufficientBtcException::class, UnableToBuildTransactionException::class)
    abstract override fun createUnsignedTransaction(receivers: List<BtcReceiver>, minerFeeToUse: Long): UnsignedTransaction
    @Throws(BtcOutputTooSmallException::class, InsufficientBtcException::class, UnableToBuildTransactionException::class)
    abstract override fun createUnsignedTransaction(outputs: OutputList, minerFeeToUse: Long): UnsignedTransaction
    abstract override fun getBalance(): BalanceSatoshis
    abstract override fun getCurrencyBasedBalance(): CurrencyBasedBalance
    abstract override fun getUnspentTransactionOutputSummary(): List<TransactionOutputSummary>
    abstract override fun getTransactionSummary(txid: Sha256Hash): TransactionSummary
    override fun onlySyncWhenActive(): Boolean {
        return false
    }

    companion object {
        private val MIN_SYNC_INTERVAL = ImmutableMap.of(
            SyncMode.Mode.FAST_SYNC, 1000,
            SyncMode.Mode.ONE_ADDRESS, 1000,
            SyncMode.Mode.NORMAL_SYNC, 30 * 1000,
            SyncMode.Mode.FULL_SYNC, 120 * 1000
        )
    }
}