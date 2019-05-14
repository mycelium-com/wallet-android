package com.mrd.bitlib.crypto;

import org.junit.Test;

import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.HexUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EcdhTest {
   private static final InMemoryPrivateKey ALICE_KEY = new InMemoryPrivateKey("Kyqg1PJsc5QzLC8rv5BwC156aXBiZZuEyt6FqRQRTXBjTX96bNkW", NetworkParameters.productionNetwork);
   private static final InMemoryPrivateKey CAROL_KEY = new InMemoryPrivateKey("KyqHExGgWAkmPB4h3pk7VJWLA9nMN4jCQen1LfveZN5tyDn75dYH", NetworkParameters.productionNetwork);
   private static final String SECRET = "ecd7601c320b3aa96d2f18df3097e9e2b15b180c76a02a1b1e82cccf6751c328";

   @Test
   public void sharedSecretTest() {
      // Generate the same secret for both
      byte[] aliceSecret = Ecdh.calculateSharedSecret(CAROL_KEY.getPublicKey(), ALICE_KEY);
      byte[] carolSecret = Ecdh.calculateSharedSecret(ALICE_KEY.getPublicKey(), CAROL_KEY);

      // Did we get the same expected secret?
      assertEquals(SECRET, HexUtils.toHex(aliceSecret));
      assertEquals(SECRET, HexUtils.toHex(carolSecret));
   }
}
