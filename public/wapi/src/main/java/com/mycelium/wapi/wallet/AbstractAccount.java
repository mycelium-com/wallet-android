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

import com.google.common.base.Preconditions;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.OutputTooSmallException;
import com.mrd.bitlib.StandardTransactionBuilder.UnsignedTransaction;
import com.mrd.bitlib.crypto.*;
import com.mrd.bitlib.model.*;
import com.mrd.bitlib.model.Transaction.TransactionParsingException;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.WapiException;
import com.mycelium.WapiLogger;
import com.mycelium.wapi.api.WapiResponse;
import com.mycelium.wapi.api.request.BroadcastTransactionRequest;
import com.mycelium.wapi.api.request.CheckTransactionsRequest;
import com.mycelium.wapi.api.request.GetTransactionsRequest;
import com.mycelium.wapi.api.request.QueryUnspentOutputsRequest;
import com.mycelium.wapi.api.response.BroadcastTransactionResponse;
import com.mycelium.wapi.api.response.CheckTransactionsResponse;
import com.mycelium.wapi.api.response.GetTransactionsResponse;
import com.mycelium.wapi.api.response.QueryUnspentOutputsResponse;
import com.mycelium.wapi.model.*;
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher;
import com.mycelium.wapi.wallet.WalletManager.Event;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.*;

public abstract class AbstractAccount implements WalletAccount {
   public static final String USING_ARCHIVED_ACCOUNT = "Using archived account";
   protected static final int COINBASE_MIN_CONFIRMATIONS = 100;
   private static final int MAX_TRANSACTIONS_TO_HANDLE_SIMULTANEOUSLY = 100;

   public interface EventHandler {
      void onEvent(UUID accountId, Event event);
   }

   protected NetworkParameters _network;
   protected Wapi _wapi;
   protected WapiLogger _logger;
   private AccountBacking _backing;
   protected Balance _cachedBalance;
   private EventHandler _eventHandler;
   protected boolean _allowZeroConfSpending = true;      //on per default, we warn users if they use it

   protected AbstractAccount(AccountBacking backing, NetworkParameters network, Wapi wapi) {
      _network = network;
      _logger = wapi.getLogger();
      _wapi = wapi;
      _backing = backing;
   }

   @Override
   public void setAllowZeroConfSpending(boolean allowZeroConfSpending) {
      _allowZeroConfSpending = allowZeroConfSpending;
   }

   /**
    * set the event handler for this account
    *
    * @param eventHandler the event handler for this account
    */
   void setEventHandler(EventHandler eventHandler) {
      _eventHandler = eventHandler;
   }

   protected void postEvent(Event event) {
      if (_eventHandler != null) {
         _eventHandler.onEvent(this.getId(), event);
      }
   }

   /**
    * Determine whether a transaction was sent from one of our own addresses.
    * <p/>
    * This is a costly operation as we first have to lookup the transaction and
    * then it's funding outputs
    *
    * @param txid the ID of the transaction to investigate
    * @return true if one of the funding outputs were sent from one of our own
    * addresses
    */
   protected boolean isFromMe(Sha256Hash txid) {
      Transaction t = TransactionEx.toTransaction(_backing.getTransaction(txid));
      if (t == null) {
         return false;
      }
      return isFromMe(t);
   }

   /**
    * Determine whether a transaction was sent from one of our own addresses.
    * <p/>
    * This is a costly operation as we have to lookup funding outputs of the
    * transaction
    *
    * @param t the transaction to investigate
    * @return true iff one of the funding outputs were sent from one of our own
    * addresses
    */
   protected boolean isFromMe(Transaction t) {
      for (TransactionInput input : t.inputs) {
         TransactionOutputEx funding = _backing.getParentTransactionOutput(input.outPoint);
         if (funding == null || funding.isCoinBase) {
            continue;
         }
         ScriptOutput fundingScript = ScriptOutput.fromScriptBytes(funding.script);
         Address fundingAddress = fundingScript.getAddress(_network);
         if (isMine(fundingAddress)) {
            return true;
         }
      }
      return false;
   }

   /**
    * Determine whether a transaction output was sent from one of our own
    * addresses
    *
    * @param output the output to investigate
    * @return true iff the putput was sent from one of our own addresses
    */
   protected boolean isMine(TransactionOutputEx output) {
      ScriptOutput script = ScriptOutput.fromScriptBytes(output.script);
      return isMine(script);
   }

   /**
    * Determine whether an output script was created by one of our own addresses
    *
    * @param script the script to investigate
    * @return true iff the script was created by one of our own addresses
    */
   protected boolean isMine(ScriptOutput script) {
      Address address = script.getAddress(_network);
      return isMine(address);
   }

   @Override
   public abstract UUID getId();

   protected static UUID addressToUUID(Address address) {
      return new UUID(BitUtils.uint64ToLong(address.getAllAddressBytes(), 1), BitUtils.uint64ToLong(
            address.getAllAddressBytes(), 9));
   }

