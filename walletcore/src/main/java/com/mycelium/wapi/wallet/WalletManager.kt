package com.mycelium.wapi.wallet

import com.mrd.bitlib.crypto.Bip39
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.api.lib.FeeEstimation
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher
import com.mycelium.wapi.wallet.btc.SynchronizeAbleWalletBtcAccount
import com.mycelium.wapi.wallet.btc.WalletManagerBacking
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.manager.*
import java.util.*


class WalletManager(val _secureKeyValueStore: SecureKeyValueStore,
                    val backing: WalletManagerBacking<*,*>,
                    val network: NetworkParameters,
                    val wapi: Wapi,
                    val currenciesSettingsMap: MutableMap<Currency, CurrencySettings>
                    ) {
    private val MASTER_SEED_ID = HexUtils.toBytes("D64CA2B680D8C8909A367F28EB47F990")
    private val MAX_AGE_FEE_ESTIMATION = (2 * 60 * 60 * 1000).toLong() // 2 hours
    private val MIN_AGE_FEE_ESTIMATION = (20 * 60 * 1000).toLong() // 20 minutes

    private val accounts = mutableMapOf<UUID, WalletAccount<*, *>>()
    private val walletModules = mutableMapOf<String, WalletModule>()
    private val _observers = LinkedList<Observer>()
    private val _lastFeeEstimations = backing.loadLastFeeEstimation();
    private val _logger = wapi.getLogger()
    lateinit var _identityAccountKeyManager: IdentityAccountKeyManager
    var isNetworkConnected: Boolean = false
    var walletListener: WalletListener? = null

    lateinit var accountScanManager: AccountScanManager

    var state: State = State.OFF

    @Volatile
    private var activeAccountId: UUID? = null

    fun add(walletModule: WalletModule) = walletModules.put(walletModule.getId(), walletModule)

    fun remove(walletModule: WalletModule) = walletModules.remove(walletModule.getId())

    fun init() {
        for (walletModule in walletModules.values) {
            accounts.putAll(walletModule.loadAccounts())
        }
        startSynchronization(SyncMode.FULL_SYNC_ALL_ACCOUNTS)
    }

    fun getAccountIds(): List<UUID> = accounts.keys.toList()

    fun getModuleById(id: String) : WalletModule? = walletModules[id]


    fun getAccountBy(address: GenericAddress): UUID? {
        var result: UUID? = null
        for (account in accounts.values) {
            if (account.isMineAddress(address)) {
                result = account.id
                break
            }
        }
        return result
    }

    fun setIsNetworkConnected(connected: Boolean) {
        isNetworkConnected = connected
    }

    fun hasPrivateKey(address: GenericAddress): Boolean {
        var result = false
        for (account in accounts.values) {
            if (account.canSpend() && account.isMineAddress(address)) {
                result = true
                break
            }
        }
        return result
    }


    fun getLastFeeEstimations(): FeeEstimation {
        if (Date().time - _lastFeeEstimations.getValidFor().getTime() >= MAX_AGE_FEE_ESTIMATION) {
            _logger.logError("Using stale fee estimation!") // this is still better
        }
        return _lastFeeEstimations
    }

    fun createAccounts(config: Config): List<UUID> {
        val result = mutableMapOf<UUID, WalletAccount<*, *>>()
        walletModules.values.forEach {
            if (it.canCreateAccount(config)) {
                val account = it.createAccount(config)
                account?.let {
                    result[account.id] = account
                }
            }
        }
        accounts.putAll(result)
        return result.keys.toList()
    }

    fun deleteAccount(id: UUID) {
        val account = accounts[id]
        account?.let {
            accounts.remove(id)
            walletModules.values.forEach {
                it.deleteAccount(account)
            }
        }
    }

    fun hasAccount(id: UUID): Boolean = accounts.containsKey(id)

    fun getAccount(id: UUID): WalletAccount<*, *>? = accounts[id]

    @JvmOverloads
    fun startSynchronization(mode: SyncMode = SyncMode.NORMAL_FORCED) {
        if (!isNetworkConnected) {
            return
        }
        Thread(Synchronizer(this, mode)).start()
    }

    fun startSynchronization(acc: UUID): Boolean {
        // Launch synchronizer thread
        val activeAccount = getAccount(acc) as SynchronizeAbleWalletBtcAccount
        Thread(Synchronizer(this, SyncMode.NORMAL, listOf(activeAccount)))
        return isNetworkConnected
    }

    fun getAccounts(): List<WalletAccount<*, *>> = accounts.values.toList()

    fun setActiveAccount(accountId: UUID) {
        activeAccountId = accountId
        activeAccountId?.let {
            if (hasAccount(accountId)) {
                val account = getAccount(it)
                if (account != null) {
                    // this account might not be synchronized - start a background sync
                    startSynchronization(SyncMode.NORMAL)
                }
            }
        }
    }

    /**
     * Determine whether this address is managed by an account of the wallet
     *
     * @param address the address to query for
     * @return if any account in the wallet manager has the address
     */
    fun isMyAddress(address: GenericAddress): Boolean {
        return getAccountByAddress(address) != null
    }

    /**
     * Get the account associated with an address if any
     *
     * @param address the address to query for
     * @return the first account UUID if found.
     */
    @Synchronized
    fun getAccountByAddress(address: GenericAddress): UUID? {
        for (account in accounts.values) {
            if (account.isMineAddress(address)) {
                return account.id
            }
        }
        return null
    }

    //TODO what about not bitcoin network
    fun getBlockHeight(coinType: CryptoCurrency): Int {
        //TODO: should we iterate over all accounts and find max blockheight ?
        val account = accounts.values.elementAt(0)
        return account.blockChainHeight
    }

    fun activateFirstAccount() {
//        filterAndConvert(MAIN_SEED_BTC_HD_ACCOUNT).get(0).activateAccount()
    }

    fun getCurrencySettings(currency: Currency): CurrencySettings? {
        return currenciesSettingsMap.get(currency)
    }
    /**
     * Add an observer that gets callbacks when the wallet manager state changes
     * or account events occur.
     *
     * @param observer the observer to add
     */
    fun addObserver(observer: Observer) {
        synchronized(_observers) {
            _observers.add(observer)
        }
    }

    fun setCurrencySettings(currency: Currency, settings: CurrencySettings) {
        currenciesSettingsMap.put(currency, settings)
    }


    fun getSpendingAccounts() : List<WalletAccount<*, *>> {
        return accounts.values.filter { it.canSpend() }
    }

    fun getSpendingAccountsWithBalance() : List<WalletAccount<*, *>> {
        return accounts.values.filter { it.canSpend() && it.accountBalance.spendable.isPositive }
    }

    /**
     * Configure the BIP32 master seed of this wallet manager
     *
     * @param masterSeed the master seed to use.
     * @param cipher     the cipher used to encrypt the master seed. Must be the same
     * cipher as the one used by the secure storage instance
     * @throws InvalidKeyCipher if the cipher is invalid
     */
    @Throws(InvalidKeyCipher::class)
    fun configureBip32MasterSeed(masterSeed: Bip39.MasterSeed, cipher: KeyCipher) {
        if (hasBip32MasterSeed()) {
            throw RuntimeException("HD key store already loaded")
        }
        _secureKeyValueStore.encryptAndStoreValue(MASTER_SEED_ID, masterSeed.toBytes(false), cipher)
    }

    fun getArchivedAccounts(): List<WalletAccount<*, *>> {
        return accounts.values.filter { it.isArchived && it.canSpend() }
    }

    /**
     * Get the active accounts managed by the wallet manager, excluding on-the-fly-accounts
     *
     * @return the active accounts managed by the wallet manager
     */

    fun getActiveAccounts(): List<WalletAccount<*, *>> {
        return accounts.values.filter { it.isActive && it.canSpend() }
    }

    /**
     * Get the active HD-accounts managed by the wallet manager, excluding on-the-fly-accounts and single-key accounts
     *
     * @return the list of accounts
     */

    fun getActiveMasterseedAccounts(): List<WalletAccount<*, *>> {
        return accounts.values.filter { it is HDAccount && it.isDerivedFromInternalMasterseed }
    }

    /**
     * Get the active BTC HD-accounts managed by the wallet manager, excluding on-the-fly-accounts and single-key accounts
     *
     * @return the list of accounts
     */
    fun getActiveHDAccounts(): List<WalletAccount<*, *>> {
        return accounts.values.filter { it is HDAccount && !it.isArchived }
    }

    /**
     * Call this method to disable transaction history synchronization for single address accounts.
     * <p>
     * This is useful if the wallet manager is used for cold storage spending where the transaction history is
     * uninteresting. Disabling transaction history synchronization makes synchronization faster especially if the
     * address has been used a lot.
     */
    fun disableTransactionHistorySynchronization() {

    }
    /**
     * Get the master seed in plain text
     *
     * @param cipher the cipher used to decrypt the master seed
     * @return the master seed in plain text
     * @throws InvalidKeyCipher if the cipher is invalid
     */
    @Throws(InvalidKeyCipher::class)
    fun getMasterSeed(cipher: KeyCipher): Bip39.MasterSeed {
        val binaryMasterSeed = _secureKeyValueStore.getDecryptedValue(MASTER_SEED_ID, cipher)
        val masterSeed = Bip39.MasterSeed.fromBytes(binaryMasterSeed, false)
        if (!masterSeed.isPresent) {
            throw RuntimeException()
        }
        return masterSeed.get()
    }

    @Throws(InvalidKeyCipher::class)
    fun getIdentityAccountKeyManager(cipher: KeyCipher): IdentityAccountKeyManager {
        if (null != _identityAccountKeyManager) {
            return _identityAccountKeyManager
        }
        if (!hasBip32MasterSeed()) {
            throw RuntimeException("accessed identity account with no master seed configured")
        }
        val rootNode = HdKeyNode.fromSeed(getMasterSeed(cipher).bip32Seed, null)
        _identityAccountKeyManager = IdentityAccountKeyManager.createNew(rootNode, _secureKeyValueStore, cipher)
        return _identityAccountKeyManager
    }

    /**
     * Determine whether the wallet manager has a master seed configured
     *
     * @return true if a master seed has been configured for this wallet manager
     */
    fun hasBip32MasterSeed(): Boolean {
        return _secureKeyValueStore.hasCiphertextValue(MASTER_SEED_ID)
    }

    /**
     * Implement this interface to get a callback when the wallet manager changes
     * state or when some event occurs
     */
    interface Observer {
        /**
         * Callback which occurs when the state of a wallet manager changes while
         * the wallet is synchronizing
         *
         * @param wallet the wallet manager instance
         * @param state  the new state of the wallet manager
         */
        fun onWalletStateChanged(wallet: WalletManager, state: State)

        /**
         * Callback telling that an event occurred while synchronizing
         *
         * @param wallet    the wallet manager
         * @param accountId the ID of the account causing the event
         * @param events    the event that occurred
         */
        fun onAccountEvent(wallet: WalletManager, accountId: UUID, events: Event)
    }

    enum class Event {
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