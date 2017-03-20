package com.mrd.bitlib.model;


import com.mrd.bitlib.model.hdpath.HdKeyPath;

/**
 * Subclass for address to additional keep the info where this address is generated from
 *
 */
public class HdDerivedAddress extends Address {
   private final HdKeyPath path;

   public HdDerivedAddress(byte[] bytes, String stringAddress, HdKeyPath path) {
      super(bytes, stringAddress);
      this.path = path;
   }

   public HdDerivedAddress(Address address, HdKeyPath path) {
      super(address.getAllAddressBytes());
      this.path = path;
   }

   public HdKeyPath getBip32Path(){
      return path;
   }
}
