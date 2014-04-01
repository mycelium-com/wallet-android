package com.mrd.bitlib.crypto;

import junit.framework.Assert;

import org.junit.Test;

import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.HexUtils;

public class EcdhTest {

   private static final String PRIVATE_KEY_1 = "Kyqg1PJsc5QzLC8rv5BwC156aXBiZZuEyt6FqRQRTXBjTX96bNkW";
   private static final String PRIVATE_KEY_2 = "KyqHExGgWAkmPB4h3pk7VJWLA9nMN4jCQen1LfveZN5tyDn75dYH";
   private static final String SECRET = "ecd7601c320b3aa96d2f18df3097e9e2b15b180c76a02a1b1e82cccf6751c328";

   @Test
   public void sharedSecretTest() {

      // My signing keys
      InMemoryPrivateKey myPrv = new InMemoryPrivateKey(PRIVATE_KEY_1, NetworkParameters.productionNetwork);

      // Foreign signing keys
      InMemoryPrivateKey foreignPrv = new InMemoryPrivateKey(PRIVATE_KEY_2, NetworkParameters.productionNetwork);

      // Generate the same secret for both me and the foreigner

      // What I'll do
      byte[] mySecret = Ecdh.calculateSharedSecret(foreignPrv.getPublicKey(), myPrv);

      // What the foreigner will do
      byte[] foreignerSecret = Ecdh.calculateSharedSecret(myPrv.getPublicKey(), foreignPrv);

      // Did we get the same secret?
      Assert.assertTrue(BitUtils.areEqual(mySecret, foreignerSecret));

      // Does it match our test value
      Assert.assertEquals(HexUtils.toHex(mySecret), SECRET);
   }

}
