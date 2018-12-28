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

package com.mycelium.wallet.event;

import android.os.Handler;
import com.google.common.base.Optional;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.ExchangeRateManager;
import com.mycelium.wapi.wallet.WalletManager;
import com.squareup.otto.Bus;

import java.util.UUID;

/**
 * This Observer takes WAPI Events and translates into posts to EventBus
 */
public class EventTranslator implements WalletManager.Observer, ExchangeRateManager.Observer {
   private Handler handler;
   private Bus bus;

   public EventTranslator(Handler handler, Bus bus) {
      this.handler = handler;
      this.bus = bus;
   }

   //post events from UI thread
   private void postEvent(final Object event) {
      handler.post(new Runnable() {
         @Override
         public void run() {
            bus.post(event);
         }
      });
   }

   @Override
   public void onWalletStateChanged(WalletManager wallet, WalletManager.State state) {
      //based on state fire sync started and sync stopped?
      if (state == WalletManager.State.READY) {
         postEvent(new SyncStopped());
      } else if (state == WalletManager.State.SYNCHRONIZING) {
         postEvent(new SyncStarted());
      }
   }

   @Override
   public void onAccountEvent(WalletManager wallet, UUID accountId, WalletManager.Event event) {
      switch (event) {
         case SERVER_CONNECTION_ERROR:
            postEvent(new SyncFailed());
            break;
         case BROADCASTED_TRANSACTION_ACCEPTED:
            postEvent(new TransactionBroadcasted());
            break;
         case BROADCASTED_TRANSACTION_DENIED:
            //One of the transactions was rejected by the network
            break;
         case BALANCE_CHANGED:
            postEvent(new BalanceChanged(accountId));
            break;
         case TRANSACTION_HISTORY_CHANGED:
            //Transaction history changed
            break;
         case RECEIVING_ADDRESS_CHANGED:
            Optional<Address> receivingAddress = wallet.getAccount(accountId).getReceivingAddress();
            postEvent(new ReceivingAddressChanged(receivingAddress));
            break;
         case SYNC_PROGRESS_UPDATED:
            postEvent(new SyncProgressUpdated(accountId));
            break;
         case MALFORMED_OUTGOING_TRANSACTIONS_FOUND:
            postEvent(new MalformedOutgoingTransactionsFound(accountId));
         default:
            //unknown event
      }
   }

   @Override
   public void refreshingExchangeRatesSucceeded() {
      postEvent(new ExchangeRatesRefreshed());
   }

   @Override
   public void refreshingExchangeRatesFailed() {
      postEvent(new RefreshingExchangeRatesFailed());
   }

   @Override
   public void exchangeSourceChanged() {
      postEvent(new ExchangeSourceChanged());
   }
}