   protected boolean synchronizeUnspentOutputs(Collection<Address> addresses) {
      // Get the current unspent outputs as dictated by the block chain
      QueryUnspentOutputsResponse UnspentOutputResponse;
      try {
         UnspentOutputResponse = _wapi.queryUnspentOutputs(new QueryUnspentOutputsRequest(Wapi.VERSION, addresses))
               .getResult();
      } catch (WapiException e) {
         _logger.logError("Server connection failed with error code: " + e.errorCode, e);
         postEvent(Event.SERVER_CONNECTION_ERROR);
         return false;
      }
      Collection<TransactionOutputEx> remoteUnspent = UnspentOutputResponse.unspent;
      // Store the current block height
      setBlockChainHeight(UnspentOutputResponse.height);
      // Make a map for fast lookup
      Map<OutPoint, TransactionOutputEx> remoteMap = toMap(remoteUnspent);

      // Get the current unspent outputs as it is believed to be locally
      Collection<TransactionOutputEx> localUnspent = _backing.getAllUnspentOutputs();
      // Make a map for fast lookup
      Map<OutPoint, TransactionOutputEx> localMap = toMap(localUnspent);

      // Find remotely removed unspent outputs
      for (TransactionOutputEx l : localUnspent) {
         TransactionOutputEx r = remoteMap.get(l.outPoint);
         if (r == null) {
            // An output has gone. Maybe it was spent in another wallet, or
            // never confirmed due to missing fees, double spend, or mutated.
            // Either way, we delete it locally
            _backing.deleteUnspentOutput(l.outPoint);
         }
      }

      // Find remotely added unspent outputs
      Set<Sha256Hash> transactionsToAddOrUpdate = new HashSet<Sha256Hash>();
      List<TransactionOutputEx> unspentOutputsToAddOrUpdate = new LinkedList<TransactionOutputEx>();
      for (TransactionOutputEx r : remoteUnspent) {
         TransactionOutputEx l = localMap.get(r.outPoint);
         if (l == null || l.height != r.height) {
            // New remote output or new height (Maybe it confirmed or we
            // might even have had a reorg). Either way we just update it
            unspentOutputsToAddOrUpdate.add(r);
            transactionsToAddOrUpdate.add(r.outPoint.hash);
            // Note: We are not adding the unspent output to the DB just yet. We
            // first want to verify the full set of funding transactions of the
            // transaction that this unspent output belongs to
         }
      }

      // Fetch updated or added transactions
      if (transactionsToAddOrUpdate.size() > 0) {
         GetTransactionsResponse response;
         try {
            response = _wapi.getTransactions(new GetTransactionsRequest(Wapi.VERSION, transactionsToAddOrUpdate))
                  .getResult();
         } catch (WapiException e) {
            _logger.logError("Server connection failed with error code: " + e.errorCode, e);
            postEvent(Event.SERVER_CONNECTION_ERROR);
            return false;
         }
         try {
            handleNewExternalTransactions(response.transactions);
         } catch (WapiException e) {
            _logger.logError("Server connection failed with error code: " + e.errorCode, e);
            postEvent(Event.SERVER_CONNECTION_ERROR);
            return false;
         }
         // Finally update out list of unspent outputs with added or updated
         // outputs
         for (TransactionOutputEx output : unspentOutputsToAddOrUpdate) {
            _backing.putUnspentOutput(output);
         }
      }

      return true;
   }

   protected static Map<OutPoint, TransactionOutputEx> toMap(Collection<TransactionOutputEx> list) {
      Map<OutPoint, TransactionOutputEx> map = new HashMap<OutPoint, TransactionOutputEx>();
      for (TransactionOutputEx t : list) {
         map.put(t.outPoint, t);
      }
      return map;
   }

   protected void handleNewExternalTransactions(Collection<TransactionEx> transactions) throws WapiException {
      if (transactions.size() <= MAX_TRANSACTIONS_TO_HANDLE_SIMULTANEOUSLY) {
         handleNewExternalTransactionsInt(transactions);
      } else {
         // We have quite a list of transactions to handle, do it in batches
         ArrayList<TransactionEx> all = new ArrayList<TransactionEx>(transactions);
         for (int i = 0; i < all.size(); i += MAX_TRANSACTIONS_TO_HANDLE_SIMULTANEOUSLY) {
            int endIndex = Math.min(all.size(), i + MAX_TRANSACTIONS_TO_HANDLE_SIMULTANEOUSLY);
            List<TransactionEx> sub = all.subList(i, endIndex);
            handleNewExternalTransactionsInt(sub);
         }
      }
   }

   private void handleNewExternalTransactionsInt(Collection<TransactionEx> transactions) throws WapiException {
      // Transform and put into two arrays with matching indexes
      ArrayList<TransactionEx> texArray = new ArrayList<TransactionEx>(transactions.size());
      ArrayList<Transaction> txArray = new ArrayList<Transaction>(transactions.size());
      for (TransactionEx tex : transactions) {
         try {
            txArray.add(Transaction.fromByteReader(new ByteReader(tex.binary)));
            texArray.add(tex);
         } catch (TransactionParsingException e) {
            // We hit a transaction that we cannot parse. Log but otherwise ignore it
            _logger.logError("Received transaction that we cannot parse: " + tex.txid.toString());
            continue;
         }
      }

      // Grab and handle parent transactions
      fetchStoreAndValidateParentOutputs(txArray);

      // Store transaction locally
      for (int i = 0; i < txArray.size(); i++) {
         _backing.putTransaction(texArray.get(i));
         onNewTransaction(texArray.get(i), txArray.get(i));
      }
   }

