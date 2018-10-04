/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mycelium.wapi.wallet;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import com.mrd.bitlib.crypto.*;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.HexUtils;
import com.mycelium.WapiLogger;
import com.mycelium.wapi.api.Wapi;
import com.mycelium.wapi.api.WapiException;
import com.mycelium.wapi.api.WapiResponse;
import com.mycelium.wapi.api.lib.FeeEstimation;
import com.mycelium.wapi.api.response.MinerFeeEstimationResponse;
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher;
import com.mycelium.wapi.wallet.bip44.*;
import com.mycelium.wapi.wallet.bip44.HDAccountContext.*;
import com.mycelium.wapi.wallet.single.PublicPrivateKeyStore;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;
import com.mycelium.wapi.wallet.single.SingleAddressAccountContext;
import com.mycelium.wapi.wallet.single.SingleAddressBCHAccount;

import javax.annotation.Nonnull;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.*;
import static com.mycelium.wapi.wallet.bip44.HDAccountContext.*;

/**
 * Allows you to manage a wallet that contains multiple HD accounts and
 * 'classic' single address accounts.
 */
// TODO: we might optimize away full TX history for cold storage spending
public class WalletManager {
    private static final byte[] MASTER_SEED_ID = HexUtils.toBytes("D64CA2B680D8C8909A367F28EB47F990");
    // maximum age where we say a fetched fee estimation is valid
    private static final long MAX_AGE_FEE_ESTIMATION = 2 * 60 * 60 * 1000; // 2 hours
    private static final long MIN_AGE_FEE_ESTIMATION = 20 * 60 * 1000; // 20 minutes

    public AccountScanManager accountScanManager;
    private final Set<AccountProvider> _extraAccountProviders = new HashSet<>();
    private final Set<String> _extraAccountsCurrencies = new HashSet<>();
    private final Map<UUID, WalletAccount> _extraAccounts = new HashMap<>();
    private final SecureKeyValueStore _secureKeyValueStore;
    private WalletManagerBacking _backing;
    private final Map<UUID, WalletAccount> _walletAccounts;
    private final Map<UUID, UUID> _btcToBchAccounts;
    private final List<HDAccount> HDAccounts;
    private final Collection<Observer> _observers;
    private State _state;
    private Thread _synchronizationThread;
    private AccountEventManager _accountEventManager;
    private NetworkParameters _network;
    private Wapi _wapi;
    private WapiLogger _logger;
    private final ExternalSignatureProviderProxy _signatureProviders;
    private IdentityAccountKeyManager _identityAccountKeyManager;
    private volatile UUID _activeAccountId;
    private FeeEstimation _lastFeeEstimations;
    private SpvBalanceFetcher _spvBalanceFetcher;
    private volatile boolean isNetworkConnected;
    private Map<Currency, CurrencySettings> currenciesSettingsMap = new HashMap<>(); //this maps currencies to their settings.

    /**
     * Create a new wallet manager instance
     *
     * @param backing the backing to use for storing everything related to wallet accounts
     * @param network the network used
     * @param wapi    the Wapi instance to use
     */
    public WalletManager(SecureKeyValueStore secureKeyValueStore, WalletManagerBacking backing,
                         NetworkParameters network, Wapi wapi, ExternalSignatureProviderProxy signatureProviders,
                         SpvBalanceFetcher spvBalanceFetcher, boolean isNetworkConnected, Map<Currency, CurrencySettings> currenciesSettingsMap) {
        _secureKeyValueStore = secureKeyValueStore;
        _backing = backing;
        _network = network;
        _wapi = wapi;
        _signatureProviders = signatureProviders;
        _logger = _wapi.getLogger();
        _walletAccounts = Maps.newHashMap();
        HDAccounts = new ArrayList<>();
        _state = State.READY;
        _accountEventManager = new AccountEventManager();
        _observers = new LinkedList<>();
        _spvBalanceFetcher = spvBalanceFetcher;
        _btcToBchAccounts = new HashMap<>();
        this.isNetworkConnected = isNetworkConnected;
        _lastFeeEstimations = _backing.loadLastFeeEstimation();
        this.currenciesSettingsMap = currenciesSettingsMap;
        loadAccounts();
    }

    public void addExtraAccounts(AccountProvider accountProvider) {
        _extraAccountProviders.add(accountProvider);
        refreshExtraAccounts();
    }

    public void refreshExtraAccounts() {
        _extraAccounts.clear();
        _extraAccountsCurrencies.clear();
        for (AccountProvider accounts : _extraAccountProviders) {
            for (WalletAccount account : accounts.getAccounts().values()) {
                if (!_extraAccounts.containsKey(account.getId())) {
                    _extraAccounts.put(account.getId(), account);
                    _extraAccountsCurrencies.add(account.getAccountDefaultCurrency());
                }
            }
        }
    }

    public Set<String> getAllActiveFiatCurrencies() {
        return ImmutableSet.copyOf(_extraAccountsCurrencies);
    }

    /**
     * Get the current state
     *
     * @return the current state
     */
    public State getState() {
        return _state;
    }

    /**
     * Add an observer that gets callbacks when the wallet manager state changes
     * or account events occur.
     *
     * @param observer the observer to add
     */
    public void addObserver(Observer observer) {
        synchronized (_observers) {
            _observers.add(observer);
        }
    }

    /**
     * Remove an observer
     *
     * @param observer the observer to remove
     */
    public void removeObserver(Observer observer) {
        synchronized (_observers) {
            _observers.remove(observer);
        }
    }

    /**
     * Create a new read-only account using as "single address"
     *
     * @return the ID of the new account
     */
    public UUID createSingleAddressAccount(Address address) {
        UUID id = SingleAddressAccount.calculateId(address);

        synchronized (_walletAccounts) {
            if (_walletAccounts.containsKey(id)) {
                return id;
            }
            _backing.beginTransaction();
            try {
                SingleAddressAccountContext context = new SingleAddressAccountContext(id, ImmutableMap.of(address.getType(), address),
                        false, 0);
                _backing.createSingleAddressAccountContext(context);
                BTCSettings btcSettings = (BTCSettings) currenciesSettingsMap.get(Currency.BTC);
                SingleAddressAccountBacking accountBacking = checkNotNull(_backing.getSingleAddressAccountBacking(context.getId()));
                PublicPrivateKeyStore store = new PublicPrivateKeyStore(_secureKeyValueStore);
                SingleAddressAccount account = new SingleAddressAccount(context, store, _network, accountBacking, _wapi,
                        btcSettings.getChangeAddressModeReference());
                context.persist(accountBacking);
                _backing.setTransactionSuccessful();
                addAccount(account);

                if (_spvBalanceFetcher != null) {
                    SingleAddressBCHAccount singleAddressBCHAccount = new SingleAddressBCHAccount(context,
                            store, _network, accountBacking, _wapi, _spvBalanceFetcher);
                    addAccount(singleAddressBCHAccount);
                    _btcToBchAccounts.put(account.getId(), singleAddressBCHAccount.getId());
                    _spvBalanceFetcher.requestTransactionsFromUnrelatedAccountAsync(singleAddressBCHAccount.getId().toString(), /* IntentContract.UNRELATED_ACCOUNT_TYPE_SA */2);
                }
            } finally {
                _backing.endTransaction();
            }
        }
        return id;
    }

