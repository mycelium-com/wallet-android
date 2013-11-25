/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.IndependentTransactionOutput;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.OutPoint;
import com.mrd.bitlib.model.SourcedTransactionOutput;
import com.mrd.bitlib.util.Sha256Hash;
import com.mrd.mbwapi.api.AddressOutputState;
import com.mrd.mbwapi.api.ApiError;
import com.mrd.mbwapi.api.GetTransactionDataResponse;
import com.mrd.mbwapi.api.QueryAddressSetStatusResponse;
import com.mycelium.wallet.api.AbstractCallbackHandler;
import com.mycelium.wallet.api.AndroidAsyncApi;
import com.mycelium.wallet.api.AsyncTask;
import com.mycelium.wallet.event.BlockchainError;
import com.mycelium.wallet.event.BlockchainReady;
import com.mycelium.wallet.event.SyncStarted;
import com.mycelium.wallet.event.SyncStopped;
import com.mycelium.wallet.persistence.PersistedOutput;
import com.mycelium.wallet.persistence.TxOutDb;
import com.squareup.otto.Bus;

public class BlockChainAddressTracker {

   private static final String LOG_TAG = "BlockchainAddressTracker";
   public static final String PROCESS_REFRESH_OUTPUT_INVENTORY = "refresh output inventory";

   private AndroidAsyncApi _asyncApi;
   private TxOutDb _txOutDb;
   private Context _context;
   private Bus _eventBus;
   private volatile AsyncTask _currentTask;
   private final Object _sync = new Object();
   private int _last_observed_block_height;
   private NetworkParameters _network;

   public BlockChainAddressTracker(AndroidAsyncApi asyncApi, TxOutDb txOutDb, Context applicationContext, Bus eventBus,
         NetworkParameters network) {
      _asyncApi = asyncApi;
      _txOutDb = txOutDb;
      _context = applicationContext;
      _network = network;
      _eventBus = eventBus;
   }

   public int getLastObservedBlockHeight() {
      if (_last_observed_block_height == 0) {
         SharedPreferences preferences = _context.getSharedPreferences(Constants.SETTINGS_NAME, Activity.MODE_PRIVATE);
         _last_observed_block_height = preferences.getInt(Constants.LAST_OBSERVED_BLOCK_HEIGHT_SETTING, 0);
      }
      return _last_observed_block_height;
   }

   private void setLastObservedBlockHeight(int height) {
      if (height != _last_observed_block_height) {
         _last_observed_block_height = height;
         Editor editor = _context.getSharedPreferences(Constants.SETTINGS_NAME, Activity.MODE_PRIVATE).edit();
         editor.putInt(Constants.LAST_OBSERVED_BLOCK_HEIGHT_SETTING, _last_observed_block_height).commit();
      }
   }

   public TransactionOutputInfo getOutputInfo(Collection<Address> addresses) {
      synchronized (_sync) {
         Set<PersistedOutput> confirmed = new HashSet<PersistedOutput>();
         Set<PersistedOutput> receivingForeign = new HashSet<PersistedOutput>();
         Set<PersistedOutput> receivingChange = new HashSet<PersistedOutput>();
         Set<PersistedOutput> sending = new HashSet<PersistedOutput>();

         // Determine the lowest update time for all addresses
         long lowestUpdateTime = Long.MAX_VALUE;
         long highestUpdateTime = Long.MIN_VALUE;

         for (Address address : addresses) {

            // Add confirmed outputs
            confirmed.addAll(_txOutDb.getConfirmedByAddress(address));

            // Add receiving splitting them into foreign and change outputs
            for (SourcedTransactionOutput output : _txOutDb.getReceivingByAddress(address)) {
               PersistedOutput receivingOutput = new PersistedOutput(output.outPoint, output.address, -1, output.value,
                     output.script, false);
               if (hasOverlap(output.senders, addresses)) {
                  receivingChange.add(receivingOutput);
               } else {
                  receivingForeign.add(receivingOutput);
               }
            }

            // Add sending outputs
            sending.addAll(_txOutDb.getSendingByAddress(address));

            // Get the chain height at which the address was updated
            long updateTime = _txOutDb.getUpdateTimeForAddress(address);
            lowestUpdateTime = Math.min(updateTime, lowestUpdateTime);
            highestUpdateTime = Math.max(updateTime, highestUpdateTime);
         }
         TransactionOutputInfo result = new TransactionOutputInfo(confirmed, receivingForeign, receivingChange,
               sending, lowestUpdateTime, highestUpdateTime, _last_observed_block_height);
         return result;
      }
   }