   private void fetchStoreAndValidateParentOutputs(ArrayList<Transaction> transactions) throws WapiException {
      Map<Sha256Hash, TransactionEx> parentTransactions = new HashMap<Sha256Hash, TransactionEx>();
      Map<OutPoint, TransactionOutputEx> parentOutputs = new HashMap<OutPoint, TransactionOutputEx>();

      // Find list of parent outputs to fetch
      Collection<Sha256Hash> toFetch = new HashSet<Sha256Hash>();
      for (Transaction t : transactions) {
         for (TransactionInput in : t.inputs) {
            if (in.outPoint.hash.equals(OutPoint.COINBASE_OUTPOINT.hash)) {
               // Coinbase input, so no parent
               continue;
            }
            TransactionOutputEx parentOutput = _backing.getParentTransactionOutput(in.outPoint);
            if (parentOutput != null) {
               // We already have the parent output, no need to fetch the entire
               // parent transaction
               parentOutputs.put(parentOutput.outPoint, parentOutput);
               continue;
            }
            TransactionEx parentTransaction = _backing.getTransaction(in.outPoint.hash);
            if (parentTransaction != null) {
               // We had the parent transaction in our own transactions, no need to
               // fetch it remotely
               parentTransactions.put(parentTransaction.txid, parentTransaction);
            } else {
               // Need to fetch it
               toFetch.add(in.outPoint.hash);
            }
         }
      }

      // Fetch missing parent transactions
      if (toFetch.size() > 0) {
         GetTransactionsResponse result = _wapi.getTransactions(new GetTransactionsRequest(Wapi.VERSION, toFetch))
               .getResult();
         for (TransactionEx tx : result.transactions) {
            // Verify transaction hash. This is important as we don't want to
            // have a transaction output associated with an outpoint that
            // doesn't match.
            // This is the end users protection against a rogue server that lies
            // about the value of an output and makes you pay a large fee.
            Sha256Hash hash = HashUtils.doubleSha256(tx.binary).reverse();
            if (hash.equals(tx.txid)) {
               parentTransactions.put(tx.txid, tx);
            } else {
               _logger.logError("Failed to validate transaction hash from server. Expected: " + tx.txid
                     + " Calculated: " + hash);
               throw new RuntimeException("Failed to validate transaction hash from server. Expected: " + tx.txid
                     + " Calculated: " + hash);
            }
         }
      }

      // We should now have all parent transactions or parent outputs. There is
      // a slight probability that one of them was not found due to double
      // spends and/or malleability and network latency etc.

      // Now figure out which parent outputs we need to persist
      List<TransactionOutputEx> toPersist = new LinkedList<TransactionOutputEx>();
      for (Transaction t : transactions) {
         for (TransactionInput in : t.inputs) {
            if (in.outPoint.hash.equals(OutPoint.COINBASE_OUTPOINT.hash)) {
               // coinbase input, so no parent
               continue;
            }
            TransactionOutputEx parentOutput = parentOutputs.get(in.outPoint);
            if (parentOutput != null) {
               // We had it all along
               continue;
            }
            TransactionEx parentTex = parentTransactions.get(in.outPoint.hash);
            if (parentTex != null) {
               // Parent output not found, maybe we already have it
               parentOutput = TransactionEx.getTransactionOutput(parentTex, in.outPoint.index);
               toPersist.add(parentOutput);
               continue;
            }
            _logger.logError("Parent transaction not found: " + in.outPoint.hash);
         }
      }

      // Persist
      for (TransactionOutputEx output : toPersist) {
         _backing.putParentTransactionOutput(output);
      }
   }

   protected Balance calculateLocalBalance() {

      Collection<TransactionOutputEx> unspentOutputs = new HashSet<TransactionOutputEx>(_backing.getAllUnspentOutputs());
      long confirmed = 0;
      long pendingChange = 0;
      long pendingSending = 0;
      long pendingReceiving = 0;


      //
      // Determine the value we are receiving and create a set of outpoints for fast lookup
      //
      Set<OutPoint> unspentOutPoints = new HashSet<OutPoint>();
      for (TransactionOutputEx output : unspentOutputs) {
         if (output.height == -1) {
            if (isFromMe(output.outPoint.hash)) {
               pendingChange += output.value;
            } else {
               pendingReceiving += output.value;
            }
         } else {
            confirmed += output.value;
         }
         unspentOutPoints.add(output.outPoint);
      }

      //
      // Determine the value we are sending
      //

      // Get the current set of unconfirmed transactions
      List<Transaction> unconfirmed = new ArrayList<Transaction>();
      for (TransactionEx tex : _backing.getUnconfirmedTransactions()) {
         try {
            Transaction t = Transaction.fromByteReader(new ByteReader(tex.binary));
            unconfirmed.add(t);
         } catch (TransactionParsingException e) {
            // never happens, we have parsed it before
         }
      }

      for (Transaction t : unconfirmed) {
         // For each input figure out if WE are sending it by fetching the
         // parent transaction and looking at the address
         boolean weSend = false;
         for (TransactionInput input : t.inputs) {
            // Find the parent transaction
            if (input.outPoint.hash.equals(Sha256Hash.ZERO_HASH)) {
               continue;
            }
            TransactionOutputEx parent = _backing.getParentTransactionOutput(input.outPoint);
            if (parent == null) {
               _logger.logError("Unable to find parent transaction output: " + input.outPoint);
               continue;
            }
            TransactionOutput parentOutput = transform(parent);
            Address fundingAddress = parentOutput.script.getAddress(_network);
            if (isMine(fundingAddress)) {
               // One of our addresses are sending coins
               pendingSending += parentOutput.value;
               weSend = true;
            }
         }

         // Now look at the outputs and if it contains change for us, then subtract that from the sending amount
         // if it is already spent in another transaction
         for (int i = 0; i < t.outputs.length; i++) {
            TransactionOutput output = t.outputs[i];
            Address destination = output.script.getAddress(_network);
            if (weSend && isMine(destination)) {
               // The funds are sent from us to us
               OutPoint outPoint = new OutPoint(t.getHash(), i);
               if (!unspentOutPoints.contains(outPoint)) {
                  // This output has been spent, subtract it from the amount sent
                  pendingSending -= output.value;
               }
            }
         }

      }

      int blockHeight = getBlockChainHeight();
      return new Balance(confirmed, pendingReceiving, pendingSending, pendingChange, System.currentTimeMillis(),
            blockHeight, true, _allowZeroConfSpending);
   }

