package com.mycelium;

import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.RandomSource;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.Transaction;
import com.mycelium.net.HttpEndpoint;
import com.mycelium.net.HttpsEndpoint;
import com.mycelium.net.ServerEndpoints;
import com.mycelium.net.TorHttpsEndpoint;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.WapiClientElectrumX;
import com.mycelium.wapi.api.jsonrpc.TcpEndpoint;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.SecureKeyValueStore;
import com.mycelium.wapi.wallet.SendRequest;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.btc.InMemoryWalletManagerBacking;
import com.mycelium.wapi.wallet.btc.WalletManagerBacking;
import com.mycelium.wapi.wallet.btc.bip44.ExternalSignatureProviderProxy;
import com.mycelium.wapi.wallet.coins.BitcoinMain;
import com.mycelium.wapi.wallet.coins.BitcoinTest;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.GenericAssetInfo;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.exceptions.TransactionBroadcastException;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

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
        WalletManager walletManager = new WalletManager(store,
                backing,
                network,
                wapiClient,
                externalSignatureProviderProxy,
                null,
                true);

        try {

            walletManager.configureBip32MasterSeed(masterSeed, AesKeyCipher.defaultKeyCipher());
            walletManager.createAdditionalBip44Account(AesKeyCipher.defaultKeyCipher());


            List<WalletAccount> accounts = walletManager.getActiveAccounts();

            WalletAccount account = accounts.get(0);
            account.synchronize(SyncMode.NORMAL);

            System.out.println("Account balance: " + account.getAccountBalance().getSpendable().toString());

            InMemoryPrivateKey key = new InMemoryPrivateKey(new MyRandomSource());

            UUID accountUUID = walletManager.createSingleAddressAccount(key, AesKeyCipher.defaultKeyCipher());
            WalletAccount saWalletAccount = walletManager.getAccount(accountUUID);
            GenericAddress saAddress = saWalletAccount.getReceiveAddress();

            System.out.println("Generated new SA account address: " + saAddress.toString());

            GenericAssetInfo currency = network.isProdnet() ? BitcoinMain.get() : BitcoinTest.get();
            Value amountToSend = Value.valueOf(currency, 1000000);
            SendRequest sendRequest = account.getSendToRequest(saAddress, amountToSend);
            account.completeAndSignTx(sendRequest);

            account.broadcastTx(sendRequest.tx);

        } catch (KeyCipher.InvalidKeyCipher | WalletAccount.WalletAccountException | TransactionBroadcastException ex) {
            ex.printStackTrace();
        }

    }
}