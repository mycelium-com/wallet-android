package com.mycelium.wallet.lt;

import com.mycelium.lt.location.Geocode;

public class AddressDescription {
   public Geocode location;

   public AddressDescription(Geocode address) {
      this.location = address;
   }

   @Override
   public String toString() {
      return location.formattedAddress;
   }
}
