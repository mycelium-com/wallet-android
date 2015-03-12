package com.mrd.bitlib.model;


public class HdDerivedAddress extends Address {
   private byte[] _bip32Path = null;

   public HdDerivedAddress(byte[] bytes, String stringAddress, byte[] bip32Path) {
      super(bytes, stringAddress);
      _bip32Path = bip32Path;
   }

   public HdDerivedAddress(byte[] bytes, byte[] bip32Path) {
      super(bytes);
      _bip32Path = bip32Path;
   }

   public HdDerivedAddress(Address address, byte[] bip32Path) {
      super(address.getAllAddressBytes());
      _bip32Path = bip32Path;
   }

   public void setBip32Path(byte[] Path){
      _bip32Path = Path;
   }

   public byte[] getBip32Path(){
      return _bip32Path;
   }
}