   private boolean hasOverlap(Set<Address> set, Collection<Address> collection) {
      for (Address a : collection) {
         if (set.contains(a)) {
            return true;
         }
      }
      return false;
   }

   public synchronized void updateAddresses(Collection<Address> addresses) {
      _eventBus.post(new SyncStarted(PROCESS_REFRESH_OUTPUT_INVENTORY));
      synchronized (_sync) {
         QueryActiveOutputsInventoryHandler queryHandler;
         queryHandler = new QueryActiveOutputsInventoryHandler(addresses, _network);
         if (_currentTask != null) {
            Log.w(LOG_TAG, "Started a second task while another task is in progress, blocking caller");
            while (_currentTask == null) {
               try {
                  Thread.sleep(100);// todo why?? if waiting for task do
                                    // future.await()
               } catch (InterruptedException e) {
                  //todo insert uncaught error handler
                  // Ignore
               }
            }
         }
         _currentTask = _asyncApi.getActiveOutputInventory(addresses, queryHandler);
      }
   }

   private class QueryActiveOutputsInventoryHandler implements AbstractCallbackHandler<QueryAddressSetStatusResponse> {
      private Collection<Address> _addresses;
      private NetworkParameters _network;

      public QueryActiveOutputsInventoryHandler(Collection<Address> addresses, NetworkParameters network) {
         _addresses = addresses;
         _network = network;
      }

      @Override
      public void handleCallback(QueryAddressSetStatusResponse response, ApiError exception) {
         try {
            if (exception != null) {
               Log.e(LOG_TAG, "Failed to get active output inventory: " + exception.errorMessage);
               _currentTask = null;
               reportError(exception);
               return;
            }

            boolean doSuccessCallback = false;
            synchronized (_sync) {
               int chainHeight = response.chainHeight;

               // Construct a map from addresses to block chain address output
               // state
               Map<Address, AddressOutputState> blockChainInfoMap = constructBlockChainInfoMap(response);

               response = null; // Allow GC to collect response

               // Construct a map from addresses to DB address output state
               Map<Address, AddressOutputState> dbInfoMap = constructDbInfoMap(blockChainInfoMap);

               // Get the list of outputs to fetch
               List<OutPoint> toFetch = findOutputsToFetch(blockChainInfoMap, dbInfoMap);
               List<OutPoint> sourcedOutputsToFetch = findSourcedOutputsToFetch(blockChainInfoMap, dbInfoMap);

               if (toFetch.isEmpty() && sourcedOutputsToFetch.isEmpty()) {
                  // We have all outputs in our database already, we just may
                  // need
                  // to do some bookkeeping
                  updateDb(blockChainInfoMap, dbInfoMap, new LinkedList<IndependentTransactionOutput>(),
                        new LinkedList<SourcedTransactionOutput>(), chainHeight, _network);
                  _currentTask = null;
                  doSuccessCallback = true;
               } else {
                  // We need to fetch some outputs. Updating database later
                  _currentTask = _asyncApi.getTransactionData(toFetch, sourcedOutputsToFetch,
                        new LinkedList<Sha256Hash>(), new GetOutputsHandler(_addresses, blockChainInfoMap, dbInfoMap,
                              chainHeight, _network));
               }
            }
            if (doSuccessCallback) {
               reportBalanceChanged();
            }
         } finally {
            _eventBus.post(new SyncStopped(PROCESS_REFRESH_OUTPUT_INVENTORY));
         }
      }

   }

   private class GetOutputsHandler implements AbstractCallbackHandler<GetTransactionDataResponse> {
      @SuppressWarnings("unused")
      private Collection<Address> _addresses;
      private Map<Address, AddressOutputState> _blockChainInfoMap;
      private Map<Address, AddressOutputState> _dbInfoMap;
      private int _chainHeight;
      private NetworkParameters _network;

