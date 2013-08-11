package com.mrd.mbwapi.api;

import com.google.common.base.Optional;

import com.mrd.bitlib.model.Address;

public interface AddressShort {

   int TIMEOUT = 2000;

   Optional<Address> query(String input);
}
