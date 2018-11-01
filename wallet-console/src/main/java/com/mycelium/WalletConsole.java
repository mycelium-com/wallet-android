package com.mycelium;

import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.RandomSource;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.net.HttpEndpoint;
import com.mycelium.net.HttpsEndpoint;
import com.mycelium.net.ServerEndpoints;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.WapiClientElectrumX;
import com.mycelium.wapi.api.jsonrpc.TcpEndpoint;
import com.mycelium.wapi.wallet.*;
import com.mycelium.wapi.wallet.bip44.ChangeAddressMode;
import com.mycelium.wapi.wallet.btc.InMemoryWalletManagerBacking;
import com.mycelium.wapi.wallet.btc.WalletManagerBacking;
import com.mycelium.wapi.wallet.btc.bip44.AdditionalHDAccountConfig;
import com.mycelium.wapi.wallet.btc.bip44.BitcoinHDModule;
import com.mycelium.wapi.wallet.btc.bip44.ExternalSignatureProviderProxy;
import com.mycelium.wapi.wallet.eth.EthAccount;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class WalletConsole {

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

    public static void main(String[] args) {
        WalletManagerBacking backing = new InMemoryWalletManagerBacking();

        final ServerEndpoints testnetWapiEndpoints = new ServerEndpoints(new HttpEndpoint[]{
                new HttpsEndpoint("https://mws30.mycelium.com/wapitestnet", "ED:C2:82:16:65:8C:4E:E1:C7:F6:A2:2B:15:EC:30:F9:CD:48:F8:DB"),
        });

        final WapiLogger wapiLogger = new WapiLogger() {
            @Override
            public void logError(String message) {
            }

            @Override
            public void logError(String message, Exception e) {
            }

            @Override
            public void logInfo(String message) {
            }
        };

        final TcpEndpoint[] tcpEndpoints = new TcpEndpoint[]{new TcpEndpoint("electrumx.mycelium.com", 4432)};
        Wapi wapiClient = new WapiClientElectrumX(testnetWapiEndpoints, tcpEndpoints, wapiLogger, "0");

        ExternalSignatureProviderProxy externalSignatureProviderProxy = new ExternalSignatureProviderProxy();

        SecureKeyValueStore store = new SecureKeyValueStore(backing, new MyRandomSource());

        Bip39.MasterSeed masterSeed =  Bip39.generateSeedFromWordList(new String[]{"cliff", "battle","noise","aisle","inspire","total","sting","vital","marble","add","daring","mouse"}, "");

        NetworkParameters network = NetworkParameters.testNetwork;
        Map<Currency, CurrencySettings> currenciesSettingsMap = new HashMap<>();

        BTCSettings btcSettings = new BTCSettings(AddressType.P2SH_P2WPKH, new Reference<>(ChangeAddressMode.P2SH_P2WPKH));
        currenciesSettingsMap.put(Currency.BTC, btcSettings);

        WalletManager walletManager = new WalletManager(store,
                backing,
                network,
                wapiClient);

        try {

            // create and add HD Module
            walletManager.configureBip32MasterSeed(masterSeed, AesKeyCipher.defaultKeyCipher());
            BitcoinHDModule bitcoinHDModule = new BitcoinHDModule(backing, store, network, wapiClient, currenciesSettingsMap);
            walletManager.add(bitcoinHDModule);

            // create account
            walletManager.createAccounts(new AdditionalHDAccountConfig());

            EthAccount ethAccount = new EthAccount();
            walletManager.addAccount(ethAccount);

            System.out.println("ETH Account balance: " + ethAccount.getAccountBalance().getSpendable().toString());
            System.out.println("ETH Account balance: " + ethAccount.getAccountBalance().getSpendable().toFriendlyString());
            // display HD account balance
            //List<WalletAccount<?,?>> accounts = walletManager.getActiveAccounts();
            //WalletAccount account = accounts.get(0);
            //account.synchronize(SyncMode.NORMAL);
           // System.out.println("HD Account balance: " + account.getAccountBalance().getSpendable().toString());

        } catch (KeyCipher.InvalidKeyCipher ex) {
            ex.printStackTrace();
        }

    }
}