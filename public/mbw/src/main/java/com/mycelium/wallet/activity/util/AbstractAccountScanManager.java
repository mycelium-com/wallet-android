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
import com.mrd.bitlib.model.*;
import com.mycelium.wapi.wallet.AccountScanManager;
import com.mycelium.wapi.wallet.WalletManager;

import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class AbstractAccountScanManager implements AccountScanManager {
   protected final Context _context;
   final private NetworkParameters network;
   private Events handler=null;
   private AsyncTask<Void, ScanStatus, Integer> scanAsyncTask = null;
   private TreeMap<Integer, HdKeyNodeWrapper> foundAccounts = new TreeMap<Integer, HdKeyNodeWrapper>();
   protected LinkedBlockingQueue<Optional<String>> passphraseSyncQueue = new LinkedBlockingQueue<Optional<String>>(1);
   protected Handler mainThreadHandler;

   public volatile AccountStatus currentAccountState = AccountStatus.unknown;
   public volatile Status currentState = Status.unableToScan;

   public AbstractAccountScanManager(Context context, NetworkParameters network){
      _context = context;
      mainThreadHandler = new Handler(Looper.getMainLooper());

      this.network = network;
   }

   public class ScanStatus {
      public final Status state;
      public final AccountStatus accountState;

      public ScanStatus(Status state, AccountStatus accountState){
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

   @Override
   public void setEventHandler(Events handler){
      this.handler = handler;
   }


   protected abstract boolean onBeforeScan();

   @Override
   public void startBackgroundAccountScan(final AccountCallback scanningCallback) {
      if (currentAccountState == AccountStatus.scanning || currentAccountState == AccountStatus.done) {
         // currently scanning or have already all account - just call the callback for all already known accounts
         if (AbstractAccountScanManager.this.handler != null){
            for (HdKeyNodeWrapper a : foundAccounts.values()){
               AbstractAccountScanManager.this.handler.onAccountFound(a);
            }
         }
      }else {
         // start a background task which iterates over all accounts and calls the callback
         // to check if there was activity on it
         scanAsyncTask = new AsyncTask<Void, ScanStatus, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
               publishProgress(new ScanStatus(AccountScanManager.Status.initializing, AccountStatus.unknown));
               if (onBeforeScan()) {
                  publishProgress(new ScanStatus(AccountScanManager.Status.readyToScan, AccountStatus.scanning));
               } else {
                  return 0;
               }

               // scan through the accounts, to find the first unused one
               int accountIndex = 0;
               do {
                  HdKeyNode rootNode;
                  Optional<HdKeyNode> accountPubKeyNode = AbstractAccountScanManager.this.getAccountPubKeyNode(accountIndex);
                  if (!accountPubKeyNode.isPresent()){
                     publishProgress(new ScanStatus(AccountScanManager.Status.initializing, AccountStatus.unknown));
                     break;
                  }

                  rootNode = accountPubKeyNode.get();

                  // leave accountID empty for now - set it later if it is a already used account
                  HdKeyNodeWrapper acc = new HdKeyNodeWrapper(accountIndex, rootNode, null);
                  UUID newAccount = scanningCallback.checkForTransactions(acc);
                  if (newAccount != null) {
                     HdKeyNodeWrapper foundAccount = new HdKeyNodeWrapper(accountIndex, rootNode, newAccount);
                     publishProgress(new FoundAccountStatus(foundAccount));
                  } else {
                     publishProgress(new ScanStatus(AccountScanManager.Status.initializing, AccountStatus.unknown));
                     break;
                  }
                  accountIndex++;
               } while (!isCancelled());
               publishProgress(new ScanStatus(AccountScanManager.Status.readyToScan, AccountStatus.done));

               return accountIndex;
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
                     if (AbstractAccountScanManager.this.handler != null) AbstractAccountScanManager.this.handler.onAccountFound(foundAccount);
                     foundAccounts.put(foundAccount.accountIndex, foundAccount);
                  }
               }
            }

            @Override
            protected void onCancelled() {
               super.onCancelled();
            }

            @Override
            protected void onCancelled(Integer integer) {
               super.onCancelled(integer);
            }
         };

         scanAsyncTask.execute();
      }
   }

   protected synchronized void setState(final Status state, final AccountStatus accountState) {
      if (this.handler != null) {
         mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
               AbstractAccountScanManager.this.handler.onStatusChanged(state, accountState);
            }
         });
      }
      currentState = state;
      currentAccountState = accountState;
   }

   @Override
   public void stopBackgroundAccountScan() {
      if (scanAsyncTask != null){
         scanAsyncTask.cancel(true);
         currentAccountState = AccountStatus.unknown;
      }
   }

   @Override
   public void forgetAccounts(){
      if (currentAccountState == AccountStatus.scanning ){
         stopBackgroundAccountScan();
      }
      currentAccountState = AccountStatus.unknown;
      foundAccounts.clear();
   }

   protected Optional<String> waitForPassphrase(){
      // call external passphrase request ...
      this.handler.onPassphraseRequest();

      Optional<String> passphrase;
      // ... and block until we get one
      while (true) {
         try {
            passphrase = passphraseSyncQueue.take();
            break;
         } catch (InterruptedException ignore) {
         }
      }
      return passphrase;
   }

   protected NetworkParameters getNetwork(){
      return network;
   }

   protected boolean postErrorMessage(final String msg){
      if (handler != null) {
         mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
               handler.onScanError(msg);
            }
         });
         return  true;
      }
      return false;
   }

   @Override
   public void setPassphrase(String passphrase){
      passphraseSyncQueue.add(Optional.fromNullable(passphrase));
   }

   abstract public UUID createOnTheFlyAccount(HdKeyNode accountRoot, WalletManager walletManager, int accountIndex);

}
