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

package com.mycelium.wallet.api;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.squareup.otto.Bus;
import com.squareup.otto.Produce;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.OutPoint;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.Sha256Hash;
import com.mrd.mbwapi.api.*;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.api.ApiCache.TransactionInventory;
import com.mycelium.wallet.api.ApiCache.TransactionInventory.Item;
import com.mycelium.wallet.event.BlockchainError;
import com.mycelium.wallet.event.ExchangeRateError;
import com.mycelium.wallet.event.ExchangeRateUpdated;
import com.mycelium.wallet.event.SyncStarted;
import com.mycelium.wallet.event.SyncStopped;
import com.mycelium.wallet.event.TransactionHistoryReady;
import com.mycelium.wallet.event.WalletVersionEvent;


/**
 * This class is an asynchronous wrapper for the MPB Client API. All the public
 * methods are non-blocking. Methods that return an AsyncTask are executing one
 * or more MPB Client API functions in the background. For each of those
 * functions there is a corresponding interface with a call-back function that
 * the caller must implement. This function is called once the AsyncTask has
 * completed or failed.
 */
public abstract class AsynchronousApi {

   public static final String PROCESS_EXCHANGE_RATE = "ExchangeRate";
   public static final String PROCESS_TX_SUMMARY = "Transaction summary";
   public static final String PROCESS_UPDATECHECK = "update check";


   private final MyceliumWalletApi _api;
   private final ApiCache _cache;
   private final Bus eventBus;

   /**
    * Create a new asynchronous API instance.
    *
    * @param api   The MWAPI instance used for communicating with the MWAPI server.
    * @param cache The account cache instance used.
    */
   public AsynchronousApi(MyceliumWalletApi api, ApiCache cache, Bus eventBus) {
      _api = api;
      _cache = cache;
      this.eventBus = eventBus;
   }


   abstract protected CallbackRunnerInvoker createCallbackRunnerInvoker();


   public void getWalletVersion(final WalletVersionRequest versionRequest) {
      eventBus.post(new SyncStarted(PROCESS_UPDATECHECK));
      AbstractCallbackHandler<WalletVersionResponse> callback = new AbstractCallbackHandler<WalletVersionResponse>() {
         @Override
         public void handleCallback(WalletVersionResponse response, ApiError exception) {
            eventBus.post(new SyncStopped(PROCESS_UPDATECHECK));
            final WalletVersionEvent latestVersion;
            if (response == null) {
               latestVersion = new WalletVersionEvent();
            } else {
               latestVersion = new WalletVersionEvent(response);
            }
            eventBus.post(latestVersion);
         }
      };
      getWalletVersion(versionRequest,callback);
   }

   public void getWalletVersion(final WalletVersionRequest req, AbstractCallbackHandler<WalletVersionResponse> callback) {
      executeRequest(new AbstractCaller<WalletVersionResponse>(callback) {
         @Override
         protected void callFunction() throws ApiException {
            _response = _api.getVersionInfo(req);
         }
      });
   }

   abstract private class SynchronousFunctionCaller implements Runnable, AsyncTask {

      protected ApiError _error;
      private volatile boolean _canceled;

      @Override
      public void cancel() {
         _canceled = true;
      }

