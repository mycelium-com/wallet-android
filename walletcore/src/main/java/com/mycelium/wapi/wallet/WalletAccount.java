package com.mycelium.wapi.wallet;

import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.coins.Balance;
import com.mycelium.wapi.wallet.coins.CoinType;
import com.mycelium.wapi.wallet.exceptions.TransactionBroadcastException;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public interface WalletAccount<T extends GenericTransaction, A extends GenericAddress> {
    class WalletAccountException extends Exception {
        public WalletAccountException(Throwable cause) {
            super(cause);
        }

        public WalletAccountException(String s) {
            super(s);
        }
    }

    void completeAndSignTx(SendRequest<T> request) throws WalletAccountException;

    void completeTransaction(SendRequest<T> request) throws WalletAccountException;

    void signTransaction(SendRequest<T> request) throws WalletAccountException;

    void broadcastTx(T tx) throws TransactionBroadcastException;

    CoinType getCoinType();

    Balance getAccountBalance();

    T getTransaction(Sha256Hash transactionId);

    List<GenericTransaction> getTransactions(int offset, int limit);

    /**
     * Synchronize this account
     * <p/>
     * This method should only be called from the wallet manager
     *
     * @param mode synchronization parameter
     * @return false if synchronization failed due to failed blockchain
     * connection
     */
    boolean synchronize(SyncMode mode);

    /**
     * Get the block chain height as it were last time this account was
     * synchronized;
     *
     * @return the block chain height as it were last time this account was
     * synchronized;
     */
    int getBlockChainHeight();

    /**
     * Can this account be used for spending, or is it read-only?
     */
    boolean canSpend();

    /**
     * Is this account archived?
     * <p/>
     * An archived account is not tracked, and cannot be used until it has been
     * activated.
     */
    boolean isArchived();

    /**
     * Is this account active?
     * <p/>
     * An account is active if it is not archived
     * It is tracked and can be used.
     */
    boolean isActive();

    /**
     * Archive the account.
     * <p/>
     * An archived account is no longer tracked or monitored, and you cannot get
     * the current balance or transaction history from it. An end user would
     * archive an account to reduce network latency, storage, and CPU
     * requirements. This is in particular important for HD accounts, which
     * monitor an ever increasing set of addresses.
     * <p/>
     * An account that has been archived can always be unarchived without loss of
     * funds. When unarchiving the account needs to be synchronized.
     * <p/>
     * This method has no effect if the account is archived already.
     */
    void archiveAccount();

    /**
     * Activate an account.
     * <p/>
     * This puts an account into the active state. Only active accounts are
     * monitored and can be used. When activating an account that was archived is
     * needs to be synchronized before it can be used.
     * <p/>
     * This method has no effect if the account is already active.
     */
    void activateAccount();

    /**
     * In order to rescan an account.
     * <p/>
     * This causes the locally cached data to be dropped.
     * BalanceSatoshis and transaction history will get deleted.
     * Data will be re-created upon next synchronize.
     */
    void dropCachedData();

    /**
     * Is the account visible in UI
     */
    boolean isVisible();

    /**
     * Returns true, if this account is based on the internal masterseed.
     */
    boolean isDerivedFromInternalMasterseed();
    /**
     * Returns account id
     */
    UUID getId();

    /**
     * Returns true, if this account is currently in process of synchronization.
     */
    boolean isSynchronizing();
}
