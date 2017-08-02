package com.mycelium.wallet.colu;

import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.Transaction;
import com.mycelium.WapiLogger;
import com.mycelium.wallet.ExchangeRateManager;
import com.mycelium.wallet.MbwEnvironment;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.activity.util.BlockExplorer;
import com.mycelium.wallet.colu.json.AddressInfo;
import com.mycelium.wallet.colu.json.AddressTransactionsInfo;
import com.mycelium.wallet.colu.json.Asset;
import com.mycelium.wallet.colu.json.ColuBroadcastTxid;
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
import com.mycelium.wapi.wallet.AbstractAccount;
import com.mycelium.wapi.wallet.AccountBacking;
import com.mycelium.wapi.wallet.AccountProvider;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher;
import com.mycelium.wapi.wallet.SecureKeyValueStore;
import com.mycelium.wapi.wallet.SingleAddressAccountBacking;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ColuManager implements AccountProvider {

    private static final String TAG = "ColuManager";

    private final MbwEnvironment env;
    private final MbwManager mgr;
    private final Bus eventBus;
    private final Handler handler;
    private final ExchangeRateManager exchangeRateManager;
    private final ColuClient coluClient;
    private final Map<UUID, WalletAccount> _walletAccounts;
    private final MetadataStorage metadataStorage;
    private SqliteColuManagerBacking _backing;
    private final WapiLogger logger;
    private final HashMap<UUID, ColuAccount> coluAccounts;
    private NetworkParameters _network;
    private final SecureKeyValueStore _secureKeyValueStore;
    private WalletManager.State state;

    public static final int TIME_INTERVAL_BETWEEN_BALANCE_FUNDING_CHECKS = 50;
    public static final int DUST_OUTPUT_SIZE = 600;
    public static final int METADATA_OUTPUT_SIZE = 1;

    final org.bitcoinj.core.NetworkParameters netParams;
    final org.bitcoinj.core.Context context;
    private EventTranslator eventTranslator;

    public ColuManager(SecureKeyValueStore secureKeyValueStore, SqliteColuManagerBacking backing,
                       MbwManager manager, MbwEnvironment env,
                       final Bus eventBus, Handler handler,
                       MetadataStorage metadataStorage, ExchangeRateManager exchangeRateManager,
                       WapiLogger logger) {
        this._secureKeyValueStore = secureKeyValueStore;
        this._backing = backing;
        this.env = env;
        this.mgr = manager;
        this.eventBus = eventBus;
        this.handler = handler;
        this.metadataStorage = metadataStorage;
        this.exchangeRateManager = exchangeRateManager;
        this.logger = logger;
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

        this.context = org.bitcoinj.core.Context.getOrCreate(netParams);
        this._walletAccounts = Maps.newHashMap();

        coluClient = createClient();

        handler.post(new Runnable() {
            @Override
            public void run() {
                eventBus.register(ColuManager.this);
            }
        });
        coluAccounts = new HashMap<>();
        loadAccounts();
    }

    public BlockExplorer getBlockExplorer() {
        String baseUrl;
        if (this._network.isProdnet()) {
            baseUrl = "http://coloredcoins.org/explorer/";
        } else if (this._network.isTestnet()) {
            baseUrl = "http://coloredcoins.org/explorer/testnet/";
        } else {
            baseUrl = "http://coloredcoins.org/explorer/testnet/";
        }

        return new BlockExplorer("CCO", "coloredcoins.org"
                , baseUrl + "address/", baseUrl + "tx/"
                , baseUrl + "address/", baseUrl + "tx/");
    }


    private void saveEnabledAssetIds() {
        String all = Joiner.on(",").join(Iterables.transform(coluAccounts.values(), new Function<ColuAccount, String>() {
            @Nullable
            @Override
            public String apply(@Nullable ColuAccount input) {
                Preconditions.checkNotNull(input);
                return input.getColuAsset().id;
            }
        }));
        metadataStorage.storeColuAssetIds(all);
    }

    public WalletManager.State getState() {
        return state;
    }

    public WapiClient getWapi() {
        return mgr.getWapi();
    }

    public boolean hasAccountWithType(ColuAccount.ColuAssetType type) {
        for (WalletAccount account : getAccounts().values()) {
            if (account instanceof ColuAccount && ((ColuAccount) account).getColuAsset().assetType == type) return true;
        }
        return false;
    }

    public Transaction signTransaction(ColuBroadcastTxid.Json txid, ColuAccount coluAccount) {

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
        Log.d(TAG, "signTransaction: Starting signTransaction process and mapping data to bitcoinj classes");
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
        Log.d(TAG, "signTransaction: Parsed bitlib transaction: " + signedBitlibTransaction.toString());
        return signedBitlibTransaction;
    }

    public ColuBroadcastTxid.Json prepareColuTx(Address _receivingAddress,
                                                ExactCurrencyValue nativeAmount,
                                                ColuAccount coluAccount,
                                                long feePerKb) {

        if (_receivingAddress != null && nativeAmount != null) {
            Log.d(TAG, "prepareColuTx receivingAddress=" + _receivingAddress.toString()
                    + " nativeAmount=" + nativeAmount.toString());
            List<Address> srcList = coluAccount.getSendingAddresses();
            if (srcList == null) {
                Log.e(TAG, "Error: srcList is empty.");
            } else if (srcList.size() == 0) {
                Log.e(TAG, "Error: srcList has size 0.");
            }

            try {
                ColuBroadcastTxid.Json txid = coluClient.prepareTransaction(_receivingAddress, srcList, nativeAmount, coluAccount, feePerKb);

                if (txid != null) {
                    Log.d(TAG, "Received unsigned transaction: " + txid.txHex);
                    return txid;
                } else {
                    Log.e(TAG, "Did not receive unsigned transaction from colu server.");
                }
            } catch (IOException e) {
                Log.d(TAG, "prepareColuTx interrupted with IOException. Message: " + e.getMessage());
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
                Log.d(TAG, "Broadcasting colu tx " + coluSignedTransactionStr);
                ColuBroadcastTxid.Json txid = coluClient.broadcastTransaction(coluSignedTransaction);
                if (txid != null) {
                    Log.d(TAG, "broadcastTransaction: broadcast txid " + txid.txHex);
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
        Log.d(TAG, "ColuAssetIds=" + metadataStorage.getColuAssetIds());
        //TODO: migrate assets list from metadataStorage to backing as a cache table
        //TODO: auto-discover assets at load time by querying ColoredCoins servers instead on relying on local data
        loadSingleAddressAccounts();
        Iterable<String> assetsId = Splitter.on(",").split(metadataStorage.getColuAssetIds());
        for (String assetId : assetsId) {
            if (!Strings.isNullOrEmpty(assetId)) {
                Log.d(TAG, "loadAccounts: assetid=" + assetId);
                ColuAccount.ColuAsset assetDefinition = ColuAccount.ColuAsset.getAssetMap(getNetwork()).get(assetId);
                if (assetDefinition == null) {
                    Log.e(TAG, "loadAccounts: could not find asset with id " + assetId);
                } else {
                    if (createAccount(assetDefinition, null) != null) {
                        Log.d(TAG, "ColuManager: loaded asset " + assetDefinition.id);
                    }
                }
            }
        }

        // if there were no accounts active, try to fetch the balance anyhow and activate
        // all accounts with a balance > 0
        // but do it in background, as this function gets called via the constructor, which
        // gets called in the MbwManager constructor
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                scanForAccounts();
                return null;
            }
        }.execute();
    }

    class CreatedAccountInfo {
        public UUID id;
        public AccountBacking accountBacking;
    }
    /**
     * Create a new account using a single private key and address
     *
     * @param privateKey key the private key to use
     * @param cipher     the cipher used to encrypt the private key. Must be the same
     *                   cipher as the one used by the secure storage instance
     * @return the ID of the new account
     * @throws InvalidKeyCipher
     */
    public CreatedAccountInfo createSingleAddressAccount(InMemoryPrivateKey privateKey, KeyCipher cipher) throws InvalidKeyCipher {
        if (privateKey == null) {
            Log.d(TAG, "createSingleAddressAccount: null private key !");
        }
        PublicKey publicKey = privateKey.getPublicKey();
        Address address = publicKey.toAddress(_network);
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
    public CreatedAccountInfo createSingleAddressAccount(Address address) {
        CreatedAccountInfo createdAccountInfo = new CreatedAccountInfo();
        createdAccountInfo.id = SingleAddressAccount.calculateId(address);
        _backing.beginTransaction();
        try {
            SingleAddressAccountContext singleAccountContext = new SingleAddressAccountContext(createdAccountInfo.id, address, false, 0);
            _backing.createSingleAddressAccountContext(singleAccountContext);
            SingleAddressAccountBacking accountBacking = _backing.getSingleAddressAccountBacking(singleAccountContext.getId());
            Preconditions.checkNotNull(accountBacking);
            singleAccountContext.persist(accountBacking);
            createdAccountInfo.accountBacking = accountBacking;
            _backing.setTransactionSuccessful();
        } finally {
            _backing.endTransaction();
        }
        return createdAccountInfo;
    }

    // convenience method to make it easier to migrate from metadataStorage to backing later on
    public UUID getAssetAccountUUID(ColuAccount.ColuAsset coluAsset) {
        Log.d(TAG, "Looking for UUID associated to coluAsset " + coluAsset.id);
        Optional<UUID> uuid = metadataStorage.getColuUUID(coluAsset.id);
        if (uuid.isPresent()) {
            Log.d(TAG, "Found UUID for asset: " + uuid.get().toString());
            return uuid.get();
        }
        Log.d(TAG, "No UUID found for asset " + coluAsset.id);
        return null;
    }

    public void setAssetAccountUUID(ColuAccount.ColuAsset coluAsset, UUID uuid) {
        Log.d(TAG, "Associating " + uuid.toString() + " with asset " + coluAsset.id);
        metadataStorage.storeColuUUID(coluAsset.id, uuid);
    }

    public void deleteAssetAccountUUID(ColuAccount.ColuAsset coluAsset) {
        Log.d(TAG, "Deleting UUID association for " + coluAsset.id);
        metadataStorage.deleteColuUUID(coluAsset.id);
    }

    public void deleteAccount(ColuAccount account) {
        Log.d(TAG, "deleteAccount: attempting to delete account.");
        // find asset
        // disable account
        // remove key from storage
        UUID uuid = getAssetAccountUUID(account.getColuAsset());
        SingleAddressAccount acc = (SingleAddressAccount) _walletAccounts.get(uuid);
        try {
            acc.forgetPrivateKey(AesKeyCipher.defaultKeyCipher());
            Log.d(TAG, "deleteAccount: forgot private key.");
            Log.d(TAG, " _walletAccounts.size=" + _walletAccounts.size() + " coluAccounts.size=" + coluAccounts.size());
            _walletAccounts.remove(uuid);
            coluAccounts.remove(account.getId());
            Log.d(TAG, " _walletAccounts.size=" + _walletAccounts.size() + " coluAccounts.size=" + coluAccounts.size());
            deleteAssetAccountUUID(account.getColuAsset());
            saveEnabledAssetIds();
        } catch (InvalidKeyCipher e) {
            Log.e(TAG, e.toString());
        }
    }

    // create OR load account if a key already exists
// converts old dev key storage into backend storage
    private ColuAccount createAccount(ColuAccount.ColuAsset coluAsset, InMemoryPrivateKey importKey) {
        if (coluAsset == null) {
            Log.e(TAG, "createAccount called without asset !");
            return null;
        }

        InMemoryPrivateKey accountKey = null;
        CreatedAccountInfo createdAccountInfo = new CreatedAccountInfo();

        // case 1: check if private key already exists in secure store for this asset
        if (_walletAccounts.containsKey(getAssetAccountUUID(coluAsset))) {
            // account exists in mycelium colu keystore, use it to create ColuAccount object
            Log.d(TAG, "Found UUID in metatadaStorage mapping for asset and loaded key from mycelium secure store.");
            try {
                createdAccountInfo.id = getAssetAccountUUID(coluAsset);
                SingleAddressAccount account = (SingleAddressAccount) _walletAccounts.get(createdAccountInfo.id);
                accountKey = account.getPrivateKey(AesKeyCipher.defaultKeyCipher());
                createdAccountInfo.accountBacking = account.getAccountBacking();
            } catch (InvalidKeyCipher e) {
                Log.e(TAG, e.toString());
            }
        }

        // case 3: new account or import account
        if (accountKey == null) {
            try {
                if (importKey != null) {
                    accountKey = importKey;
                } else {
                    accountKey = new InMemoryPrivateKey(mgr.getRandomSource(), true);
                }
                createdAccountInfo = createSingleAddressAccount(accountKey, AesKeyCipher.defaultKeyCipher());
                setAssetAccountUUID(coluAsset, createdAccountInfo.id);
                Log.d(TAG, "createAccount: new key " + accountKey.getBase58EncodedPrivateKey(getNetwork()));
            } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
                throw new RuntimeException(invalidKeyCipher);
            }
        }

        ColuAccount account = new ColuAccount(
                ColuManager.this, createdAccountInfo.accountBacking, metadataStorage, accountKey,
                exchangeRateManager, handler, eventBus, logger, coluAsset
        );

        coluAccounts.put(account.getId(), account);
        loadSingleAddressAccounts();  // reload account from mycelium secure store
        // loaded account should be in the list
        if (_walletAccounts.containsKey(getAssetAccountUUID(coluAsset))) {
            Log.d(TAG, "createAccount: SUCCESS ! Key found in mycelium secure store.");
        } else {
            Log.d(TAG, "createAccount: Error, key not found in mycelium secure store.");
        }

        return account;
    }

    private void loadSingleAddressAccounts() {
        Log.d(TAG, "Loading single address accounts");
        List<SingleAddressAccountContext> contexts = _backing.loadSingleAddressAccountContexts();
        for (SingleAddressAccountContext context : contexts) {
            PublicPrivateKeyStore store = new PublicPrivateKeyStore(_secureKeyValueStore);
            SingleAddressAccountBacking accountBacking = _backing.getSingleAddressAccountBacking(context.getId());
            Preconditions.checkNotNull(accountBacking);
            SingleAddressAccount account = new SingleAddressAccount(context, store, _network, accountBacking, getWapi());
            addAccount(account);

            for(ColuAccount coluAccount : coluAccounts.values()) {
                if (coluAccount.getAddress().equals(account.getAddress())) {
                    coluAccount.setLinkedAccount(account);
                    break;
                }

            }
        }
    }

    public void addAccount(AbstractAccount account) {
        synchronized (_walletAccounts) {
            _walletAccounts.put(account.getId(), account);
            Log.d(TAG, "Account Added: " + account.getId());
        }
    }

    public NetworkParameters getNetwork() {
        return env.getNetwork();
    }

    // enables account associated with asset
    public UUID enableAsset(ColuAccount.ColuAsset coluAsset, InMemoryPrivateKey key) {
        // check if we already have it enabled
        ColuAccount account = getAccountForColuAsset(coluAsset);
        if (account != null) {
            return account.getId();
        }

        ColuAccount newAccount = createAccount(coluAsset, key);

        // check if we already have a label for this account, otherwise set the default one
        String label = metadataStorage.getLabelByAccount(newAccount.getId());
        if (Strings.isNullOrEmpty(label)) {
            metadataStorage.storeAccountLabel(newAccount.getId(), newAccount.getDefaultLabel());
        }

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

    @android.support.annotation.Nullable
    private ColuAccount getAccountForColuAsset(ColuAccount.ColuAsset asset) {
        for (ColuAccount account : coluAccounts.values()) {
            if (account.getColuAsset().equals(asset)) {
                return account;
            }
        }
        return null;
    }

    // getAccounts is called by WalletManager
    @Override
    public Map<UUID, WalletAccount> getAccounts() {
        Map<UUID, WalletAccount> allAccounts = new HashMap<>();
        allAccounts.putAll(coluAccounts);
        allAccounts.putAll(_walletAccounts);
        return allAccounts;
    }

    @Override
    public ColuAccount getAccount(UUID id) {
        return coluAccounts.get(id);
    }

    // this method updates balances for all colu accounts
    public boolean scanForAccounts() {
        try {
            getBalances();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "error while scanning for accounts: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean hasAccount(UUID uuid) {
        return coluAccounts.containsKey(uuid);
    }

    private ColuClient createClient() {
        return new ColuClient(_network);
    }

    public void getBalances() throws Exception {
        Log.e(TAG, "ColuManager::getBalances start");
        for (HashMap.Entry entry : coluAccounts.entrySet()) {
            Log.e(TAG, "ColuManager::getBalances in loop");
            UUID uuid = (UUID) entry.getKey();
            ColuAccount account = (ColuAccount) entry.getValue();
            Log.e(TAG, "ColuManager::getBalances in loop uuid=" + uuid.toString() + " asset " + account.getColuAsset().id);

            updateAccountBalance(account);
        }   // for loop over accounts
    }

    public void updateAccountBalance(ColuAccount account) throws IOException {
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

        Log.d(TAG, "Retrieving addressInfoWithTransactions");
        // retrieve history from colu server
        AddressTransactionsInfo.Json addressInfoWithTransactions = coluClient.getAddressTransactions(address.get());
        if (addressInfoWithTransactions == null) {
            Log.d(TAG, " addressInfoWithTransactios is null");
            return;
        }

        getAddressBalance(addressInfoWithTransactions, account);

        Log.d(TAG, "retrieved addressInfoWithTransactions");
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
            Log.d(TAG, "Warning ! Error accessing unspent outputs response: " + e.getMessage());
        }

        account.setUtxos(addressInfoWithTransactions.utxos);

        // start additional code to retrieve extended info from wapi server
        GetTransactionsRequest trRequest = new GetTransactionsRequest(2, allTxidList);
        WapiResponse<GetTransactionsResponse> wapiResponse = wapiClient.getTransactions(trRequest);
        GetTransactionsResponse trResponse = null;
        if (wapiResponse == null) {
            Log.d(TAG, "Warning ! Could not fetch wapiresponse. Some data may be unavailable.");
            return;
        }
        Log.d(TAG, "Received wapiResponse, extracting result");
        try {
            trResponse = wapiResponse.getResult();
        } catch (Exception e) {
            Log.d(TAG, "Warning ! Error accessing transaction response: " + e.getMessage());
        }

        if (trResponse != null && trResponse.transactions != null) {
            account.setHistoryTxInfos(trResponse.transactions);
        }
    }

    private CurrencyBasedBalance getAddressBalance(AddressTransactionsInfo.Json atInfo, ColuAccount account) {
        long assetConfirmedAmount = 0;
        long assetReceivingAmount = 0;
        long assetSendingAmount = 0;

        int assetScale = 0;
        long satoshiAmount = 0;

        for(Tx.Json tx : atInfo.transactions) {
            if (tx.blockheight != -1)
                continue;

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
                            if (!asset.assetId.equals(account.getColuAsset().id))
                                continue;
                            if (isInitiatedByMe)
                                assetSendingAmount += asset.amount;
                        }
                    } else {
                        for (Asset.Json asset : vout.assets) {
                            if (!asset.assetId.equals(account.getColuAsset().id))
                                continue;

                            if (!isInitiatedByMe)
                                assetReceivingAmount += asset.amount;
                        }
                    }
            }
        }

        for(Utxo.Json utxo : atInfo.utxos) {
            satoshiAmount = satoshiAmount + utxo.value;
            for (Asset.Json txidAsset : utxo.assets) {
                if (txidAsset.assetId.equals(account.getColuAsset().id)) {
                    assetConfirmedAmount += txidAsset.amount;
                    assetScale = txidAsset.divisibility;
                }
            }
        }

        // set balance in account
        BigDecimal assetConfirmedBalance = BigDecimal.valueOf(assetConfirmedAmount, assetScale);
        BigDecimal assetReceivingBalance = BigDecimal.valueOf(assetReceivingAmount, assetScale);
        BigDecimal assetSendingBalance = BigDecimal.valueOf(assetSendingAmount, assetScale);
        ExactCurrencyValue confirmed = ExactCurrencyValue.from(assetConfirmedBalance, account.getColuAsset().name);
        ExactCurrencyValue sending = ExactCurrencyValue.from(assetSendingBalance, account.getColuAsset().name);
        ExactCurrencyValue receiving = ExactCurrencyValue.from(assetReceivingBalance, account.getColuAsset().name);
        CurrencyBasedBalance newBalanceFiat = new CurrencyBasedBalance(confirmed, sending, receiving);
        account.setBalanceFiat(newBalanceFiat);
        account.setBalanceSatoshi(satoshiAmount);
        return newBalanceFiat;
    }

    public ColuClient getClient() {
        return coluClient;
    }

    public boolean isColuAsset(String assetName) {
        for (String asset : ColuAccount.ColuAsset.getAllAssetNames(getNetwork())) {
            if (asset.contentEquals(assetName)) {
                return true;
            }
        }
        return false;
    }

    public ColuAccount.ColuAsset getColuAddressAsset(PublicKey key) throws IOException {

        AddressInfo.Json addressInfo = coluClient.getBalance(key.toAddress(getNetwork()));

        if (addressInfo != null) {
            if (addressInfo.utxos != null) {
                Log.d(TAG, "isColuAddress: processing " + addressInfo.utxos.size() + " utxos.");
                for (Utxo.Json utxo : addressInfo.utxos) {
                    Log.d(TAG, "isColuAddress: utxo " + utxo.txid);
                    // adding utxo to list of txid list request
                    for (Asset.Json txidAsset : utxo.assets) {
                        Log.d(TAG, "isColuAddress: utxo " + utxo.txid + " asset " + txidAsset.assetId);
                        for (String knownAssetId : ColuAccount.ColuAsset.getAssetMap(getNetwork()).keySet()) {
                            if (txidAsset.assetId.equals(knownAssetId)) {
                                return ColuAccount.ColuAsset.getAssetMap(getNetwork()).get(knownAssetId);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public void startSynchronization() {
        eventTranslator.onWalletStateChanged(null, state = WalletManager.State.SYNCHRONIZING);
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                scanForAccounts();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                eventBus.post(new BalanceChanged(null));
                eventTranslator.onWalletStateChanged(null, state = WalletManager.State.READY);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
