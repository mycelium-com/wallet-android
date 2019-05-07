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

import com.google.common.collect.Lists;
import com.mrd.bitlib.*;
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.OutputTooSmallException;
import com.mrd.bitlib.crypto.*;
import com.mrd.bitlib.model.*;
import com.mrd.bitlib.model.Transaction.TransactionParsingException;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.WapiLogger;
import com.mycelium.wapi.ColuTransferInstructionsParser;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.WapiException;
import com.mycelium.wapi.api.WapiResponse;
import com.mycelium.wapi.api.lib.TransactionExApi;
import com.mycelium.wapi.api.request.BroadcastTransactionRequest;
import com.mycelium.wapi.api.request.CheckTransactionsRequest;
import com.mycelium.wapi.api.request.GetTransactionsRequest;
import com.mycelium.wapi.api.request.QueryUnspentOutputsRequest;
import com.mycelium.wapi.api.response.BroadcastTransactionResponse;
import com.mycelium.wapi.api.response.CheckTransactionsResponse;
import com.mycelium.wapi.api.response.GetTransactionsResponse;
import com.mycelium.wapi.api.response.QueryUnspentOutputsResponse;
import com.mycelium.wapi.model.Balance;
import com.mycelium.wapi.model.TransactionDetails;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.model.TransactionOutputEx;
import com.mycelium.wapi.model.TransactionOutputSummary;
import com.mycelium.wapi.model.TransactionStatus;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher;
import com.mycelium.wapi.wallet.WalletManager.Event;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.mrd.bitlib.StandardTransactionBuilder.createOutput;
import static com.mrd.bitlib.TransactionUtils.MINIMUM_OUTPUT_VALUE;
import static com.mycelium.wapi.wallet.currency.ExactBitcoinValue.ZERO;
import static java.util.Collections.singletonList;

public abstract class AbstractAccount extends SynchronizeAbleWalletAccount {
   private static final int COINBASE_MIN_CONFIRMATIONS = 100;
   private static final int MAX_TRANSACTIONS_TO_HANDLE_SIMULTANEOUSLY = 199;
   private final ColuTransferInstructionsParser coluTransferInstructionsParser;

   public interface EventHandler {
      void onEvent(UUID accountId, Event event);
   }

   protected final NetworkParameters _network;
   protected final Wapi _wapi;
   protected final WapiLogger _logger;
   protected boolean _allowZeroConfSpending = true;      //on per default, we warn users if they use it
   protected Balance _cachedBalance;

   private EventHandler _eventHandler;
   private final AccountBacking _backing;
   protected int syncTotalRetrievedTransactions = 0;

