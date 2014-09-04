
package com.mycelium.wallet.event;

import java.util.UUID;

public class HdAccountCreated {
   public final UUID account;

   public HdAccountCreated(UUID account) {
      this.account = account;
   }
}
