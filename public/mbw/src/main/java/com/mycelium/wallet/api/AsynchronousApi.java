/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
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

import com.mrd.mbwapi.api.*;
import com.mycelium.wallet.event.SyncStarted;
import com.mycelium.wallet.event.SyncStopped;
import com.mycelium.wallet.event.WalletVersionEvent;
import com.squareup.otto.Bus;


/**
 * This class is an asynchronous wrapper for the MPB Client API. All the public
 * methods are non-blocking. Methods that return an AsyncTask are executing one
 * or more MPB Client API functions in the background. For each of those
 * functions there is a corresponding interface with a call-back function that
 * the caller must implement. This function is called once the AsyncTask has
 * completed or failed.
 */
public abstract class AsynchronousApi {


   private final MyceliumWalletApi _api;
   private final Bus eventBus;

   /**
    * Create a new asynchronous API instance.
    *
    * @param api The MWAPI instance used for communicating with the MWAPI server.
    */
   public AsynchronousApi(MyceliumWalletApi api, Bus eventBus) {
      _api = api;
      this.eventBus = eventBus;
   }


   abstract protected CallbackRunnerInvoker createCallbackRunnerInvoker();


   public void getWalletVersion(final WalletVersionRequest versionRequest) {
      AbstractCallbackHandler<WalletVersionResponse> callback = new AbstractCallbackHandler<WalletVersionResponse>() {
         @Override
         public void handleCallback(WalletVersionResponse response, ApiError exception) {
            final WalletVersionEvent latestVersion;
            if (response == null) {
               latestVersion = new WalletVersionEvent();
            } else {
               latestVersion = new WalletVersionEvent(response);
            }
            eventBus.post(latestVersion);
         }
      };
      getWalletVersion(versionRequest, callback);
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

}
