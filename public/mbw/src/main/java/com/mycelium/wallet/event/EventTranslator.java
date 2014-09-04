package com.mycelium.wallet.event;

//TODO: which events to we need to translate?

import android.os.Handler;
import com.mycelium.wapi.wallet.WalletManager;
import com.squareup.otto.Bus;

import java.util.UUID;

/**
 * This Observer takes WAPI Events and translates into posts to EventBus
 */
public class EventTranslator implements WalletManager.Observer {
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
      //do we need to fire events? update spinning refresh button etc?
      //based on state fire sync started and sync stopped?
      if (state == WalletManager.State.READY) {
         postEvent(new SyncStopped(SyncStopped.WALLET_MANAGER_SYNC_READY));
      } else if (state == WalletManager.State.SYNCHRONIZING) {
         postEvent((new SyncStarted(SyncStarted.WALLET_MANAGER_SYNC_STARTED)));
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
            postEvent(new ReceivingAddressChanged(wallet.getAccount(accountId).getReceivingAddress()));
            break;
         default:
            //unknown event - do we want to fail?
      }
   }
}
