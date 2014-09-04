package com.mycelium.wallet.event;

import java.util.UUID;

public class SelectedAccountChanged {
   public final UUID account;

   public SelectedAccountChanged(UUID account) {
         this.account =   account;
   }
}