    /**
     * Create a new read-only account using as "single address"
     *
     * @return the ID of the new account
     */
    public UUID createSingleAddressAccount(PublicKey publicKey) {
        UUID id = SingleAddressAccount.calculateId(publicKey.toAddress(_network, AddressType.P2SH_P2WPKH));
        synchronized (_walletAccounts) {
            if (_walletAccounts.containsKey(id)) {
                return id;
            }
            _backing.beginTransaction();
            try {
                BTCSettings btcSettings = (BTCSettings) currenciesSettingsMap.get(Currency.BTC);
                AddressType defaultAddressType = btcSettings.getDefaultAddressType();
                SingleAddressAccountContext context = new SingleAddressAccountContext(id,
                        publicKey.getAllSupportedAddresses(_network), false, 0, defaultAddressType);
                _backing.createSingleAddressAccountContext(context);
                SingleAddressAccountBacking accountBacking = checkNotNull(_backing.getSingleAddressAccountBacking(context.getId()));
                PublicPrivateKeyStore store = new PublicPrivateKeyStore(_secureKeyValueStore);
                SingleAddressAccount account = new SingleAddressAccount(context, store, _network, accountBacking, _wapi,
                        btcSettings.getChangeAddressModeReference());
                context.persist(accountBacking);
                _backing.setTransactionSuccessful();
                addAccount(account);

                if (_spvBalanceFetcher != null) {
                    SingleAddressBCHAccount singleAddressBCHAccount = new SingleAddressBCHAccount(context,
                            store, _network, accountBacking, _wapi, _spvBalanceFetcher);
                    addAccount(singleAddressBCHAccount);
                    _btcToBchAccounts.put(account.getId(), singleAddressBCHAccount.getId());
                    _spvBalanceFetcher.requestTransactionsFromUnrelatedAccountAsync(singleAddressBCHAccount.getId().toString(), /* IntentContract.UNRELATED_ACCOUNT_TYPE_SA */2);
                }
            } finally {
                _backing.endTransaction();
            }
        }
        return id;
    }

    /**
     * Create a new Bp44 account using an accountRoot or xPrivKey (unrelated to the Masterseed)
     *
     * @param hdKeyNodes the xPub/xPrv, yPub/yPrv, zPub/zPrv to use
     * @return the ID of the new account
     */
    public UUID createUnrelatedBip44Account(List<HdKeyNode> hdKeyNodes) {
        final int accountIndex = 0;  // use any index for this account, as we don't know and we don't care
        final Map<BipDerivationType, HDAccountKeyManager> keyManagerMap = new HashMap<>();
        final List<BipDerivationType> derivationTypes = new ArrayList<>();

        // get a subKeyStorage, to ensure that the data for this key does not get mixed up
        // with other derived or imported keys.
        SecureSubKeyValueStore secureStorage = getSecureStorage().createNewSubKeyStore();

        for (HdKeyNode hdKeyNode : hdKeyNodes) {
            BipDerivationType derivationType = hdKeyNode.getDerivationType();
            derivationTypes.add(derivationType);
            if (hdKeyNode.isPrivateHdKeyNode()) {
                try {
                    keyManagerMap.put(derivationType, HDAccountKeyManager.createFromAccountRoot(hdKeyNode, _network,
                            accountIndex, secureStorage, AesKeyCipher.defaultKeyCipher(), derivationType));
                } catch (InvalidKeyCipher invalidKeyCipher) {
                    throw new RuntimeException(invalidKeyCipher);
                }
            } else {
                keyManagerMap.put(derivationType, HDPubOnlyAccountKeyManager.createFromPublicAccountRoot(hdKeyNode,
                        _network, accountIndex, secureStorage, derivationType));
            }
        }
        final UUID id = keyManagerMap.get(derivationTypes.get(0)).getAccountId();

        synchronized (_walletAccounts) {
            // check if it already exists
            boolean isUpgrade = false;
            if (_walletAccounts.containsKey(id)) {
                isUpgrade = !_walletAccounts.get(id).canSpend() && hdKeyNodes.get(0).isPrivateHdKeyNode();
                if (!isUpgrade) {
                    return id;
                }
            }
            _backing.beginTransaction();
            try {

                // Generate the context for the account
                HDAccountContext context;
                BTCSettings btcSettings = (BTCSettings) currenciesSettingsMap.get(Currency.BTC);
                AddressType defaultAddressType = btcSettings.getDefaultAddressType();
                if (hdKeyNodes.get(0).isPrivateHdKeyNode()) {
                    context = new HDAccountContext(id, accountIndex, false, ACCOUNT_TYPE_UNRELATED_X_PRIV,
                            secureStorage.getSubId(), derivationTypes, defaultAddressType);
                } else {
                    context = new HDAccountContext(id, accountIndex, false, ACCOUNT_TYPE_UNRELATED_X_PUB,
                            secureStorage.getSubId(), derivationTypes, defaultAddressType);
                }
                if (isUpgrade) {
                    _backing.upgradeBip44AccountContext(context);
                } else {
                    _backing.createBip44AccountContext(context);
                }
                // Get the backing for the new account
                Bip44AccountBacking accountBacking = getBip44AccountBacking(context.getId());

                // Create actual account
                HDAccount account;
                if (hdKeyNodes.get(0).isPrivateHdKeyNode()) {
                    account = new HDAccount(context, keyManagerMap, _network, accountBacking, _wapi,
                            btcSettings.getChangeAddressModeReference());
                } else {
                    account = new HDPubOnlyAccount(context, keyManagerMap, _network, accountBacking, _wapi);
                }

                // Finally persist context and add account
                context.persist(accountBacking);
                _backing.setTransactionSuccessful();
                if (!isUpgrade) {
                    addAccount(account);
                    HDAccounts.add(account);
                } else {
                    _walletAccounts.remove(id);
                    _walletAccounts.put(id, account);
                }
                if (_spvBalanceFetcher != null) {
                    Bip44BCHAccount bip44BCHAccount;
                    if (hdKeyNodes.get(0).isPrivateHdKeyNode()) {
                        bip44BCHAccount = new Bip44BCHAccount(context, keyManagerMap, _network, accountBacking, _wapi, _spvBalanceFetcher);
                    } else {
                        bip44BCHAccount = new Bip44BCHPubOnlyAccount(context, keyManagerMap, _network, accountBacking, _wapi, _spvBalanceFetcher);
                    }
                    addAccount(bip44BCHAccount);
                    _btcToBchAccounts.put(account.getId(), bip44BCHAccount.getId());
                    _spvBalanceFetcher.requestTransactionsFromUnrelatedAccountAsync(bip44BCHAccount.getId().toString(), /* IntentContract.UNRELATED_ACCOUNT_TYPE_HD */ 1);
                }
                return id;
            } finally {
                _backing.endTransaction();
            }
        }
    }

