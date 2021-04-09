package com.mycelium.wapi.wallet.bip44;

import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.RandomSource;
import com.mrd.bitlib.model.BitcoinAddress;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.generated.wallet.database.WalletDB;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.wallet.*;
import com.mycelium.wapi.wallet.btc.BTCSettings;
import com.mycelium.wapi.wallet.btc.ChangeAddressMode;
import com.mycelium.wapi.wallet.btc.InMemoryBtcWalletManagerBacking;
import com.mycelium.wapi.wallet.btc.Reference;
import com.mycelium.wapi.wallet.btc.BtcWalletManagerBacking;
import com.mycelium.wapi.wallet.btc.bip44.AdditionalHDAccountConfig;
import com.mycelium.wapi.wallet.btc.bip44.BitcoinHDModule;
import com.mycelium.wapi.wallet.btc.bip44.HDAccount;
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager;
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage;
import com.mycelium.wapi.wallet.metadata.MetadataKeyCategory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class HDAccountTest {
    private static final String MASTER_SEED_WORDS = "degree rain vendor coffee push math onion inside pyramid blush stick treat";
    private static final String MASTER_SEED_ACCOUNT_0_EXTERNAL_0_ADDRESS = "32LRQQsZt2dAzZq5HADLDEw5Fn8NzLhT35";
    private static final String MASTER_SEED_ACCOUNT_0_INTERNAL_0_ADDRESS = "38irRg7yBNjrpiAFxK2ac6GX1EHhYyjCLy";
    private HDAccount account;

    @Before
    public void setup() throws KeyCipher.InvalidKeyCipher {
        RandomSource fakeRandomSource = mock(RandomSource.class);
        Wapi fakeWapi = mock(Wapi.class);
        LoadingProgressUpdater fakeLoadingProgressUpdater = mock(LoadingProgressUpdater.class);

        BtcWalletManagerBacking backing = new InMemoryBtcWalletManagerBacking();
        SecureKeyValueStore store = new SecureKeyValueStore(backing, fakeRandomSource);
        KeyCipher cipher = AesKeyCipher.defaultKeyCipher();

        // Determine the next BIP44 account index
        Bip39.MasterSeed masterSeed = Bip39.generateSeedFromWordList(MASTER_SEED_WORDS.split(" "), "");

        HashMap<String, CurrencySettings> currenciesSettingsMap = new HashMap<>();
        currenciesSettingsMap.put(BitcoinHDModule.ID, new BTCSettings(AddressType.P2SH_P2WPKH, new Reference<>(ChangeAddressMode.PRIVACY)));

        WalletDB db = Mockito.mock(WalletDB.class);

        WalletManager walletManager = new WalletManager(NetworkParameters.productionNetwork, fakeWapi,
                currenciesSettingsMap, db);

        MasterSeedManager masterSeedManager = new MasterSeedManager(store);
        masterSeedManager.configureBip32MasterSeed(masterSeed, cipher);

        walletManager.add(new BitcoinHDModule(backing, store, NetworkParameters.productionNetwork,
                fakeWapi, (BTCSettings) currenciesSettingsMap.get(BitcoinHDModule.ID), new IMetaDataStorage() {
            @Override
            public void storeKeyCategoryValueEntry(MetadataKeyCategory keyCategory, String value) {
            }

            @Override
            public String getKeyCategoryValueEntry(String key, String category, String defaultValue) {
                return "";
            }

            @Override
            public Optional<String> getFirstKeyForCategoryValue(String category, String value) {
                return Optional.absent();
            }
        }, null, fakeLoadingProgressUpdater, null));

        UUID account1Id = walletManager.createAccounts(new AdditionalHDAccountConfig()).get(0);

        account = (HDAccount) walletManager.getAccount(account1Id);
    }

    /**
     * Test that the first two addresses we generate agree with a specific seed agree with Wallet32
     */
    @Test
    public void addressGenerationTest() {
        assertEquals(BitcoinAddress.fromString(MASTER_SEED_ACCOUNT_0_EXTERNAL_0_ADDRESS), account.getReceivingAddress().get());
        assertEquals(BitcoinAddress.fromString(MASTER_SEED_ACCOUNT_0_INTERNAL_0_ADDRESS), account.getChangeAddress());
    }
}