   private TransactionOutput transform(TransactionOutputEx parent) {
      ScriptOutput script = ScriptOutput.fromScriptBytes(parent.script);
      return new TransactionOutput(parent.value, script);
   }

   /**
    * Broadcast outgoing transactions.
    * <p/>
    * This method should only be called from the wallet manager
    *
    * @return false if synchronization failed due to failed blockchain
    * connection
    */
   @Override
   public synchronized boolean broadcastOutgoingTransactions() {
      checkNotArchived();
      List<Sha256Hash> broadcastedIds = new LinkedList<Sha256Hash>();
      Map<Sha256Hash, byte[]> transactions = _backing.getOutgoingTransactions();

      for (byte[] rawTransaction : transactions.values()) {
         TransactionEx tex = TransactionEx.fromUnconfirmedTransaction(rawTransaction);

         BroadcastResult result = broadcastTransaction(TransactionEx.toTransaction(tex));
         if (result == BroadcastResult.SUCCESS){
            broadcastedIds.add(tex.txid);
            _backing.removeOutgoingTransaction(tex.txid);
         }else{
            if (result == BroadcastResult.REJECTED) {
               // invalid tx
               _backing.deleteTransaction(tex.txid);
               _backing.removeOutgoingTransaction(tex.txid);
            }else{
               // No connection --> retry next sync
            }
         }
      }
      if (!broadcastedIds.isEmpty()) {
         onTransactionsBroadcasted(broadcastedIds);
      }
      return true;
   }

   @Override
   public TransactionEx getTransaction(Sha256Hash txid){
      return _backing.getTransaction(txid);
   }

   @Override
   public synchronized BroadcastResult broadcastTransaction(Transaction transaction) {
      checkNotArchived();
      try {
         WapiResponse<BroadcastTransactionResponse> response = _wapi.broadcastTransaction(
               new BroadcastTransactionRequest(Wapi.VERSION, transaction.toBytes()));
         if (response.getErrorCode() == Wapi.ERROR_CODE_SUCCESS) {
            if (response.getResult().success) {
               markTransactionAsSpent(transaction);
               postEvent(Event.BROADCASTED_TRANSACTION_ACCEPTED);
               return BroadcastResult.SUCCESS;
            } else {
               // This transaction was rejected must be double spend or
               // malleability, delete it locally.
               _logger.logError("Failed to broadcast transaction due to a double spend or malleability issue");
               postEvent(Event.BROADCASTED_TRANSACTION_DENIED);
               return BroadcastResult.REJECTED;
            }
         }else if (response.getErrorCode() == Wapi.ERROR_CODE_NO_SERVER_CONNECTION){
            postEvent(Event.SERVER_CONNECTION_ERROR);
            _logger.logError("Server connection failed with ERROR_CODE_NO_SERVER_CONNECTION");
            return BroadcastResult.NO_SERVER_CONNECTION;
         }else{
            postEvent(Event.BROADCASTED_TRANSACTION_DENIED);
            _logger.logError("Server connection failed with error: " + response.getErrorCode());
            return BroadcastResult.REJECTED;
         }

      } catch (WapiException e) {
         postEvent(Event.SERVER_CONNECTION_ERROR);
         _logger.logError("Server connection failed with error code: " + e.errorCode, e);
         return BroadcastResult.NO_SERVER_CONNECTION;
      }
   }

   protected void checkNotArchived() {
      if (isArchived()) {
         _logger.logError(USING_ARCHIVED_ACCOUNT);
         throw new RuntimeException(USING_ARCHIVED_ACCOUNT);
      }
   }

   @Override
   public abstract boolean isArchived();

   @Override
   public abstract boolean isActive();

   protected abstract void onNewTransaction(TransactionEx tex, Transaction t);

   protected void onTransactionsBroadcasted(List<Sha256Hash> txids) {
   }

   @Override
   public abstract boolean canSpend();

   @Override
   public List<TransactionSummary> getTransactionHistory(int offset, int limit) {
      // Note that this method is not synchronized, and we might fetch the transaction history while synchronizing
      // accounts. That should be ok as we write to the DB in a sane order.

      List<TransactionSummary> history = new ArrayList<TransactionSummary>();
      checkNotArchived();
      int blockChainHeight = getBlockChainHeight();
      List<TransactionEx> list = _backing.getTransactionHistory(offset, limit);
      for (TransactionEx tex : list) {
         TransactionSummary item = transform(tex, blockChainHeight);
         if (item != null) {
            history.add(item);
         }
      }
      return history;

   }

   @Override
   public abstract int getBlockChainHeight();

   protected abstract void setBlockChainHeight(int blockHeight);

   @Override
   public Transaction signTransaction(UnsignedTransaction unsigned, KeyCipher cipher)
         throws InvalidKeyCipher {
      checkNotArchived();
      if (!isValidEncryptionKey(cipher)) {
         throw new InvalidKeyCipher();
      }
      // Make all signatures, this is the CPU intensive part
      List<byte[]> signatures = StandardTransactionBuilder.generateSignatures(
            unsigned.getSignatureInfo(),
            new PrivateKeyRing(cipher)
      );

      // Apply signatures and finalize transaction
      Transaction transaction = StandardTransactionBuilder.finalizeTransaction(unsigned, signatures);
      return transaction;
   }