    public UUID createExternalSignatureAccount(List<? extends HdKeyNode> hdKeyNodes,
                                               ExternalSignatureProvider externalSignatureProvider, int accountIndex) {
        SecureSubKeyValueStore newSubKeyStore = getSecureStorage().createNewSubKeyStore();
        final Map<BipDerivationType, HDPubOnlyAccountKeyManager> keyManagerMap = new HashMap<>();
        final List<BipDerivationType> derivationTypes = new ArrayList<>();
        for (HdKeyNode hdKeyNode : hdKeyNodes) {
            BipDerivationType derivationType = hdKeyNode.getDerivationType();
            derivationTypes.add(derivationType);

            keyManagerMap.put(derivationType,HDPubOnlyAccountKeyManager.createFromPublicAccountRoot(hdKeyNode,
                    _network, accountIndex, newSubKeyStore, derivationType));
        }
        final UUID id = keyManagerMap.get(derivationTypes.get(0)).getAccountId();

        synchronized (_walletAccounts) {
            _backing.beginTransaction();
            try {

                // check if it already exists
                if (_walletAccounts.containsKey(id)) {
                    return id;
                }

                AddressType defaultAddressType = ((BTCSettings) currenciesSettingsMap.get(Currency.BTC)).getDefaultAddressType();
                // Generate the context for the account
                HDAccountContext context = new HDAccountContext(id, accountIndex, false,
                        externalSignatureProvider.getBIP44AccountType(), newSubKeyStore.getSubId(), derivationTypes, defaultAddressType);
                _backing.createBip44AccountContext(context);

                // Get the backing for the new account
                Bip44AccountBacking accountBacking = getBip44AccountBacking(context.getId());

                BTCSettings btcSettings = (BTCSettings) currenciesSettingsMap.get(Currency.BTC);
                // Create actual account
                HDAccount account = new HDAccountExternalSignature(context, keyManagerMap, _network,
                        accountBacking, _wapi, externalSignatureProvider, btcSettings.getChangeAddressModeReference());

                // Finally persist context and add account
                context.persist(accountBacking);
                _backing.setTransactionSuccessful();
                addAccount(account);
                HDAccounts.add(account);
                return account.getId();
            } finally {
                _backing.endTransaction();
            }
        }
    }

    /**
     * Create a new account using a single private key and address
     *
     * @param privateKey key the private key to use
     * @param cipher     the cipher used to encrypt the private key. Must be the same
     *                   cipher as the one used by the secure storage instance
     * @return the ID of the new account
     */
    public UUID createSingleAddressAccount(InMemoryPrivateKey privateKey, KeyCipher cipher) throws InvalidKeyCipher {
        PublicKey publicKey = privateKey.getPublicKey();
        PublicPrivateKeyStore store = new PublicPrivateKeyStore(_secureKeyValueStore);
        for (Address address : publicKey.getAllSupportedAddresses(_network).values()) {
            store.setPrivateKey(address, privateKey, cipher);
        }
        return createSingleAddressAccount(publicKey);
    }

    /**
     * Delete an account that uses a single address
     * <p>
     * This method cannot be used for deleting Masterseed-based HD accounts.
     *
     * @param id the ID of the account to delete.
     */
    public void deleteUnrelatedAccount(UUID id, KeyCipher cipher) throws InvalidKeyCipher {
        synchronized (_walletAccounts) {
            WalletAccount account = _walletAccounts.get(id);
            if (account instanceof AbstractAccount) {
                AbstractAccount abstractAccount = (AbstractAccount) account;
                abstractAccount.setEventHandler(null);
            }
            if (account instanceof SingleAddressAccount) {
                SingleAddressAccount singleAddressAccount = (SingleAddressAccount) account;
                singleAddressAccount.forgetPrivateKey(cipher);
                _backing.deleteSingleAddressAccountContext(id);
                _walletAccounts.remove(id);
                if (_spvBalanceFetcher != null) {
                    _spvBalanceFetcher.requestUnrelatedAccountRemoval(id.toString());
                }
            } else if (account instanceof HDAccount) {
                HDAccount hdAccount = (HDAccount) account;
                if (hdAccount.isDerivedFromInternalMasterseed()) {
                    throw new RuntimeException("cant delete masterseed based accounts");
                }
                hdAccount.clearBacking();
                HDAccounts.remove(hdAccount);
                _backing.deleteBip44AccountContext(id);
                _walletAccounts.remove(id);
                if (_spvBalanceFetcher != null) {
                    _spvBalanceFetcher.requestHdWalletAccountRemoval(((HDAccount) account).getAccountIndex());
                }
            }

            if (_btcToBchAccounts.containsKey(id)) {
                _walletAccounts.remove(_btcToBchAccounts.get(id));
                _btcToBchAccounts.remove(id);
            }
        }
    }

    /**
     * Call this method to disable transaction history synchronization for single address accounts.
     * <p>
     * This is useful if the wallet manager is used for cold storage spending where the transaction history is
     * uninteresting. Disabling transaction history synchronization makes synchronization faster especially if the
     * address has been used a lot.
     */
    public void disableTransactionHistorySynchronization() {
        //TODO SegWit make this working again, as in number of addresses to sync increases 3 times.
    }

    /**
     * Get the IDs of the accounts managed by the wallet manager
     *
     * @return the IDs of the accounts managed by the wallet manager
     */
    public List<UUID> getAccountIds() {
        List<UUID> list = new ArrayList<>(_walletAccounts.size() + _extraAccounts.size());
        for (WalletAccount account : getAllAccounts()) {
            list.add(account.getId());
        }
        return list;
    }

    /**
     * Get the active accounts managed by the wallet manager, excluding on-the-fly-accounts
     *
     * @return the active accounts managed by the wallet manager
     */
    public List<WalletAccount> getActiveAccounts() {
        return filterAndConvert(not(IS_ARCHIVE));
    }

    /**
     * Get the active HD-accounts managed by the wallet manager, excluding on-the-fly-accounts and single-key accounts
     *
     * @return the list of accounts
     */
    public List<WalletAccount> getActiveMasterseedAccounts() {
        return filterAndConvert(and(MAIN_SEED_HD_ACCOUNT, not(IS_ARCHIVE)));
    }
    /**
     * Get the active BTC HD-accounts managed by the wallet manager, excluding on-the-fly-accounts and single-key accounts
     *
     * @return the list of accounts
     */
    public List<WalletAccount> getActiveAccounts(final WalletAccount.Type type) {
        return filterAndConvert(and(new Predicate<WalletAccount>() {
            @Override
            public boolean apply(WalletAccount input) {
                return input.getType() == type;
            }
        }, not(IS_ARCHIVE)));
    }

    /**
     * Get the active none-HD-accounts managed by the wallet manager, excluding on-the-fly-accounts and single-key accounts
     *
     * @return the list of accounts
     */
    public List<WalletAccount> getActiveOtherAccounts() {
        return filterAndConvert(not(or(MAIN_SEED_HD_ACCOUNT, IS_ARCHIVE)));
    }

    /**
     * Get archived accounts managed by the wallet manager
     *
     * @return the archived accounts managed by the wallet manager
     */
    public List<WalletAccount> getArchivedAccounts() {
        return filterAndConvert(IS_ARCHIVE);
    }

    /**
     * Get accounts that can spend and are active
     *
     * @return the list of accounts
     */
    public List<WalletAccount> getSpendingAccounts() {
        return filterAndConvert(ACTIVE_CAN_SPEND);
    }

