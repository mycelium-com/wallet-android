package com.mycelium.wapi.wallet;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.mrd.bitlib.model.Address;

public class SyncMode {
   // sync all accounts, all known addresses
   public final static SyncMode FULL_SYNC_ALL_ACCOUNTS = new SyncMode(Mode.FULL_SYNC, false, false, false, false);
   // full sync current account, all known addresses
   public final static SyncMode FULL_SYNC_CURRENT_ACCOUNT_FORCED = new SyncMode(Mode.FULL_SYNC, false, true, false, true);
   // onlyActiveAccount
   public final static SyncMode NORMAL = new SyncMode(Mode.NORMAL_SYNC, false, true, false, false);
   // onlyActiveAccount, ignoreSyncInterval
   public final static SyncMode NORMAL_FORCED = new SyncMode(Mode.NORMAL_SYNC, false, true, false, true);
   // onlyActiveAccount, ignoreSyncInterval
   public final static SyncMode NORMAL_ALL_ACCOUNTS_FORCED = new SyncMode(Mode.NORMAL_SYNC, false, false, false, true);
   // ignoreTransactionHistory, onlyActiveAccount
   public final static SyncMode NORMAL_WITHOUT_TX_LOOKUP = new SyncMode(Mode.NORMAL_SYNC, true, true, false, false);
   // fast sync, check for incoming tx
   public final static SyncMode FAST_SYNC_CURRENT_ACCOUNT = new SyncMode(Mode.FAST_SYNC, true, true, true, false);

   public final Mode mode;

   // should the transaction history be synchronized
   // This is useful if the wallet manager is used for cold storage spending where the transaction history is
   // uninteresting. Disabling transaction history synchronization makes synchronization faster especially if the
   // address has been used a lot.
   public final boolean ignoreTransactionHistory;

   // Only synchronize the current selected account
   public final boolean onlyActiveAccount;

   // Dont try to fetch current miner fee
   public final boolean ignoreMinerFeeFetch;

   // if mode is ONE_ADDRESS, only care about that address
   public final Address addressToSync;

   // Ignores timeouts
   // If true, for each SyncMode the last time is noted, and this sync is only
   // executed, if the time span since last sync is greater than the MIN_TIME_BETWEEN_SYNC[mode]
   // (per account)
   public final boolean ignoreSyncInterval;

   SyncMode(Mode mode, boolean ignoreTransactionHistory, boolean onlyActiveAccount, boolean ignoreMinerFeeFetch, boolean ignoreSyncInterval) {
      this.mode = mode;
      this.ignoreTransactionHistory = ignoreTransactionHistory;
      this.onlyActiveAccount = onlyActiveAccount;
      this.ignoreMinerFeeFetch = ignoreMinerFeeFetch;
      this.ignoreSyncInterval = ignoreSyncInterval;
      this.addressToSync = null;
   }

   public SyncMode(Address onlyAddress) {
      this.mode = Mode.ONE_ADDRESS;
      this.ignoreTransactionHistory = true;
      this.onlyActiveAccount = true;
      this.ignoreMinerFeeFetch = true;
      this.ignoreSyncInterval = false;
      this.addressToSync = onlyAddress;
   }

   public enum Mode {
      // Synchronizes all addresses (all old, external/internal and lookahead)
      FULL_SYNC(Integer.MAX_VALUE, 20),

      // Synchronizes the most recent addresses and lookahead
      NORMAL_SYNC(20, 20),

      // Synchronizes only the current address (external) - only short lookahead
      FAST_SYNC(0, 3),

      // Synchronizes only the current address (external) - only short lookahead
      ONE_ADDRESS(0, 0);

      public final int lookAhead;
      public final int lookBack;

      Mode(int lookBack, int lookAhead) {
         this.lookBack = lookBack;
         this.lookAhead = lookAhead;
      }
   }

   @Override
   public String toString() {
      final String modeDesc = Joiner.on(", ").skipNulls().
            join(mode, Strings.emptyToNull(
                  Joiner.on(" ").skipNulls().join(
                        ignoreTransactionHistory ? "ignoreTransactionHistory" : null,
                        onlyActiveAccount ? "onlyActiveAccount" : null,
                        ignoreMinerFeeFetch ? "ignoreMinerFeeFetch" : null,
                        ignoreSyncInterval ? "ignoreSyncInterval" : null,
                        addressToSync
                  )
            ));
      return "SyncMode{" + modeDesc + '}';
   }
}
