package com.mycelium;

import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.RandomSource;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.generated.wallet.database.WalletDB;
import com.mycelium.net.HttpEndpoint;
import com.mycelium.net.HttpsEndpoint;
import com.mycelium.net.ServerEndpoints;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.WapiClientElectrumX;
import com.mycelium.wapi.api.jsonrpc.TcpEndpoint;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.SynchronizeFinishedListener;
import com.mycelium.wapi.wallet.btc.BTCSettings;
import com.mycelium.wapi.wallet.CurrencySettings;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.SecureKeyValueStore;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.btc.BtcWalletManagerBacking;
import com.mycelium.wapi.wallet.btc.ChangeAddressMode;
import com.mycelium.wapi.wallet.btc.InMemoryBtcWalletManagerBacking;
import com.mycelium.wapi.wallet.btc.Reference;
import com.mycelium.wapi.wallet.btc.bip44.AdditionalHDAccountConfig;
import com.mycelium.wapi.wallet.btc.bip44.BitcoinHDModule;
import com.mycelium.wapi.wallet.btc.bip44.ExternalSignatureProviderProxy;
import com.mycelium.wapi.wallet.btc.bip44.HDAccount;
import com.mycelium.wapi.wallet.btc.single.BitcoinSingleAddressModule;
import com.mycelium.wapi.wallet.btc.single.PublicPrivateKeyStore;
import com.mycelium.wapi.wallet.genericdb.AdaptersKt;
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager;
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage;
import com.mycelium.wapi.wallet.metadata.MetadataKeyCategory;
import com.squareup.sqldelight.db.SqlDriver;
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

class WalletConsole {
    public static final String[] ColoredCoinsApiURLs = {"http://testnet.api.coloredcoins.org/v3/"};
    public static final String[] ColuBlockExplorerApiURLs = {"http://testnet.explorer.coloredcoins.org/api/"};
//    public static final String[] ColoredCoinsApiURLs = {"https://coloredcoinsd.gear.mycelium.com/v3/", "https://api.coloredcoins.org/v3/"};
//    public static final String[] ColuBlockExplorerApiURLs = {"https://coloredcoins.gear.mycelium.com/api/", "https://explorer.coloredcoins.org/api/"};

    private static class MemoryBasedStorage implements IMetaDataStorage {
        private HashMap<String, String> map = new HashMap<>();
        @Override
        public void storeKeyCategoryValueEntry(MetadataKeyCategory keyCategory, String value) {
            map.put(keyCategory.category + "_" + keyCategory.key, value);
        }

        @Override
        public String getKeyCategoryValueEntry(String key, String category, String defaultValue) {
            return map.get(category + "_" + key);
        }

