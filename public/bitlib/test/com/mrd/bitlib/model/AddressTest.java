package com.mrd.bitlib.model;

import java.security.SecureRandom;

import org.junit.Test;

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.crypto.RandomSource;

public class AddressTest {

   public static final RandomSource RANDOM_SOURCE = new RandomSource() {

      @Override
      public void nextBytes(byte[] bytes) {
         new SecureRandom().nextBytes(bytes);
      }
   };

   @Test
   public void toStringTest() {
      InMemoryPrivateKey priv = new InMemoryPrivateKey(RANDOM_SOURCE);
      PublicKey pub = priv.getPublicKey();
      Address addr = Address.fromStandardPublicKey(pub, NetworkParameters.productionNetwork);
      System.out.println(addr.toString());
   }

}
