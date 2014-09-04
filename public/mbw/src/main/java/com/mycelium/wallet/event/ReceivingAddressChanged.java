package com.mycelium.wallet.event;

import com.mrd.bitlib.model.Address;

public class ReceivingAddressChanged {
   public final Address address;

   public ReceivingAddressChanged(Address address) {
      this.address = address;
   }
}
