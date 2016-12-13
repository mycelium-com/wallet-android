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

package com.mycelium.wallet.activity.util;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.hdpath.Bip44CoinType;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mycelium.wapi.wallet.AccountScanManager;
import com.mycelium.wapi.wallet.WalletManager;
import com.satoshilabs.trezor.protobuf.TrezorType;
import com.squareup.otto.Bus;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class AbstractAccountScanManager implements AccountScanManager {
   protected final Context context;
   private final NetworkParameters network;
   private AsyncTask<Void, ScanStatus, Void> scanAsyncTask = null;
   private final ArrayList<HdKeyNodeWrapper> foundAccounts = new ArrayList<HdKeyNodeWrapper>();
   protected final Bus eventBus;
   protected final LinkedBlockingQueue<Optional<String>> passphraseSyncQueue = new LinkedBlockingQueue<Optional<String>>(1);
   protected final Handler mainThreadHandler;

   public volatile AccountStatus currentAccountState = AccountStatus.unknown;
   public volatile Status currentState = Status.unableToScan;
   private volatile Optional<HdKeyNode> nextUnusedAccount = Optional.absent();

   public AbstractAccountScanManager(Context context, NetworkParameters network, Bus eventBus) {
      this.context = context;
      this.eventBus = eventBus;
      this.network = network;

      mainThreadHandler = new Handler(Looper.getMainLooper());
   }


   public class ScanStatus {
      public final Status state;
      public final AccountStatus accountState;

      public ScanStatus(Status state, AccountStatus accountState) {
         this.state = state;
         this.accountState = accountState;
      }
   }

   public class FoundAccountStatus extends ScanStatus {
      public final HdKeyNodeWrapper account;

      public FoundAccountStatus(HdKeyNodeWrapper account) {
         super(Status.readyToScan, AccountStatus.scanning);
         this.account = account;
      }
   }

   protected abstract boolean onBeforeScan();

   @Override
   public void startBackgroundAccountScan(final AccountCallback scanningCallback) {
      if (currentAccountState == AccountStatus.scanning || currentAccountState == AccountStatus.done) {
         // currently scanning or have already all account - just post the events for all already known accounts
         for (HdKeyNodeWrapper a : foundAccounts) {
            eventBus.post(new OnAccountFound(a));
         }
      } else {
         // start a background task which iterates over all accounts and calls the callback
         // to check if there was activity on it
         scanAsyncTask = new AsyncTask<Void, ScanStatus, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
               publishProgress(new ScanStatus(AccountScanManager.Status.initializing, AccountStatus.unknown));
               if (onBeforeScan()) {
                  publishProgress(new ScanStatus(AccountScanManager.Status.readyToScan, AccountStatus.scanning));
               } else {
                  return null;
               }

               // scan through the accounts, to find the first unused one
               Optional<HdKeyPath> lastScannedPath = Optional.absent();
               Optional<HdKeyNode> lastAccountPubKeyNode = Optional.absent();
               boolean wasUsed = false;
               do {
                  HdKeyNode rootNode;
                  Optional<? extends HdKeyPath> accountPathToScan =
                        AbstractAccountScanManager.this.getAccountPathToScan(lastScannedPath, wasUsed);

                  // we have scanned all accounts - get out of here...
                  if (!accountPathToScan.isPresent()) {
                     // remember the last xPub key as the next-unused one
                     nextUnusedAccount = lastAccountPubKeyNode;
                     break;
                  }

                  Optional<HdKeyNode> accountPubKeyNode =
                        AbstractAccountScanManager.this.getAccountPubKeyNode(accountPathToScan.get());
                  lastAccountPubKeyNode = accountPubKeyNode;



                  // unable to retrieve the account (eg. device unplugged) - cancel scan
                  if (!accountPubKeyNode.isPresent()) {
                     publishProgress(new ScanStatus(AccountScanManager.Status.initializing, AccountStatus.unknown));
                     break;
                  }

                  rootNode = accountPubKeyNode.get();

                  // leave accountID empty for now - set it later if it is a already used account
                  HdKeyNodeWrapper acc = new HdKeyNodeWrapper(accountPathToScan.get(), rootNode, null);
                  UUID newAccount = scanningCallback.checkForTransactions(acc);
                  lastScannedPath = Optional.of(accountPathToScan.get());

                  if (newAccount != null) {
                     HdKeyNodeWrapper foundAccount =
                           new HdKeyNodeWrapper(accountPathToScan.get(), rootNode, newAccount);

                     publishProgress(new FoundAccountStatus(foundAccount));
                     wasUsed = true;
                  } else {
                     wasUsed = false;
                  }
               } while (!isCancelled());
               publishProgress(new ScanStatus(AccountScanManager.Status.readyToScan, AccountStatus.done));
               return null;
            }


            @Override
            protected void onPreExecute() {
               super.onPreExecute();
               currentState = AccountScanManager.Status.initializing;
            }

            @Override
            protected void onProgressUpdate(ScanStatus... stateInfo) {
               for (ScanStatus si : stateInfo) {
                  setState(si.state, si.accountState);

                  if (si instanceof FoundAccountStatus) {
                     HdKeyNodeWrapper foundAccount = ((FoundAccountStatus) si).account;
                     eventBus.post(new OnAccountFound(foundAccount));
                     foundAccounts.add(foundAccount);
                  }
               }
            }

         };

         scanAsyncTask.execute();
      }
   }

   protected synchronized void setState(final Status state, final AccountStatus accountState) {
      mainThreadHandler.post(new Runnable() {
         @Override
         public void run() {
            eventBus.post(new OnStatusChanged(state, accountState));
         }
      });
      currentState = state;
      currentAccountState = accountState;
   }

   @Override
   public void stopBackgroundAccountScan() {
      if (scanAsyncTask != null) {
         scanAsyncTask.cancel(true);
         currentAccountState = AccountStatus.unknown;
      }
   }

   @Override
   public Optional<HdKeyNode> getNextUnusedAccount(){
      return nextUnusedAccount;
   }


   @Override
   public void forgetAccounts() {
      if (currentAccountState == AccountStatus.scanning) {
         stopBackgroundAccountScan();
      }
      currentAccountState = AccountStatus.unknown;
      foundAccounts.clear();
   }

   protected Optional<String> waitForPassphrase() {
      // call external passphrase request ...
      mainThreadHandler.post(new Runnable() {
         @Override
         public void run() {
            eventBus.post(new OnPassphraseRequest());
         }
      });

      // ... and block until we get one
      while (true) {
         try {
            return passphraseSyncQueue.take();
         } catch (InterruptedException ignore) {
         }
      }
   }

   protected NetworkParameters getNetwork() {
      return network;
   }

   protected boolean postErrorMessage(final String msg) {
      mainThreadHandler.post(new Runnable() {
         @Override
         public void run() {
            eventBus.post(new OnScanError(msg));
         }
      });
      return true;
   }

   protected boolean postErrorMessage(final String msg, final TrezorType.FailureType failureType) {
      mainThreadHandler.post(new Runnable() {
         @Override
         public void run() {
            // need to map to the known error types, because wapi does not import the trezor lib
            if (failureType == TrezorType.FailureType.Failure_NotInitialized){
               eventBus.post(new OnScanError(msg, OnScanError.ErrorType.NOT_INITIALIZED));
            } else {
               eventBus.post(new OnScanError(msg));
            }
         }
      });
      return true;
   }

   @Override
   public void setPassphrase(String passphrase) {
      passphraseSyncQueue.add(Optional.fromNullable(passphrase));
   }

   abstract public UUID createOnTheFlyAccount(HdKeyNode accountRoot, WalletManager walletManager, int accountIndex);

   // returns the next Bip44 account based on the last scanned account
   @Override
   public Optional<? extends HdKeyPath> getAccountPathToScan(Optional<? extends HdKeyPath> lastPath, boolean wasUsed){
      Bip44CoinType bip44CoinType = HdKeyPath.BIP44.getBip44CoinType(getNetwork());

      // this is the first call - no lastPath given
      if (!lastPath.isPresent()) {
         return Optional.of(bip44CoinType.getAccount(0));
      }

      // otherwise use the next bip44 account, as long as the last one had activity on it
      HdKeyPath last = lastPath.get();
      if (wasUsed){
         return Optional.of(bip44CoinType.getAccount(last.getLastIndex() + 1));
      }

      // if we are already at the bip44 branch and the last account had no activity, then we are done
      return Optional.absent();
   }
}
