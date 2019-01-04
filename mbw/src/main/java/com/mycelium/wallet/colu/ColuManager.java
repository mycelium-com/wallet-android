package com.mycelium.wallet.colu;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.Transaction;
import com.mycelium.wallet.MbwEnvironment;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.activity.util.BlockExplorer;
import com.mycelium.wallet.colu.json.AddressInfo;
import com.mycelium.wallet.colu.json.AddressTransactionsInfo;
import com.mycelium.wallet.colu.json.Asset;
import com.mycelium.wallet.colu.json.AssetMetadata;
import com.mycelium.wallet.colu.json.ColuBroadcastTxHex;
import com.mycelium.wallet.colu.json.ColuBroadcastTxId;
import com.mycelium.wallet.colu.json.Tx;
import com.mycelium.wallet.colu.json.Utxo;
import com.mycelium.wallet.colu.json.Vin;
import com.mycelium.wallet.colu.json.Vout;
import com.mycelium.wallet.event.BalanceChanged;
import com.mycelium.wallet.event.EventTranslator;
import com.mycelium.wallet.event.ExtraAccountsChanged;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.WapiClient;
import com.mycelium.wapi.api.WapiException;
import com.mycelium.wapi.api.WapiResponse;
import com.mycelium.wapi.api.request.GetTransactionsRequest;
import com.mycelium.wapi.api.request.QueryUnspentOutputsRequest;
import com.mycelium.wapi.api.response.GetTransactionsResponse;
import com.mycelium.wapi.api.response.QueryUnspentOutputsResponse;
import com.mycelium.wapi.wallet.*;
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher;
import com.mycelium.wapi.wallet.bip44.ChangeAddressMode;
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;
import com.mycelium.wapi.wallet.single.PublicPrivateKeyStore;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;
import com.mycelium.wapi.wallet.single.SingleAddressAccountContext;
import com.squareup.otto.Bus;

import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;

public class ColuManager implements AccountProvider {
    private static final String TAG = "ColuManager";
    private static final int MAX_ACCOUNTS_NUMBER = 1000;

    private final MbwEnvironment env;
    private final MbwManager mgr;
    private final Bus eventBus;
    private final Handler handler;
    private final ColuClient coluClient;
    private final Map<UUID, WalletAccount> _walletAccounts;
    private final MetadataStorage metadataStorage;
    private SqliteColuManagerBacking _backing;
    private final HashMap<UUID, ColuAccount> coluAccounts;
    private NetworkParameters _network;
    private final SecureKeyValueStore _secureKeyValueStore;
    private WalletManager.State state;
    private volatile boolean isNetworkConnected;
    private Map<ColuAccount.ColuAssetType, AssetMetadata> assetsMetadata = new HashMap<>();

    public static final int TIME_INTERVAL_BETWEEN_BALANCE_FUNDING_CHECKS = 50;
    public static final int METADATA_OUTPUT_SIZE = 1;
    private static final int AVERAGE_COLU_TX_SIZE = 212;

    private final org.bitcoinj.core.NetworkParameters netParams;
    private EventTranslator eventTranslator;

    public ColuManager(SecureKeyValueStore secureKeyValueStore, SqliteColuManagerBacking backing,
                       MbwManager manager, MbwEnvironment env,
                       final Bus eventBus, Handler handler,
                       MetadataStorage metadataStorage, boolean isNetworkConnected) {
        this._secureKeyValueStore = secureKeyValueStore;
        this._backing = backing;
        this.env = env;
        this.mgr = manager;
        this.eventBus = eventBus;
        this.handler = handler;
        this.metadataStorage = metadataStorage;
        this.isNetworkConnected = isNetworkConnected;
        eventTranslator = new EventTranslator(handler, eventBus);

        //Setting up the network
        this._network = env.getNetwork();
        if (this._network.isProdnet()) {
            this.netParams = MainNetParams.get();
        } else if (this._network.isTestnet()) {
            this.netParams = TestNet3Params.get();
        } else {
            this.netParams = RegTestParams.get();
        }

        this._walletAccounts = Maps.newHashMap();

        coluClient = createClient();

        handler.post(new Runnable() {
            @Override
            public void run() {
                eventBus.register(ColuManager.this);
            }
        });
        coluAccounts = new HashMap<>();
        loadAssetsMetadata();
        loadAccounts();
    }

