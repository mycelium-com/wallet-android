package com.mrd.bitlib.model.hdpath;

import com.google.common.base.Optional;
import com.google.common.primitives.UnsignedInteger;


public class Bip44Chain extends Bip44Account {

   public Bip44Chain(Bip44Account parent, UnsignedInteger index, boolean hardened) {
      super(parent, index, hardened);
   }

   public Bip44Address getAddress(UnsignedInteger index){
      return new Bip44Address(this, index, false);
   }
   public Bip44Address getAddress(int index){
      return new Bip44Address(this, UnsignedInteger.valueOf(index), false);
   }

   public boolean isExternal(){
      Optional<Bip44Chain> chainType = findPartOf(Bip44Chain.class);
      if (chainType.isPresent()) {
         return chainType.get().index.intValue() == 0;
      }else{
         throw new RuntimeException("No chaintyp present");
      }

   }

   @Override
   protected HdKeyPath knownChildFactory(UnsignedInteger index, boolean hardened) {
      if (!hardened){
         return  new Bip44Address(this, index, false);
      }else{
         return new HdKeyPath(this, index, hardened);
      }
   }
}