   public synchronized void queueTransaction(Transaction transaction) {
      // Store transaction in outgoing buffer, so we can broadcast it
      // later
      byte[] rawTransaction = transaction.toBytes();
      _backing.putOutgoingTransaction(transaction.getHash(), rawTransaction);
      markTransactionAsSpent(transaction);
   }

   @Override
   public synchronized boolean deleteTransaction(Sha256Hash transactionId) {
      TransactionEx tex = _backing.getTransaction(transactionId);
      if (tex == null) return false;
      Transaction tx = TransactionEx.toTransaction(tex);
      _backing.beginTransaction();
      try {
         // See if any of the outputs are stored locally and remove them
         for (int i = 0; i < tx.outputs.length; i++) {
            TransactionOutput output = tx.outputs[i];
            OutPoint outPoint = new OutPoint(tx.getHash(), i);
            TransactionOutputEx utxo = _backing.getUnspentOutput(outPoint);
            if (utxo != null) {
               _backing.deleteUnspentOutput(outPoint);
            }
         }
         // remove it from the backing
         _backing.deleteTransaction(transactionId);
         _backing.setTransactionSuccessful();
      } finally {
         _backing.endTransaction();
      }
      updateLocalBalance(); //will still need a new sync besides re-calculating
      return true;
   }

   @Override
   public synchronized boolean cancelQueuedTransaction(Sha256Hash transaction) {
      Map<Sha256Hash, byte[]> outgoingTransactions = _backing.getOutgoingTransactions();

      if (!outgoingTransactions.containsKey(transaction)){
         return false;
      }

      Transaction tx;
      try {
         tx = Transaction.fromBytes(outgoingTransactions.get(transaction));
      } catch (TransactionParsingException e) {
         return false;
      }

      _backing.beginTransaction();
      try {

         // See if any of the outputs are stored locally and remove them
         for (int i = 0; i < tx.outputs.length; i++) {
            TransactionOutput output = tx.outputs[i];
            OutPoint outPoint = new OutPoint(tx.getHash(), i);
            TransactionOutputEx utxo = _backing.getUnspentOutput(outPoint);
            if (utxo != null) {
               _backing.deleteUnspentOutput(outPoint);
            }
         }

         // Remove a queued transaction from our outgoing buffer
         _backing.removeOutgoingTransaction(transaction);

         // remove it from the backing
         _backing.deleteTransaction(transaction);
         _backing.setTransactionSuccessful();
      }finally {
         _backing.endTransaction();
      }

      // calc the new balance to remove the outgoing amount
      // the total balance will still be wrong, as we already deleted some UTXOs to build the queued transaction
      // these will get restored after the next sync
      updateLocalBalance();

      //markTransactionAsSpent(transaction);
      return true;
   }

   private void markTransactionAsSpent(Transaction transaction) {
      _backing.beginTransaction();
      try {
         // Remove inputs from unspent, marking them as spent
         for (TransactionInput input : transaction.inputs) {
            TransactionOutputEx parentOutput = _backing.getUnspentOutput(input.outPoint);
            if (parentOutput != null) {
               _backing.deleteUnspentOutput(input.outPoint);
               _backing.putParentTransactionOutput(parentOutput);
            }
         }

         // See if any of the outputs are for ourselves and store them as
         // unspent
         for (int i = 0; i < transaction.outputs.length; i++) {
            TransactionOutput output = transaction.outputs[i];
            if (isMine(output.script)) {
               _backing.putUnspentOutput(new TransactionOutputEx(new OutPoint(transaction.getHash(), i), -1,
                     output.value, output.script.getScriptBytes(), false));
            }
         }

         // Store transaction locally, so we have it in our history and don't
         // need to fetch it in a minute
         _backing.putTransaction(TransactionEx.fromUnconfirmedTransaction(transaction));
         _backing.setTransactionSuccessful();
      } finally {
         _backing.endTransaction();
      }

      // Tell account that we have a new transaction
      onNewTransaction(TransactionEx.fromUnconfirmedTransaction(transaction), transaction);

      // Calculate local balance cache. It has changed because we have done
      // some spending
      updateLocalBalance();
      persistContextIfNecessary();

   }

   protected abstract void persistContextIfNecessary();

   @Override
   public void checkAmount(Receiver receiver, long kbMinerFee, CurrencyValue enteredAmount) throws InsufficientFundsException, OutputTooSmallException {
      createUnsignedTransaction(Arrays.asList(receiver), kbMinerFee);
   }

   @Override
   public NetworkParameters getNetwork() {
      return _network;
   }

   protected Collection<TransactionOutputEx> getSpendableOutputs() {
      Collection<TransactionOutputEx> list = _backing.getAllUnspentOutputs();

      // Prune confirmed outputs for coinbase outputs that are not old enough
      // for spending. Also prune unconfirmed receiving coins except for change
      int blockChainHeight = getBlockChainHeight();
      Iterator<TransactionOutputEx> it = list.iterator();
      while (it.hasNext()) {
         TransactionOutputEx output = it.next();
         if (output.isCoinBase) {
            int confirmations = blockChainHeight - output.height;
            if (confirmations < COINBASE_MIN_CONFIRMATIONS) {
               it.remove();
               continue;
            }
         }
         // Unless we allow zero confirmation spending we prune all unconfirmed outputs sent from foreign addresses
         if (!_allowZeroConfSpending) {
            if (output.height == -1 && !isFromMe(output.outPoint.hash)) {
               // Prune receiving coins that is not change sent to ourselves
               it.remove();
            }
         }
      }
      return list;
   }