      @Override
      public void run() {
         try {
            callFunction();
         } catch (ApiException e) {
            _error = new ApiError(e.errorCode, e.getMessage());
         } finally {
            if (_canceled) {
               return; //todo fix please this will swallow OOME and other errors
            }
            callback();
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


   private synchronized void executeRequest(SynchronousFunctionCaller caller) {
      Thread thread = new Thread(caller);
      thread.start();
   }

   /**
    * Get a summary of the current exchange rates
    *
    * @return an {@link AsyncTask} instance that allows the caller to cancel the
    * call back.
    */
   public AsyncTask getExchangeSummary(final String currency) {
      eventBus.post(new SyncStarted(PROCESS_EXCHANGE_RATE));
      final AbstractCallbackHandler<ExchangeSummary[]> callback = new AbstractCallbackHandler<ExchangeSummary[]>() {
         @Override
         public void handleCallback(ExchangeSummary[] response, ApiError exception) {
            if (exception != null) {
               eventBus.post(new ExchangeRateError(exception));
            } else {
               eventBus.post(new ExchangeRateUpdated(response));
            }
            eventBus.post(new SyncStopped(PROCESS_EXCHANGE_RATE));
         }
      };

      AbstractCaller<ExchangeSummary[]> caller = new AbstractCaller<ExchangeSummary[]>(callback) {

         @Override
         protected void callFunction() throws ApiException {
            CurrencyCode code = CurrencyCode.fromShortString(currency);
            _response = _api.getRate(code);
         }

      };
      executeRequest(caller);
      return caller;
   }

   /**
    * Get the transaction history inventory of a list of bitcoin addresses.
    *
    * @return an {@link AsyncTask} instance that allows the caller to cancel the
    * call back.
    */
   public AsyncTask getTransactionSummary(final Collection<Address> addresses) {
      eventBus.post(new SyncStarted(PROCESS_TX_SUMMARY));
      AbstractCaller<QueryTransactionSummaryResponse> caller = new AbstractCaller<QueryTransactionSummaryResponse>(
            new AbstractCallbackHandler<QueryTransactionSummaryResponse>() {
               @Override
               public void handleCallback(QueryTransactionSummaryResponse response, ApiError exception) {
                  if (exception == null) {
                     eventBus.post(new TransactionHistoryReady(response));
                  } else {
                     eventBus.post(new BlockchainError(exception));
                  }
                  eventBus.post(new SyncStopped(PROCESS_TX_SUMMARY));
               }
            }) {

         @Override
         protected void callFunction() throws ApiException {
            QueryTransactionInventoryRequest request = new QueryTransactionInventoryRequest(new LinkedList<Address>(
                  addresses), Constants.TRANSACTION_HISTORY_LENGTH);

            // Get the inventory
            QueryTransactionInventoryExResponse inv = _api.queryTransactionInventoryEx(request);
            int chainHeight = inv.chainHeight;

            // Fetch what we can from the cache, the rest we get from the server
            List<TransactionSummary> txList = new LinkedList<TransactionSummary>();
            List<Sha256Hash> hashesToFetch = new LinkedList<Sha256Hash>();
            for (Map.Entry<Address, List<QueryTransactionInventoryExResponse.Item>> entry : inv.inventoryMap.entrySet()) {
               for (QueryTransactionInventoryExResponse.Item item : entry.getValue()) {
                  // Fetch from cache
                  TransactionSummary txSummary = _cache.getTransactionSummary(item.hash.toString());

                  if (txSummary == null || txSummary.height == -1 || chainHeight - txSummary.height <= 6) {
                     // Fetch transaction if we don't have it in our cache, if
                     // the
                     // cached version is unconfirmed, or if
                     // the height of the cached version is less than 6
                     // confirmations
                     hashesToFetch.add(item.hash);
                  } else {
                     txList.add(txSummary);
                  }
               }
            }

            // Fetch the rest from the server, if any
            if (hashesToFetch.size() > 0) {
               QueryTransactionSummaryResponse result = _api
                     .queryTransactionSummary(new QueryTransactionSummaryRequest(hashesToFetch));
               txList.addAll(result.transactions);

               // Note that chain height might have changed since the call above
               chainHeight = result.chainHeight;

               // Insert new transactions in cache
               for (TransactionSummary t : result.transactions) {
                  _cache.addTransactionSummary(t);
               }

            }

            // Insert inventory in cache
            for (Map.Entry<Address, List<QueryTransactionInventoryExResponse.Item>> entry : inv.inventoryMap.entrySet()) {
               _cache.setTransactionInventory(entry.getKey(), toInventory(entry.getValue(), inv.chainHeight));
            }

            // Sort result
            Collections.sort(txList);

            _response = new QueryTransactionSummaryResponse(txList, chainHeight);
         }

      };
      executeRequest(caller);
      return caller;
   }

   private TransactionInventory toInventory(List<QueryTransactionInventoryExResponse.Item> items, int chainHeight) {
      List<Item> translatedItems = new LinkedList<TransactionInventory.Item>();
      for (QueryTransactionInventoryExResponse.Item item : items) {
         translatedItems.add(new TransactionInventory.Item(item.hash, item.height));
      }
      return new TransactionInventory(translatedItems, chainHeight);
   }


   /**
    * Get the active output inventory of a set of addresses.
    *
    * @param callbackHandler The callback handler to call
    * @return an {@link AsyncTask} instance that allows the caller to cancel the
    * call back.
    */
   public AsyncTask getActiveOutputInventory(final Collection<Address> addresses,
                                             AbstractCallbackHandler<QueryAddressSetStatusResponse> callbackHandler) {
      AbstractCaller<QueryAddressSetStatusResponse> caller = new AbstractCaller<QueryAddressSetStatusResponse>(
            callbackHandler) {

         @Override
         protected void callFunction() throws ApiException {
            QueryAddressSetStatusRequest request = new QueryAddressSetStatusRequest(addresses);
            _response = _api.queryActiveOutputsInventory(request);
         }

      };
      executeRequest(caller);
      return caller;
   }

   /**
    * Get a list of outputs and transactions for a list of outpoints and
    * transaction IDs.
    *
    * @param callbackHandler The callback handler to call
    * @return an {@link AsyncTask} instance that allows the caller to cancel the
    * call back.
    */
   public AsyncTask getTransactionData(final List<OutPoint> outputsToGet, final List<OutPoint> sourcedOutputsToGet,
                                       final List<Sha256Hash> txIds, AbstractCallbackHandler<GetTransactionDataResponse> callbackHandler) {
      AbstractCaller<GetTransactionDataResponse> caller = new AbstractCaller<GetTransactionDataResponse>(
            callbackHandler) {

         @Override
         protected void callFunction() throws ApiException {
            GetTransactionDataRequest request = new GetTransactionDataRequest(outputsToGet, sourcedOutputsToGet, txIds);
            _response = _api.getTransactionData(request);
         }

      };
      executeRequest(caller);
      return caller;
   }

   /**
    * Broadcast a transaction
    *
    * @param callbackHandler The callback handler to call
    * @return an {@link AsyncTask} instance that allows the caller to cancel the
    * call back.
    */
   public AsyncTask broadcastTransaction(final Transaction transaction,
                                         AbstractCallbackHandler<BroadcastTransactionResponse> callbackHandler) {
      AbstractCaller<BroadcastTransactionResponse> caller = new AbstractCaller<BroadcastTransactionResponse>(
            callbackHandler) {

         @Override
         protected void callFunction() throws ApiException {
            BroadcastTransactionRequest request = new BroadcastTransactionRequest(transaction);
            _response = _api.broadcastTransaction(request);
         }

      };
      executeRequest(caller);
      return caller;
   }

}