      public GetOutputsHandler(Collection<Address> addresses, Map<Address, AddressOutputState> blockChainInfoMap,
            Map<Address, AddressOutputState> dbInfoMap, int chainHeight, NetworkParameters network) {
         _addresses = addresses;
         _blockChainInfoMap = blockChainInfoMap;
         _dbInfoMap = dbInfoMap;
         _chainHeight = chainHeight;
         _network = network;
      }

      @Override
      public void handleCallback(GetTransactionDataResponse response, ApiError exception) {
         if (exception != null) {
            Log.e(LOG_TAG, "Failed to get outputs: " + exception.errorMessage);
            _currentTask = null;
            reportError(exception);
            return;
         }

         synchronized (_sync) {
            // Update database
            updateDb(_blockChainInfoMap, _dbInfoMap, response.outputs, response.sourcedOutputs, _chainHeight, _network);
            _currentTask = null;
         }
         reportBalanceChanged();
      }

   }

   private Map<Address, AddressOutputState> constructDbInfoMap(Map<Address, AddressOutputState> blockChainInfoMap) {
      Map<Address, AddressOutputState> dbInfoMap = new HashMap<Address, AddressOutputState>();
      for (AddressOutputState blockChainInfo : blockChainInfoMap.values()) {

         // Get the database view of this address
         AddressOutputState dbInfo = _txOutDb.getAddressOutputState(blockChainInfo.address);

         // Add it to the database map for swift access
         dbInfoMap.put(dbInfo.address, dbInfo);
      }
      return dbInfoMap;
   }

   private Map<Address, AddressOutputState> constructBlockChainInfoMap(QueryAddressSetStatusResponse response) {
      Map<Address, AddressOutputState> addressInfoMap = new HashMap<Address, AddressOutputState>();
      for (AddressOutputState info : response.addressInfo) {
         addressInfoMap.put(info.address, info);
      }
      return addressInfoMap;
   }

   private void updateDb(Map<Address, AddressOutputState> blockChainInfoMap,
         Map<Address, AddressOutputState> dbInfoMap, List<IndependentTransactionOutput> fetchedOutputs,
         List<SourcedTransactionOutput> fetchedSourcedOutputs, int chainHeight, NetworkParameters network) {

      // Insert fetched records
      updateDbWithFetched(fetchedOutputs, fetchedSourcedOutputs, blockChainInfoMap, network);

      // Copy to sending if necessary
      for (AddressOutputState blockChainInfo : blockChainInfoMap.values()) {

         AddressOutputState dbInfo = dbInfoMap.get(blockChainInfo.address);

         // Check if block chain sending is up to date. We may have to update
         // the state
         for (OutPoint p : blockChainInfo.sending) {
            if (!dbInfo.sending.contains(p)) {
               // block chain has a sending output that the DB does not
               // have
               if (dbInfo.confirmed.contains(p)) {
                  // Copy from confirmed instead of getting it from server
                  PersistedOutput output = _txOutDb.getConfirmed(p);
                  if (output == null) {
                     continue;
                  }
                  _txOutDb.insertOrReplaceSending(output);
               } else if (dbInfo.receiving.contains(p)) {
                  // Copy from receiving instead of getting it from server
                  SourcedTransactionOutput output = _txOutDb.getReceiving(p);
                  if (output == null) {
                     continue;
                  }
                  PersistedOutput sending = new PersistedOutput(output.outPoint, output.address, -1, output.value,
                        output.script, false);
                  _txOutDb.insertOrReplaceSending(sending);
               } else {
                  // This scenario is already covered as we have just fetched it
                  // from the server
               }
            }
         }
      }

      // Delete surplus records
      deleteRecords(blockChainInfoMap, dbInfoMap);

      // Update address update height
      updateAddressUpdateHeights(blockChainInfoMap.keySet(), chainHeight);

      // Store the current block chain height
      setLastObservedBlockHeight(chainHeight);
   }

   private void updateAddressUpdateHeights(Set<Address> addresses, int chainHeight) {
      long updateTime = new Date().getTime();
      for (Address address : addresses) {
         _txOutDb.insertOrReplaceAddress(address, updateTime);
      }
   }

