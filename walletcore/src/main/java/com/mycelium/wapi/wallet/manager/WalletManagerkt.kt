package com.mycelium.wapi.wallet.manager

import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import java.util.*


object WalletManagerkt {
    private val accounts = mutableMapOf<UUID, WalletAccount<*, *>>()
    private val walletModules = mutableSetOf<WalletModule>()

    private var state: State = State.OFF
    @Volatile
    private var activeAccountId: UUID? = null

    var isNetworkConnected = false

    fun add(walletModule: WalletModule) = walletModules.add(walletModule)

    fun remove(walletModule: WalletModule) = walletModules.remove(walletModule)

    fun init() {
        for (walletModule in walletModules) {
            accounts.putAll(walletModule.loadAccounts())
        }
    }

    fun getState() = state

    fun getAccountIds(): List<UUID> = accounts.keys.toList()

    fun isMy(address: GenericAddress) = getAccountBy(address) != null

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

    fun createAccounts(config: Config): List<UUID> {
        val result = mutableMapOf<UUID, WalletAccount<*, *>>()
        walletModules.forEach {
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
        accounts.remove(id)
    }

    fun hasAccount(id: UUID): Boolean = accounts.containsKey(id)

    fun getAccount(id: UUID): WalletAccount<*, *>? = accounts[id]

    @JvmOverloads
    fun startSynchronization(mode: SyncMode = SyncMode.NORMAL_FORCED) {
//        if (!isNetworkConnected) {
//            return
//        }
        Thread(Synchronizer(mode)).start()
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

    //TODO what about not bitcoin network
    fun getBlockHeight(coinType: CryptoCurrency): Int {
        //TODO: should we iterate over all accounts and find max blockheight ?
        val account = accounts.values.elementAt(0)
        return account.blockChainHeight
    }

    fun activateFirstAccount() {
//        filterAndConvert(MAIN_SEED_BTC_HD_ACCOUNT).get(0).activateAccount()
    }
}