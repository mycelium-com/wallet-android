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

package com.mrd.mbwapi.async;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mrd.bitlib.crypto.PublicKeyRing;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.Sha256Hash;
import com.mrd.mbwapi.api.ApiError;
import com.mrd.mbwapi.api.ApiException;
import com.mrd.mbwapi.api.Balance;
import com.mrd.mbwapi.api.MyceliumWalletApi;
import com.mrd.mbwapi.api.BroadcastTransactionRequest;
import com.mrd.mbwapi.api.BroadcastTransactionResponse;
import com.mrd.mbwapi.api.QueryBalanceRequest;
import com.mrd.mbwapi.api.QueryBalanceResponse;
import com.mrd.mbwapi.api.QueryTransactionInventoryRequest;
import com.mrd.mbwapi.api.QueryTransactionInventoryResponse;
import com.mrd.mbwapi.api.QueryTransactionSummaryRequest;
import com.mrd.mbwapi.api.QueryTransactionSummaryResponse;
import com.mrd.mbwapi.api.QueryUnspentOutputsRequest;
import com.mrd.mbwapi.api.QueryUnspentOutputsResponse;
import com.mrd.mbwapi.api.TransactionSummary;

/**
 * This class is an asynchronous wrapper for the Bitcoin Client API. All the
 * public methods are non-blocking. Methods that return an AsyncTask are
 * executing one or more Bitcoin Client API functions in the background. For
 * each of those functions there is a corresponding interface with a call-back
 * function that the caller must implement. This function is called once the
 * AsyncTask has completed or failed.
 */
public abstract class AsynchronousApi {

   abstract protected CallbackRunnerInvoker createCallbackRunnerInvoker();

   abstract private class SynchronousFunctionCaller implements Runnable, AsyncTask {

      protected ApiError _error;
      private boolean _canceled;

      @Override
      public void cancel() {
         _canceled = true;
      }

      @Override
      public void run() {
         synchronized (_lock) {
            try {
               callFunction();
            } catch (ApiException e) {
               _error = new ApiError(e.errorCode, e.getMessage());
            } finally {
               if (_canceled) {
                  return;
               }
               callback();
            }
         }
      }

      abstract protected void callFunction() throws ApiException;

      abstract protected void callback();

   }

   private abstract class AbstractCaller<T> extends SynchronousFunctionCaller {

      private AbstractCallbackHandler<T> _callbackHandler;
      private CallbackRunnerInvoker _callbackInvoker;
      protected T _response;

      private AbstractCaller(AbstractCallbackHandler<T> callbackHandler) {
         _callbackHandler = callbackHandler;
         _callbackInvoker = createCallbackRunnerInvoker();
      }

      @Override
      protected abstract void callFunction() throws ApiException;

      protected void callback() {
         _callbackInvoker.invoke(new AbstractCallbackRunner<T>(_callbackHandler, _response, _error));
      }
   }

   private static class AbstractCallbackRunner<T> implements Runnable {
      private AbstractCallbackHandler<T> _callbackHandler;
      private T _response;
      private ApiError _error;

      private AbstractCallbackRunner(AbstractCallbackHandler<T> callbackHandler, T response, ApiError error) {
         _callbackHandler = callbackHandler;
         _response = response;
         _error = error;
      }

      @Override
      public void run() {
         _callbackHandler.handleCallback(_response, _error);
      }
   }

   private class QueryBalanceCaller extends AbstractCaller<QueryBalanceResponse> {

      private QueryBalanceCaller(AbstractCallbackHandler<QueryBalanceResponse> callbackHandler) {
         super(callbackHandler);
      }

      @Override
      protected void callFunction() throws ApiException {
         QueryBalanceRequest request = new QueryBalanceRequest(getBitcoinAddresses());
         _response = _api.queryBalance(request);
         _accountCache.cacheBalance(_keyRing.getAddresses(), _response.balance);
      }
   }

   private class QueryRecentTransactionsCaller extends AbstractCaller<QueryTransactionSummaryResponse> {

      private int _limit;

      private QueryRecentTransactionsCaller(int limit,
            AbstractCallbackHandler<QueryTransactionSummaryResponse> callbackHandler) {
         super(callbackHandler);
         this._limit = Math.min(limit, QueryTransactionInventoryRequest.MAXIMUM);
      }

      @Override
      protected void callFunction() throws ApiException {
         // Query the transaction inventory
         QueryTransactionInventoryRequest invRequest;
         invRequest = new QueryTransactionInventoryRequest(getBitcoinAddresses(), _limit);
         QueryTransactionInventoryResponse inv = _api.queryTransactionInventory(invRequest);

         // Build a map of all the transactions we already have and a list of
         // transactions to query
         Map<Sha256Hash, TransactionSummary> map = new HashMap<Sha256Hash, TransactionSummary>();
         List<Sha256Hash> toFetch = new LinkedList<Sha256Hash>();
         for (QueryTransactionInventoryResponse.Item item : inv.transactions) {
            if (_accountCache.hasTransactionSummary(item.hash)) {
               map.put(item.hash, _accountCache.getTransactionSummary(item.hash));
            } else {
               toFetch.add(item.hash);
            }
         }

         // Query the transactions we do not have
         if (toFetch.size() > 0) {
            QueryTransactionSummaryRequest request = new QueryTransactionSummaryRequest(toFetch);
            QueryTransactionSummaryResponse result = _api.queryTransactionSummary(request);
            for (TransactionSummary item : result.transactions) {
               // Put each transaction into our map and also in the cache
               map.put(item.hash, item);
               _accountCache.cacheTransactionSummary(item);
            }
         }

         // Build result from our map
         List<TransactionSummary> transactions = new ArrayList<TransactionSummary>(inv.transactions.size());
         for (QueryTransactionInventoryResponse.Item item : inv.transactions) {
            TransactionSummary s = map.get(item.hash);
            ;
            // Fix the height as the cached value is probably wrong by now
            s.height = item.height;
            transactions.add(s);
         }
         // Sort by height and date
         Collections.sort(transactions);
         _response = new QueryTransactionSummaryResponse(transactions, inv.chainHeight);
      }
   }

