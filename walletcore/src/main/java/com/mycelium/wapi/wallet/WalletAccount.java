/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycelium.wapi.wallet;

import com.google.common.base.Optional;
import com.megiontechnologies.Bitcoins;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.OutputTooSmallException;
import com.mrd.bitlib.UnsignedTransaction;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.OutputList;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.model.*;
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public interface WalletAccount {
   void checkAmount(Receiver receiver, long kbMinerFee, CurrencyValue enteredAmount) throws InsufficientFundsException, OutputTooSmallException, StandardTransactionBuilder.UnableToBuildTransactionException;

   class BroadcastResult {
      private String errorMessage = null;
      private final BroadcastResultType resultType;

      public BroadcastResult(BroadcastResultType resultType){
         this.resultType = resultType;
      }
      public BroadcastResult(String errorMessage, BroadcastResultType resultType){
         this.errorMessage = errorMessage;
         this.resultType = resultType;
      }

      public String getErrorMessage() {
         return errorMessage;
      }

      public BroadcastResultType getResultType() {
         return resultType;
      }
   }

   enum BroadcastResultType {
      SUCCESS,
      REJECTED,
      NO_SERVER_CONNECTION,

      REJECT_MALFORMED,
      REJECT_DUPLICATE,
      REJECT_NONSTANDARD,
      REJECT_INSUFFICIENT_FEE
   }

   enum Type {
      BTCSINGLEADDRESS, BTCBIP44,
      BCHSINGLEADDRESS, BCHBIP44,
      COINAPULT, COLU, UNKNOWN,
      DASH,
   }

   Type getType();

   /**
    * Get the network that this account is for.
    *
    * @return the network that this account is for.
    */
   NetworkParameters getNetwork();

   /**
    * Determine whether an address is one of our own addresses
    *
    * @param address the address to check
    * @return true iff this address is one of our own
    */
   boolean isMine(Address address);

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
    * Get is account sync in progress
    */
   boolean isSyncing();

   /**
    * Get the unique ID of this account
    */
   UUID getId();

   /**
    * Set whether this account is allowed to do zero confirmation spending
    * <p/>
    * Zero confirmation spending is disabled by default. When enabled then zero confirmation outputs send from address
    * not part of this account will be part of the spendable outputs.
    *
    * @param allowZeroConfSpending if true the account will allow zero confirmation spending
    */
   void setAllowZeroConfSpending(boolean allowZeroConfSpending);

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
   Optional<Address> getReceivingAddress();

   /**
    * Can this account be used for spending, or is it read-only?
    */
   boolean canSpend();

   /**
    * Get the current balance of this account based on the last synchronized
    * state.
    */
   Balance getBalance();

   CurrencyBasedBalance getCurrencyBasedBalance();

   /**
    * Get the transaction history of this account based on the last synchronized
    * state.
    *
    * @param offset the offset into the transaction history
    * @param limit  the maximum number of records to retrieve
    */
   List<TransactionSummary> getTransactionHistory(int offset, int limit);


   /**
    * Get the transaction history of this account since the stated timestamp
    * @param receivingSince only include tx older than this
    */
   List<TransactionSummary> getTransactionsSince(Long receivingSince);

   TransactionSummary getTransactionSummary(Sha256Hash txid);

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
    * In order to rescan an account.
    * <p/>
    * This causes the locally cached data to be dropped.
    * Balance and transaction history will get deleted.
    * Data will be re-created upon next synchronize.
    */
   void dropCachedData();

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
   UnsignedTransaction createUnsignedTransaction(List<Receiver> receivers, long minerFeeToUse) throws OutputTooSmallException,
           InsufficientFundsException, StandardTransactionBuilder.UnableToBuildTransactionException;

   /**
    * Create a new unsigned transaction sending funds to one or more defined script outputs.
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
    * @param outputs the receiving output (script and amount)
    * @param minerFeeToUse use this minerFee
    * @return an unsigned transaction.
    * @throws OutputTooSmallException    if one of the outputs were too small
    * @throws InsufficientFundsException if not enough funds were present to create the unsigned
    *                                    transaction
    */
   UnsignedTransaction createUnsignedTransaction(OutputList outputs, long minerFeeToUse) throws OutputTooSmallException,
           InsufficientFundsException, StandardTransactionBuilder.UnableToBuildTransactionException;

   /**
    * Sign an unsigned transaction without broadcasting it.
    *
    * @param unsigned     an unsigned transaction
    * @param cipher       the key cipher to use for decrypting the private key
    * @return the signed transaction.
    * @throws InvalidKeyCipher
    */
   Transaction signTransaction(UnsignedTransaction unsigned, KeyCipher cipher)
         throws InvalidKeyCipher;

   boolean broadcastOutgoingTransactions();

   /**
    * Broadcast a transaction
    * @param transaction the transaction to broadcast
    * @return the broadcast result
    */
   BroadcastResult broadcastTransaction(Transaction transaction);

   /**
    * returns the transactionex for the hash from the backing, if available
    * @param txid transaction hash
    * @return the corresponding transaction or null
    */
   TransactionEx getTransaction(Sha256Hash txid);

   /**
    * Queue a transaction for broadcasting.
    * <p/>
    * The transaction is broadcast on next synchronization.
    *
    * @param transaction     an transaction
    */
   void queueTransaction(TransactionEx transaction);

   /**
    * Remove a pending outgoing tx from the queue
    *
    * A new synchronisation is needed afterwards, as we already purged some UTXOs as we saved the
    * tx in the queue
    *
    * @param transactionId     an transaction id
    */
   boolean cancelQueuedTransaction(Sha256Hash transactionId);

   /**
    * Delete a transaction from the backing
    * Snyc is needed afterwards
    */
   boolean deleteTransaction(Sha256Hash transactionId);

   /**
    * Remove all queued transactions
    */
   void removeAllQueuedTransactions();

   /**
    * Determine the maximum spendable amount you can send in a transaction
    */
   ExactCurrencyValue calculateMaxSpendableAmount(long minerFeeToUse);

   /**
    * Determine the maximum spendable amount you can send in a transaction, with considering destination address type.
    */
   ExactCurrencyValue calculateMaxSpendableAmount(long minerFeeToUse, @Nullable Address destinationAddress);

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
    * Returns true, if this account is based on the internal masterseed.
    */
   boolean isDerivedFromInternalMasterseed();

   /*
   * returns true if this is one of our already used or monitored internal (="change") addresses
   */
   boolean isOwnInternalAddress(Address address);

   /**
    * Create a new unsigned Proof of Payment according to
    * <a href="https://github.com/bitcoin/bips/blob/master/bip-0120.mediawiki">BIP 120</a>.
    * @param txid The transaction id for the transaction to prove.
    * @param nonce The nonce, generated by the server requesting the PoP.
    * @return An UnsignedTransaction that represents the unsigned PoP.
    */
   UnsignedTransaction createUnsignedPop(Sha256Hash txid, byte[] nonce);

   /*
   * returns true if this is one of our already used or monitored external (=normal receiving) addresses
   */
   boolean isOwnExternalAddress(Address address);

   /**
    * Get the summary list of unspent transaction outputs for this account.
    *
    * @return the summary list of unspent transaction outputs for this account.
    */
   List<TransactionOutputSummary> getUnspentTransactionOutputSummary();

   /**
    * Only sync this account if it is the active one
    * @return false if this account should always be synced
    */
   boolean onlySyncWhenActive();

   /**
    * Returns the account native currency as a ISO String, e.g. "BTC", "USD", ...
    */
   String getAccountDefaultCurrency();


   /**
    * Is the account visible in UI
    */
   boolean isVisible();

   /**
    * Returns the number of retrieved transactions during synchronization
    */
   int getSyncTotalRetrievedTransactions();

   /**
    * Class representing a receiver of funds
    */
   class Receiver implements Serializable {
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

      public Receiver(Address address, Bitcoins amount) {
         this(address, amount.getLongValue());
      }
   }
}
