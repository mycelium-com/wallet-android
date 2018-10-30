package com.mycelium.wallet

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import com.google.common.collect.ImmutableMap
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.AccountListChanged
import com.mycelium.wallet.event.SelectedAccountChanged
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.coinapult.Currency
import com.mycelium.wapi.wallet.colu.ColuPubOnlyAccount
import com.squareup.otto.Subscribe
import java.util.*
import java.util.concurrent.Semaphore

object AccountManager : AccountProvider {
    val accounts: HashMap<UUID, WalletAccount<out GenericTransaction, out GenericAddress>> = hashMapOf()
    private val accountsSemaphore = Semaphore(100)
    private val archivedAccounts: HashMap<UUID, WalletAccount<out GenericTransaction, out GenericAddress>> = hashMapOf()
    private val archivedAccountsSemaphore = Semaphore(100)
    private val masterSeedAccounts: HashMap<UUID, WalletAccount<out GenericTransaction, out GenericAddress>> = hashMapOf()
    private val masterSeedAccountsSemaphore = Semaphore(100)

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
            accounts.putAll(walletManager.getActiveAccounts())
            accountsSemaphore.release(100)
            archivedAccountsSemaphore.acquire(100)
            archivedAccounts.clear()
            archivedAccounts.putAll(walletManager.getArchivedAccounts())
            archivedAccountsSemaphore.release(100)
            masterSeedAccountsSemaphore.acquire(100)
            masterSeedAccounts.clear()
            masterSeedAccounts.putAll(walletManager.getActiveMasterseedAccounts())
            masterSeedAccountsSemaphore.release(100)
            return null
        }

        override fun onPostExecute(result: Void?) {
            mbwManager.eventBus.post(AccountListChanged())
        }
    }

    override fun getAccounts(): ImmutableMap<UUID, WalletAccount<out GenericTransaction, out GenericAddress>> = ImmutableMap.copyOf<UUID, WalletAccount<out GenericTransaction, out GenericAddress>>(accounts)

    fun getBTCSingleAddressAccounts(): ImmutableMap<UUID, WalletAccount<out GenericTransaction, out GenericAddress>> =
            getFilteredAccounts(accountsSemaphore, accounts) {
                it.value is SingleAddressAccount && !Utils.checkIsLinked(it.value, accounts.values)
            }

    fun getBTCBip44Accounts() = getAccountsByType<HDAccount>()

    fun getBCHSingleAddressAccounts() = getAccountsByType<SingleAddressBCHAccount>()

    fun getBCHBip44Accounts() = getAccountsByType<Bip44BCHAccount>()

//    fun getCoinapultAccounts() = getAccountsByType<CoinapultAccount>()

//    fun getColuAccounts() = getAccountsByType<ColuAccount>()

    fun getActiveAccounts(): ImmutableMap<UUID, WalletAccount<out GenericTransaction, out GenericAddress>> =
            getFilteredAccounts(accountsSemaphore, accounts) {
                it.value.isVisible
            }

    private inline fun <reified T> getAccountsByType(): ImmutableMap<UUID, WalletAccount<out GenericTransaction, out GenericAddress>> =
            getFilteredAccounts(accountsSemaphore, accounts) {
                it.value is T && it.value.isVisible
            }

    fun getBTCMasterSeedAccounts(): ImmutableMap<UUID, WalletAccount<out GenericTransaction, out GenericAddress>> =
            getFilteredAccounts(masterSeedAccountsSemaphore, masterSeedAccounts) {
                it.value is HDAccount && it.value.isVisible
            }

    fun getArchivedAccounts(): ImmutableMap<UUID, WalletAccount<out GenericTransaction, out GenericAddress>> =
            getFilteredAccounts(archivedAccountsSemaphore, archivedAccounts) {
                it.value.isVisible
            }

    override fun getAccount(uuid: UUID?): WalletAccount<out GenericTransaction, out GenericAddress>? = accounts[uuid]

    override fun hasAccount(uuid: UUID?): Boolean = accounts.containsKey(uuid)

    private fun HashMap<UUID, WalletAccount<out GenericTransaction, out GenericAddress>>.putAll(from: List<WalletAccount<out GenericTransaction, out GenericAddress>>) {
        val result: MutableMap<UUID, WalletAccount<out GenericTransaction, out GenericAddress>> = mutableMapOf()
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

    private fun getFilteredAccounts(s: Semaphore, accounts: Map<UUID, WalletAccount<out GenericTransaction, out GenericAddress>>,
                                    filter: (mapEntry: Map.Entry<UUID, WalletAccount<out GenericTransaction, out GenericAddress>>)
                                    -> Boolean): ImmutableMap<UUID, WalletAccount<out GenericTransaction, out GenericAddress>> {
        s.acquire()
        val filteredAccounts = ImmutableMap.copyOf<UUID, WalletAccount<out GenericTransaction, out GenericAddress>>(accounts.filter { filter(it) })
        s.release()
        return filteredAccounts
    }
}

fun WalletManager.getBTCSingleAddressAccounts() = getAccounts().filter { it is SingleAddressAccount }

fun WalletManager.getColuAccounts(): List<WalletAccount<*, *>> = getAccounts().filter { it is ColuPubOnlyAccount && it.isVisible }

fun WalletManager.getCoinapultAccounts(): List<WalletAccount<*, *>> = getAccounts().filter { it is com.mycelium.wapi.wallet.coinapult.CoinapultAccount && it.isVisible }

fun WalletManager.getCoinapultAccount(currency: Currency):WalletAccount<*, *>? = getCoinapultAccounts().find { it.coinType == currency }