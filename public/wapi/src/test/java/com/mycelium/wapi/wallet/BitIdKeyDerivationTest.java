package com.mycelium.wapi.wallet;

import com.mrd.bitlib.crypto.*;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

import java.security.SecureRandom;

public class BitIdKeyDerivationTest {
   private static class MyRandomSource implements RandomSource {
      SecureRandom _rnd;
      public MyRandomSource() {
         _rnd = new SecureRandom(new byte[]{42});
      }
      @Override
      public void nextBytes(byte[] bytes) {
         _rnd.nextBytes(bytes);
      }
   }

   private final String[] WORD_LIST = {"alcohol", "woman", "abuse", "must", "during", "monitor",
                                       "noble", "actual", "mixed", "trade", "anger", "aisle"};

   private final String WEBSITE = "https://satoshi@bitcoin.org/login";
   private final String PUBKEY = "023a472219ad3327b07c18273717bb3a40b39b743756bf287fbd5fa9d263237f45";
   private final String ADDRESS = "17F17smBTX9VTZA9Mj8LM5QGYNZnmziCjL";

   @Test
   public void storeAndRetrieveEncrypted() throws KeyCipher.InvalidKeyCipher {
      Bip39.MasterSeed seed = Bip39.generateSeedFromWordList(WORD_LIST, "");
      HdKeyNode rootNode = HdKeyNode.fromSeed(seed.getBip32Seed());
      SecureKeyValueStore store = new SecureKeyValueStore(new InMemoryWalletManagerBacking(), new MyRandomSource());
      KeyCipher cipher = AesKeyCipher.defaultKeyCipher();

      IdentityAccountKeyManager identityManager = IdentityAccountKeyManager.createNew(rootNode, store, cipher);
      InMemoryPrivateKey priv = identityManager.getPrivateKeyForWebsite(WEBSITE, cipher);
      PublicKey pub = priv.getPublicKey();
      Address address = pub.toAddress(NetworkParameters.productionNetwork);

      assertTrue(pub.toString().equals(PUBKEY));
      assertTrue(address.toString().equals(ADDRESS));
   }
}
