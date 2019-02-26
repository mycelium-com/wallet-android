package com.mycelium.wapi.wallet;

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.coins.Balance;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.exceptions.GenericBuildTransactionException;
import com.mycelium.wapi.wallet.exceptions.GenericInsufficientFundsException;
import com.mycelium.wapi.wallet.exceptions.GenericOutputTooSmallException;
import com.mycelium.wapi.wallet.exceptions.GenericTransactionBroadcastException;

import java.util.List;
import java.util.UUID;

public interface WalletAccount<T extends GenericTransaction, A extends GenericAddress> {
    void setAllowZeroConfSpending(boolean b);

    void completeTransaction(SendRequest<T> request) throws GenericBuildTransactionException, GenericInsufficientFundsException, GenericOutputTooSmallException;

    void signTransaction(SendRequest<T> request, KeyCipher keyCipher) throws KeyCipher.InvalidKeyCipher;

    BroadcastResult broadcastTx(T tx) throws GenericTransactionBroadcastException;

    /**
     * Get current receive address
     */
    GenericAddress getReceiveAddress();

    CryptoCurrency getCoinType();

    Balance getAccountBalance();

    /**
     * Determine whether an address is one of our own addresses
     *
     * @param address the address to check
     * @return true iff this address is one of our own
     */
    boolean isMineAddress(GenericAddress address);

    boolean isExchangeable();

    T getTx(Sha256Hash transactionId);

    List<T> getTransactions(int offset, int limit);

    /**
     * Get the transaction history of this account since the stated timestamp
     * @param receivingSince only include tx older than this
     */
    List<T> getTransactionsSince(long receivingSince);

    SendRequest<T> getSendToRequest(A destination, Value amount, Value fee);

    List<GenericTransaction.GenericOutput> getUnspentOutputs();

    String getLabel();

    void setLabel(String label);

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

    boolean broadcastOutgoingTransactions();

    void removeAllQueuedTransactions();

    /**
     * Determine the maximum spendable amount you can send in a transaction
     * Destination address can be null
     */
    Value calculateMaxSpendableAmount(long minerFeePerKilobyte, A destinationAddress);

    /**
     * Returns the number of retrieved transactions during synchronization
     */
    int getSyncTotalRetrievedTransactions();

    FeeEstimationsGeneric getFeeEstimations();

    int getTypicalEstimatedTransactionSize();

    /**
     * Returns the private key used by the account to sign transactions
     */
    InMemoryPrivateKey getPrivateKey(KeyCipher cipher) throws KeyCipher.InvalidKeyCipher;

    A getDummyAddress();

    A getDummyAddress(String subType);
}
