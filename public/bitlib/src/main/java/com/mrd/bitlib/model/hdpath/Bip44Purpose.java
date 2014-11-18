package com.mrd.bitlib.model.hdpath;

import com.google.common.primitives.UnsignedInteger;
import com.mrd.bitlib.model.NetworkParameters;


public class Bip44Purpose extends HdKeyPath {
   public Bip44Purpose(HdKeyPath parent, UnsignedInteger index, boolean hardened) {
      super(parent, index, hardened);
   }

   public Bip44CoinType getCoinTypeBitcoin(){
      return  new Bip44CoinType(this, UnsignedInteger.valueOf(0), true);
   }

   public Bip44CoinType getCoinTypeBitcoinTestnet(){
      return  new Bip44CoinType(this, UnsignedInteger.valueOf(1), true);
   }

   public Bip44CoinType getCoinTypeBitcoin(boolean testnet){
      if (testnet){
         return getCoinTypeBitcoinTestnet();
      }else{
         return getCoinTypeBitcoin();
      }
   }

   public Bip44CoinType getBip44CoinType(NetworkParameters forNetwork){
      return getCoinTypeBitcoin(forNetwork.isTestnet());
   }

   @Override
   protected HdKeyPath knownChildFactory(UnsignedInteger index, boolean hardened) {
      if (hardened) {
         return new Bip44CoinType(this, index, true);
      } else {
         return new HdKeyPath(this, index, hardened);
      }
   }
}