    private void loadAssetsMetadata() {
        for (ColuAccount.ColuAssetType assetType : ColuAccount.ColuAssetType.values()) {
            String id = checkNotNull(ColuAccount.ColuAsset.getByType(assetType)).id;
            Optional<BigDecimal> coinSupply = metadataStorage.getColuAssetCoinSupply(id);
            if (coinSupply.isPresent()) {
                assetsMetadata.put(assetType, new AssetMetadata(id, coinSupply.get()));
            }
        }
    }

    public BlockExplorer getBlockExplorer() {
        String baseUrl;
        if (_network.isProdnet()) {
            baseUrl = "http://coloredcoins.org/explorer/";
        } else if (_network.isTestnet()) {
            baseUrl = "http://coloredcoins.org/explorer/testnet/";
        } else {
            baseUrl = "http://coloredcoins.org/explorer/testnet/";
        }

        return new BlockExplorer("CCO", "coloredcoins.org"
                , baseUrl + "address/", baseUrl + "tx/"
                , baseUrl + "address/", baseUrl + "tx/");
    }

    public long getColuTransactionFee(long feePerKb) {
        return (AVERAGE_COLU_TX_SIZE * feePerKb) / 1000;
    }

    private void saveEnabledAssetIds() {
        List<String> assetIds = new ArrayList<>();

        for(ColuAccount account : coluAccounts.values()) {
            if (!assetIds.contains(account.getColuAsset().id)) {
                assetIds.add(account.getColuAsset().id);
            }
        }

        String all = Joiner.on(",").join(assetIds);
        metadataStorage.storeColuAssetIds(all);
    }

    public WalletManager.State getState() {
        return state;
    }

    public WapiClient getWapi() {
        return mgr.getWapi();
    }

    public boolean hasAccountWithType(Address address, ColuAccount.ColuAssetType type) {
        for (WalletAccount account : getAccounts().values()) {
            if (account instanceof ColuAccount
                    && ((ColuAccount) account).getColuAsset().assetType == type
                    && ((ColuAccount) account).getAddress().equals(address)) {
                return true;
            } else if (account instanceof SingleAddressAccount
                    && type == null
                    && ((SingleAddressAccount) account).getAddress().equals(address)) {
                return true;
            }
        }
        return false;
    }

    public Transaction signTransaction(ColuBroadcastTxHex.Json txid, ColuAccount coluAccount) {
        if (txid == null) {
            Log.e(TAG, "signTransaction: No transaction to sign !");
            return null;
        }
        if (coluAccount == null) {
            Log.e(TAG, "signTransaction: No colu account associated to transaction to sign !");
            return null;
        }

        // use bitcoinj classes and two methods above to generate signatures
        // and sign transaction
        // then convert to mycelium wallet transaction format
        // Step 1: map to bitcoinj classes

        // DEV only 1 key
        byte[] txBytes;

        try {
            txBytes = Hex.decodeHex(txid.txHex.toCharArray());
        } catch (org.apache.commons.codec.DecoderException e) {
            Log.e(TAG, "signTransaction: exception while decoding transaction hex code.");
            return null;
        }

        if (txBytes == null) {
            Log.e(TAG, "signTransaction: failed to decode transaction hex code.");
            return null;
        }

        org.bitcoinj.core.Transaction signTx = new org.bitcoinj.core.Transaction(this.netParams, txBytes);

        byte[] privateKeyBytes = coluAccount.getPrivateKey().getPrivateKeyBytes();
        byte[] publicKeyBytes = coluAccount.getPrivateKey().getPublicKey().getPublicKeyBytes();
        ECKey ecKey = ECKey.fromPrivateAndPrecalculatedPublic(privateKeyBytes, publicKeyBytes);

        Script inputScript = ScriptBuilder.createOutputScript(ecKey.toAddress(this.netParams));

        for (int i = 0; i < signTx.getInputs().size(); i++) {
            TransactionSignature signature = signTx.calculateSignature(i, ecKey, inputScript, org.bitcoinj.core.Transaction.SigHash.ALL, false);
            Script scriptSig = ScriptBuilder.createInputScript(signature, ecKey);
            signTx.getInput(i).setScriptSig(scriptSig);
        }

        byte[] signedTransactionBytes = signTx.bitcoinSerialize();
        Transaction signedBitlibTransaction;
        try {
            signedBitlibTransaction = Transaction.fromBytes(signedTransactionBytes);
        } catch (Transaction.TransactionParsingException e) {
            Log.e(TAG, "signTransaction: Error parsing bitcoinj transaction ! msg: " + e.getMessage());
            return null;
        }
        return signedBitlibTransaction;
    }