   protected abstract Address getChangeAddress();

   protected static Collection<UnspentTransactionOutput> transform(Collection<TransactionOutputEx> source) {
      List<UnspentTransactionOutput> outputs = new ArrayList<UnspentTransactionOutput>();
      for (TransactionOutputEx s : source) {
         ScriptOutput script = ScriptOutput.fromScriptBytes(s.script);
         outputs.add(new UnspentTransactionOutput(s.outPoint, s.height, s.value, script));
      }
      return outputs;
   }

   @Override
   public synchronized ExactCurrencyValue calculateMaxSpendableAmount(long minerFeeToUse) {
      checkNotArchived();
      Collection<UnspentTransactionOutput> spendableOutputs = transform(getSpendableOutputs());
      long satoshis = 0;
      for (UnspentTransactionOutput output : spendableOutputs) {
         satoshis += output.value;
      }
      // Iteratively figure out whether we can send everything by subtracting
      // the miner fee for every iteration
      while (true) {
         satoshis -= minerFeeToUse;
         if (satoshis <= 0) {
            return ExactBitcoinValue.ZERO;
         }

         // Create transaction builder
         StandardTransactionBuilder stb = new StandardTransactionBuilder(_network);

         // Try and add the output
         try {
            // Note, null address used here, we just use it for measuring the
            // transaction size
            stb.addOutput(Address.getNullAddress(_network), satoshis);
         } catch (OutputTooSmallException e1) {
            // The amount we try to send is lower than what the network allows
            return ExactBitcoinValue.ZERO;
         }

         // Try to create an unsigned transaction
         try {
            stb.createUnsignedTransaction(spendableOutputs, getChangeAddress(), new PublicKeyRing(), _network, minerFeeToUse);
            // We have enough to pay the fees, return the amount as the maximum
            return ExactBitcoinValue.from(satoshis);
         } catch (InsufficientFundsException e) {
            // We cannot send this amount, try again with a little higher fee
            continue;
         }
      }
   }

   protected abstract InMemoryPrivateKey getPrivateKeyForAddress(Address address, KeyCipher cipher)
         throws InvalidKeyCipher;

   protected abstract PublicKey getPublicKeyForAddress(Address address);

   @Override
   public synchronized UnsignedTransaction createUnsignedTransaction(List<Receiver> receivers, long minerFeeToUse)
         throws OutputTooSmallException, InsufficientFundsException {
      checkNotArchived();

      // Determine the list of spendable outputs
      Collection<UnspentTransactionOutput> spendable = transform(getSpendableOutputs());

      // Create the unsigned transaction
      StandardTransactionBuilder stb = new StandardTransactionBuilder(_network);
      for (Receiver receiver : receivers) {
         stb.addOutput(receiver.address, receiver.amount);
      }
      Address changeAddress = getChangeAddress();
      UnsignedTransaction unsigned = stb.createUnsignedTransaction(spendable, changeAddress, new PublicKeyRing(),
            _network, minerFeeToUse);
      return unsigned;
   }

   @Override
   public UnsignedTransaction createUnsignedTransaction(OutputList outputs, long minerFeeToUse) throws OutputTooSmallException, InsufficientFundsException {
      checkNotArchived();

      // Determine the list of spendable outputs
      Collection<UnspentTransactionOutput> spendable = transform(getSpendableOutputs());

      // Create the unsigned transaction
      StandardTransactionBuilder stb = new StandardTransactionBuilder(_network);
      stb.addOutputs(outputs);
      Address changeAddress = getChangeAddress();
      UnsignedTransaction unsigned = stb.createUnsignedTransaction(spendable, changeAddress, new PublicKeyRing(),
            _network, minerFeeToUse);
      return unsigned;
   }

   @Override
   public Balance getBalance() {
      // public method that needs no synchronization
      checkNotArchived();
      // We make a copy of the reference for a reason. Otherwise the balance
      // might change right when we make a copy
      Balance b = _cachedBalance;
      return new Balance(b.confirmed, b.pendingReceiving, b.pendingSending, b.pendingChange, b.updateTime,
            b.blockHeight, isSynchronizing(), b.allowsZeroConfSpending);
   }

   @Override
   public CurrencyBasedBalance getCurrencyBasedBalance() {
      Balance balance = getBalance();
      ExactCurrencyValue confirmed = ExactBitcoinValue.from(balance.getSpendableBalance());
      ExactCurrencyValue sending = ExactBitcoinValue.from(balance.getSendingBalance());
      ExactCurrencyValue receiving = ExactBitcoinValue.from(balance.getReceivingBalance());
      return new CurrencyBasedBalance(confirmed,sending, receiving);
   }

   /**
    * Update the balance by summing up the unspent outputs in local persistence.
    *
    * @return true if the balance changed, false otherwise
    */
   protected boolean updateLocalBalance() {
      Balance balance = calculateLocalBalance();
      if (!balance.equals(_cachedBalance)) {
         _cachedBalance = balance;
         postEvent(Event.BALANCE_CHANGED);
         return true;
      }
      return false;
   }