    /**
     * Get accounts that can spend and have a positive balance
     *
     * @return the list of accounts
     */
    public List<WalletAccount> getSpendingAccountsWithBalance() {
        return filterAndConvert(and(ACTIVE_CAN_SPEND, HAS_BALANCE));
    }

    private List<WalletAccount> filterAndConvert(Predicate<WalletAccount> filter) {
        return Lists.newArrayList(Iterables.filter(getAllAccounts(), filter));
    }

    /**
     * Check whether the wallet manager has a particular account
     *
     * @param id the account to look for
     * @return true if the wallet manager has an account with the specified ID
     */
    public boolean hasAccount(UUID id) {
        return _walletAccounts.containsKey(id) || _extraAccounts.containsKey(id);
    }

    /**
     * Get a wallet account
     *
     * @param id the ID of the account to get
     * @return a wallet account
     */
    public WalletAccount getAccount(UUID id) {
        WalletAccount normalAccount = _walletAccounts.get(id);
        if (normalAccount == null) {
            normalAccount = _extraAccounts.get(id);
        }
        return normalAccount;
    }

    /**
     * Get a wallet account
     *
     * @param index the index of the account to get
     * @return a wallet account
     */
    public HDAccount getBip44Account(int index) {
        HDAccount result = null;
        for (HDAccount HDAccount :
                HDAccounts) {
            if(HDAccount.getAccountIndex() == index) {
                result = HDAccount;
                break;
            }
        }
        return checkNotNull(result);
    }


    /**
     * Get a BCH wallet account
     *
     * @param index the index of the account to get
     * @return a wallet account
     */
    public Bip44BCHAccount getBip44BCHAccount(int index) {
        HDAccount HDAccount = getBip44Account(index);
        UUID bchBip44AccountID = _btcToBchAccounts.get(HDAccount.getId());
        return (Bip44BCHAccount)_walletAccounts.get(bchBip44AccountID);
    }

    /**
     * Checks if the account is already created.
     *
     * @param index the index of the account to get
     * @return a wallet account
     */
    public boolean doesBip44AccountExists(int index) {
        for (HDAccount HDAccount :
                HDAccounts) {
            if(HDAccount.getAccountIndex() == index) {
                return true;
            }
        }
        return false;
    }

    /**
     * Make the wallet manager synchronize all its active accounts.
     * <p>
     * Synchronization occurs in the background. To get feedback register an
     * observer.
     */
    public void startSynchronization() {
        startSynchronization(SyncMode.NORMAL_FORCED);
    }

    public void startSynchronization(SyncMode mode) {
        if (!isNetworkConnected) {
            return;
        }
        // Launch synchronizer thread
        Synchronizer synchronizer;
        if (hasAccount(_activeAccountId)) {
            SynchronizeAbleWalletAccount activeAccount = (SynchronizeAbleWalletAccount) getAccount(_activeAccountId);
            synchronizer = new Synchronizer(mode, activeAccount);
        } else {
            // we dont know the active account
            synchronizer = new Synchronizer(mode);
        }
        startSynchronizationThread(synchronizer);
    }

    /**
     * This method only used to synchronize the unrelated bip44 account (created from xpub from scan)..
     * .. to retrieve the address to send funds. If there is no network,
     * user will be notified and only the first address will be reused (SendMainActivity.java)
     * @return whether the network is present or not.
     */
    public boolean startSynchronization(UUID receivingAcc) {
        // Launch synchronizer thread
        SynchronizeAbleWalletAccount activeAccount = (SynchronizeAbleWalletAccount) getAccount(receivingAcc);
        startSynchronizationThread(new Synchronizer(SyncMode.NORMAL, activeAccount));
        return isNetworkConnected;
    }

    private synchronized void startSynchronizationThread(Synchronizer synchronizer) {
        if (_synchronizationThread != null) {
            // Already running
            return;
        }
        // Launch fastSynchronizer thread
        _synchronizationThread = new Thread(synchronizer);
        _synchronizationThread.setDaemon(true);
        _synchronizationThread.setName(synchronizer.getClass().getSimpleName());
        _synchronizationThread.start();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int Bip44Accounts = 0;
        int simpleAccounts = 0;
        for (UUID id : getAccountIds()) {
            if (_walletAccounts.get(id) instanceof HDAccount) {
                Bip44Accounts++;
            } else if (_walletAccounts.get(id) instanceof SingleAddressAccount) {
                simpleAccounts++;
            }
        }
        sb.append("Accounts: ").append(_walletAccounts.size()).append(" Active: ").append(getActiveAccounts().size())
        .append(" HD: ").append(Bip44Accounts).append(" Simple:").append(simpleAccounts);
        return sb.toString();
    }

    /**
     * Determine whether this address is managed by an account of the wallet
     *
     * @param address the address to query for
     * @return if any account in the wallet manager has the address
     */
    public boolean isMyAddress(Address address) {
        return getAccountByAddress(address).isPresent();
    }


    /**
     * Get the account associated with an address if any
     *
     * @param address the address to query for
     * @return the first account UUID if found.
     */
    public synchronized Optional<UUID> getAccountByAddress(Address address) {
        for (WalletAccount account : getAllAccounts()) {
            if (account.isMine(address)) {
                return Optional.of(account.getId());
            }
        }
        return Optional.absent();
    }

    /**
     * Determine whether any account in the wallet manager has the private key for the specified address
     *
     * @param address the address to query for
     * @return true if any account in the wallet manager has the private key for the specified address
     */
    public synchronized boolean hasPrivateKeyForAddress(Address address) {
        // don't use getAccountByAddress here, as we might have the same address in an pub-only account and a normal account too
        for (WalletAccount account : getAllAccounts()) {
            if (account.canSpend() && account.isMine(address)) {
                return true;
            }
        }
        return false;
    }

    private void setStateAndNotify(State state) {
        _state = state;
        synchronized (_observers) {
            for (Observer o : _observers) {
                o.onWalletStateChanged(this, _state);
            }
        }
    }

    private void loadAccounts() {
        if (hasBip32MasterSeed()) {
            loadBip44Accounts();
        }
        // Load all single address accounts
        loadSingleAddressAccounts();
    }

