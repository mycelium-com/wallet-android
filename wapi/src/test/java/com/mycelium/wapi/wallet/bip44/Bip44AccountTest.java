package com.mycelium.wapi.wallet.bip44;

import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.RandomSource;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.WapiLogger;
import com.mycelium.wapi.wallet.*;
import org.junit.Test;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Bip44AccountTest {
   private static final String MASTER_SEED_WORDS = "degree rain vendor coffee push math onion inside pyramid blush stick treat";
   private static final String MASTER_SEED_512_A0_R0_ADDRESS = "1F1QAzNLutBEuB4QZLXghqu6PdxEFdb2PV";
   private static final String MASTER_SEED_512_A0_C0_ADDRESS = "1PGrHHNjVXBr8JJhg9zRQVFvmUSu9XsMeV";

   /**
    * Test that the first two addresses we generate agree with a specific seed agree with Wallet32
    */
   @Test
   public void addressGenerationTest() throws KeyCipher.InvalidKeyCipher {
      RandomSource fakeRandomSource = mock(RandomSource.class);
      Wapi fakeWapi = mock(Wapi.class);
      WapiLogger fakeLogger = mock(WapiLogger.class);
      when(fakeWapi.getLogger()).thenReturn(fakeLogger);

      SecureKeyValueStore store = new SecureKeyValueStore(new InMemoryWalletManagerBacking(), fakeRandomSource);
      KeyCipher cipher = AesKeyCipher.defaultKeyCipher();

      WalletManagerBacking backing = new InMemoryWalletManagerBacking();

      // Determine the next BIP44 account index
      Bip39.MasterSeed masterSeed = Bip39.generateSeedFromWordList(MASTER_SEED_WORDS.split(" "), "");

      WalletManager walletManager = new WalletManager(store, backing, NetworkParameters.productionNetwork, fakeWapi, null);

      walletManager.configureBip32MasterSeed(masterSeed, cipher);

      UUID account1Id = walletManager.createAdditionalBip44Account(cipher);

      Bip44Account account1 = (Bip44Account) walletManager.getAccount(account1Id);

      assertEquals(Address.fromString(MASTER_SEED_512_A0_R0_ADDRESS), account1.getReceivingAddress().get());
      assertEquals(Address.fromString(MASTER_SEED_512_A0_C0_ADDRESS), account1.getChangeAddress());
   }
}