        @Override
        public Optional<String> getFirstKeyForCategoryValue(String category, String value) {
            for(Map.Entry<String, String> entry : map.entrySet()) {
                String[] keyAndCategory = entry.getKey().split("_");
                if (category.equals(keyAndCategory[1]) && entry.getValue().equals(value)) {
                    return Optional.of(keyAndCategory[0]);
                }
            }
            return Optional.absent();
        }
    }

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

/*
    public static void main(String[] args) {
        BtcWalletManagerBacking backing = new InMemoryBtcWalletManagerBacking();

        final ServerEndpoints testnetWapiEndpoints = new ServerEndpoints(new HttpEndpoint[]{
                new HttpsEndpoint("https://mws30.mycelium.com/wapitestnet", "ED:C2:82:16:65:8C:4E:E1:C7:F6:A2:2B:15:EC:30:F9:CD:48:F8:DB"),
        });

        Wapi wapiClient = new WapiClientElectrumX(testnetWapiEndpoints, new TcpEndpoint[]{new TcpEndpoint("electrumx.mycelium.com", 4432, true)}, "0", 1);
        Wapi btcvWapi = new WapiClientElectrumX(testnetWapiEndpoints, new TcpEndpoint[]{new TcpEndpoint("electrumx-mainnet1.bitcoinvault.global", 443, true)}, "0", 1);

        ExternalSignatureProviderProxy externalSignatureProviderProxy = new ExternalSignatureProviderProxy();

        SecureKeyValueStore store = new SecureKeyValueStore(backing, new MyRandomSource());

//        Bip39.MasterSeed masterSeed =  Bip39.generateSeedFromWordList(new String[]{"cliff", "battle","noise","aisle","inspire","total","sting","vital","marble","add","daring","mouse"}, "");
        Bip39.MasterSeed masterSeed = Bip39.generateSeedFromWordList(new String[]{"oil", "oil", "oil", "oil", "oil", "oil", "oil", "oil", "oil", "oil", "oil", "oil"}, "");

        NetworkParameters network = NetworkParameters.productionNetwork;

        HashMap<String, CurrencySettings> currenciesSettingsMap = new HashMap<>();
        BTCSettings btcSettings = new BTCSettings(AddressType.P2SH_P2WPKH, new Reference<>(ChangeAddressMode.P2SH_P2WPKH));
        currenciesSettingsMap.put(BitcoinHDModule.ID, btcSettings);


        SqlDriver driver = new JdbcSqliteDriver( "jdbc:sqlite::memory:", new Properties());
        WalletDB.Companion.getSchema().create(driver);
        WalletDB db = WalletDB.Companion.invoke(driver, AdaptersKt.getAccountBackingAdapter(), AdaptersKt.getAccountContextAdapter(),
                AdaptersKt.getBTCVAccountBackingAdapter(), AdaptersKt.getBTCVContextAdapter(),
                AdaptersKt.getBTCVOutgoingTxAdapter(), AdaptersKt.getBTCVPtxoAdapter(),
                AdaptersKt.getBTCVRefersPtxoAdapter(), AdaptersKt.getBTCVTransactionAdapter(),
                AdaptersKt.getBTCVUtxoAdapter(),
                AdaptersKt.getErc20ContextAdapter(), AdaptersKt.getEthAccountBackingAdapter(), AdaptersKt.getEthContextAdapter(),
                AdaptersKt.getFeeEstimatorAdapter(), AdaptersKt.getFioAccountBackingAdapter(), AdaptersKt.getFioContextAdapter(),
                AdaptersKt.getFioKnownNamesAdapter(), AdaptersKt.getFioNameAccountMappingsAdapter(),
                AdaptersKt.getFioOtherBlockchainTransactionsAdapter(),
                AdaptersKt.getFioReceivedRequestsAdapter(), AdaptersKt.getFioSentRequestsAdapter());

        WalletManager walletManager = new WalletManager(
                network,
                wapiClient,
                btcvWapi,
                currenciesSettingsMap,
                db);
        walletManager.setIsNetworkConnected(true);

        MasterSeedManager masterSeedManager = new MasterSeedManager(store);
        try {
            // create and add HD Module
            masterSeedManager.configureBip32MasterSeed(masterSeed, AesKeyCipher.defaultKeyCipher());

            BitcoinHDModule bitcoinHDModule = new BitcoinHDModule(backing, store, network, wapiClient, btcSettings, new MemoryBasedStorage(), null,null, null);
            walletManager.add(bitcoinHDModule);

            // create sample HD accounts
            final HDAccount hdAccount1 = (HDAccount) walletManager.getAccount(walletManager.createAccounts(new AdditionalHDAccountConfig()).get(0));
            final HDAccount hdAccount2 = (HDAccount) walletManager.getAccount(walletManager.createAccounts(new AdditionalHDAccountConfig()).get(0));
            HDAccount hdAccount3 = (HDAccount) walletManager.getAccount(walletManager.createAccounts(new AdditionalHDAccountConfig()).get(0));

            SynchronizeFinishedListener listener = new SynchronizeFinishedListener();
            walletManager.setWalletListener(listener);
            walletManager.startSynchronization();
            listener.waitForSyncFinished();

            PublicPrivateKeyStore publicPrivateKeyStore = new PublicPrivateKeyStore(store);

            BitcoinSingleAddressModule bitcoinSingleAddressModule = new BitcoinSingleAddressModule(backing, publicPrivateKeyStore
                    , network, wapiClient, btcSettings, walletManager,null, null, null);
            walletManager.add(bitcoinSingleAddressModule);

//             display HD account balance
            List<WalletAccount<?>> accounts = walletManager.getActiveSpendingAccounts();
            WalletAccount account = accounts.get(0);
            account.synchronize(SyncMode.NORMAL);
            System.out.println("HD Account balance: " + account.getAccountBalance().getSpendable().toString());
        } catch (KeyCipher.InvalidKeyCipher ex) {
            ex.printStackTrace();
        }
    }
*/