   protected AbstractAccount(AccountBacking backing, NetworkParameters network, Wapi wapi) {
      _network = network;
      _logger = wapi.getLogger();
      _wapi = wapi;
      _backing = backing;
      coluTransferInstructionsParser = new ColuTransferInstructionsParser(_logger);
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
    * <p>
    * This is a costly operation as we first have to lookup the transaction and
    * then it's funding outputs
    *
    * @param txid the ID of the transaction to investigate
    * @return true if one of the funding outputs were sent from one of our own
    * addresses
    */
   protected boolean isFromMe(Sha256Hash txid) {
      Transaction t = TransactionEx.toTransaction(_backing.getTransaction(txid));
      return t != null && isFromMe(t);
   }

   /**
    * Determine whether a transaction was sent from one of our own addresses.
    * <p>
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
      return new UUID(BitUtils.uint64ToLong(address.getAllAddressBytes(), 0), BitUtils.uint64ToLong(
            address.getAllAddressBytes(), 8));
   }

   /**
    * Checks for all UTXO of the requested addresses and deletes or adds them locally if necessary
    * returns -1 if something went wrong or otherwise the number of new UTXOs added to the local
    * database
    */
   protected int synchronizeUnspentOutputs(Collection<Address> addresses) {
      // Get the current unspent outputs as dictated by the block chain
      QueryUnspentOutputsResponse unspentOutputResponse;
      try {
            unspentOutputResponse = _wapi.queryUnspentOutputs(new QueryUnspentOutputsRequest(Wapi.VERSION, addresses))
               .getResult();
      } catch (WapiException e) {
         _logger.logError("Server connection failed with error code: " + e.errorCode, e);
         postEvent(Event.SERVER_CONNECTION_ERROR);
         return -1;
      }
      Collection<TransactionOutputEx> remoteUnspent = unspentOutputResponse.unspent;
      // Store the current block height
      setBlockChainHeight(unspentOutputResponse.height);
      // Make a map for fast lookup
      Map<OutPoint, TransactionOutputEx> remoteMap = toMap(remoteUnspent);

      // Get the current unspent outputs as it is believed to be locally
      Collection<TransactionOutputEx> localUnspent = _backing.getAllUnspentOutputs();
      // Make a map for fast lookup
      Map<OutPoint, TransactionOutputEx> localMap = toMap(localUnspent);

      Set<Sha256Hash> transactionsToAddOrUpdate = new HashSet<>();
      Set<Address> addressesToDiscover = new HashSet<>();

      // Find remotely removed unspent outputs
      for (TransactionOutputEx l : localUnspent) {
         TransactionOutputEx r = remoteMap.get(l.outPoint);
         if (r == null) {
            // An output has gone. Maybe it was spent in another wallet, or
            // never confirmed due to missing fees, double spend, or mutated.

            // we need to fetch associated transactions, to see the outgoing tx in the history
            ScriptOutput scriptOutput = ScriptOutput.fromScriptBytes(l.script);
            boolean removeLocally = true;

            // Start of the hack to prevent actual local data removal if server still didn't process just sent tx
            youngTransactions:
            for (TransactionEx transactionEx : _backing.getTransactionsSince(System.currentTimeMillis() -
                    TimeUnit.SECONDS.toMillis(15))) {
                TransactionOutputEx output;
                int i = 0;
                while ((output = TransactionEx.getTransactionOutput(transactionEx, i++)) != null) {
                   if (output.equals(l) && !_backing.hasParentTransactionOutput(l.outPoint)) {
                      removeLocally = false;
                      break youngTransactions;
                   }
                }
            }
            // End of hack

            if (scriptOutput != null && removeLocally) {
               Address address = scriptOutput.getAddress(_network);
               if (addresses.contains(address)) {
                  // the output was associated with an address we were scanning for
                  // we should have got back that output from the servers
                  // this means it got probably spent via another wallet
                  // scan this address for all associated transaction to keep the history in sync
                  if (!address.equals(Address.getNullAddress(_network))) {
                     addressesToDiscover.add(address);
                  }
               } else {
                  removeLocally = false;
               }
            }

            if (removeLocally) {
               // delete the UTXO locally
               _backing.deleteUnspentOutput(l.outPoint);
            }
         }
      }

      int newUtxos = 0;

      // Find remotely added unspent outputs
      List<TransactionOutputEx> unspentOutputsToAddOrUpdate = new LinkedList<>();
      for (TransactionOutputEx r : remoteUnspent) {
         TransactionOutputEx l = localMap.get(r.outPoint);
         if (l == null) {
            // We might have already spent transaction, but if getUnspent used connection to different server
            // it would not know that output is already spent.
            l = _backing.getParentTransactionOutput(r.outPoint);
         }
         if (l == null || l.height != r.height) {
            // New remote output or new height (Maybe it confirmed or we
            // might even have had a reorg). Either way we just update it
            unspentOutputsToAddOrUpdate.add(r);
            transactionsToAddOrUpdate.add(r.outPoint.txid);
            // Note: We are not adding the unspent output to the DB just yet. We
            // first want to verify the full set of funding transactions of the
            // transaction that this unspent output belongs to
            if (l == null) {
               // this is a new UTXO. it might be necessary to sync older addresses too
               // as this might be a change utxo from spending smth from a address we own
               // too but did not sync here
               newUtxos ++;
            }
         }
      }

      // Fetch updated or added transactions
      if (transactionsToAddOrUpdate.size() > 0) {
         GetTransactionsResponse response;
         try {
            response = getTransactionsBatched(transactionsToAddOrUpdate).getResult();
            handleNewExternalTransactions(response.transactions);
         } catch (WapiException e) {
            _logger.logError("Server connection failed with error code: " + e.errorCode, e);
            postEvent(Event.SERVER_CONNECTION_ERROR);
            return -1;
         }
         try {
            _backing.beginTransaction();
            // Finally update out list of unspent outputs with added or updated
            // outputs
            for (TransactionOutputEx output : unspentOutputsToAddOrUpdate) {
               // check if the output really belongs to one of our addresses
               // prevent getting out local cache into a undefined state, if the server screws up
               if (isMine(output)) {
                  _backing.putUnspentOutput(output);
               } else {
                  _logger.logError("We got an UTXO that does not belong to us: " + output.toString());
               }
            }
            _backing.setTransactionSuccessful();
         } finally {
            _backing.endTransaction();
         }
      }

      // if we removed some UTXO because of a sync, it means that there are transactions
      // we don't yet know about. Run a discover for all addresses related to the UTXOs we removed
      if (addressesToDiscover.size() > 0) {
         try {
            doDiscoveryForAddresses(Lists.newArrayList(addressesToDiscover));
         } catch (WapiException ignore) {
         }
      }
      return newUtxos;
   }

   protected WapiResponse<GetTransactionsResponse> getTransactionsBatched(Collection<Sha256Hash> txids)  {
      final GetTransactionsRequest fullRequest = new GetTransactionsRequest(Wapi.VERSION, txids);
      return _wapi.getTransactions(fullRequest);
   }

   protected abstract Map<BipDerivationType, Boolean> doDiscoveryForAddresses(List<Address> lookAhead) throws WapiException;

   private static Map<OutPoint, TransactionOutputEx> toMap(Collection<TransactionOutputEx> list) {
      Map<OutPoint, TransactionOutputEx> map = new HashMap<>();
      for (TransactionOutputEx t : list) {
         map.put(t.outPoint, t);
      }
      return map;
   }

   protected void handleNewExternalTransactions(Collection<TransactionExApi> transactions) throws WapiException {
      if (transactions.size() <= MAX_TRANSACTIONS_TO_HANDLE_SIMULTANEOUSLY) {
         handleNewExternalTransactionsInt(transactions);
         syncTotalRetrievedTransactions += transactions.size();
         updateSyncProgress();
      } else {
         // We have quite a list of transactions to handle, do it in batches
         ArrayList<TransactionExApi> all = new ArrayList<>(transactions);
         for (int i = 0; i < all.size(); i += MAX_TRANSACTIONS_TO_HANDLE_SIMULTANEOUSLY) {
            int endIndex = Math.min(all.size(), i + MAX_TRANSACTIONS_TO_HANDLE_SIMULTANEOUSLY);
            Collection<TransactionExApi> sub = all.subList(i, endIndex);
            handleNewExternalTransactionsInt(sub);
            syncTotalRetrievedTransactions += (endIndex - i);
            updateSyncProgress();
         }
      }
   }

   private void handleNewExternalTransactionsInt(Collection<TransactionExApi> transactions) throws WapiException {
      // Transform and put into two arrays with matching indexes
      List<Transaction> txArray = new ArrayList<>(transactions.size());
      for (TransactionEx tex : transactions) {
         try {
            txArray.add(Transaction.fromByteReader(new ByteReader(tex.binary)));
         } catch (TransactionParsingException e) {
            // We hit a transaction that we cannot parse. Log but otherwise ignore it
            _logger.logError("Received transaction that we cannot parse: " + tex.txid.toString());
         }
      }

      // Grab and handle parent transactions
      fetchStoreAndValidateParentOutputs(txArray,false);

      // Store transaction locally
      _backing.putTransactions(transactions);

      for (int i = 0; i < txArray.size(); i++) {
         onNewTransaction(txArray.get(i));
      }
   }

   public void fetchStoreAndValidateParentOutputs(List<Transaction> transactions, boolean doRemoteFetching) throws WapiException {
      Map<Sha256Hash, TransactionEx> parentTransactions = new HashMap<>();
      Map<OutPoint, TransactionOutputEx> parentOutputs = new HashMap<>();

      // Find list of parent outputs to fetch
      Collection<Sha256Hash> toFetch = new HashSet<>();
      for (Transaction t : transactions) {

         for (TransactionInput in : t.inputs) {
            if (in.outPoint.txid.equals(OutPoint.COINBASE_OUTPOINT.txid)) {
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
            TransactionEx parentTransaction = _backing.getTransaction(in.outPoint.txid);
            if (parentTransaction == null) {
               // Check current transactions list for parents
               for (Transaction transaction : transactions) {
                  if (transaction.getId().equals(in.outPoint.txid)) {
                     parentTransaction = TransactionEx.fromUnconfirmedTransaction(transaction);
                     break;
                  }
               }
            }

            if (parentTransaction != null) {
               // We had the parent transaction in our own transactions, no need to
               // fetch it remotely
               parentTransactions.put(parentTransaction.txid, parentTransaction);
            } else if (doRemoteFetching) {
               // Need to fetch it
               toFetch.add(in.outPoint.txid);
            }
         }
      }

      // Fetch missing parent transactions
      if (toFetch.size() > 0) {
         GetTransactionsResponse result = getTransactionsBatched(toFetch).getResult(); // _wapi.getTransactions(new GetTransactionsRequest(Wapi.VERSION, toFetch)).getResult();
         for (TransactionExApi tx : result.transactions) {
            // Verify transaction hash. This is important as we don't want to
            // have a transaction output associated with an outpoint that
            // doesn't match.
            // This is the end users protection against a rogue server that lies
            // about the value of an output and makes you pay a large fee.
            Sha256Hash hash = HashUtils.doubleSha256(tx.binary).reverse();
            if (hash.equals(tx.hash)) {
               parentTransactions.put(tx.txid, tx);
            } else {
               _logger.logError("Failed to validate transaction hash from server. Expected: " + tx.txid
                       + " Calculated: " + hash);
               //TODO: Document what's happening here.
               //Question: Crash and burn? Really? How about user feedback? Here, wapi returned a transaction that doesn't hash to the txid it is supposed to txhash to, right?
               throw new RuntimeException("Failed to validate transaction hash from server. Expected: " + tx.txid
                       + " Calculated: " + hash);
            }
         }
      }

      // We should now have all parent transactions or parent outputs. There is
      // a slight probability that one of them was not found due to double
      // spends and/or malleability and network latency etc.

      // Now figure out which parent outputs we need to persist
      List<TransactionOutputEx> toPersist = new LinkedList<>();
      for (Transaction t : transactions) {
         for (TransactionInput in : t.inputs) {
            if (in.outPoint.txid.equals(OutPoint.COINBASE_OUTPOINT.txid)) {
               // coinbase input, so no parent
               continue;
            }
            TransactionOutputEx parentOutput = parentOutputs.get(in.outPoint);
            if (parentOutput != null) {
               // We had it all along
               continue;
            }
            TransactionEx parentTex = parentTransactions.get(in.outPoint.txid);
            if (parentTex != null) {
               // Parent output not found, maybe we already have it
               parentOutput = TransactionEx.getTransactionOutput(parentTex, in.outPoint.index);
               toPersist.add(parentOutput);
               continue;
            }
         }
      }

      // Persist
      if (toPersist.size() <= MAX_TRANSACTIONS_TO_HANDLE_SIMULTANEOUSLY) {
         _backing.putParentTransactionOuputs(toPersist);
      } else {
         // We have quite a list of transactions to handle, do it in batches
         ArrayList<TransactionOutputEx> all = new ArrayList<>(toPersist);
         for (int i = 0; i < all.size(); i += MAX_TRANSACTIONS_TO_HANDLE_SIMULTANEOUSLY) {
            int endIndex = Math.min(all.size(), i + MAX_TRANSACTIONS_TO_HANDLE_SIMULTANEOUSLY);
            List<TransactionOutputEx> sub = all.subList(i, endIndex);
            _backing.putParentTransactionOuputs(sub);
         }
      }
   }

   protected Balance calculateLocalBalance() {
      Collection<TransactionOutputEx> unspentOutputs = new HashSet<>(_backing.getAllUnspentOutputs());
      long confirmed = 0;
      long pendingChange = 0;
      long pendingSending = 0;
      long pendingReceiving = 0;

      //
      // Determine the value we are receiving and create a set of outpoints for fast lookup
      //
      Set<OutPoint> unspentOutPoints = new HashSet<>();
      for (TransactionOutputEx output : unspentOutputs) {
         if (isColuDustOutput(output)) {
            continue;
         }
         if (output.height == -1) {
            if (isFromMe(output.outPoint.txid)) {
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
      List<Transaction> unconfirmed = new ArrayList<>();
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
            if (input.outPoint.txid.equals(Sha256Hash.ZERO_HASH)) {
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
               OutPoint outPoint = new OutPoint(t.getId(), i);
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
    * <p>
    * This method should only be called from the wallet manager
    *
    * @return false if synchronization failed due to failed blockchain
    * connection
    */
   @Override
   public synchronized boolean broadcastOutgoingTransactions() {
      checkNotArchived();
      List<Sha256Hash> broadcastedIds = new LinkedList<>();
      Map<Sha256Hash, byte[]> transactions = _backing.getOutgoingTransactions();

      int malformedTransactionsCount = 0;

      for (byte[] rawTransaction : transactions.values()) {
         Transaction transaction;
         try {
            transaction = Transaction.fromBytes(rawTransaction);
         } catch (TransactionParsingException e) {
            _logger.logError("Unable to parse transaction from bytes: " + HexUtils.toHex(rawTransaction), e);
            return  false;
         }
         BroadcastResult result = broadcastTransaction(transaction);
         if (result.getResultType() == BroadcastResultType.SUCCESS) {
            broadcastedIds.add(transaction.getId());
            _backing.removeOutgoingTransaction(transaction.getId());
         } else if (result.getResultType() == BroadcastResultType.REJECT_MALFORMED) {
            malformedTransactionsCount++;
         }
      }

      if (malformedTransactionsCount > 0) {
         postEvent(Event.MALFORMED_OUTGOING_TRANSACTIONS_FOUND);
      }

      if (!broadcastedIds.isEmpty()) {
         onTransactionsBroadcasted(broadcastedIds);
      }
      return true;
   }

   @Override
   public TransactionEx getTransaction(Sha256Hash txid) {
      return _backing.getTransaction(txid);
   }

   @Override
   public synchronized BroadcastResult broadcastTransaction(Transaction transaction) {
      checkNotArchived();
      try {
         WapiResponse<BroadcastTransactionResponse> response = _wapi.broadcastTransaction(
               new BroadcastTransactionRequest(Wapi.VERSION, transaction.toBytes()));
         int errorCode = response.getErrorCode();
         if (errorCode == Wapi.ERROR_CODE_SUCCESS) {
            if (response.getResult().success) {
               markTransactionAsSpent(TransactionEx.fromUnconfirmedTransaction(transaction));
               postEvent(Event.BROADCASTED_TRANSACTION_ACCEPTED);
               return new BroadcastResult(BroadcastResultType.SUCCESS);
            } else {
               // This transaction was rejected must be double spend or
               // malleability, delete it locally.
               _logger.logError("Failed to broadcast transaction due to a double spend or malleability issue");
               postEvent(Event.BROADCASTED_TRANSACTION_DENIED);
               return new BroadcastResult(BroadcastResultType.REJECT_DUPLICATE);
            }
         } else if (errorCode == Wapi.ERROR_CODE_NO_SERVER_CONNECTION) {
            postEvent(Event.SERVER_CONNECTION_ERROR);
            _logger.logError("Server connection failed with ERROR_CODE_NO_SERVER_CONNECTION");
            return new BroadcastResult(BroadcastResultType.NO_SERVER_CONNECTION);
         } else if(errorCode == Wapi.ElectrumxError.REJECT_MALFORMED.getErrorCode()) {
            return new BroadcastResult(response.getErrorMessage(), BroadcastResultType.REJECT_MALFORMED);
         } else if(errorCode == Wapi.ElectrumxError.REJECT_DUPLICATE.getErrorCode()) {
            return new BroadcastResult(response.getErrorMessage(), BroadcastResultType.REJECT_DUPLICATE);
         } else if(errorCode == Wapi.ElectrumxError.REJECT_NONSTANDARD.getErrorCode()) {
            return new BroadcastResult(response.getErrorMessage(), BroadcastResultType.REJECT_NONSTANDARD);
         } else if(errorCode == Wapi.ElectrumxError.REJECT_INSUFFICIENT_FEE.getErrorCode()) {
            return new BroadcastResult(response.getErrorMessage(), BroadcastResultType.REJECT_INSUFFICIENT_FEE);
         } else {
            postEvent(Event.BROADCASTED_TRANSACTION_DENIED);
            _logger.logError("Server connection failed with error: " + errorCode);
            return new BroadcastResult(BroadcastResultType.REJECTED);
         }
      } catch (WapiException e) {
         postEvent(Event.SERVER_CONNECTION_ERROR);
         _logger.logError("Server connection failed with error code: " + e.errorCode, e);
         return new BroadcastResult(BroadcastResultType.NO_SERVER_CONNECTION);
      }
   }

   protected void checkNotArchived() {
      final String usingArchivedAccount = "Using archived account";
      if (isArchived()) {
         _logger.logError(usingArchivedAccount);
         throw new RuntimeException(usingArchivedAccount);
      }
   }

   @Override
   public abstract boolean isArchived();

   @Override
   public abstract boolean isActive();

   protected abstract void onNewTransaction(Transaction t);

   protected void onTransactionsBroadcasted(List<Sha256Hash> txids) {
   }

   @Override
   public abstract boolean canSpend();

   @Override
   public List<TransactionSummary> getTransactionHistory(int offset, int limit) {
      // Note that this method is not synchronized, and we might fetch the transaction history while synchronizing
      // accounts. That should be ok as we write to the DB in a sane order.

      List<TransactionSummary> history = new ArrayList<>();
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
   public List<TransactionSummary> getTransactionsSince(Long receivingSince) {
      List<TransactionSummary> history = new ArrayList<>();
      checkNotArchived();
      int blockChainHeight = getBlockChainHeight();
      List<TransactionEx> list = _backing.getTransactionsSince(receivingSince);
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
            unsigned.getSigningRequests(),
            new PrivateKeyRing(cipher)
      );

      // Apply signatures and finalize transaction
      return StandardTransactionBuilder.finalizeTransaction(unsigned, signatures);
   }

   @Override
   public synchronized void queueTransaction(TransactionEx transaction) {
      // Store transaction in outgoing buffer, so we can broadcast it
      // later
      byte[] rawTransaction = transaction.binary;
      _backing.putOutgoingTransaction(transaction.txid, rawTransaction);
      markTransactionAsSpent(transaction);
   }

   @Override
   public synchronized boolean deleteTransaction(Sha256Hash transactionId) {
      TransactionEx tex = _backing.getTransaction(transactionId);
      if (tex == null) {
         return false;
      }
      Transaction tx = TransactionEx.toTransaction(tex);
      _backing.beginTransaction();
      try {
         // See if any of the outputs are stored locally and remove them
         for (int i = 0; i < tx.outputs.length; i++) {
            OutPoint outPoint = new OutPoint(tx.getId(), i);
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
   public void removeAllQueuedTransactions() {
      Map<Sha256Hash, byte[]> outgoingTransactions = _backing.getOutgoingTransactions();

      for(Sha256Hash key : outgoingTransactions.keySet()) {
         cancelQueuedTransaction(key);
      }
   }

   @Override
   public synchronized boolean cancelQueuedTransaction(Sha256Hash transaction) {
      Map<Sha256Hash, byte[]> outgoingTransactions = _backing.getOutgoingTransactions();

      if (!outgoingTransactions.containsKey(transaction)) {
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
            OutPoint outPoint = new OutPoint(tx.getId(), i);
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
      } finally {
         _backing.endTransaction();
      }

      // calc the new balance to remove the outgoing amount
      // the total balance will still be wrong, as we already deleted some UTXOs to build the queued transaction
      // these will get restored after the next sync
      updateLocalBalance();

      //markTransactionAsSpent(transaction);
      return true;
   }

   private void markTransactionAsSpent(TransactionEx transaction) {
      _backing.beginTransaction();
      final Transaction parsedTransaction;
      try {
         parsedTransaction = Transaction.fromBytes(transaction.binary);
      } catch (TransactionParsingException e) {
         _logger.logInfo(String.format("Unable to parse transaction %s: %s", transaction.txid, e.getMessage()));
         return;
      }
      try {
         // Remove inputs from unspent, marking them as spent
         for (TransactionInput input : parsedTransaction.inputs) {
            TransactionOutputEx parentOutput = _backing.getUnspentOutput(input.outPoint);
            if (parentOutput != null) {
               _backing.deleteUnspentOutput(input.outPoint);
               _backing.putParentTransactionOutput(parentOutput);
            }
         }

         // See if any of the outputs are for ourselves and store them as
         // unspent
         for (int i = 0; i < parsedTransaction.outputs.length; i++) {
            TransactionOutput output = parsedTransaction.outputs[i];
            if (isMine(output.script)) {
               _backing.putUnspentOutput(new TransactionOutputEx(new OutPoint(parsedTransaction.getId(), i), -1,
                     output.value, output.script.getScriptBytes(), false));
            }
         }

         // Store transaction locally, so we have it in our history and don't
         // need to fetch it in a minute
         _backing.putTransaction(transaction);
         _backing.setTransactionSuccessful();
      } finally {
         _backing.endTransaction();
      }

      // Tell account that we have a new transaction
      onNewTransaction(parsedTransaction);

      // Calculate local balance cache. It has changed because we have done
      // some spending
      updateLocalBalance();
      persistContextIfNecessary();
   }

   protected abstract void persistContextIfNecessary();

   @Override
   public void checkAmount(Receiver receiver, long kbMinerFee, CurrencyValue enteredAmount) throws InsufficientFundsException, OutputTooSmallException, StandardTransactionBuilder.UnableToBuildTransactionException {
      createUnsignedTransaction(singletonList(receiver), kbMinerFee);
   }

   @Override
   public NetworkParameters getNetwork() {
      return _network;
   }
   
   // TODO: 07.10.17 these values are subject to change and not a solid way to detect cc outputs.
   public static final int COLU_MAX_DUST_OUTPUT_SIZE_TESTNET = 600;
   public static final int COLU_MAX_DUST_OUTPUT_SIZE_MAINNET = 10000;

   //Retrieves indexes of colu outputs if the transaction is determined to be colu transaction
   //In the case of non-colu transaction returns empty list
   private List<Integer> getColuOutputIndexes(Transaction tx) throws ParseException {
      if (tx == null) {
         return new ArrayList<>();
      }
      for(int i = 0 ; i < tx.outputs.length;i++) {
         TransactionOutput curOutput = tx.outputs[i];
         byte[] scriptBytes = curOutput.script.getScriptBytes();
         //Check the protocol identifier 0x4343 ASCII representation of the string CC ("Colored Coins")
         if (curOutput.value == 0 && coluTransferInstructionsParser.isValidColuScript(scriptBytes)) {
            List<Integer> indexesList = coluTransferInstructionsParser.retrieveOutputIndexesFromScript(scriptBytes);
            //Since all assets with remaining amounts are automatically transferred to the last output,
            //add the last output to indexes list.
            //At least CC transaction could consist of have two outputs if it has no change - dust output that represents
            //transferred assets value and an empty output containing OP_RETURN data.
            //If the CC transaction has the change to transfer, it will be represented at least as the third dust output
            if (tx.outputs.length > 2) {
               indexesList.add(tx.outputs.length - 1);
            }
            return indexesList;
         }
      }

      return new ArrayList<>();
   }

   private boolean isColuTransaction(Transaction tx) {
      try {
         return !getColuOutputIndexes(tx).isEmpty();
      } catch (ParseException e) {
         // the current only use case is safe to be treated as not colored-coin even though we might misinterpret a colored-coin script.
         return false;
      }
   }

   private boolean isColuDustOutput(TransactionOutputEx output) {
      Transaction transaction = TransactionEx.toTransaction(_backing.getTransaction(output.outPoint.txid));
      try {
         if (getColuOutputIndexes(transaction).contains(output.outPoint.index)) {
            return true;
         }
      } catch (ParseException e) {
         // better safe than sorry:
         // if we can't interpret the script, we assume it is a colore coin output as before introducing the script interpretation.
         // usually we can read the script, so bigger colored coins txos get interpreted as such and smaller utxos are spendable
         int coluDustOutputSize = _network.isTestnet() ? COLU_MAX_DUST_OUTPUT_SIZE_TESTNET : COLU_MAX_DUST_OUTPUT_SIZE_MAINNET;
         if (output.value <= coluDustOutputSize) {
            return true;
         }
      }
      return false;
   }

   /**
    * @param minerFeePerKbToUse Determines the dust level, at which including a UTXO costs more than it is worth.
    * @return all UTXOs that are spendable now, as they are neither locked coinbase outputs nor unconfirmed received coins if _allowZeroConfSpending is not set nor dust.
    */
   protected Collection<TransactionOutputEx> getSpendableOutputs(long minerFeePerKbToUse) {
      long satDustOutput = StandardTransactionBuilder.MAX_INPUT_SIZE * minerFeePerKbToUse / 1000;
      Collection<TransactionOutputEx> allUnspentOutputs = _backing.getAllUnspentOutputs();

      // Prune confirmed outputs for coinbase outputs that are not old enough
      // for spending. Also prune unconfirmed receiving coins except for change
      int blockChainHeight = getBlockChainHeight();
      Iterator<TransactionOutputEx> it = allUnspentOutputs.iterator();
      while (it.hasNext()) {
         TransactionOutputEx output = it.next();
         // we remove all outputs that don't cover their costs (dust)
         // coinbase outputs are not spendable and this should not be overridden
         // Unless we allow zero confirmation spending we prune all unconfirmed outputs sent from foreign addresses
         if (output.value < satDustOutput ||
                     output.isCoinBase && blockChainHeight - output.height < COINBASE_MIN_CONFIRMATIONS ||
                     !_allowZeroConfSpending && output.height == -1 && !isFromMe(output.outPoint.txid)) {
            it.remove();
         } else {
            if (isColuDustOutput(output)) {
               it.remove();
            }
         }
      }
      return allUnspentOutputs;
   }

   protected abstract Address getChangeAddress(Address destinationAddress);

   protected abstract Address getChangeAddress();

   protected abstract Address getChangeAddress(List<Address> destinationAddresses);

   private static Collection<UnspentTransactionOutput> transform(Collection<TransactionOutputEx> source) {
      List<UnspentTransactionOutput> outputs = new ArrayList<>();
      for (TransactionOutputEx s : source) {
         ScriptOutput script = ScriptOutput.fromScriptBytes(s.script);
         outputs.add(new UnspentTransactionOutput(s.outPoint, s.height, s.value, script));
      }
      return outputs;
   }

   @Override
   public synchronized ExactCurrencyValue calculateMaxSpendableAmount(long minerFeePerKbToUse) {
      return calculateMaxSpendableAmount(minerFeePerKbToUse, null);
   }

   @Override
   public synchronized ExactCurrencyValue calculateMaxSpendableAmount(long minerFeePerKbToUse, Address destinationAddress) {
      checkNotArchived();
      Collection<UnspentTransactionOutput> spendableOutputs = transform(getSpendableOutputs(minerFeePerKbToUse));
      long satoshis = 0;

      // sum up the maximal available number of satoshis (i.e. sum of all spendable outputs)
      for (UnspentTransactionOutput output : spendableOutputs) {
         satoshis += output.value;
      }

      // TODO: 25.06.17 the following comment was justifying to assume two outputs, which might wrongly lead to no spendable funds or am I reading the wrongly? I assume one output only for the max.
      // we will use all of the available inputs and it will be only one output
      // but we use "2" here, because the tx-estimation in StandardTransactionBuilder always includes an
      // output into its estimate - so add one here too to arrive at the same tx fee
      FeeEstimatorBuilder estimatorBuilder = new FeeEstimatorBuilder().setArrayOfInputs(spendableOutputs)
              .setMinerFeePerKb(minerFeePerKbToUse);
      addOutputToEstimation(destinationAddress, estimatorBuilder);
      FeeEstimator estimator = estimatorBuilder.createFeeEstimator();
      long feeToUse = estimator.estimateFee();

      satoshis -= feeToUse;
      if (satoshis <= 0) {
         return ZERO;
      }

      // Create transaction builder
      StandardTransactionBuilder stb = new StandardTransactionBuilder(_network);

      AddressType destinationAddressType;
      if (destinationAddress != null) {
         destinationAddressType = destinationAddress.getType();
      } else {
         destinationAddressType = AddressType.P2PKH;
      }
      // Try and add the output
      try {
         // Note, null address used here, we just use it for measuring the transaction size
         stb.addOutput(Address.getNullAddress(_network, destinationAddressType), satoshis);
      } catch (OutputTooSmallException e1) {
         // The amount we try to send is lower than what the network allows
         return ZERO;
      }

      // Try to create an unsigned transaction
      try {
         stb.createUnsignedTransaction(spendableOutputs, getChangeAddress(Address.getNullAddress(_network, destinationAddressType)),
                 new PublicKeyRing(), _network, minerFeePerKbToUse);
         // We have enough to pay the fees, return the amount as the maximum
         return ExactBitcoinValue.from(satoshis);
      } catch (InsufficientFundsException | StandardTransactionBuilder.UnableToBuildTransactionException e) {
         return ZERO;
      }
   }

   private void addOutputToEstimation(Address outputAddress, FeeEstimatorBuilder estimatorBuilder) {
      if (outputAddress != null) {
         estimatorBuilder.addOutput(outputAddress.getType());
      } else {
         estimatorBuilder.setLegacyOutputs(1);
      }
   }

   protected abstract InMemoryPrivateKey getPrivateKey(PublicKey publicKey, KeyCipher cipher)
         throws InvalidKeyCipher;

   protected abstract InMemoryPrivateKey getPrivateKeyForAddress(Address address, KeyCipher cipher)
         throws InvalidKeyCipher;

   public abstract List<AddressType> getAvailableAddressTypes();

   public abstract Address getReceivingAddress(AddressType addressType);

   public abstract void setDefaultAddressType(AddressType addressType);

   protected abstract PublicKey getPublicKeyForAddress(Address address);

   @Override
   public synchronized UnsignedTransaction createUnsignedTransaction(List<Receiver> receivers, long minerFeeToUse)
         throws OutputTooSmallException, InsufficientFundsException, StandardTransactionBuilder.UnableToBuildTransactionException {
      checkNotArchived();

      // Determine the list of spendable outputs
      Collection<UnspentTransactionOutput> spendable = transform(getSpendableOutputs(minerFeeToUse));

      // Create the unsigned transaction
      StandardTransactionBuilder stb = new StandardTransactionBuilder(_network);
      List<Address> addressList = new ArrayList<>();
      for (Receiver receiver : receivers) {
         stb.addOutput(receiver.address, receiver.amount);
         addressList.add(receiver.address);
      }
      Address changeAddress = getChangeAddress(addressList);
      return stb.createUnsignedTransaction(spendable, changeAddress, new PublicKeyRing(),
            _network, minerFeeToUse);
   }

   @Override
   public UnsignedTransaction createUnsignedTransaction(OutputList outputs, long minerFeeToUse) throws OutputTooSmallException, InsufficientFundsException, StandardTransactionBuilder.UnableToBuildTransactionException {
      checkNotArchived();

      // Determine the list of spendable outputs
      Collection<UnspentTransactionOutput> spendable = transform(getSpendableOutputs(minerFeeToUse));

      // Create the unsigned transaction
      StandardTransactionBuilder stb = new StandardTransactionBuilder(_network);
      stb.addOutputs(outputs);
      Address changeAddress = getChangeAddress();
      return stb.createUnsignedTransaction(spendable, changeAddress, new PublicKeyRing(),
            _network, minerFeeToUse);
   }

   /**
    * Create a new, unsigned transaction that spends from a UTXO of the provided transaction.
    * @see WalletAccount#createUnsignedTransaction(List, long)
    *
    * @param txid transaction to spend from
    * @param minerFeeToUse fee to use to pay up for txid and the new transaction
    * @param satoshisPaid amount already paid by parent transaction
    */
   public UnsignedTransaction createUnsignedCPFPTransaction(Sha256Hash txid, long minerFeeToUse, long satoshisPaid) throws InsufficientFundsException, StandardTransactionBuilder.UnableToBuildTransactionException {
      checkNotArchived();
      Set<UnspentTransactionOutput> utxos = new HashSet<>(transform(getSpendableOutputs(minerFeeToUse)));
      TransactionDetails parent = getTransactionDetails(txid);
      long totalSpendableSatoshis = 0;
      // do we have an output to spend from?
      List<UnspentTransactionOutput> utxosToSpend = new ArrayList<>();
      for(UnspentTransactionOutput utxo : utxos) {
         if(utxo.outPoint.txid.equals(txid)) {
            totalSpendableSatoshis += utxo.value;
            utxosToSpend.add(utxo);
            utxos.remove(utxo);
            //makeText(this, "Found a UTXO", LENGTH_SHORT).show();
            // we ideally use just one UTXO even if more than one is owers. This leaves room to add further children at the same depth to pay for parents
            break;
         }
      }
      if(utxosToSpend.isEmpty()) {
         //makeText(this, "We own no UTXOs to bump the transaction!", LENGTH_LONG).show();
         //finish();
         throw new StandardTransactionBuilder.UnableToBuildTransactionException("We have no UTXO");
      }
      Address changeAddress = getChangeAddress();
      long parentChildFeeSat;
      do {
         FeeEstimatorBuilder builder = new FeeEstimatorBuilder().setArrayOfInputs(utxosToSpend);
         addOutputToEstimation(changeAddress, builder);
         long childSize = builder.createFeeEstimator().estimateTransactionSize();
         long parentChildSize = parent.rawSize + childSize;
         parentChildFeeSat = parentChildSize * minerFeeToUse / 1000 - satoshisPaid;
         if(parentChildFeeSat < childSize * minerFeeToUse / 1000) {
            // if child doesn't get itself to target priority, it's not needed to boost a parent to it.
            throw new StandardTransactionBuilder.UnableToBuildTransactionException("parent needs no boosting");
         }
         long value = totalSpendableSatoshis - parentChildFeeSat;
         // we have to pay for fee plus one output. Zero outputs are not allowed.
         // See https://github.com/bitcoin/bitcoin/blob/ba7220b5e82fcfbb7a4912a49e563944a428ab91/src/validation.cpp#L497
         if(value < MINIMUM_OUTPUT_VALUE && !utxos.isEmpty()) {
            // we can't pay the fee with the UTXOs at hand
            UnspentTransactionOutput utxo = utxos.iterator().next();
            utxosToSpend.add(utxo);
            totalSpendableSatoshis += utxo.value;
            utxos.remove(utxo);
            continue;
         }
         List<TransactionOutput> outputs = singletonList(createOutput(changeAddress, value, _network));
          return new UnsignedTransaction(outputs, utxosToSpend, new PublicKeyRing(), _network, 0, UnsignedTransaction.NO_SEQUENCE);
      } while(!utxos.isEmpty());
      throw new InsufficientFundsException(0, parentChildFeeSat);
   }

   @Override
   public Balance getBalance() {
      // public method that needs no synchronization
      checkNotArchived();
      // We make a copy of the reference for a reason. Otherwise the balance
      // might change right when we make a copy
      Balance b = _cachedBalance;
      return b != null ? new Balance(b.confirmed, b.pendingReceiving, b.pendingSending, b.pendingChange, b.updateTime,
              b.blockHeight, isSynchronizing(), b.allowsZeroConfSpending)
              : new Balance(0, 0, 0, 0, 0, 0, isSynchronizing(), false);
   }

   @Override
   public CurrencyBasedBalance getCurrencyBasedBalance() {
      Balance balance = getBalance();
      long spendableBalance = balance.getSpendableBalance();
      long sendingBalance = balance.getSendingBalance();
      long receivingBalance = balance.getReceivingBalance();

      if (spendableBalance < 0) {
         throw new IllegalArgumentException(String.format(Locale.getDefault(), "spendableBalance < 0: %d; account: %s", spendableBalance, this.getClass().toString()));
      }
      if (sendingBalance < 0) {
         sendingBalance = 0;
      }
      if (receivingBalance < 0) {
         receivingBalance = 0;
      }

      ExactCurrencyValue confirmed = ExactBitcoinValue.from(spendableBalance);
      ExactCurrencyValue sending = ExactBitcoinValue.from(sendingBalance);
      ExactCurrencyValue receiving = ExactBitcoinValue.from(receivingBalance);
      return new CurrencyBasedBalance(confirmed, sending, receiving);
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

   private TransactionSummary transform(TransactionEx tex, int blockChainHeight) {
      Transaction tx;
      try {
         tx = Transaction.fromByteReader(new ByteReader(tex.binary));
      } catch (TransactionParsingException e) {
         // Should not happen as we have parsed the transaction earlier
         _logger.logError("Unable to parse ");
         return null;
      }

      boolean isColuTransaction = isColuTransaction(tx);

      if (isColuTransaction) {
         return null;
      }

      // Outputs
      long satoshis = 0;
      List<Address> toAddresses = new ArrayList<>();
      Address destAddress = null;
      for (TransactionOutput output : tx.outputs) {
         final Address address = output.script.getAddress(_network);
         if (isMine(output.script)) {
            satoshis += output.value;
         } else {
            destAddress = address;
         }
         if (address != null && !address.equals(Address.getNullAddress(_network))) {
            toAddresses.add(address);
         }
      }

      // Inputs
      if (!tx.isCoinbase()) {
         for (TransactionInput input : tx.inputs) {
            // find parent output
            TransactionOutputEx funding = _backing.getParentTransactionOutput(input.outPoint);
            if (funding == null) {
               funding = _backing.getUnspentOutput(input.outPoint);
            }
            if (funding == null) {
               continue;
            }
            if (isMine(funding)) {
               satoshis -= funding.value;
            }
         }
      }
      // else {
      //    For coinbase transactions there is nothing to subtract
      // }
      int confirmations;
      if (tex.height == -1) {
         confirmations = 0;
      } else {
         confirmations = Math.max(0, blockChainHeight - tex.height + 1);
      }

      // only track a destinationAddress if it is an outgoing transaction (i.e. send money to someone)
      // to prevent the user that he tries to return money to an address he got bitcoin from.
      if (satoshis >= 0) {
         destAddress = null;
      }

      boolean isQueuedOutgoing = _backing.isOutgoingTransaction(tx.getId());

      // see if we have a riskAssessment for this tx available in memory (i.e. valid for last sync)
      final ConfirmationRiskProfileLocal risk = riskAssessmentForUnconfirmedTx.get(tx.getId());

      return new TransactionSummary(
            tx.getId(),
            ExactBitcoinValue.from(Math.abs(satoshis)),
            satoshis >= 0,
            tex.time,
            tex.height,
            confirmations,
            isQueuedOutgoing,
            risk,
            com.google.common.base.Optional.fromNullable(destAddress),
            toAddresses);
   }

   @Override
   public List<TransactionOutputSummary> getUnspentTransactionOutputSummary() {
      // Note that this method is not synchronized, and we might fetch the transaction history while synchronizing
      // accounts. That should be ok as we write to the DB in a sane order.

      // Get all unspent outputs for this account
      Collection<TransactionOutputEx> outputs = _backing.getAllUnspentOutputs();

      // Transform it to a list of summaries
      List<TransactionOutputSummary> list = new ArrayList<>();
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
      List<Sha256Hash> txids = new ArrayList<>(list.size());
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
         TransactionEx localTransactionEx = _backing.getTransaction(t.txid);
         Transaction parsedTransaction;
         if (localTransactionEx != null) {
            try {
               parsedTransaction = Transaction.fromBytes(localTransactionEx.binary);
            } catch (TransactionParsingException ignore) {
               parsedTransaction = null;
            }
         } else {
            parsedTransaction = null;
         }

         // check if this transaction is unconfirmed and spends any inputs that got already spend
         // by any other transaction we know
         boolean isDoubleSpend = false;
         if (parsedTransaction != null && localTransactionEx.height == -1) {
            for (TransactionInput input : parsedTransaction.inputs) {
               Collection<Sha256Hash> otherTx = _backing.getTransactionsReferencingOutPoint(input.outPoint);
               // remove myself
               otherTx.remove(parsedTransaction.getId());
               if (!otherTx.isEmpty()) {
                  isDoubleSpend = true;
               }
            }
         }

         // if this transaction summary has a risk assessment set, remember it
         if (t.rbfRisk || t.unconfirmedChainLength > 0 || isDoubleSpend) {
            riskAssessmentForUnconfirmedTx.put(t.txid, new ConfirmationRiskProfileLocal(t.unconfirmedChainLength, t.rbfRisk, isDoubleSpend));
         } else {
            // otherwise just remove it if we ever got one
            riskAssessmentForUnconfirmedTx.remove(t.txid);
         }

         // does the server know anything about this tx?
         if (!t.found) {
            if (localTransactionEx != null) {
               // We have a transaction locally that did not get reported back by the server
               // put it into the outgoing queue and mark it as "not transmitted" (even as it might be an incoming tx)
               queueTransaction(localTransactionEx);
            } else {
               // we haven't found it locally (shouldn't happen here) - so delete it to be sure
               _backing.deleteTransaction(t.txid);
            }
            continue;
         } else {
            // we got it back from the server and it got confirmations - remove it from out outgoing queue
            if (t.height > -1 || _backing.isOutgoingTransaction(t.txid)) {
               _backing.removeOutgoingTransaction(t.txid);
            }
         }

         // update the local transaction
         if (localTransactionEx != null && (localTransactionEx.height != t.height)) {
            // The transaction got a new height. There could be
            // several reasons for that. It confirmed, or might also be a reorg.
            TransactionEx newTex = new TransactionEx(localTransactionEx.txid, localTransactionEx.hash, t.height, localTransactionEx.time, localTransactionEx.binary);
            _logger.logInfo(String.format("Replacing: %s With: %s", localTransactionEx.toString(), newTex.toString()));
            _backing.putTransaction(newTex);
            postEvent(Event.TRANSACTION_HISTORY_CHANGED);
         }
      }
      return true;
   }

   // local cache for received risk assessments for unconfirmed transactions - does not get persisted in the db
   private HashMap<Sha256Hash, ConfirmationRiskProfileLocal> riskAssessmentForUnconfirmedTx = new HashMap<>();

   protected abstract boolean isSynchronizing();

   public class PublicKeyRing implements IPublicKeyRing {
      @Override
      public PublicKey findPublicKeyByAddress(Address address) {
         PublicKey publicKey = getPublicKeyForAddress(address);
         if (publicKey != null) {
            if (address.getType() == AddressType.P2SH_P2WPKH
                    || address.getType() == AddressType.P2WPKH) {
               return new PublicKey(publicKey.getPubKeyCompressed());
            }
            return publicKey;
         }
         // something unexpected happened - the account might be in a undefined state
         // drop local cached data (transaction history, addresses - metadata will be kept)
         dropCachedData();

         // let the app crash anyway, so that we get notified. after restart it should resync the account completely
         throw new RuntimeException(String.format("Unable to find public key for address %s acc:%s", address.toString(), AbstractAccount.this.getClass().toString()));
      }
   }

   public class PrivateKeyRing extends PublicKeyRing implements IPublicKeyRing, IPrivateKeyRing {

      KeyCipher _cipher;

      public PrivateKeyRing(KeyCipher cipher) {
         _cipher = cipher;
      }

      @Override
      public BitcoinSigner findSignerByPublicKey(PublicKey publicKey) {
         InMemoryPrivateKey privateKey;
         try {
            privateKey = getPrivateKey(publicKey, _cipher);
         } catch (InvalidKeyCipher e) {
            throw new RuntimeException("Unable to decrypt private key for public key " + publicKey.toString());
         }
         if (privateKey != null) {
            return privateKey;
         }
         throw new RuntimeException("Unable to find private key for public key " + publicKey.toString());
      }
   }

   @Override
   public TransactionSummary getTransactionSummary(Sha256Hash txid) {
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

      List<TransactionDetails.Item> inputs = new ArrayList<>(tx.inputs.length);
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

      return new TransactionDetails(
            txid, tex.height, tex.time,
            inputs.toArray(new TransactionDetails.Item[inputs.size()]), outputs,
            tx.toBytes(false).length
      );
   }

   public UnsignedTransaction createUnsignedPop(Sha256Hash txid, byte[] nonce) {
      checkNotArchived();

      try {
         TransactionEx txExToProve = _backing.getTransaction(txid);
         Transaction txToProve = Transaction.fromByteReader(new ByteReader(txExToProve.binary));

         List<UnspentTransactionOutput> funding = new ArrayList<>(txToProve.inputs.length);
         for (TransactionInput input : txToProve.inputs) {
            TransactionEx inTxEx = _backing.getTransaction(input.outPoint.txid);
            Transaction inTx = Transaction.fromByteReader(new ByteReader(inTxEx.binary));
            UnspentTransactionOutput unspentOutput = new UnspentTransactionOutput(input.outPoint, inTxEx.height,
                  inTx.outputs[input.outPoint.index].value,
                  inTx.outputs[input.outPoint.index].script);

            funding.add(unspentOutput);
         }

         TransactionOutput popOutput = createPopOutput(txid, nonce);

         PopBuilder popBuilder = new PopBuilder(_network);

         return popBuilder.createUnsignedPop(singletonList(popOutput), funding,
               new PublicKeyRing(), _network);
      } catch (TransactionParsingException e) {
         throw new RuntimeException("Cannot parse transaction: " + e.getMessage(), e);
      }
   }

   private TransactionOutput createPopOutput(Sha256Hash txidToProve, byte[] nonce) {
      ByteBuffer byteBuffer = ByteBuffer.allocate(41);
      byteBuffer.put((byte) Script.OP_RETURN);

      byteBuffer.put((byte) 1).put((byte) 0); // version 1, little endian

      byteBuffer.put(txidToProve.getBytes()); // txid

      if (nonce == null || nonce.length != 6) {
         throw new IllegalArgumentException("Invalid nonce. Expected 6 bytes.");
      }
      byteBuffer.put(nonce); // nonce

      ScriptOutput scriptOutput = ScriptOutputStrange.fromScriptBytes(byteBuffer.array());
      return new TransactionOutput(0L, scriptOutput);
   }

   @Override
   public boolean onlySyncWhenActive() {
      return false;
   }

   @Override
   public String getAccountDefaultCurrency() {
      return CurrencyValue.BTC;
   }

   public AccountBacking getAccountBacking() {
      return this._backing;
   }

   public int getSyncTotalRetrievedTransactions() {
      return syncTotalRetrievedTransactions;
   }

   public void updateSyncProgress() {
      postEvent(Event.SYNC_PROGRESS_UPDATED);
   }
}

