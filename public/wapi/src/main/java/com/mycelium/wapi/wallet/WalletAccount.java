package com.mycelium.wapi.wallet;

import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.OutputTooSmallException;
import com.mrd.bitlib.StandardTransactionBuilder.UnsignedTransaction;
import com.mrd.bitlib.crypto.RandomSource;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.model.Balance;
import com.mycelium.wapi.model.TransactionDetails;
import com.mycelium.wapi.model.TransactionOutputSummary;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public interface WalletAccount {

   public enum BroadcastResult { SUCCESS, REJECTED, NO_SERVER_CONNECTION};

   /**
    * Get the network that this account is for.
    *
    * @return the network that this account is for.
    */
   NetworkParameters getNetwork();

   /**
    * Get the unique ID of this account
    */
   UUID getId();

   /**
    * Get the block chain height as it were last time this account was
    * synchronized;
    *
    * @return the block chain height as it were last time this account was
    * synchronized;
    */
   int getBlockChainHeight();

   /**
    * Get the current receiving address of this account. Some accounts will
    * continuously use the same receiving address while others return new ones
    * as they get used.
    */
   Address getReceivingAddress();

   /**
    * Can this account be used for spending, or is it read-only?
    */
   boolean canSpend();

   /**
    * Get the current balance of this account based on the last synchronized
    * state.
    */
   Balance getBalance();

   /**
    * Get the transaction history of this account based on the last synchronized
    * state.
    *
    * @param offset the offset into the transaction history
    * @param limit  the maximum number of records to retrieve
    */
   List<TransactionSummary> getTransactionHistory(int offset, int limit);

   /**
    * Get the details of a transaction that originated from this account
    *
    * @param txid the ID of the transaction
    * @return the details of a transaction
    */
   TransactionDetails getTransactionDetails(Sha256Hash txid);

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
    * Create a new unsigned transaction sending funds to one or more addresses.
    * <p/>
    * The unsigned transaction must be signed and queued before it will affect
    * the transaction history.
    * <p/>
    * If you call this method twice without signing and queuing the unsigned
    * transaction you are likely to create another unsigned transaction that
    * double spends the first one. In other words, if you call this method and
    * do not sign and queue the unspent transaction, then you should discard the
    * unsigned transaction.
    *
    * @param receivers the receiving address and amount to send
    * @return an unsigned transaction.
    * @throws OutputTooSmallException    if one of the outputs were too small
    * @throws InsufficientFundsException if not enough funds were present to create the unsigned
    *                                    transaction
    */
   UnsignedTransaction createUnsignedTransaction(List<Receiver> receivers) throws OutputTooSmallException,
         InsufficientFundsException;

   /**
    * Sign an unsigned transaction without broadcasting it.
    *
    * @param unsigned     an unsigned transaction
    * @param cipher       the key cipher to use for decrypting the private key
    * @param randomSource a random source
    * @return the signed transaction.
    * @throws InvalidKeyCipher
    */
   Transaction signTransaction(UnsignedTransaction unsigned, KeyCipher cipher, RandomSource randomSource)
         throws InvalidKeyCipher;

   /**
    * Broadcast a transaction
    * @param transaction the transaction to broadcast
    * @return the broadcast result
    */
   BroadcastResult broadcastTransaction(Transaction transaction);

   /**
    * Queue a transaction for broadcasting.
    * <p/>
    * The transaction is broadcasted on next synchronization.
    *
    * @param transaction     an transaction
    */
   void queueTransaction(Transaction transaction);

   /**
    * Determine the maximum spendable amount you can send in a transaction
    */
   long calculateMaxSpendableAmount();

   /**
    * Determine whether the provided encryption key is valid for this wallet account.
    * <p/>
    * This function allows you to verify whether the user has entered the right encryption key for the wallet.
    *
    * @param cipher the encryption key to verify
    * @return true iff the encryption key is valid for this wallet account
    */
   boolean isValidEncryptionKey(KeyCipher cipher);


   /**
    * Get the summary list of unspent transaction outputs for this account.
    *
    * @return the summary list of unspent transaction outputs for this account.
    */
   public List<TransactionOutputSummary> getUnspentTransactionOutputSummary();

   /**
    * Class representing a receiver of funds
    */
   public static class Receiver implements Serializable {
      private static final long serialVersionUID = 1L;

      /**
       * The address to send funds to
       */
      public final Address address;

      /**
       * The amount to send measured in satoshis
       */
      public final long amount;

      public Receiver(Address address, long amount) {
         this.address = address;
         this.amount = amount;
      }
   }

}