    /*

    This code should be rewritten and moved to colu-related unit tests

    private void addColuModule() {

              new ColuApiImpl(coluClient), new ColuWalletBacking(), new AccountListener() {
                @Override
                public void balanceUpdated(@NotNull WalletAccount<?, ?> walletAccount) {

                }
            }, new IMetaDataStorage() {
                @Override
                public void storeKeyCategoryValueEntry(MetadataKeyCategory keyCategory, String value) {

                }

                @Override
                public String getKeyCategoryValueEntry(String key, String category, String defaultValue) {
                    return null;
                }

                @Override
                public Optional<String> getFirstKeyForCategoryValue(String category, String value) {
                    return null;
                }
            });


            ColuClient coluClient = new ColuClient(network, ColoredCoinsApiURLs, ColuBlockExplorerApiURLs);
            ColuModule coluModule = new ColuModule(network, publicPrivateKeyStore,

                walletManager.add(coluModule);
            List<UUID> colu = walletManager.createAccounts(new PrivateColuConfig(
                    new InMemoryPrivateKey("", network)
                    , RMCCoin.INSTANCE, AesKeyCipher.defaultKeyCipher()));

            SingleAddressAccount coluSAAccount = null;
            PrivateColuAccount coluAccount = null;
            if (walletManager.getAccount(colu.get(0)) instanceof PrivateColuAccount) {
                coluAccount = (PrivateColuAccount) walletManager.getAccount(colu.get(0));
            } else {
                coluSAAccount = (SingleAddressAccount) walletManager.getAccount(colu.get(0));
            }
            if (walletManager.getAccount(colu.get(1)) instanceof PrivateColuAccount) {
                coluAccount = (PrivateColuAccount) walletManager.getAccount(colu.get(1));
            } else {
                coluSAAccount = (SingleAddressAccount) walletManager.getAccount(colu.get(1));
            }


            new Synchronizer(walletManager, SyncMode.NORMAL , Arrays.asList(hdAccount1, hdAccount2, hdAccount3, coluAccount, coluSAAccount)).run();

            coluParseAndSign("010000000310d7fc7204151a520798363c6cc239c12d807b6e49f0ae51979faea17c58bf" +
                             "280100000000ffffffff4e193a5a9c123cd896bf2ca368b82f0f336bf9ab1d74ad7acdf" +
                             "4b05189aed8920000000000ffffffff56827cfa76e22baa3186b7a61aad57d22d7fafd2" +
                             "952c91f15ecbae9eae090b4f0100000000ffffffff0470170000000000001976a914e3d" +
                             "60320db38011d6b36d1a62382bc679e444f7688ac0000000000000000086a0643430215" +
                             "00015bfa6501000000001976a914ac1e4e536ebb4403ad8fac14686ae4812a76c6e388a" +
                             "c70170000000000001976a914ac1e4e536ebb4403ad8fac14686ae4812a76c6e388ac00000000"
                             ,coluAccount, coluSAAccount);

//            sendBtcFrom2Account(network, hdAccount1, hdAccount2, hdAccount3);

//            sendColuWithFundingAccount(coluAccount, coluSAAccount, hdAccount2);
    }

    private static void coluParseAndSign(String data, PrivateColuAccount coluAccount, SingleAddressAccount singleAddressAccount) {
        try {
            byte[] txBytes = Hex.decodeHex(data.toCharArray());
            Transaction transaction = Transaction.fromBytes(txBytes);
            InMemoryPrivateKey key = singleAddressAccount.getPrivateKey(AesKeyCipher.defaultKeyCipher());
            TransactionInput[] inputs = transaction.inputs;
            for (int i = 0; i < inputs.length; i++) {
                TransactionInput input = inputs[i];
                TransactionDetails.Item transactionDetails = singleAddressAccount.getTransactionDetails(input.outPoint.txid).outputs[input.outPoint.index];
                if(singleAddressAccount.isMine(transactionDetails.address)) {
                    input.script = new ScriptInputStandard(key.makeStandardBitcoinSignature(transaction.getTxDigestHash(i))
                            , key.getPublicKey().getPublicKeyBytes());
                }
            }
        } catch (DecoderException | Transaction.TransactionParsingException | KeyCipher.InvalidKeyCipher e) {
            e.printStackTrace();
        }
    }

    private static void sendColuWithFundingAccount(PrivateColuAccount coluAccount, SingleAddressAccount coluSAAccount, HDAccount hdAccount1) {
        Address<ColuTransactionSummary> sendRequest = coluAccount.getSendToRequest(
                new BtcAddress(RMCCoin.INSTANCE
                        , Address.fromString("1MmgmNmKTzaNmQRi3DEmzULrxpPnxszh1c"))
                , Value.valueOf(RMCCoin.INSTANCE, 1));
        if (sendRequest instanceof ColuTransaction) {
            sendRequest.fee = Value.valueOf(coluSAAccount.getCoinType(), 10000000);
            List<BtcAddress> funding = new ArrayList<>();
            for (TransactionOutputSummary transactionOutputSummary : hdAccount1.getUnspentTransactionOutputSummary()) {
                funding.add(new BtcAddress(hdAccount1.getCoinType(), transactionOutputSummary.address));
            }
            ((ColuTransaction) sendRequest).setFundingAddress(funding);
        }
        coluAccount.completeTransaction(sendRequest);
        coluAccount.signTx(sendRequest, AesKeyCipher.defaultKeyCipher());
    }

    private static void sendBtcFrom2Account(final NetworkParameters network, final HDAccount hdAccount1, final HDAccount hdAccount2, HDAccount hdAccount3) throws KeyCipher.InvalidKeyCipher {
        List<UnspentTransactionOutput> transform1 = transform(hdAccount1.getAccountBacking().getAllUnspentOutputs());
        List<UnspentTransactionOutput> transform2 = transform(hdAccount2.getAccountBacking().getAllUnspentOutputs());

        try {
            StandardTransactionBuilder stb = new StandardTransactionBuilder(network);

            BtcReceiver receiver = new BtcReceiver( ((BtcAddress)hdAccount3.getReceiveAddress()).getAddress(), 10000l);

            stb.addOutput(((BtcAddress) receiver.address).getAddress(), receiver.amount);

            UnsignedTransaction unsignedTransaction = stb.createUnsignedTransaction(Arrays.asList(transform1.get(0), transform2.get(0))
                    , hdAccount1.getChangeAddress()
                    , new IPublicKeyRing() {
                        @Override
                        public PublicKey findPublicKeyByAddress(Address address) {
                            if (hdAccount1.isMine(address)) {
                                try {
                                    return hdAccount1.getPrivateKeyForAddress(address, AesKeyCipher.defaultKeyCipher()).getPublicKey();
                                } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
                                    invalidKeyCipher.printStackTrace();
                                }
                            } else if (hdAccount2.isMine(address)) {
                                try {
                                    return hdAccount2.getPrivateKeyForAddress(address, AesKeyCipher.defaultKeyCipher()).getPublicKey();
                                } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
                                    invalidKeyCipher.printStackTrace();
                                }
                            }
                            return null;
                        }
                    },
                    network, 1000);


            // Make all signatures, this is the CPU intensive part
            List<byte[]> signatures = StandardTransactionBuilder.generateSignatures(
                    unsignedTransaction.getSigningRequests(),
                    new IPrivateKeyRing() {
                        @Override
                        public BitcoinSigner findSignerByPublicKey(PublicKey publicKey) {
                            try {
                                InMemoryPrivateKey inMemoryPrivateKey1 = hdAccount1.getPrivateKey(publicKey, AesKeyCipher.defaultKeyCipher());
                                if (inMemoryPrivateKey1 != null) {
                                    return inMemoryPrivateKey1;
                                }
                                InMemoryPrivateKey inMemoryPrivateKey2 = hdAccount2.getPrivateKey(publicKey, AesKeyCipher.defaultKeyCipher());
                                if (inMemoryPrivateKey2 != null) {
                                    return inMemoryPrivateKey2;
                                }

                            } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
                                invalidKeyCipher.printStackTrace();
                            }
                            return null;
                        }
                    }
            );
            Transaction transaction = StandardTransactionBuilder.finalizeTransaction(unsignedTransaction, signatures);
            hdAccount1.broadcastTransaction(transaction);

        } catch (StandardTransactionBuilder.OutputTooSmallException e) {
            e.printStackTrace();
        } catch (StandardTransactionBuilder.InsufficientFundsException e) {
            e.printStackTrace();
        } catch (StandardTransactionBuilder.UnableToBuildTransactionException e) {
            e.printStackTrace();
        }
    }

    private static List<UnspentTransactionOutput> transform(Collection<TransactionOutputEx> source) {
        List<UnspentTransactionOutput> outputs = new ArrayList<>();
        for (TransactionOutputEx s : source) {
            ScriptOutput script = ScriptOutput.fromScriptBytes(s.script);
            outputs.add(new UnspentTransactionOutput(s.outPoint, s.height, s.value, script));
        }
        return outputs;
    }

    */
}