   protected TransactionSummary transform(TransactionEx tex, int blockChainHeight) {
      Transaction tx;
      try {
         tx = Transaction.fromByteReader(new ByteReader(tex.binary));
      } catch (TransactionParsingException e) {
         // Should not happen as we have parsed the transaction earlier
         _logger.logError("Unable to parse ");
         return null;
      }
      return transform(tx, tex.time, tex.height, blockChainHeight);
   }

   protected TransactionSummary transform(Transaction tx, int time, int height, int blockChainHeight) {
      long value = 0;
      Address destAddress = null;
      for (TransactionOutput output : tx.outputs) {
         if (isMine(output.script)) {
            value += output.value;
         }else{
            destAddress = output.script.getAddress(_network);
         }
      }

      if (tx.isCoinbase()) {
         // For coinbase transactions there is nothing to subtract
      } else {
         for (TransactionInput input : tx.inputs) {
            // find parent output
            TransactionOutputEx funding = _backing.getParentTransactionOutput(input.outPoint);
            if (funding == null) {
               _logger.logError("Unable to find parent output for: " + input.outPoint);
               continue;
            }
            if (isMine(funding)) {
               value -= funding.value;
            }
         }
      }

      int confirmations;
      if (height == -1) {
         confirmations = 0;
      } else {
         confirmations = Math.max(0, blockChainHeight - height + 1);
      }

      // only track a destinationAddress if it is an outgoing transaction (i.e. send money to someone)
      // to prevent the user that he tries to return money to an address he got bitcoin from.
      if (value >= 0){
         destAddress = null;
      }

      boolean isQueuedOutgoing = _backing.isOutgoingTransaction(tx.getHash());

      return new TransactionSummary(tx.getHash(), value, time, height, confirmations, isQueuedOutgoing, com.google.common.base.Optional.fromNullable(destAddress));
   }

   @Override
   public List<TransactionOutputSummary> getUnspentTransactionOutputSummary() {
      // Note that this method is not synchronized, and we might fetch the transaction history while synchronizing
      // accounts. That should be ok as we write to the DB in a sane order.

      // Get all unspent outputs for this account
      Collection<TransactionOutputEx> outputs = _backing.getAllUnspentOutputs();

      // Transform it to a list of summaries
      List<TransactionOutputSummary> list = new ArrayList<TransactionOutputSummary>();
      int blockChainHeight = getBlockChainHeight();
      for (TransactionOutputEx output : outputs) {

         ScriptOutput script = ScriptOutput.fromScriptBytes(output.script);
         Address address;
         if (script == null) {
            address = Address.getNullAddress(_network);
            // This never happens as we have parsed this script before
         } else {
            address = script.getAddress(_network);
         }
         int confirmations;
         if (output.height == -1) {
            confirmations = 0;
         } else {
            confirmations = Math.max(0, blockChainHeight - output.height + 1);
         }

         TransactionOutputSummary summary = new TransactionOutputSummary(output.outPoint, output.value, output.height, confirmations, address);
         list.add(summary);
      }
      // Sort & return
      Collections.sort(list);
      return list;
   }


   protected boolean monitorYoungTransactions() {
      Collection<TransactionEx> list = _backing.getYoungTransactions(5, getBlockChainHeight());
      if (list.isEmpty()) {
         return true;
      }
      List<Sha256Hash> txids = new ArrayList<Sha256Hash>(list.size());
      for (TransactionEx tex : list) {
         txids.add(tex.txid);
      }
      CheckTransactionsResponse result;
      try {
         result = _wapi.checkTransactions(new CheckTransactionsRequest(txids)).getResult();
      } catch (WapiException e) {
         postEvent(Event.SERVER_CONNECTION_ERROR);
         _logger.logError("Server connection failed with error code: " + e.errorCode, e);
         // We failed to check transactions
         return false;
      }
      for (TransactionStatus t : result.transactions) {
         TransactionEx tex = _backing.getTransaction(t.txid);
         if (!t.found) {
            if (tex != null) {
               // We have a transaction locally that did not get reported back by the server
               // put it into the outgoing queue and mark it as "not transmitted" (even as it might be an incomming tx)
               try {
                  Transaction transaction = Transaction.fromBytes(tex.binary);
                  queueTransaction(transaction);
               } catch (TransactionParsingException ignore) {
                  // ignore this tx and just delete it
                  _backing.deleteTransaction(t.txid);
               }
            } else {
               // we haven't found it locally (shouldn't happen here) - so delete it to be sure
               _backing.deleteTransaction(t.txid);
            }
            continue;
         }
         Preconditions.checkNotNull(tex);
         if (tex.height != t.height || tex.time != t.time) {
            // The transaction got a new height or timestamp. There could be
            // several reasons for that. It got a new timestamp from the server,
            // it confirmed, or might also be a reorg.
            TransactionEx newTex = new TransactionEx(tex.txid, t.height, t.time, tex.binary);
            _logger.logInfo(String.format("Replacing: %s With: %s", tex.toString(), newTex.toString()));
            postEvent(Event.TRANSACTION_HISTORY_CHANGED);
            _backing.deleteTransaction(tex.txid);
            _backing.putTransaction(newTex);
         }
      }
      return true;
   }

   protected abstract boolean isSynchronizing();

   public class PublicKeyRing implements IPublicKeyRing {

      @Override
      public PublicKey findPublicKeyByAddress(Address address) {
         PublicKey publicKey = getPublicKeyForAddress(address);
         if (publicKey != null) {
            return publicKey;
         }
         throw new RuntimeException("Unable to find public key for address " + address.toString());
      }

   }

   public class PrivateKeyRing implements IPublicKeyRing, IPrivateKeyRing {

      KeyCipher _cipher;

