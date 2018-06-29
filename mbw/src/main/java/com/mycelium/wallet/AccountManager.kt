package com.mycelium.wallet

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import com.google.common.collect.ImmutableMap
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.AccountListChanged
import com.mycelium.wallet.event.ExtraAccountsChanged
import com.mycelium.wallet.event.SelectedAccountChanged
import com.mycelium.wapi.wallet.AccountProvider
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletAccount.Type
import com.mycelium.wapi.wallet.WalletAccount.Type.*
import com.mycelium.wapi.wallet.WalletManager
import com.squareup.otto.Subscribe
import java.util.*
import java.util.concurrent.Semaphore
import kotlin.collections.HashMap

object AccountManager : AccountProvider {
    val accounts: HashMap<UUID, WalletAccount> = hashMapOf()
    private val accountsSemaphore = Semaphore(100)
    private val archivedAccounts: HashMap<UUID, WalletAccount> = hashMapOf()
    private val archivedAccountsSemaphore = Semaphore(100)
    private val masterSeedAccounts: HashMap<UUID, WalletAccount> = hashMapOf()
    private val masterSeedAccountsSemaphore  = Semaphore(100)

    init {
        Handler(Looper.getMainLooper()).post {
            MbwManager.getInstance(WalletApplication.getInstance()).eventBus.register(this)
        }
        FillAccountsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    class FillAccountsTask : AsyncTask<Void, Void, Void>() {
        val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())!!
        override fun doInBackground(vararg params: Void): Void? {
            val walletManager: WalletManager = mbwManager.getWalletManager(false)
            accountsSemaphore.acquire(100)
            accounts.clear()
            accounts.putAll(walletManager.activeAccounts)
            accountsSemaphore.release(100)
            archivedAccountsSemaphore.acquire(100)
            archivedAccounts.clear()
            archivedAccounts.putAll(walletManager.archivedAccounts)
            archivedAccountsSemaphore.release(100)
            masterSeedAccountsSemaphore.acquire(100)
            masterSeedAccounts.clear()
            masterSeedAccounts.putAll(walletManager.activeMasterseedAccounts)
            masterSeedAccountsSemaphore.release(100)
            return null
        }

        override fun onPostExecute(result: Void?) {
            mbwManager.eventBus.post(AccountListChanged())
        }
    }

    override fun getAccounts(): ImmutableMap<UUID, WalletAccount> = ImmutableMap.copyOf<UUID, WalletAccount>(accounts)

    fun getBTCSingleAddressAccounts(): ImmutableMap<UUID, WalletAccount> {
        accountsSemaphore.acquire()
        val filteredAccounts = ImmutableMap.copyOf<UUID, WalletAccount>(accounts.filter {
            it.value.type == BTCSINGLEADDRESS && !Utils.checkIsLinked(it.value, accounts.values)
        })
        accountsSemaphore.release()
        return filteredAccounts
    }

    fun getBTCBip44Accounts() = getAccountsByType(BTCBIP44)

    fun getBCHSingleAddressAccounts() = getAccountsByType(BCHSINGLEADDRESS)

    fun getBCHBip44Accounts() = getAccountsByType(BCHBIP44)

    fun getCoinapultAccounts() = getAccountsByType(COINAPULT)

    fun getColuAccounts() = getAccountsByType(COLU)

    fun getDashAccounts() = getAccountsByType(DASH)

    fun getActiveAccounts(): ImmutableMap<UUID, WalletAccount> {
        accountsSemaphore.acquire()
        val filteredAccounts = ImmutableMap.copyOf<UUID, WalletAccount>(
                synchronized(accounts) {
                    accounts.filter {
                        it.value.isVisible
                    }
                })
        accountsSemaphore.release()
        return filteredAccounts
    }

    private fun getAccountsByType(coinType: Type): ImmutableMap<UUID, WalletAccount> {
        accountsSemaphore.acquire()
        val filteredAccounts = ImmutableMap.copyOf<UUID, WalletAccount>(accounts.filter {
            it.value.type == coinType && it.value.isVisible
        })
        accountsSemaphore.release()
        return filteredAccounts
    }

    fun getBTCMasterSeedAccounts(): ImmutableMap<UUID, WalletAccount> {
        masterSeedAccountsSemaphore.acquire()
        val filteredAccounts = ImmutableMap.copyOf<UUID, WalletAccount>(masterSeedAccounts.filter {
            it.value.type == BTCBIP44 && it.value.isVisible
        })
        masterSeedAccountsSemaphore.release()
        return filteredAccounts
    }

    fun getArchivedAccounts() : ImmutableMap<UUID, WalletAccount> {
        archivedAccountsSemaphore.acquire()
        val filteredAccounts = ImmutableMap.copyOf<UUID, WalletAccount>(archivedAccounts.filter {
            it.value.isVisible
        })
        archivedAccountsSemaphore.release()
        return filteredAccounts
    }

    override fun getAccount(uuid: UUID?): WalletAccount? = accounts[uuid]

    override fun hasAccount(uuid: UUID?): Boolean = accounts.containsKey(uuid)

    private fun HashMap<UUID, WalletAccount>.putAll(from: List<WalletAccount>) {
        val result: MutableMap<UUID, WalletAccount> = mutableMapOf()
        for (walletAccount in from) {
            result[walletAccount.id] = walletAccount
        }
        putAll(result)
    }

    @Subscribe
    fun accountChanged(event: AccountChanged) {
        FillAccountsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    @Subscribe
    fun selectedAccountChanged(event: SelectedAccountChanged) {
        FillAccountsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    @Subscribe
    fun extraAccountsChanged(event: ExtraAccountsChanged) {
        FillAccountsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }
}

