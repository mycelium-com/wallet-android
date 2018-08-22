package com.mycelium.wapi.wallet;

import com.mrd.bitlib.crypto.RandomSource;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.HexUtils;
import com.mycelium.wapi.wallet.btc.InMemoryWalletManagerBacking;

import org.junit.Test;

import java.security.SecureRandom;

import static org.junit.Assert.assertTrue;

public class SecureKeyValueStoreTest {
   private static final byte[] ID_1 = HexUtils.toBytes("000102030405060708090a0b0c0d0e0f");
   private static final byte[] VALUE_1 = HexUtils.toBytes("0123456789abcdef");

   private static class MyRandomSource implements RandomSource {
      SecureRandom _rnd;

      MyRandomSource() {
         _rnd = new SecureRandom(new byte[]{42});
      }

      @Override
      public void nextBytes(byte[] bytes) {
         _rnd.nextBytes(bytes);
      }
   }

   @Test
   public void storeAndRetrieveEncrypted() throws KeyCipher.InvalidKeyCipher {
      SecureKeyValueStore store = new SecureKeyValueStore(new InMemoryWalletManagerBacking(), new MyRandomSource());
      KeyCipher cipher = AesKeyCipher.defaultKeyCipher();
      store.encryptAndStoreValue(ID_1, VALUE_1, cipher);
      byte[] result = store.getDecryptedValue(ID_1, cipher);
      assertTrue(BitUtils.areEqual(result, VALUE_1));
   }

   @Test
   public void storeAndRetrievePlaintext() throws KeyCipher.InvalidKeyCipher {
      SecureKeyValueStore store = new SecureKeyValueStore(new InMemoryWalletManagerBacking(), new MyRandomSource());
      store.storePlaintextValue(ID_1, VALUE_1);
      byte[] result = store.getPlaintextValue(ID_1);
      assertTrue(BitUtils.areEqual(result, VALUE_1));
   }
}
