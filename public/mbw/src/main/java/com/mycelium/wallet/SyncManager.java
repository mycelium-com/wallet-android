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

import android.util.Log;
import com.google.common.collect.Sets;
import com.mycelium.wallet.api.AndroidAsyncApi;
import com.mycelium.wallet.event.*;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.Set;

public class SyncManager {

   private final Bus eventBus;
   private final MbwManager _mbwManager;
   private final AndroidAsyncApi asyncApi;
   private final RecordManager recordManager;
   private final BlockChainAddressTracker tracker;

   private BalanceInfo balanceBeforeRefresh;


   Set<String> runningProcesses = Sets.newHashSet();
    private VersionManager versionManager;

    public SyncManager(Bus eventBus, MbwManager _mbwManager, AndroidAsyncApi asyncApi, RecordManager recordManager, BlockChainAddressTracker tracker, VersionManager versionManager) {
      this.eventBus = eventBus;
      this._mbwManager = _mbwManager;
      this.asyncApi = asyncApi;
      this.recordManager = recordManager;
      this.tracker = tracker;
      this.versionManager = versionManager;
      eventBus.register(this); //does not need unregister,
      // since it is a singleton and will be killed with the whole VM
   }

   public void triggerUpdate() {
      asyncApi.getExchangeSummary(_mbwManager.getFiatCurrency());
      Wallet wallet = getWallet();
      balanceBeforeRefresh = wallet.getLocalBalance(tracker);
      //request balance update
      wallet.requestUpdate(tracker);
      versionManager.checkForUpdate();
   }

   //for sync purposes, we select a different wallet than normally, ignoring segregated mode setting.
   private Wallet getWallet() {
      final Record selectedRecord = recordManager.getSelectedRecord();
      if (selectedRecord.tag == Record.Tag.ARCHIVE) {
         return new Wallet(selectedRecord);
      } else {
         return new Wallet(recordManager.getRecords(Record.Tag.ACTIVE), selectedRecord);
      }
   }

   @Subscribe
   public void balanceChanged(BlockchainReady blockchainReady) {
      final BalanceInfo balanceAfterRefresh = getWallet().getLocalBalance(_mbwManager.getBlockChainAddressTracker());
      if (!balanceAfterRefresh.equals(balanceBeforeRefresh)) {
         //refresh history
         _mbwManager.getAsyncApi().getTransactionSummary(getWallet().getAddressSet());
      }
   }

   @Subscribe
   public void transactionsUpdated(TransactionHistoryReady transactionHistoryReady) {
      //interesting, but it seems we are done already
   }

   @Subscribe
   public void updateStarted(SyncStarted syncStarted) {
      Log.i(Constants.TAG, "started process: " + syncStarted.process);
      if (runningProcesses.isEmpty()) {
         eventBus.post(new RefreshStatus(true));
      }
      runningProcesses.add(syncStarted.process);
   }

   @Subscribe
   public void updateStopped(SyncStopped syncStopped) {
      Log.i(Constants.TAG, "stopped process: " + syncStopped.process);
      runningProcesses.remove(syncStopped.process);
      if (runningProcesses.isEmpty()) {
         eventBus.post(new RefreshStatus(false));
      }
   }


   public RefreshStatus currentStatus() {
      return new RefreshStatus(!runningProcesses.isEmpty());
   }
}