      public PrivateKeyRing(KeyCipher cipher) {
         _cipher = cipher;
      }

      @Override
      public PublicKey findPublicKeyByAddress(Address address) {
         PublicKey publicKey = getPublicKeyForAddress(address);
         if (publicKey != null) {
            return publicKey;
         }
         throw new RuntimeException("Unable to find public key for address " + address.toString());
      }

      @Override
      public BitcoinSigner findSignerByPublicKey(PublicKey publicKey) {
         Address address = publicKey.toAddress(_network);
         InMemoryPrivateKey privateKey;
         try {
            privateKey = getPrivateKeyForAddress(address, _cipher);
         } catch (InvalidKeyCipher e) {
            throw new RuntimeException("Unable to decrypt private key for address " + address.toString());
         }
         if (privateKey != null) {
            return privateKey;
         }
         throw new RuntimeException("Unable to find private key for address " + address.toString());
      }

   }


   @Override
   public TransactionSummary getTransactionSummary(Sha256Hash txid){
      TransactionEx tx = _backing.getTransaction(txid);
      return transform(tx, tx.height);
   }

   @Override
   public TransactionDetails getTransactionDetails(Sha256Hash txid) {
      // Note that this method is not synchronized, and we might fetch the transaction history while synchronizing
      // accounts. That should be ok as we write to the DB in a sane order.

      TransactionEx tex = _backing.getTransaction(txid);
      Transaction tx = TransactionEx.toTransaction(tex);
      if (tx == null) {
         throw new RuntimeException();
      }

      List<TransactionDetails.Item> inputs = new ArrayList<TransactionDetails.Item>(tx.inputs.length);
      if (tx.isCoinbase()) {
         // We have a coinbase transaction. Create one input with the sum of the outputs as its value,
         // and make the address the null address
         long value = 0;
         for (TransactionOutput out : tx.outputs) {
            value += out.value;
         }
         inputs.add(new TransactionDetails.Item(Address.getNullAddress(_network), value, true));
      } else {
         // Populate the inputs
         for (TransactionInput input : tx.inputs) {
            Sha256Hash parentHash = input.outPoint.hash;
            // Get the parent transaction
            TransactionOutputEx parentOutput = _backing.getParentTransactionOutput(input.outPoint);
            if (parentOutput == null) {
               // We never heard about the parent, skip
               continue;
            }
            // Determine the parent address
            Address parentAddress;
            ScriptOutput parentScript = ScriptOutput.fromScriptBytes(parentOutput.script);
            if (parentScript == null) {
               // Null address means we couldn't figure out the address, strange script
               parentAddress = Address.getNullAddress(_network);
            } else {
               parentAddress = parentScript.getAddress(_network);
            }
            inputs.add(new TransactionDetails.Item(parentAddress, parentOutput.value, false));
         }
      }
      // Populate the outputs
      TransactionDetails.Item[] outputs = new TransactionDetails.Item[tx.outputs.length];
      for (int i = 0; i < tx.outputs.length; i++) {
         Address address = tx.outputs[i].script.getAddress(_network);
         outputs[i] = new TransactionDetails.Item(address, tx.outputs[i].value, false);
      }

      return new TransactionDetails(txid, tex.height, tex.time, inputs.toArray(new TransactionDetails.Item[]{}), outputs);
   }

   public UnsignedTransaction createUnsignedPop(Sha256Hash txid, byte[] nonce) {
      checkNotArchived();

      try {
         TransactionEx txExToProve = _backing.getTransaction(txid);
         Transaction txToProve = Transaction.fromByteReader(new ByteReader(txExToProve.binary));

         List<UnspentTransactionOutput> funding = new ArrayList<UnspentTransactionOutput>(txToProve.inputs.length);
         for (TransactionInput input : txToProve.inputs) {
            TransactionEx inTxEx = _backing.getTransaction(input.outPoint.hash);
            Transaction inTx = Transaction.fromByteReader(new ByteReader(inTxEx.binary));
            UnspentTransactionOutput unspentOutput = new UnspentTransactionOutput(input.outPoint, inTxEx.height,
                    inTx.outputs[input.outPoint.index].value,
                    inTx.outputs[input.outPoint.index].script);

            funding.add(unspentOutput);
         }

         TransactionOutput popOutput = createPopOutput(txid, nonce);

         StandardTransactionBuilder stb = new StandardTransactionBuilder(_network);

         UnsignedTransaction unsignedTransaction = stb.createUnsignedPop(Collections.singletonList(popOutput), funding,
                 new PublicKeyRing(), _network);

         return unsignedTransaction;
      } catch (TransactionParsingException e) {
         throw new RuntimeException("Cannot parse transaction", e);
      }
   }

   private TransactionOutput createPopOutput(Sha256Hash txidToProve, byte[] nonce) {

      ByteBuffer byteBuffer = ByteBuffer.allocate(41);
      byteBuffer.put((byte) Script.OP_RETURN);

      byteBuffer.put((byte)1).put((byte)0); // version 1

      byteBuffer.put(txidToProve.getBytes()); // txid

      if (nonce == null || nonce.length != 6) {
         throw new IllegalArgumentException("Invalid nonce. Expected 6 bytes.");
      }
      byteBuffer.put(nonce); // nonce

      ScriptOutput scriptOutput = ScriptOutputStrange.fromScriptBytes(byteBuffer.array());
      TransactionOutput output = new TransactionOutput(0L, scriptOutput);
      return output;
   }

   @Override
   public boolean onlySyncWhenActive() {
      return false;
   }
}