    private void loadBip44Accounts() {
        _logger.logInfo("Loading BIP44 accounts");
        List<HDAccountContext> contexts = _backing.loadBip44AccountContexts();
        for (HDAccountContext context : contexts) {
            Map<BipDerivationType, HDAccountKeyManager> keyManagerMap = new HashMap<>();
            HDAccount account;
            if (context.getAccountType() == ACCOUNT_TYPE_FROM_MASTERSEED
                    && context.getIndexesMap().size() < BipDerivationType.values().length) {
                try {
                    AesKeyCipher cipher = AesKeyCipher.defaultKeyCipher();
                    Bip39.MasterSeed masterSeed = getMasterSeed(cipher);
                    for (BipDerivationType derivationType : BipDerivationType.values()) {
                        HdKeyNode root = HdKeyNode.fromSeed(masterSeed.getBip32Seed(), derivationType);
                        if (context.getIndexesMap().get(derivationType) == null) {
                            keyManagerMap.put(derivationType, HDAccountKeyManager.createNew(root, _network,
                                    context.getAccountIndex(), _secureKeyValueStore, cipher, derivationType));
                            context.getIndexesMap().put(derivationType, new AccountIndexesContext(-1, -1, 0));
                        }
                    }
                } catch (InvalidKeyCipher invalidKeyCipher) {
                    _logger.logError(invalidKeyCipher.getMessage());
                }
            }
            Bip44AccountBacking accountBacking = getBip44AccountBacking(context.getId());

            loadKeyManagers(context, keyManagerMap);

            account = getBip44Account(context, keyManagerMap, accountBacking);

            addAccount(account);
            HDAccounts.add(account);

            if (_spvBalanceFetcher != null) {
                Bip44BCHAccount bchAccount;

                switch (context.getAccountType()) {
                    case ACCOUNT_TYPE_UNRELATED_X_PUB:
                        bchAccount = new Bip44BCHPubOnlyAccount(context, keyManagerMap, _network, accountBacking, _wapi, _spvBalanceFetcher);
                        break;
                    default:
                        bchAccount = new Bip44BCHAccount(context, keyManagerMap, _network, accountBacking, _wapi, _spvBalanceFetcher);
                }

                addAccount(bchAccount);
                _btcToBchAccounts.put(account.getId(), bchAccount.getId());

                if (context.getAccountType() == ACCOUNT_TYPE_FROM_MASTERSEED) {
                    _spvBalanceFetcher.requestTransactionsAsync(bchAccount.getAccountIndex());
                } else {
                    _spvBalanceFetcher.requestTransactionsFromUnrelatedAccountAsync(bchAccount.getId().toString(), /* IntentContract.UNRELATED_ACCOUNT_TYPE_HD */ 1);
                }
            }
        }
    }

    private HDAccount getBip44Account(HDAccountContext context, Map<BipDerivationType, HDAccountKeyManager> keyManagerMap, Bip44AccountBacking accountBacking) {
        HDAccount account;
        BTCSettings btcSettings = (BTCSettings) currenciesSettingsMap.get(Currency.BTC);
        switch (context.getAccountType()) {
            case ACCOUNT_TYPE_FROM_MASTERSEED:
                // Normal account - derived from masterseed
                account = new HDAccount(context, keyManagerMap, _network, accountBacking, _wapi,
                        btcSettings.getChangeAddressModeReference());
                break;
            case ACCOUNT_TYPE_UNRELATED_X_PUB:
                // Imported xPub-based account
                account = new HDPubOnlyAccount(context, keyManagerMap, _network, accountBacking, _wapi);
                break;
            case ACCOUNT_TYPE_UNRELATED_X_PRIV:
                // Imported xPriv-based account
                account = new HDAccount(context, keyManagerMap, _network, accountBacking, _wapi,
                        btcSettings.getChangeAddressModeReference());
                break;
            case ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR:
                account = new HDAccountExternalSignature(context, keyManagerMap, _network, accountBacking, _wapi,
                        _signatureProviders.get(ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR),
                        btcSettings.getChangeAddressModeReference());
                break;
            case ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER:
                account = new HDAccountExternalSignature(context, keyManagerMap, _network, accountBacking, _wapi,
                        _signatureProviders.get(ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER),
                        btcSettings.getChangeAddressModeReference());
                break;
            case ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY:
                account = new HDAccountExternalSignature(context, keyManagerMap, _network, accountBacking, _wapi,
                        _signatureProviders.get(ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY),
                        btcSettings.getChangeAddressModeReference());
                break;
            default:
                throw new IllegalArgumentException("Unknown account type " + context.getAccountType());
        }
        return account;
    }

    private void loadKeyManagers(HDAccountContext context, Map<BipDerivationType, HDAccountKeyManager> keyManagerMap) {
        for (BipDerivationType derivationType : context.getIndexesMap().keySet()) {
            switch (context.getAccountType()) {
                case ACCOUNT_TYPE_FROM_MASTERSEED:
                    // Normal account - derived from masterseed
                    keyManagerMap.put(derivationType, new HDAccountKeyManager(context.getAccountIndex(), _network,
                            _secureKeyValueStore, derivationType));
                    break;
                case ACCOUNT_TYPE_UNRELATED_X_PUB:
                    // Imported xPub-based account
                    SecureKeyValueStore subKeyStore = _secureKeyValueStore.getSubKeyStore(context.getAccountSubId());
                    keyManagerMap.put(derivationType, new HDPubOnlyAccountKeyManager(context.getAccountIndex(),
                            _network, subKeyStore, derivationType));
                    break;
                case ACCOUNT_TYPE_UNRELATED_X_PRIV:
                    // Imported xPriv-based account
                    SecureKeyValueStore subKeyStoreXpriv = _secureKeyValueStore.getSubKeyStore(context.getAccountSubId());
                    keyManagerMap.put(derivationType, new HDAccountKeyManager(context.getAccountIndex(),
                            _network, subKeyStoreXpriv, derivationType));
                    break;
                case ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR:
                    SecureKeyValueStore subKeyStoreTrezor = _secureKeyValueStore.getSubKeyStore(context.getAccountSubId());
                    keyManagerMap.put(derivationType, new HDPubOnlyAccountKeyManager(context.getAccountIndex(),
                            _network, subKeyStoreTrezor, derivationType));
                    break;
                case ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER:
                    SecureKeyValueStore subKeyStoreLedger = _secureKeyValueStore.getSubKeyStore(context.getAccountSubId());
                    keyManagerMap.put(derivationType, new HDPubOnlyAccountKeyManager(context.getAccountIndex(),
                            _network, subKeyStoreLedger, derivationType));
                    break;
                case ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY:
                    SecureKeyValueStore subKeyStoreKeepKey = _secureKeyValueStore.getSubKeyStore(context.getAccountSubId());
                    keyManagerMap.put(derivationType, new HDPubOnlyAccountKeyManager(context.getAccountIndex(),
                            _network, subKeyStoreKeepKey, derivationType));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown account type " + context.getAccountType());
            }
        }
    }

    private void loadSingleAddressAccounts() {
        _logger.logInfo("Loading single address accounts");
        List<SingleAddressAccountContext> contexts = _backing.loadSingleAddressAccountContexts();
        for (SingleAddressAccountContext context : contexts) {
            PublicPrivateKeyStore store = new PublicPrivateKeyStore(_secureKeyValueStore);
            BTCSettings btcSettings = (BTCSettings) currenciesSettingsMap.get(Currency.BTC);
            SingleAddressAccountBacking accountBacking = checkNotNull(_backing.getSingleAddressAccountBacking(context.getId()));
            SingleAddressAccount account = new SingleAddressAccount(context, store, _network, accountBacking, _wapi,
                    btcSettings.getChangeAddressModeReference());
            addAccount(account);

            if (_spvBalanceFetcher != null) {
                SingleAddressBCHAccount bchAccount = new SingleAddressBCHAccount(context, store, _network, accountBacking, _wapi, _spvBalanceFetcher);
                addAccount(bchAccount);
                _btcToBchAccounts.put(account.getId(), bchAccount.getId());
                _spvBalanceFetcher.requestTransactionsFromUnrelatedAccountAsync(bchAccount.getId().toString(), /* IntentContract.UNRELATED_ACCOUNT_TYPE_SA */ 2);
            }
        }
    }

