package com.mycelium.wallet.event;

import java.util.UUID;

public class BalanceChanged {

    public final UUID account;

   public BalanceChanged(UUID account) {
      this.account = account;
   }
}