    public ColuBroadcastTxHex.Json prepareColuTx(Address _receivingAddress,
                                                 ExactCurrencyValue nativeAmount,
                                                 ColuAccount coluAccount,
                                                 long feePerKb) {

        if (_receivingAddress != null && nativeAmount != null) {
            List<Address> srcList = coluAccount.getSendingAddresses();
            try {
                ColuBroadcastTxHex.Json txid = coluClient.prepareTransaction(_receivingAddress, srcList, nativeAmount, coluAccount, getColuTransactionFee(feePerKb));

                if (txid != null) {
                    return txid;
                } else {
                    Log.e(TAG, "Did not receive unsigned transaction from colu server.");
                }
            } catch (IOException e) {
                Log.e(TAG, "prepareColuTx interrupted with IOException. Message: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "prepareColuTx _receivingAddress or nativeAmount is null !");
        }
        return null;
    }

    public boolean broadcastTransaction(Transaction coluSignedTransaction) {
        String coluSignedTransactionStr = coluSignedTransaction.toString();
        if (coluSignedTransactionStr != null && !coluSignedTransactionStr.isEmpty()) {
            try {
                ColuBroadcastTxId.Json txJson = coluClient.broadcastTransaction(coluSignedTransaction);
                if (txJson != null) {
                    return true;
                } else {
                    Log.w(TAG, "broadcastTransaction: no txid returned !");
                }
            } catch (IOException e) {
                Log.e(TAG, "broadcastTransaction: encountered IOException: " + e.getMessage());
            }
        }
        return false;
    }

    private void loadAccounts() {
        //TODO: migrate assets list from metadataStorage to backing as a cache table
        //TODO: auto-discover assets at load time by querying ColoredCoins servers instead on relying on local data
        loadSingleAddressAccounts();
        Iterable<String> assetsId = metadataStorage.getColuAssetIds();
        for (String assetId : assetsId) {
            if (!Strings.isNullOrEmpty(assetId)) {
                ColuAccount.ColuAsset assetDefinition = ColuAccount.ColuAsset.getAssetMap().get(assetId);
                if (assetDefinition == null) {
                    Log.e(TAG, "loadAccounts: could not find asset with id " + assetId);
                } else {
                    UUID[] uuids = getAssetAccountUUIDs(assetDefinition);
                    if (uuids.length > 0) {
                        for (UUID uuid : uuids) {
                            loadColuAccount(assetDefinition, uuid);
                        }
                    }
                }
            }
        }

        // if there were no accounts active, try to fetch the balance anyhow and activate
        // all accounts with a balance > 0
        // but do it in background, as this function gets called via the constructor, which
        // gets called in the MbwManager constructor
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (!isNetworkConnected) {
                    return;
                }
                scanForAccounts(SyncMode.FULL_SYNC_ALL_ACCOUNTS);
            }
        });
    }

    public void setNetworkConnected(boolean networkConnected) {
        isNetworkConnected = networkConnected;
    }

    class CreatedAccountInfo {
        public UUID id;
        AccountBacking accountBacking;
    }
    /**
     * Create a new account using a single private key and address
     *
     * @param privateKey key the private key to use
     * @param cipher     the cipher used to encrypt the private key. Must be the same
     *                   cipher as the one used by the secure storage instance
     * @return the ID of the new account
     */
    private CreatedAccountInfo createSingleAddressAccount(InMemoryPrivateKey privateKey, KeyCipher cipher) throws InvalidKeyCipher {
        PublicKey publicKey = privateKey.getPublicKey();
        Address address = publicKey.toAddress(_network, AddressType.P2PKH);
        PublicPrivateKeyStore store = new PublicPrivateKeyStore(_secureKeyValueStore);
        store.setPrivateKey(address, privateKey, cipher);
        return createSingleAddressAccount(address);
    }

    /**
     * Create a new read-only account using a single address
     *
     * @param address the address to use
     * @return the ID of the new account
     */
    private CreatedAccountInfo createSingleAddressAccount(Address address) {
        CreatedAccountInfo createdAccountInfo = new CreatedAccountInfo();
        createdAccountInfo.id = SingleAddressAccount.calculateId(address);
        _backing.beginTransaction();
        try {
            SingleAddressAccountContext singleAccountContext = new SingleAddressAccountContext(createdAccountInfo.id,
                    ImmutableMap.of(address.getType(), address), false, 0, address.getType());
            _backing.createSingleAddressAccountContext(singleAccountContext);
            SingleAddressAccountBacking accountBacking = checkNotNull(_backing.getSingleAddressAccountBacking(singleAccountContext.getId()));
            singleAccountContext.persist(accountBacking);
            createdAccountInfo.accountBacking = accountBacking;
            _backing.setTransactionSuccessful();
        } finally {
            _backing.endTransaction();
        }
        return createdAccountInfo;
    }

    public AssetMetadata getAssetMetadata(ColuAccount.ColuAssetType coluAssetType) {
        return assetsMetadata.get(coluAssetType);
    }

    // convenience method to make it easier to migrate from metadataStorage to backing later on
    private UUID[] getAssetAccountUUIDs(ColuAccount.ColuAsset coluAsset) {
        return metadataStorage.getColuAssetUUIDs(coluAsset.id);
    }

    private void addAssetAccountUUID(ColuAccount.ColuAsset coluAsset, UUID uuid) {
        metadataStorage.addColuAssetUUIDs(coluAsset.id, uuid);
    }

    private void removeAssetAccountUUID(ColuAccount.ColuAsset coluAsset, UUID uuid) {
        metadataStorage.removeColuAssetUUIDs(coluAsset.id, uuid);
    }

    private void storeColuBalance(UUID coluAccountUuid, String balance) {
        metadataStorage.storeColuBalance(coluAccountUuid, balance);
    }

    Optional<String> getColuBalance(UUID coluAccountUuid) {
        return metadataStorage.getColuBalance(coluAccountUuid);
    }

    public void forgetPrivateKey(ColuAccount account) {
        try {
            SingleAddressAccount acc = account.getLinkedAccount();
            if (acc != null)
                acc.forgetPrivateKey(AesKeyCipher.defaultKeyCipher());
            account.forgetPrivateKey();
        } catch (InvalidKeyCipher e) {
            Log.e(TAG, e.toString());
        }
    }

    public void deleteAccount(ColuAccount account) {
        // find asset
        // disable account
        // remove key from storage
        UUID uuid = account.getLinkedAccount().getId();
        SingleAddressAccount acc = (SingleAddressAccount) _walletAccounts.get(uuid);
        try {
            acc.forgetPrivateKey(AesKeyCipher.defaultKeyCipher());
            _walletAccounts.remove(uuid);
            coluAccounts.remove(account.getId());
            removeAssetAccountUUID(account.getColuAsset(), uuid);
            saveEnabledAssetIds();
        } catch (InvalidKeyCipher e) {
            Log.e(TAG, e.toString());
        }
    }

    private void createColuAccountLabel(ColuAccount account) {
        String proposedLabel;
        int i = 1;

        while (i < MAX_ACCOUNTS_NUMBER){
            proposedLabel = account.getDefaultLabel() + " " + Integer.toString(i);

            boolean foundExistingLabel = false;
            for (ColuAccount coluAccount : coluAccounts.values()) {
                if (!coluAccount.getColuAsset().equals(account.getColuAsset()) || coluAccount.getLabel() == null) {
                    continue;
                }
                String curLabel = coluAccount.getLabel();
                if (proposedLabel.equals(curLabel)) {
                    foundExistingLabel = true;
                    break;
                }
            }

            if (!foundExistingLabel) {
                account.setLabel(proposedLabel);
                metadataStorage.storeAccountLabel(account.getId(), proposedLabel);
                break;
            }

            i++;
        }
    }

    private boolean isAddressInUse(Address address) {
        Optional<UUID> accountId = mgr.getAccountId(address, null);
        return accountId.isPresent();
    }

    private ColuAccount createReadOnlyColuAccount(ColuAccount.ColuAsset coluAsset, Address address) {
        CreatedAccountInfo createdAccountInfo = createSingleAddressAccount(address);
        addAssetAccountUUID(coluAsset, createdAccountInfo.id);

        ColuAccount account = new ColuAccount(
                ColuManager.this, createdAccountInfo.accountBacking, metadataStorage, address,
                coluAsset
        );

        coluAccounts.put(account.getId(), account);
        createColuAccountLabel(account);

        loadSingleAddressAccounts();  // reload account from mycelium secure store

        return account;
    }

    private void loadColuAccount(ColuAccount.ColuAsset coluAsset, UUID uuid) {
        try {
            CreatedAccountInfo createdAccountInfo = new CreatedAccountInfo();
            SingleAddressAccount singleAddressAccount;

            if (!_walletAccounts.containsKey(uuid)) {
                return;
            }
            createdAccountInfo.id = uuid;
            singleAddressAccount = (SingleAddressAccount) _walletAccounts.get(createdAccountInfo.id);
            InMemoryPrivateKey accountKey = singleAddressAccount.getPrivateKey(AesKeyCipher.defaultKeyCipher());
            createdAccountInfo.accountBacking = singleAddressAccount.getAccountBacking();

            ColuAccount account;

            if (accountKey == null) {
                Address address = singleAddressAccount.getAddress(AddressType.P2PKH);
                // fix NPE crash from console, maybe problem in db migration
                if (address == null) {
                    return;
                }
                account = new ColuAccount(
                        ColuManager.this, createdAccountInfo.accountBacking, metadataStorage, address,
                        coluAsset);
            } else {
                account = new ColuAccount(
                        ColuManager.this, createdAccountInfo.accountBacking, metadataStorage, accountKey,
                        coluAsset
                );
            }

            coluAccounts.put(account.getId(), account);
            loadSingleAddressAccounts();  // reload account from mycelium secure store

            account.setLabel(metadataStorage.getLabelByAccount(account.getId()));
        } catch (InvalidKeyCipher e) {
            Log.e(TAG, e.toString());
        }
    }

    private ColuAccount createAccount(ColuAccount.ColuAsset coluAsset, InMemoryPrivateKey importKey) {
        if (coluAsset == null) {
            Log.e(TAG, "createAccount called without asset !");
            return null;
        }

        InMemoryPrivateKey accountKey;
        CreatedAccountInfo createdAccountInfo;

        try {
            if (importKey != null) {
                accountKey = importKey;
            } else {
                accountKey = new InMemoryPrivateKey(mgr.getRandomSource(), true);
            }
            createdAccountInfo = createSingleAddressAccount(accountKey, AesKeyCipher.defaultKeyCipher());
            addAssetAccountUUID(coluAsset, createdAccountInfo.id);
        } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
            throw new RuntimeException(invalidKeyCipher);
        }

        ColuAccount account = new ColuAccount(
                ColuManager.this, createdAccountInfo.accountBacking, metadataStorage, accountKey,
                coluAsset
        );

        coluAccounts.put(account.getId(), account);
        createColuAccountLabel(account);

        loadSingleAddressAccounts();  // reload account from mycelium secure store

        return account;
    }

    private void loadSingleAddressAccounts() {
        List<SingleAddressAccountContext> contexts = _backing.loadSingleAddressAccountContexts();
        for (SingleAddressAccountContext context : contexts) {
            PublicPrivateKeyStore store = new PublicPrivateKeyStore(_secureKeyValueStore);
            SingleAddressAccountBacking accountBacking = checkNotNull(_backing.getSingleAddressAccountBacking(context.getId()));
            SingleAddressAccount account = new SingleAddressAccount(context, store, _network, accountBacking, getWapi(),
                    new Reference<>(ChangeAddressMode.PRIVACY), false);
            addAccount(account);

            for(ColuAccount coluAccount : coluAccounts.values()) {
                if (coluAccount.getAddress().equals(account.getAddress(AddressType.P2PKH))) {
                    coluAccount.setLinkedAccount(account);
                    String accountLabel = metadataStorage.getLabelByAccount(coluAccount.getId());
                    metadataStorage.storeAccountLabel(account.getId(), accountLabel + " Bitcoin");
                    break;
                }
            }
        }
    }

    private void addAccount(AbstractAccount account) {
        synchronized (_walletAccounts) {
            _walletAccounts.put(account.getId(), account);
        }
    }

    public NetworkParameters getNetwork() {
        return env.getNetwork();
    }

    public UUID enableReadOnlyAsset(ColuAccount.ColuAsset coluAsset, Address address) {
        //Make check to ensure the address is not in use
        if (isAddressInUse(address)) {
            return null;
        }
        UUID uuid = ColuAccount.getGuidForAsset(coluAsset, address.getAllAddressBytes());

        if (coluAccounts.containsKey(uuid)) {
            return uuid;
        }

        ColuAccount newAccount = createReadOnlyColuAccount(coluAsset, address);

        // broadcast event, so that the UI shows the newly added account
        handler.post(new Runnable() {
            @Override
            public void run() {
                eventBus.post(new ExtraAccountsChanged());
            }
        });

        // and save it
        saveEnabledAssetIds();

        return newAccount.getId();
    }

    // enables account associated with asset
    public UUID enableAsset(ColuAccount.ColuAsset coluAsset, InMemoryPrivateKey key) {
        //Make check to ensure the address is not in use
        if (key != null && isAddressInUse(key.getPublicKey().toAddress(getNetwork(), AddressType.P2PKH))) {
            return null;
        }

        if (key != null) {
            UUID uuid = ColuAccount.getGuidForAsset(coluAsset, key.getPublicKey().toAddress(getNetwork(), AddressType.P2PKH).getAllAddressBytes());

            if (coluAccounts.containsKey(uuid)) {
                return uuid;
            }
        }

        ColuAccount newAccount = checkNotNull(createAccount(coluAsset, key));

        // broadcast event, so that the UI shows the newly added account
        handler.post(new Runnable() {
            @Override
            public void run() {
                eventBus.post(new ExtraAccountsChanged());
            }
        });

        // and save it
        saveEnabledAssetIds();

        return newAccount.getId();
    }

    // getAccounts is called by WalletManager
    @Override
    public Map<UUID, WalletAccount> getAccounts() {
        Map<UUID, WalletAccount> allAccounts = new HashMap<>();
        allAccounts.putAll(coluAccounts);

        for (ColuAccount coluAccount : coluAccounts.values()) {
            SingleAddressAccount linkedAccount = coluAccount.getLinkedAccount();
            if (linkedAccount != null) {
                allAccounts.put(linkedAccount.getId(), linkedAccount);
            }
        }
        return allAccounts;
    }

    @Override
    public ColuAccount getAccount(UUID id) {
        return coluAccounts.get(id);
    }

    // Updates balances for colu accounts
    public void scanForAccounts(SyncMode mode) {
        // We run Colu synchronization if we need to sync all accounts or the only active account
        // should be synced and it has COLU type
        if (mode.onlyActiveAccount && mgr.getSelectedAccount().getType() != WalletAccount.Type.COLU) {
            return;
        }

        try {
            // If we need to handle only active account - get metadata for it only
            if (mode.onlyActiveAccount) {
                ColuAccount account = (ColuAccount)mgr.getSelectedAccount();
                ColuAccount.ColuAsset asset = account.getColuAsset();
                AssetMetadata assetMetadata = coluClient.getMetadata(asset.id);
                assetsMetadata.put(asset.assetType, assetMetadata);
                metadataStorage.storeColuAssetCoinSupply(asset.id, assetMetadata.getTotalSupply());
            }
            else {
                // Sync all accounts
                for (ColuAccount.ColuAssetType assetType : ColuAccount.ColuAssetType.values()) {
                    String id = checkNotNull(ColuAccount.ColuAsset.getByType(assetType)).id;
                    AssetMetadata assetMetadata = coluClient.getMetadata(id);
                    assetsMetadata.put(assetType, assetMetadata);
                    metadataStorage.storeColuAssetCoinSupply(id, assetMetadata.getTotalSupply());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "error while get asset metadata: " + e.getMessage());
        }
        try {
            getBalances(mode);
        } catch (Exception e) {
            Log.e(TAG, "error while scanning for accounts: " + e.getMessage());
        }
    }

    @Override
    public boolean hasAccount(UUID uuid) {
        return coluAccounts.containsKey(uuid);
    }

    private ColuClient createClient() {
        return new ColuClient(_network);
    }

    private void getBalances(SyncMode syncMode) throws Exception {
        Log.e(TAG, "ColuManager::getBalances start");

        if (syncMode.onlyActiveAccount) {
            ColuAccount account = (ColuAccount)mgr.getSelectedAccount();
            account.doSynchronization(SyncMode.NORMAL);
        } else {
            for (HashMap.Entry entry : coluAccounts.entrySet()) {
                Log.e(TAG, "ColuManager::getBalances in loop");
                UUID uuid = (UUID) entry.getKey();
                ColuAccount account = (ColuAccount) entry.getValue();
                Log.e(TAG, "ColuManager::getBalances in loop uuid=" + uuid.toString() + " asset " + account.getColuAsset().id);

                account.doSynchronization(SyncMode.NORMAL);
            }   // for loop over accounts
        }
    }

    void updateAccountBalance(ColuAccount account) throws IOException {
        Optional<Address> address = account.getReceivingAddress(); // for single address account
        if (!address.isPresent()) {
            return;
        }
        Log.e(TAG, "getBalances: address=" + address.get().toString());


        // collect all tx history at that address from mycelium wapi server (non colored)
        LinkedList<com.mrd.bitlib.util.Sha256Hash> allTxidList = new LinkedList<>();

        WapiClient wapiClient = getWapi();
        if (wapiClient == null) {
            Log.e(TAG, "getTransactionSummaries: wapiClient not found !");
            return;
        }

        // retrieve history from colu server
        AddressTransactionsInfo.Json addressInfoWithTransactions = coluClient.getAddressTransactions(address.get());
        if (addressInfoWithTransactions == null) {
            return;
        }

        getAddressBalance(addressInfoWithTransactions, account);

        if (addressInfoWithTransactions.transactions != null && addressInfoWithTransactions.transactions.size() > 0) {
            account.setHistory(addressInfoWithTransactions.transactions);
            for (Tx.Json historyTx : addressInfoWithTransactions.transactions) {
                allTxidList.add(com.mrd.bitlib.util.Sha256Hash.fromString(historyTx.txid));
            }
        }

        try {
            QueryUnspentOutputsResponse unspentOutputResponse = wapiClient.queryUnspentOutputs(new QueryUnspentOutputsRequest(Wapi.VERSION, account.getSendingAddresses()))
                    .getResult();
            account.setBlockChainHeight(unspentOutputResponse.height);
        } catch (WapiException e) {
            Log.w(TAG, "Warning ! Error accessing unspent outputs response: " + e.getMessage());
        }

        account.setUtxos(addressInfoWithTransactions.utxos);

        // start additional code to retrieve extended info from wapi server
        GetTransactionsRequest trRequest = new GetTransactionsRequest(2, allTxidList);
        WapiResponse<GetTransactionsResponse> wapiResponse = wapiClient.getTransactions(trRequest);
        GetTransactionsResponse trResponse = null;
        if (wapiResponse == null) {
            return;
        }
        try {
            trResponse = wapiResponse.getResult();
        } catch (Exception e) {
            Log.w(TAG, "Warning ! Error accessing transaction response: " + e.getMessage());
        }

        if (trResponse != null && trResponse.transactions != null) {
            account.setHistoryTxInfos(trResponse.transactions);
        }
    }

    private void getAddressBalance(AddressTransactionsInfo.Json atInfo, ColuAccount account) {
        long assetConfirmedAmount = 0;
        long assetReceivingAmount = 0;
        long assetSendingAmount = 0;

        int assetScale = 0;
        long satoshiAmount = 0;
        long satoshiBtcOnlyAmount = 0;

        for(Tx.Json tx : atInfo.transactions) {
            if (tx.blockheight != -1) {
                continue;
            }

            boolean isInitiatedByMe = false;

            for(Vin.Json vin : tx.vin) {
                if (account.ownAddress(vin.previousOutput.addresses)) {
                    isInitiatedByMe = true;
                    break;
                }
            }
            for(Vout.Json vout : tx.vout) {
                if (vout.scriptPubKey.addresses != null)
                    if (!account.ownAddress(vout.scriptPubKey.addresses)) {
                        for (Asset.Json asset : vout.assets) {
                            if (!asset.assetId.equals(account.getColuAsset().id)) {
                                continue;
                            }
                            if (isInitiatedByMe) {
                                assetSendingAmount += asset.amount;
                                assetScale = asset.divisibility;
                            }
                        }
                    } else {
                        for (Asset.Json asset : vout.assets) {
                            if (!asset.assetId.equals(account.getColuAsset().id)) {
                                continue;
                            }
                            if (!isInitiatedByMe) {
                                assetReceivingAmount += asset.amount;
                                assetScale = asset.divisibility;
                            }
                        }
                    }
            }
        }

        for(Utxo.Json utxo : atInfo.utxos) {
            satoshiAmount = satoshiAmount + utxo.value;

            if (utxo.assets.size() == 0) {
                satoshiBtcOnlyAmount += utxo.value;
            }

            for (Asset.Json txidAsset : utxo.assets) {
                if (txidAsset.assetId.equals(account.getColuAsset().id)) {
                    assetConfirmedAmount += txidAsset.amount;
                    assetScale = txidAsset.divisibility;
                }
            }
        }

        // set balance in account
        // stripTrailingZeros can't strip 0.0000 to 0 (java known bug), so we need fix it
        BigDecimal assetConfirmedBalance = assetConfirmedAmount == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(assetConfirmedAmount, assetScale).stripTrailingZeros();
        BigDecimal assetReceivingBalance = assetReceivingAmount == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(assetReceivingAmount, assetScale).stripTrailingZeros();
        BigDecimal assetSendingBalance = assetSendingAmount == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(assetSendingAmount, assetScale).stripTrailingZeros();
        ExactCurrencyValue confirmed = ExactCurrencyValue.from(assetConfirmedBalance, account.getColuAsset().name);
        ExactCurrencyValue sending = ExactCurrencyValue.from(assetSendingBalance, account.getColuAsset().name);
        ExactCurrencyValue receiving = ExactCurrencyValue.from(assetReceivingBalance, account.getColuAsset().name);
        CurrencyBasedBalance newBalanceFiat = new CurrencyBasedBalance(confirmed, sending, receiving);
        account.setBalanceFiat(newBalanceFiat);
        account.setBalanceSatoshi(satoshiAmount);
        account.setBtcOnlyAmount(satoshiBtcOnlyAmount);

        storeColuBalance(account.getUuid(), assetConfirmedBalance.toString());
    }

    public ColuClient getClient() {
        return coluClient;
    }

    public boolean isColuAsset(String assetName) {
        for (String asset : ColuAccount.ColuAsset.getAllAssetNames()) {
            if (asset.contentEquals(assetName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isColoredAddress(Address address) {
        try {
            return coluClient.getAddressTransactions(address).numOfTransactions != 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Set<ColuAccount.ColuAsset> getColuAddressAssets(Address address) throws IOException {

        Set<ColuAccount.ColuAsset> assetsList = new HashSet<>();

        AddressInfo.Json addressInfo = coluClient.getBalance(address);

        if (addressInfo != null) {
            if (addressInfo.utxos != null) {
                for (Utxo.Json utxo : addressInfo.utxos) {
                    // adding utxo to list of txid list request
                    for (Asset.Json txidAsset : utxo.assets) {
                        for (String knownAssetId : ColuAccount.ColuAsset.getAssetMap().keySet()) {
                            if (txidAsset.assetId.equals(knownAssetId)) {
                                ColuAccount.ColuAsset asset =  ColuAccount.ColuAsset.getAssetMap().get(knownAssetId);
                                assetsList.add(asset);
                            }
                        }
                    }
                }
            }
        }
        return assetsList;
    }

    public void startSynchronization(SyncMode mode) {
        final SyncMode syncMode = mode;
        if (!isNetworkConnected) {
            return;
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                eventTranslator.onWalletStateChanged(null, state = WalletManager.State.SYNCHRONIZING);
            }

            @Override
            protected Void doInBackground(Void... voids) {
                scanForAccounts(syncMode);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                eventBus.post(new BalanceChanged(null));
                eventTranslator.onWalletStateChanged(null, state = WalletManager.State.READY);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