    public void addAccount(AbstractAccount account) {
        synchronized (_walletAccounts) {
            account.setEventHandler(_accountEventManager);
            _walletAccounts.put(account.getId(), account);
            _logger.logInfo("Account Added: " + account.getId());
        }
    }

    public void setNetworkConnected(boolean networkConnected) {
        isNetworkConnected = networkConnected;
    }

    public Map<Currency, CurrencySettings> getCurrenciesSettingsMap() {
        return currenciesSettingsMap;
    }

    public CurrencySettings getCurrencySettings(Currency currency) {
        return currenciesSettingsMap.get(currency);
    }

    public void setCurrencySettings(Currency currency, CurrencySettings settings) {
        currenciesSettingsMap.put(currency, settings);
    }

    private class Synchronizer implements Runnable {
        private final SyncMode syncMode;
        private final SynchronizeAbleWalletAccount currentAccount;

        private Synchronizer(SyncMode syncMode, SynchronizeAbleWalletAccount currentAccount) {
            this.syncMode = syncMode;
            this.currentAccount = currentAccount;
        }

        // use this constructor if you dont care about the current active account
        private Synchronizer(SyncMode syncMode) {
            this(syncMode, null);
        }

        @Override
        public void run() {
            setStateAndNotify(State.SYNCHRONIZING);
            try {
                synchronized (_walletAccounts) {
                    if (isNetworkConnected) {
                        if (!syncMode.ignoreMinerFeeFetch &&
                                (_lastFeeEstimations == null || _lastFeeEstimations.isExpired(MIN_AGE_FEE_ESTIMATION))) {
                            // only fetch the fee estimations if the latest available fee is older than MIN_AGE_FEE_ESTIMATION
                            fetchFeeEstimation();
                        }

                        // If we have any lingering outgoing transactions broadcast them now
                        // this function goes over all accounts - it is reasonable to
                        // exclude this from SyncMode.onlyActiveAccount behaviour
                        if (!broadcastOutgoingTransactions()) {
                            return;
                        }

                        // Synchronize selected accounts with the blockchain
                        synchronize();
                    }
                }
            } finally {
                _synchronizationThread = null;
                setStateAndNotify(State.READY);
            }
        }

        private boolean fetchFeeEstimation() {
            WapiResponse<MinerFeeEstimationResponse> minerFeeEstimations = _wapi.getMinerFeeEstimations();
            if (minerFeeEstimations != null && minerFeeEstimations.getErrorCode() == Wapi.ERROR_CODE_SUCCESS) {
                try {
                    _lastFeeEstimations = minerFeeEstimations.getResult().feeEstimation;
                    _backing.saveLastFeeEstimation(_lastFeeEstimations);
                    return true;
                } catch (WapiException e) {
                    return false;
                }
            }
            return false;
        }

        private boolean broadcastOutgoingTransactions() {
            for (WalletAccount account : getAllAccounts()) {
                if (account.isArchived()) {
                    continue;
                }
                if (!account.broadcastOutgoingTransactions()) {
                    // We failed to broadcast due to API error, we will have to try
                    // again later
                    return false;
                }
            }
            return true;
        }

