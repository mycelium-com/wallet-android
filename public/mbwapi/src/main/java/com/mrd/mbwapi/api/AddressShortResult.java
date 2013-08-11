package com.mrd.mbwapi.api;

import com.google.common.collect.ImmutableMap;

import com.mrd.bitlib.model.Address;

public class AddressShortResult {

   public final ImmutableMap<AddressShort, Address> result;

   public AddressShortResult(ImmutableMap<AddressShort, Address> result) {
      this.result = result;
   }
}
