package com.mrd.bitlib.model;

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import org.junit.Test;

import java.security.SecureRandom;

public class AddressTest {

   public static final SecureRandom SECURE_RANDOM = new SecureRandom();

   @Test
   public void toStringTest(){
      InMemoryPrivateKey priv = new InMemoryPrivateKey(SECURE_RANDOM);
      PublicKey pub = priv.getPublicKey();
      Address addr = Address.fromStandardPublicKey(pub, NetworkParameters.productionNetwork);
      System.out.println(addr.toString());
   }

}
