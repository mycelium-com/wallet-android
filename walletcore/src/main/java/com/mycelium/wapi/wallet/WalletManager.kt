package com.mycelium.wapi.wallet

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.colu.coins.ColuMain
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.genericdb.FeeEstimationsBacking
import com.mycelium.wapi.wallet.manager.*
import com.mycelium.wapi.wallet.providers.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.coroutines.sync.Mutex

class WalletManager
@JvmOverloads
constructor(val network: NetworkParameters,
            val wapi: Wapi,
            val btcvWapi: Wapi,
            private var currencySettingsMap: HashMap<String, CurrencySettings>,
            @JvmField
            var accountScanManager: AccountScanManager? = null,
            private val walletDB: WalletDB) {
    private val accounts = ConcurrentHashMap<UUID, WalletAccount<*>>()
    private val walletModules = mutableMapOf<String, WalletModule>()
    private val _observers = LinkedList<Observer>()
    private val _logger  = Logger.getLogger(WalletManager::class.java.getSimpleName())
    private val activeSyncThreads = AtomicInteger(0)

    val feeEstimations = FeeEstimations()

    fun reportStartSync() {
        val beforeIncrement = activeSyncThreads.getAndIncrement()
        if (beforeIncrement == 0) {
            updateState(State.SYNCHRONIZING)
        }
    }

    fun reportStopSync() {
        val afterDecrement = activeSyncThreads.decrementAndGet()
        if (afterDecrement == 0) {
            updateState(State.READY)
        }
    }

    private fun updateState(newState: State) {
        state = newState
    }

    fun getCurrencySettings(moduleID: String): CurrencySettings? {
        return currencySettingsMap[moduleID]
    }

    fun setCurrencySettings(moduleID: String, settings: CurrencySettings) {
        currencySettingsMap[moduleID] = settings
        walletModules[moduleID]?.setCurrencySettings(settings)
    }

    @Volatile
    var isNetworkConnected: Boolean = false

    var walletListener: WalletListener? = null

    var state: State = State.OFF
        private set

    fun add(walletModule: WalletModule) = walletModules.put(walletModule.id, walletModule)

    fun remove(walletModule: WalletModule) = walletModules.remove(walletModule.id)

    fun init() {
        for (walletModule in walletModules.values) {
            walletModule.loadAccounts()
        }

        for (walletModule in walletModules.values) {
            walletModule.afterAccountsLoaded()
        }

        // Some additional accounts could probably be added in afterAccountsLoaded()
        // Gather the final accounts map from modules
        for (walletModule in walletModules.values) {
            walletModule.getAccounts().forEach {
                accounts[it.id] = it
            }
        }

        val backing = FeeEstimationsBacking(walletDB)
        feeEstimations.addProvider(EthFeeProvider(network.isTestnet, backing))
        feeEstimations.addProvider(BtcFeeProvider(network.isTestnet, wapi, backing))
//        feeEstimations.addProvider(ColuFeeProvider(network.isTestnet, wapi, backing))
        feeEstimations.addProvider(BtcvFeeProvider(network.isTestnet, btcvWapi, backing))
        feeEstimations.addProvider(FioFeeProvider(network.isTestnet))
    }

    fun getAccountIds(): List<UUID> = accounts.keys().toList()

    fun getModuleById(id: String) : WalletModule? = walletModules[id]

    fun getAccountsBy(address: Address): List<WalletAccount<*>> =
            accounts.values.filter { it.isMineAddress(address) }

    fun setIsNetworkConnected(connected: Boolean) {
        isNetworkConnected = connected
    }

    fun hasPrivateKey(address: Address): Boolean =
            accounts.values.any { it.canSpend() && it.isMineAddress(address) }

    fun createAccounts(config: Config): List<UUID> {
        getAllActiveAccounts().interruptSync()
        val result = mutableMapOf<UUID, WalletAccount<*>>()
        addAccounts(config, result)
        accounts.putAll(result)
        return result.keys.toList()
    }

    fun createAccountsUninterruptedly(config: Config): List<UUID> {
        val result = mutableMapOf<UUID, WalletAccount<*>>()
        addAccounts(config, result)
        accounts.putAll(result)
        return result.keys.toList()
    }

    private fun addAccounts(config: Config, result: MutableMap<UUID, WalletAccount<*>>) {
        walletModules.values.forEach {
            if (it.canCreateAccount(config)) {
                try {
                    val account = it.createAccount(config)
                    result[account.id] = account

                    account.dependentAccounts.forEach { walletAccount ->
                        result[walletAccount.id] = walletAccount
                    }
                } catch (exception: IllegalStateException){
                    _logger.log(Level.SEVERE, "Account", exception)
                }
            }
        }
    }

    @JvmOverloads
    fun deleteAccount(id: UUID, keyCipher: KeyCipher = AesKeyCipher.defaultKeyCipher()) {
        accounts.remove(id)?.also { account ->
            walletModules.values.forEach {
                it.deleteAccount(account, keyCipher)
            }
        }
    }

    fun hasAccount(id: UUID): Boolean = accounts.containsKey(id)

    fun getAccount(id: UUID): WalletAccount<*>? = accounts[id]

    /**
     * @param accounts - list of any accounts
     * @return only active accounts
     */
    fun getActiveAccountsFrom(accounts: List<WalletAccount<*>>) = accounts.filter { it.isActive }

    @JvmOverloads
    fun startSynchronization(mode: SyncMode = SyncMode.NORMAL_FORCED, accounts: List<WalletAccount<*>> = listOf()) : Boolean {
        if (isNetworkConnected) {
            feeEstimations.triggerRefresh()
        }
        Synchronizer(this, mode, accounts).start()
        return isNetworkConnected
    }

    fun startSynchronization(acc: UUID?): Boolean {
        // Launch synchronizer thread
        val activeAccount = getAccount(acc ?: return false) ?: return false
        feeEstimations.triggerRefresh()
        return startSynchronization(SyncMode.NORMAL, listOf(activeAccount))
    }

    fun getAccounts(): List<WalletAccount<*>> = accounts.values.toList()

    /**
     * Determine whether this address is managed by an account of the wallet
     *
     * @param address the address to query for
     * @return if any account in the wallet manager has the address
     */
    fun isMyAddress(address: Address): Boolean = getAccountByAddress(address) != null

    /**
     * Get the account associated with an address if any
     *
     * @param address the address to query for
     * @return the first account UUID if found.
     */
    @Synchronized
    fun getAccountByAddress(address: Address): UUID? = accounts.values.firstOrNull {
            it.isMineAddress(address)
        }?.id

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

    fun getSpendingAccounts() : List<WalletAccount<*>> = accounts.values.filter { it.canSpend() }

    fun getSpendingAccountsWithBalance() : List<WalletAccount<*>> =
            accounts.values.filter { it.isActive && it.canSpend() && it.accountBalance.spendable.isPositive() }

    fun getArchivedAccounts(): List<WalletAccount<*>> = accounts.values.filter { it.isArchived }

    /**
     * Get the active accounts managed by the wallet manager, excluding on-the-fly-accounts
     *
     * @return the active accounts managed by the wallet manager
     */
    fun getActiveSpendingAccounts(): List<WalletAccount<*>> =
            accounts.values.filter { it.isActive && it.canSpend() }

    fun getAllActiveAccounts():  List<WalletAccount<*>> = accounts.values.filter { it.isActive }

    fun getAssetTypes(): List<AssetInfo> = accounts.values.map { it.coinType }.distinct()

    fun getCryptocurrenciesSymbols(): List<String> = getAssetTypes()
            .filterNot { it is ColuMain }
            .map { it.symbol }

    fun getCryptocurrenciesNames(): List<String> = getAssetTypes()
            .filterNot { it is ColuMain || it is ERC20Token }
            .map { it.name }

    fun getMasterSeedDerivedAccounts(): Map<CryptoCurrency, List<WalletAccount<*>>> =
        accounts.values.filter { it.isDerivedFromInternalMasterseed() }.groupBy { it.coinType }

    fun parseAddress(address: String): List<Address> = walletModules.values
                .flatMap { it.getSupportedAssets() }
                .distinctBy { it.id }
                .mapNotNull { AssetInfo ->
                    AssetInfo.parseAddress(address)
                }

    /**
     * Call this method to disable transaction history synchronization for single address accounts.
     * <p>
     * This is useful if the wallet manager is used for cold storage spending where the transaction history is
     * uninteresting. Disabling transaction history synchronization makes synchronization faster especially if the
     * address has been used a lot.
     */
    fun disableTransactionHistorySynchronization() {
        // TODO: implement
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
        SYNC_PROGRESS_UPDATED,
        /**
         * Malformed outgoing transaction detected
         */
        MALFORMED_OUTGOING_TRANSACTIONS_FOUND,
        /**
         * Transaction history can't be loaded due to large size
         */
        TOO_MANY_TRANSACTIONS
    }
}
