package com.mycelium.wapi.wallet.btc.bip44;

import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.RandomSource;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.WapiLogger;
import com.mycelium.wapi.wallet.*;
import com.mycelium.wapi.wallet.btc.InMemoryWalletManagerBtcBacking;
import com.mycelium.wapi.wallet.btc.WalletManagerBtcBacking;
import org.junit.Before;
import org.junit.Test;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Bip44AccountTest {
    private static final String MASTER_SEED_WORDS = "degree rain vendor coffee push math onion inside pyramid blush stick treat";
    private static final String MASTER_SEED_ACCOUNT_0_EXTERNAL_0_ADDRESS = "1F1QAzNLutBEuB4QZLXghqu6PdxEFdb2PV";
    private static final String MASTER_SEED_ACCOUNT_0_INTERNAL_0_ADDRESS = "1PGrHHNjVXBr8JJhg9zRQVFvmUSu9XsMeV";
    private Bip44BtcAccount account;

    @Before
    public void setup() throws KeyCipher.InvalidKeyCipher {
        RandomSource fakeRandomSource = mock(RandomSource.class);
        Wapi fakeWapi = mock(Wapi.class);
        WapiLogger fakeLogger = mock(WapiLogger.class);
        when(fakeWapi.getLogger()).thenReturn(fakeLogger);

        WalletManagerBtcBacking backing = new InMemoryWalletManagerBtcBacking();
        SecureKeyValueStore store = new SecureKeyValueStore(backing, fakeRandomSource);
        KeyCipher cipher = AesKeyCipher.defaultKeyCipher();

        // Determine the next BIP44 account index
        Bip39.MasterSeed masterSeed = Bip39.generateSeedFromWordList(MASTER_SEED_WORDS.split(" "), "");

        WalletManager walletManager = new WalletManager(store, backing, NetworkParameters.productionNetwork, fakeWapi, null, null, false);

        walletManager.configureBip32MasterSeed(masterSeed, cipher);

        UUID account1Id = walletManager.createAdditionalBip44Account(cipher);

        account = (Bip44BtcAccount) walletManager.getAccount(account1Id);
    }

    /**
     * Test that the first two addresses we generate agree with a specific seed agree with Wallet32
     */
    @Test
    public void addressGenerationTest() throws KeyCipher.InvalidKeyCipher {
        assertEquals(Address.fromString(MASTER_SEED_ACCOUNT_0_EXTERNAL_0_ADDRESS), account.getReceivingAddress().get());
        assertEquals(Address.fromString(MASTER_SEED_ACCOUNT_0_INTERNAL_0_ADDRESS), account.getChangeAddress());
    }

    @Test
    public void calculateMaxSpendableAmount() throws Exception {
        // TODO: 25.06.17 add UTXOs, write tests with unconfirmed and dust UTXOs.
        account.calculateMaxSpendableAmount(1000);
    }
}
