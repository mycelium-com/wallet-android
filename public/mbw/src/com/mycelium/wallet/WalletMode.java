package com.mycelium.wallet;

import android.content.Context;

public enum WalletMode {
   Aggregated(0, R.string.aggregated), Segregated(1, R.string.segregated);

   int _mode;
   int _stringResource;

   private WalletMode(int mode, int stringResource) {
      _mode = mode;
   }

   public static WalletMode fromInteger(int integer) {
      for (WalletMode mode : WalletMode.values()) {
         if (mode._mode == integer) {
            return mode;
         }
      }
      return Aggregated;
   }

   public int asInteger() {
      return _mode;
   }

   public String getName(Context context) {
      return context.getResources().getString(_stringResource);
   }
}