        private boolean synchronize() {
            if(_spvBalanceFetcher != null) {
                //If using SPV module, enters this condition.
                // Get adresses from all accounts
                if(currentAccount instanceof Bip44BCHAccount) {
                    if (currentAccount.isDerivedFromInternalMasterseed()) {
                        _spvBalanceFetcher.requestTransactionsAsync(((Bip44BCHAccount) currentAccount).getAccountIndex());
                    }  else {
                        _spvBalanceFetcher.requestTransactionsFromUnrelatedAccountAsync(currentAccount.getId().toString(), /* IntentContract.UNRELATED_ACCOUNT_TYPE_HD */ 1);
                    }
                }

                if (currentAccount instanceof SingleAddressBCHAccount) {
                    _spvBalanceFetcher.requestTransactionsFromUnrelatedAccountAsync(currentAccount.getId().toString(), /* IntentContract.UNRELATED_ACCOUNT_TYPE_SA */ 2);
                }

                for (WalletAccount account : getAllAccounts()) {
                    if (account instanceof HDAccount) {
                        //_transactionFetcher.getTransactions(((HDAccount) account).getAccountIndex());
                    } else {
                        // TODO: 28.09.17 sync single address accounts using spv, too.
                        if (!account.isArchived()) {
                            if (!account.synchronize(syncMode)) {
                                // We failed to sync due to API error, we will have to try
                                // again later
                                return false;
                            }
                        }
                    }
                }
            }
            if (syncMode.onlyActiveAccount) {
                if (currentAccount != null && !currentAccount.isArchived() && !(currentAccount instanceof Bip44BCHAccount || currentAccount instanceof SingleAddressBCHAccount)) {
                    return currentAccount.synchronize(syncMode);
                }
            } else {
                for (WalletAccount account : getAllAccounts()) {
                    if (account.isArchived() || account instanceof Bip44BCHAccount || account instanceof SingleAddressBCHAccount) {
                        continue;
                    }
                    if (!account.synchronize(syncMode)) {
                        // We failed to sync due to API error, we will have to try
                        // again later
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private Iterable<WalletAccount> getAllAccounts() {
        //New collection should be created to prevent concurrent modification of iterator
        Map<UUID, WalletAccount> walletAccounts = new HashMap<>(_walletAccounts);
        Map<UUID, WalletAccount> extraAccounts = new HashMap<>(_extraAccounts);
        return Iterables.concat(walletAccounts.values(), extraAccounts.values());
    }

    private class AccountEventManager implements AbstractAccount.EventHandler {
        @Override
        public void onEvent(UUID accountId, Event event) {
            synchronized (_observers) {
                for (Observer o : _observers) {
                    o.onAccountEvent(WalletManager.this, accountId, event);
                }
            }
        }
    }

    public void setActiveAccount(UUID accountId) {
        _activeAccountId = accountId;
        if (hasAccount(accountId)) {
            WalletAccount account = getAccount(_activeAccountId);
            if (account != null) {
                // this account might not be synchronized - start a background sync
                startSynchronization(SyncMode.NORMAL);
            }
        }
    }

    /**
     * Determine whether the wallet manager has a master seed configured
     *
     * @return true if a master seed has been configured for this wallet manager
     */
    public boolean hasBip32MasterSeed() {
        return _secureKeyValueStore.hasCiphertextValue(MASTER_SEED_ID);
    }

    /**
     * Get the master seed in plain text
     *
     * @param cipher the cipher used to decrypt the master seed
     * @return the master seed in plain text
     * @throws InvalidKeyCipher if the cipher is invalid
     */
    public Bip39.MasterSeed getMasterSeed(KeyCipher cipher) throws InvalidKeyCipher {
        byte[] binaryMasterSeed = _secureKeyValueStore.getDecryptedValue(MASTER_SEED_ID, cipher);
        Optional<Bip39.MasterSeed> masterSeed = Bip39.MasterSeed.fromBytes(binaryMasterSeed, false);
        if (!masterSeed.isPresent()) {
            throw new RuntimeException();
        }
        return masterSeed.get();
    }

    /**
     * Configure the BIP32 master seed of this wallet manager
     *
     * @param masterSeed the master seed to use.
     * @param cipher     the cipher used to encrypt the master seed. Must be the same
     *                   cipher as the one used by the secure storage instance
     * @throws InvalidKeyCipher if the cipher is invalid
     */
    public void configureBip32MasterSeed(Bip39.MasterSeed masterSeed, KeyCipher cipher) throws InvalidKeyCipher {
        if (hasBip32MasterSeed()) {
            throw new RuntimeException("HD key store already loaded");
        }
        _secureKeyValueStore.encryptAndStoreValue(MASTER_SEED_ID, masterSeed.toBytes(false), cipher);
    }

    private static final Predicate<WalletAccount> IS_ARCHIVE = new Predicate<WalletAccount>() {
        @Override
        public boolean apply(WalletAccount input) {
            return input.isArchived();
        }
    };

    private static final Predicate<WalletAccount> ACTIVE_CAN_SPEND = new Predicate<WalletAccount>() {
        @Override
        public boolean apply(WalletAccount input) {
            return input.isActive() && input.canSpend();
        }
    };

    private static final Predicate<WalletAccount> MAIN_SEED_HD_ACCOUNT = new Predicate<WalletAccount>() {
        @Override
        public boolean apply(WalletAccount input) {
            // TODO: if relevant also check if this account is derived from the main-masterseed
            return input instanceof HDAccount &&
                   input.isDerivedFromInternalMasterseed();
        }
    };

    private static final Predicate<WalletAccount> MAIN_SEED_BTC_HD_ACCOUNT = new Predicate<WalletAccount>() {
        @Override
        public boolean apply(WalletAccount input) {
            // TODO: if relevant also check if this account is derived from the main-masterseed
            return input.getType() == WalletAccount.Type.BTCBIP44 &&
                   input.isDerivedFromInternalMasterseed();
        }
    };

    private static final Predicate<WalletAccount> HAS_BALANCE = new Predicate<WalletAccount>() {
        @Override
        public boolean apply(WalletAccount input) {
            return input.getBalance().getSpendableBalance() > 0;
        }
    };

    public boolean canCreateAdditionalBip44Account() {
        if (!hasBip32MasterSeed()) {
            // No master seed
            return false;
        }
        if (getNextBip44Index() == 0) {
            // First account not created
            return true;
        }
        // We can add an additional account if the last account had activity
        HDAccount last = HDAccounts.get(HDAccounts.size() - 1);
        return last.hasHadActivity();
    }

    public void removeUnusedBip44Account(HDAccount account) {
        //we do not remove used accounts
        if (account.hasHadActivity()) {
            return;
        }
        //if its unused, we can remove it from the manager
        synchronized (_walletAccounts) {
            HDAccounts.remove(account);
            _walletAccounts.remove(account.getId());
            _backing.deleteBip44AccountContext(account.getId());

            if (_btcToBchAccounts.containsKey(account.getId())) {
                _walletAccounts.remove(_btcToBchAccounts.get(account.getId()));
                _btcToBchAccounts.remove(account.getId());
            }
        }
    }

    ///@brief returns last known blockheight on bitcoin blockchain
    public int getBlockheight() {
        int height = 0;
        //TODO: should we iterate over all accounts and find max blockheight ?
        HDAccount account = HDAccounts.get(0);
        if(account != null) {
            height = account.getBlockChainHeight();
        }
        return height;
    }

    // for the not expected case, that no account is activated (i.e. all are achieved), just enable the first one
    // because the app needs at least one active account in several places.
    public void activateFirstAccount() {
        if (HDAccounts.isEmpty()) {
            return;
        }
        filterAndConvert(MAIN_SEED_BTC_HD_ACCOUNT).get(0).activateAccount();
    }

    public int getCurrentBip44Index() {
        int maxIndex = -1;
        for (HDAccount walletAccount : HDAccounts) {
            maxIndex = Math.max(walletAccount.getAccountIndex(), maxIndex);
        }
        return maxIndex;
    }

    private int getNextBip44Index() {
        int maxIndex = -1;
        for (HDAccount walletAccount : HDAccounts) {
            maxIndex = Math.max(walletAccount.getAccountIndex(), maxIndex);
        }
        return maxIndex + 1;
    }

    /**
     * this is part of a bugfix for where the wallet skipped accounts in conflict with BIP44
     */
    // not nice the unchecked cast, but we can be sure that MAIN_SEED_HD_ACCOUNT only returns Bip44Accounts
    // TODO: why is a double-cast needed?? Skipping the List<?> cast fails, although suggested by AS
    @SuppressWarnings({"unchecked", "RedundantCast"})
    public List<Integer> getGapsBug() {
        final List<? extends HDAccount> mainAccounts =
            (List<? extends HDAccount>)(List<?>) filterAndConvert(MAIN_SEED_HD_ACCOUNT);

        // sort it according to their index
        Collections.sort(mainAccounts, new Comparator<HDAccount>() {
            @Override
            public int compare(HDAccount o1, HDAccount o2) {
                int x = o1.getAccountIndex(), y =  o2.getAccountIndex();
                return x < y?-1:(x == y?0:1);
            }
        });
        List<Integer> gaps = new LinkedList<>();
        int lastIndex = 0;
        for (HDAccount acc : mainAccounts) {
            while (acc.getAccountIndex() > lastIndex++) {
                gaps.add(lastIndex - 1);
            }
        }
        return gaps;
    }

    /**
     * this is part of a bugfix for where the wallet skipped accounts in conflict with BIP44
     */
    public List<Address> getGapAddresses(KeyCipher cipher) throws InvalidKeyCipher {
        final List<Integer> gaps = getGapsBug();
        // Get the master seed
        Bip39.MasterSeed masterSeed = getMasterSeed(cipher);
        InMemoryWalletManagerBacking tempSecureBacking = new InMemoryWalletManagerBacking();

        final SecureKeyValueStore tempSecureKeyValueStore = new SecureKeyValueStore(tempSecureBacking, new RandomSource() {
            @Override
            public void nextBytes(byte[] bytes) {
                // randomness not needed for the temporary keystore
            }
        });

        final LinkedList<Address> addresses = new LinkedList<>();
        for (Integer gapIndex : gaps) {
            for (BipDerivationType derivationType : BipDerivationType.values()) {
                // Generate the root private key
                HdKeyNode root = HdKeyNode.fromSeed(masterSeed.getBip32Seed(), derivationType);
                final HDAccountKeyManager keyManager = HDAccountKeyManager.createNew(root, _network, gapIndex,
                        tempSecureKeyValueStore, cipher, derivationType);
                addresses.add(keyManager.getAddress(false, 0)); // get first external address for the account in the gap
            }
        }

        return addresses;
    }

    public UUID createArchivedGapFiller(KeyCipher cipher, Integer accountIndex, boolean archived) throws InvalidKeyCipher {
        // Get the master seed
        Bip39.MasterSeed masterSeed = getMasterSeed(cipher);

        synchronized (_walletAccounts) {
            _backing.beginTransaction();
            try {
                // Create the base keys for the account
                Map<BipDerivationType, HDAccountKeyManager> keyManagerMap = new HashMap<>();
                for (BipDerivationType derivationType : BipDerivationType.values()) {
                    // Generate the root private key
                    HdKeyNode root = HdKeyNode.fromSeed(masterSeed.getBip32Seed(), derivationType);
                    keyManagerMap.put(derivationType, HDAccountKeyManager.createNew(root, _network, accountIndex,
                            _secureKeyValueStore, cipher, derivationType));
                }

                BTCSettings btcSettings = (BTCSettings) currenciesSettingsMap.get(Currency.BTC);
                AddressType defaultAddressType = btcSettings.getDefaultAddressType();
                // Generate the context for the account
                HDAccountContext context = new HDAccountContext(
                        keyManagerMap.get(BipDerivationType.BIP44).getAccountId(), accountIndex, false, defaultAddressType);
                _backing.createBip44AccountContext(context);

                // Get the backing for the new account
                Bip44AccountBacking accountBacking = getBip44AccountBacking(context.getId());

                // Create actual account
                HDAccount account = new HDAccount(context, keyManagerMap, _network, accountBacking, _wapi,
                        btcSettings.getChangeAddressModeReference());

                // Finally persist context and add account
                context.persist(accountBacking);
                _backing.setTransactionSuccessful();
                if (archived) {
                    account.archiveAccount();
                }

                addAccount(account);
                HDAccounts.add(account);
                return account.getId();
            } finally {
                _backing.endTransaction();
            }
        }
    }

    public UUID createAdditionalBip44Account(KeyCipher cipher) throws InvalidKeyCipher {
        if (!canCreateAdditionalBip44Account()) {
            throw new RuntimeException("Unable to create additional HD account");
        }

        // Get the master seed
        Bip39.MasterSeed masterSeed = getMasterSeed(cipher);

        synchronized (_walletAccounts) {
            // Determine the next BIP44 account index
            int accountIndex = getNextBip44Index();

            _backing.beginTransaction();
            try {
                // Create the base keys for the account
                Map<BipDerivationType, HDAccountKeyManager> keyManagerMap = new HashMap<>();
                for (BipDerivationType derivationType : BipDerivationType.values()) {
                    // Generate the root private key
                    HdKeyNode root = HdKeyNode.fromSeed(masterSeed.getBip32Seed(), derivationType);
                    keyManagerMap.put(derivationType, HDAccountKeyManager.createNew(root, _network, accountIndex,
                            _secureKeyValueStore, cipher, derivationType));
                }
                BTCSettings btcSettings = (BTCSettings) currenciesSettingsMap.get(Currency.BTC);
                AddressType defaultAddressType = btcSettings.getDefaultAddressType();
                // Generate the context for the account
                HDAccountContext context = new HDAccountContext(
                        keyManagerMap.get(BipDerivationType.BIP44).getAccountId(), accountIndex, false, defaultAddressType);
                _backing.createBip44AccountContext(context);

                // Get the backing for the new account
                Bip44AccountBacking accountBacking = getBip44AccountBacking(context.getId());

                // Create actual account
                HDAccount account = new HDAccount(context, keyManagerMap, _network, accountBacking, _wapi,
                        btcSettings.getChangeAddressModeReference());

                // Finally persist context and add account
                context.persist(accountBacking);
                _backing.setTransactionSuccessful();
                addAccount(account);
                HDAccounts.add(account);

                if(_spvBalanceFetcher != null) {
                    Bip44BCHAccount bip44BCHAccount = new Bip44BCHAccount(context, keyManagerMap, _network,
                            accountBacking, _wapi, _spvBalanceFetcher);
                    _spvBalanceFetcher.requestTransactionsAsync(bip44BCHAccount.getAccountIndex());
                    addAccount(bip44BCHAccount);
                    _btcToBchAccounts.put(account.getId(), bip44BCHAccount.getId());
                }

                return account.getId();
            } finally {
                _backing.endTransaction();
            }
        }
    }

    @Nonnull
    private Bip44AccountBacking getBip44AccountBacking(UUID id) {
        return checkNotNull(_backing.getBip44AccountBacking(id));
    }

    public SecureKeyValueStore getSecureStorage() {
        return _secureKeyValueStore;
    }

    public IdentityAccountKeyManager getIdentityAccountKeyManager(KeyCipher cipher) throws InvalidKeyCipher {
        if (null != _identityAccountKeyManager) {
            return _identityAccountKeyManager;
        }
        if (!hasBip32MasterSeed()) {
            throw new RuntimeException("accessed identity account with no master seed configured");
        }
        HdKeyNode rootNode = HdKeyNode.fromSeed(getMasterSeed(cipher).getBip32Seed(), null);
        _identityAccountKeyManager = IdentityAccountKeyManager.createNew(rootNode, _secureKeyValueStore, cipher);
        return _identityAccountKeyManager;
    }

    public FeeEstimation getLastFeeEstimations() {
        if ((new Date().getTime() - _lastFeeEstimations.getValidFor().getTime()) >= MAX_AGE_FEE_ESTIMATION) {
            _logger.logError("Using stale fee estimation!"); // this is still better
        }
        return _lastFeeEstimations;
    }

    /**
     * Implement this interface to get a callback when the wallet manager changes
     * state or when some event occurs
     */
    public interface Observer {
        /**
         * Callback which occurs when the state of a wallet manager changes while
         * the wallet is synchronizing
         *
         * @param wallet the wallet manager instance
         * @param state  the new state of the wallet manager
         */
        void onWalletStateChanged(WalletManager wallet, State state);

        /**
         * Callback telling that an event occurred while synchronizing
         *
         * @param wallet    the wallet manager
         * @param accountId the ID of the account causing the event
         * @param events    the event that occurred
         */
        void onAccountEvent(WalletManager wallet, UUID accountId, Event events);
    }

    public enum State {
        /**
         * The wallet manager is synchronizing
         */
        SYNCHRONIZING,

        /*
         * A fast sync (only a limited subset of all addresses) is running
         */
        FAST_SYNC,

        /**
         * The wallet manager is ready
         */
        READY
    }

    public enum Event {
        /**
         * There is currently no connection to the block chain. This is probably
         * due to network issues, or the Mycelium servers are down (unlikely).
         */
        SERVER_CONNECTION_ERROR,
        /**
         * The wallet broadcasted a transaction which was accepted by the network
         */
        BROADCASTED_TRANSACTION_ACCEPTED,
        /**
         * The wallet broadcasted a transaction which was rejected by the network.
         * This is an rare event which could happen if you double spend yourself,
         * or you spent an unconfirmed change which was subject to a malleability
         * attack
         */
        BROADCASTED_TRANSACTION_DENIED,
        /**
         * The balance of the account changed
         */
        BALANCE_CHANGED,
        /**
         * The transaction history of the account changed
         */
        TRANSACTION_HISTORY_CHANGED,
        /**
         * The receiving address of an account has been updated
         */
        RECEIVING_ADDRESS_CHANGED,
        /**
         * Sync progress updated
         */
        SYNC_PROGRESS_UPDATED
    }
}
