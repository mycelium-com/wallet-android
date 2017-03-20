package com.mrd.bitlib.model.hdpath;

import com.google.common.base.Optional;
import com.google.common.primitives.UnsignedInteger;
import com.mrd.bitlib.model.NetworkParameters;

import java.io.Serializable;

public class Bip44CoinType extends Bip44Purpose  {
   public Bip44CoinType(Bip44Purpose parent, UnsignedInteger index, boolean hardened) {
      super(parent, index, hardened);
   }

   public Bip44Account getAccount(UnsignedInteger id){
      return new Bip44Account(this, id, true);
   }
   public Bip44Account getAccount(int id){
      return new Bip44Account(this, UnsignedInteger.valueOf(id), true);
   }


   public boolean isTestnet(){
      Optional<Bip44CoinType> coinType = findPartOf(Bip44CoinType.class);
      if (coinType.isPresent()) {
         return coinType.get().index.intValue() == 1;
      }else{
         throw new RuntimeException("No cointype present");
      }
   }

   public NetworkParameters getNetworkparameter(){
      if (isTestnet()) {
         return NetworkParameters.testNetwork;
      }else{
         return NetworkParameters.productionNetwork;
      }
   }

   @Override
   protected HdKeyPath knownChildFactory(UnsignedInteger index, boolean hardened) {
      if (hardened){
         return  new Bip44Account(this, index, true);
      }else{
         return new HdKeyPath(this, index, hardened);
      }
   }
}
