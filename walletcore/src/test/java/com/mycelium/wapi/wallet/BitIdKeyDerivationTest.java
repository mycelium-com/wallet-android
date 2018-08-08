package com.mycelium.wapi.wallet;

import com.mrd.bitlib.crypto.*;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

   private final String[] WORD_LIST = {"abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
                                       "abandon", "abandon", "abandon", "abandon", "abandon", "about"};
   private final String PWD = "SecondIdentity";

   private final String WEBSITE = "https://satoshi@bitcoin.org/login";
   private final String PUBKEY_DEFAULT = "030a79ba07392dafab29e2bf01917dcb2b1cb235ccad9c7a59639ad0f84c3f619c";
   private final String ADDRESS_DEFAULT = "1LbxwgBqp6VYXfoadiLRVF1jaDxqL4SdRz";

   private final String PUBKEY_OTHER = "0265da9147121706403032fb22107206b0c510de65a19711eca5781edf67639598";
   private final String ADDRESS_OTHER = "11XiTMf6dULM8Uk7QohJMDEvdW6Lqy2gG";


   @Test
   public void bitIdDefaultAccount() throws KeyCipher.InvalidKeyCipher {
      Bip39.MasterSeed seed = Bip39.generateSeedFromWordList(WORD_LIST, "");
      HdKeyNode rootNode = HdKeyNode.fromSeed(seed.getBip32Seed());
      SecureKeyValueStore store = new SecureKeyValueStore(new InMemoryWalletManagerBacking(), new MyRandomSource());
      KeyCipher cipher = AesKeyCipher.defaultKeyCipher();

      IdentityAccountKeyManager identityManager = IdentityAccountKeyManager.createNew(rootNode, store, cipher);
      InMemoryPrivateKey priv = identityManager.getPrivateKeyForWebsite(WEBSITE, cipher);
      PublicKey pub = priv.getPublicKey();
      Address address = pub.toAddress(NetworkParameters.productionNetwork);

      assertEquals(PUBKEY_DEFAULT, pub.toString());
      assertEquals(ADDRESS_DEFAULT, address.toString());
   }

   @Test
   public void bitIdOtherAccount() throws KeyCipher.InvalidKeyCipher {
      Bip39.MasterSeed seed = Bip39.generateSeedFromWordList(WORD_LIST, PWD);
      HdKeyNode rootNode = HdKeyNode.fromSeed(seed.getBip32Seed());
      SecureKeyValueStore store = new SecureKeyValueStore(new InMemoryWalletManagerBacking(), new MyRandomSource());
      KeyCipher cipher = AesKeyCipher.defaultKeyCipher();

      IdentityAccountKeyManager identityManager = IdentityAccountKeyManager.createNew(rootNode, store, cipher);
      InMemoryPrivateKey priv = identityManager.getPrivateKeyForWebsite(WEBSITE, cipher);
      PublicKey pub = priv.getPublicKey();
      Address address = pub.toAddress(NetworkParameters.productionNetwork);

      assertEquals(PUBKEY_OTHER, pub.toString());
      assertEquals(ADDRESS_OTHER, address.toString());
   }

   // test vectors from https://github.com/bitid/bitid/blob/master/BIP_draft.md
   private final String[] WORD_LIST_BITID = {
           "inhale", "praise", "target", "steak", "garlic", "cricket", "paper", "better", "evil",
           "almost", "sadness", "crawl", "city", "banner", "amused", "fringe", "fox", "insect",
           "roast", "aunt", "prefer", "hollow", "basic", "ladder"};

   private final String WEBSITE_BITID = "http://bitid.bitcoin.blue/callback";
   private final String ADDRESS_BITID = "1J34vj4wowwPYafbeibZGht3zy3qERoUM1";

   @Test
   public void bitIdBipTestVector() throws KeyCipher.InvalidKeyCipher {
      Bip39.MasterSeed seed = Bip39.generateSeedFromWordList(WORD_LIST_BITID, "");
      HdKeyNode rootNode = HdKeyNode.fromSeed(seed.getBip32Seed());
      SecureKeyValueStore store = new SecureKeyValueStore(new InMemoryWalletManagerBacking(), new MyRandomSource());
      KeyCipher cipher = AesKeyCipher.defaultKeyCipher();

      IdentityAccountKeyManager identityManager = IdentityAccountKeyManager.createNew(rootNode, store, cipher);
      InMemoryPrivateKey priv = identityManager.getPrivateKeyForWebsite(WEBSITE_BITID, cipher);
      PublicKey pub = priv.getPublicKey();
      Address address = pub.toAddress(NetworkParameters.productionNetwork);

      assertEquals(ADDRESS_BITID, address.toString());
   }
}
