package com.mycelium.wapi.wallet

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.api.lib.FeeEstimation
import com.mycelium.wapi.wallet.btc.WalletManagerBacking
import com.mycelium.wapi.wallet.coins.GenericAssetInfo
import com.mycelium.wapi.wallet.exceptions.AddressMalformedException
import com.mycelium.wapi.wallet.manager.*
import org.jetbrains.annotations.TestOnly
import java.util.*
import java.util.concurrent.TimeUnit


class WalletManager(val network: NetworkParameters,
                    val wapi: Wapi) {
    val MAX_AGE_FEE_ESTIMATION = TimeUnit.HOURS.toMillis(2)

    private val accounts = mutableMapOf<UUID, WalletAccount<*, *>>()
    private val walletModules = mutableMapOf<String, WalletModule>()
    private val _observers = LinkedList<Observer>()
    private val _logger = wapi.logger

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
        return accounts.values.firstOrNull { it.isMineAddress(address) }?.id
    }

    fun setIsNetworkConnected(connected: Boolean) {
        isNetworkConnected = connected
    }

    fun hasPrivateKey(address: GenericAddress): Boolean {
        return accounts.values.any { it.canSpend() && it.isMineAddress(address) }
    }

    fun createAccounts(config: Config): List<UUID> {
        val result = mutableMapOf<UUID, WalletAccount<*, *>>()
        walletModules.values.forEach {
            if (it.canCreateAccount(config)) {
                try {
                    val account = it.createAccount(config)
                    result[account.id] = account
                } catch (exception: IllegalStateException){
                    _logger.logError("Account", exception)
                }
            }
        }
        accounts.putAll(result)
        startSynchronization(SyncMode.NORMAL, result.values.toList())
        return result.keys.toList()
    }

    @TestOnly
    fun addAccount(account: WalletAccount<*,*>) {
        accounts[account.id] = account
    }

    @JvmOverloads
    fun deleteAccount(id: UUID, keyCipher: KeyCipher = AesKeyCipher.defaultKeyCipher()) {
        val account = accounts[id]
        account?.let {
            accounts.remove(id)
            walletModules.values.forEach {
                it.deleteAccount(account, keyCipher)
            }
        }
    }

    fun hasAccount(id: UUID): Boolean = accounts.containsKey(id)

    fun getAccount(id: UUID): WalletAccount<*, *>? = accounts[id]

    /**
     * @param accounts - list of any accounts
     * @return only active accounts
     */
    fun getActiveAccountsFrom(accounts: List<WalletAccount<*,*>>) = accounts.filter { !it.isArchived }

    @JvmOverloads
    fun startSynchronization(mode: SyncMode = SyncMode.NORMAL_FORCED, accounts: List<WalletAccount<*, *>> = listOf()) {
        if (!isNetworkConnected) {
            return
        }
        Thread(Synchronizer(this, mode, accounts)).start()
    }

    fun startSynchronization(acc: UUID): Boolean {
        // Launch synchronizer thread
        val activeAccount = getAccount(acc)
        Thread(Synchronizer(this, SyncMode.NORMAL, listOf(activeAccount))).start()
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

    fun getSpendingAccounts() : List<WalletAccount<*, *>> {
        return accounts.values.filter { it.canSpend() }
    }

    fun getSpendingAccountsWithBalance() : List<WalletAccount<*, *>> {
        return accounts.values.filter { it.canSpend() && it.accountBalance.spendable.isPositive }
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

    fun getAcceptableAssetTypes(address: String): List<GenericAssetInfo> {
        return walletModules.values
                .flatMap { it.getSupportedAssets() }
                .distinctBy { it.id }
                .filter { it.isMineAddress(address)}
                .toList()
    }

    fun getAssetTypes(): List<GenericAssetInfo> {
        return accounts.values.map { it.coinType }.distinct()
    }

    fun parseAddress(address: String): List<GenericAddress> {
        return walletModules.values
                .flatMap { it.getSupportedAssets() }
                .distinctBy { it.id }
                .mapNotNull { genericAssetInfo ->
            try {
                        genericAssetInfo.parseAddress(address)
                    } catch (ex: AddressMalformedException) { null }
                }
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
        MALFORMED_OUTGOING_TRANSACTIONS_FOUND
    }
}