   private class QueryUnspentOutputsCaller extends AbstractCaller<QueryUnspentOutputsResponse> {

      private QueryUnspentOutputsCaller(AbstractCallbackHandler<QueryUnspentOutputsResponse> callbackHandler) {
         super(callbackHandler);
      }

      @Override
      protected void callFunction() throws ApiException {
         QueryUnspentOutputsRequest request = new QueryUnspentOutputsRequest(getBitcoinAddresses());
         _response = _api.queryUnspentOutputs(request);
      }
   }

   private class TransactionSubmitter extends AbstractCaller<BroadcastTransactionResponse> {
      private Transaction _transaction;

      private TransactionSubmitter(Transaction transaction,
            AbstractCallbackHandler<BroadcastTransactionResponse> callbackHandler) {
         super(callbackHandler);
         _transaction = transaction;
      }

      @Override
      protected void callFunction() throws ApiException {
         BroadcastTransactionRequest request = new BroadcastTransactionRequest(_transaction);
         _response = _api.broadcastTransaction(request);
      }
   }

   private PublicKeyRing _keyRing;
   private MyceliumWalletApi _api;
   private String _lock = "AccountManager.Runnable.lock";
   ApiCache _accountCache;

   /**
    * Create a new asynchronous API instance.
    * 
    * @param keyRing
    *           The key ring containing all Bitcoin public keys we operate on
    * @param api
    *           The MWAPI instance used for communicating with the MWAPI
    *           server.
    * @param accountCache
    *           The account cache instance used.
    */
   public AsynchronousApi(PublicKeyRing keyRing, MyceliumWalletApi api, ApiCache accountCache) {
      _keyRing = keyRing;
      _api = api;
      _accountCache = accountCache;
   }

   private synchronized void executeRequest(SynchronousFunctionCaller caller) {
      Thread thread = new Thread(caller);
      thread.start();
   }

   /**
    * Retrieve the balance in the background using the BitcoinClientApi and do a
    * callback to the callback handler once the function succeeds or fails.
    * 
    * @param callbackHandler
    *           The callback handler to call
    * @return an {@link AsyncTask} instance that allows the caller to cancel the
    *         callback.
    */
   public AsyncTask queryBalance(AbstractCallbackHandler<QueryBalanceResponse> callbackHandler) {
      QueryBalanceCaller caller = new QueryBalanceCaller(callbackHandler);
      executeRequest(caller);
      return caller;
   }

   /**
    * Retrieve the transaction summary of recent transactions in the background
    * using the BitcoinClientApi and do a callback to the callback handler once
    * the function succeeds or fails.
    * 
    * @param limit
    *           The maximum number of records to retrieve
    * @param callbackHandler
    *           The callback handler to call
    * @return an {@link AsyncTask} instance that allows the caller to cancel the
    *         callback.
    */
   public AsyncTask queryRecentTransactionSummary(int limit,
         AbstractCallbackHandler<QueryTransactionSummaryResponse> callbackHandler) {
      QueryRecentTransactionsCaller caller = new QueryRecentTransactionsCaller(limit, callbackHandler);
      executeRequest(caller);
      return caller;
   }

   /**
    * Retrieve the unspent outputs in the background using the BitcoinClientApi
    * and do a callback to the callback handler once the function succeeds or
    * fails.
    * 
    * @param callbackHandler
    *           The callback handler to call
    * @return an {@link AsyncTask} instance that allows the caller to cancel the
    *         callback.
    */
   public AsyncTask queryUnspentOutputs(AbstractCallbackHandler<QueryUnspentOutputsResponse> callbackHandler) {
      QueryUnspentOutputsCaller caller = new QueryUnspentOutputsCaller(callbackHandler);
      executeRequest(caller);
      return caller;
   }

   /**
    * Execute {@link BitcoinClientAPI.submitTransaction} in the background and
    * do a callback to
    * {@link TransactionSubmissionCallbackHandler.handleTransactionSubmission}
    * once the function succeeds or fails.
    * 
    * @param callbackHandler
    *           The callback handler to call
    * @return an {@link AsyncTask} instance that allows the caller to cancel the
    *         call back.
    */
   public AsyncTask broadcastTransaction(Transaction transaction,
         AbstractCallbackHandler<BroadcastTransactionResponse> callbackHandler) {
      TransactionSubmitter caller = new TransactionSubmitter(transaction, callbackHandler);
      executeRequest(caller);
      return caller;
   }

   /**
    * Get all Bitcoin addresses as a list.
    */
   public List<Address> getBitcoinAddresses() {
      return _keyRing.getAddresses();
   }

   /**
    * Get all Bitcoin addresses as a set.
    */
   public Set<Address> getBitcoinAddressSet() {
      return _keyRing.getAddressSet();
   }

   /**
    * Get the primary Bitcoin address. By default this is the first one in the
    * list.
    */
   public Address getPrimaryBitcoinAddress() {
      return _keyRing.getAddresses().iterator().next();
   }

   /**
    * Get the cached Bitcoin balance. The cache is automatically updated
    * whenever {@link queryBalance} is successfully called.
    */
   public Balance getCachedBalance() {
      return _accountCache.getBalance(_keyRing.getAddresses());
   }

   /**
    * Get the network used, test network or production network.
    * 
    * @return The network used, test network or production network.
    */
   public NetworkParameters getNetwork() {
      return _api.getNetwork();
   }

}