   private void updateDbWithFetched(List<IndependentTransactionOutput> outputs,
         List<SourcedTransactionOutput> sourcedOutputs, Map<Address, AddressOutputState> blockChainInfoMap,
         NetworkParameters network) {
      // Insert outputs into confirmed and sending
      for (IndependentTransactionOutput output : outputs) {
         Address address = output.script.getAddress(network);
         if (address == null) {
            continue;
         }
         AddressOutputState blockChainInfo = blockChainInfoMap.get(address);
         if (blockChainInfo == null) {
            continue;
         }
         PersistedOutput to = new PersistedOutput(output.outPoint, address, output.height, output.value,
               output.script.getScriptBytes(), output.isCoinbase);
         if (blockChainInfo.confirmed.contains(output.outPoint)) {
            _txOutDb.insertOrReplaceConfirmed(to);
         }

         if (blockChainInfo.sending.contains(output.outPoint)) {
            _txOutDb.insertOrReplaceSending(to);
         }
      }

      // Insert sourced outputs into receiving
      for (SourcedTransactionOutput output : sourcedOutputs) {
         AddressOutputState blockChainInfo = blockChainInfoMap.get(output.address);
         if (blockChainInfo == null) {
            continue;
         }
         if (blockChainInfo.receiving.contains(output.outPoint)) {
            _txOutDb.insertOrReplaceReceiving(output);
         }
      }
   }

   private void deleteRecords(Map<Address, AddressOutputState> blockChainInfoMap,
         Map<Address, AddressOutputState> dbInfoMap) {
      for (AddressOutputState dbInfo : dbInfoMap.values()) {
         AddressOutputState blockChainInfo = blockChainInfoMap.get(dbInfo.address);
         for (OutPoint outPoint : dbInfo.confirmed) {
            if (!blockChainInfo.confirmed.contains(outPoint)) {
               _txOutDb.deleteConfirmed(outPoint);
            }
         }
         for (OutPoint outPoint : dbInfo.receiving) {
            if (!blockChainInfo.receiving.contains(outPoint)) {
               _txOutDb.deleteReceiving(outPoint);
            }
         }
         for (OutPoint outPoint : dbInfo.sending) {
            if (!blockChainInfo.sending.contains(outPoint)) {
               _txOutDb.deleteSending(outPoint);
            }
         }
      }
   }

   private List<OutPoint> findOutputsToFetch(Map<Address, AddressOutputState> blockChainInfoMap,
         Map<Address, AddressOutputState> dbInfoMap) {
      List<OutPoint> toFetch = new LinkedList<OutPoint>();
      for (AddressOutputState blockChainInfo : blockChainInfoMap.values()) {

         AddressOutputState dbInfo = dbInfoMap.get(blockChainInfo.address);
         // Check if block chain confirmed outputs are present in the
         // database
         for (OutPoint p : blockChainInfo.confirmed) {
            if (!dbInfo.confirmed.contains(p)) {
               // Block chain has a confirmed output that the DB does not
               // have.
               // Even if we have it in receiving we need to fetch it as
               // we want to know its height.
               toFetch.add(p);
            }
         }

         // Check if block chain sending is up to date
         for (OutPoint p : blockChainInfo.sending) {
            if (!dbInfo.sending.contains(p)) {
               // block chain has a sending output that the DB does not
               // have
               toFetch.add(p);
            }
         }

      }
      return toFetch;
   }

   private List<OutPoint> findSourcedOutputsToFetch(Map<Address, AddressOutputState> blockChainInfoMap,
         Map<Address, AddressOutputState> dbInfoMap) {
      List<OutPoint> toFetch = new LinkedList<OutPoint>();
      for (AddressOutputState blockChainInfo : blockChainInfoMap.values()) {

         AddressOutputState dbInfo = dbInfoMap.get(blockChainInfo.address);
         // Check if block chain receiving is up to date
         for (OutPoint p : blockChainInfo.receiving) {
            if (!dbInfo.receiving.contains(p)) {
               // Block chain has a receiving output that the DB does not
               // have. Must be new, fetch it
               toFetch.add(p);
            }
         }

      }
      return toFetch;
   }

   private void reportBalanceChanged() {
      _eventBus.post(new BlockchainReady());
   }

   private void reportError(ApiError exception) {
      _eventBus.post(new BlockchainError(exception));
   }
}
