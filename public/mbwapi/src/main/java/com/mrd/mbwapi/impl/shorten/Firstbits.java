package com.mrd.mbwapi.impl.shorten;

import com.google.common.base.Optional;

import com.mrd.bitlib.model.Address;
import com.mrd.mbwapi.api.AddressShort;

public class Firstbits implements AddressShort {
   @Override
   public Optional<Address> query(String input) {
      return Optional.absent();
   }
}
