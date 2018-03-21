package com.mycelium.wallet

import android.os.Handler
import android.os.Looper
import com.google.common.collect.ImmutableMap
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.ExtraAccountsChanged
import com.mycelium.wallet.event.SelectedAccountChanged
import com.mycelium.wapi.wallet.AccountProvider
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletAccount.Type
import com.mycelium.wapi.wallet.WalletAccount.Type.*
import com.mycelium.wapi.wallet.WalletManager
import com.squareup.otto.Subscribe
import java.util.*

object AccountManager : AccountProvider {
    val accounts: HashMap<UUID, WalletAccount> = hashMapOf()
    val archivedAccounts: HashMap<UUID, WalletAccount> = hashMapOf()
    val masterSeedAccounts: HashMap<UUID, WalletAccount> = hashMapOf()

    init {
        Handler(Looper.getMainLooper()).post({
            MbwManager.getInstance(WalletApplication.getInstance()).eventBus.register(this)
        })
        fillAccounts()
    }

    private fun fillAccounts() {
        val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
        //        val coinapultManager : CoinapultManager = mbwManager.coinapultManager
        //        val coluManager : ColuManager = mbwManager.coluManager
        val walletManager: WalletManager = mbwManager.getWalletManager(false)
        accounts.clear()
        accounts.putAll(walletManager.activeAccounts)
        archivedAccounts.clear()
        archivedAccounts.putAll(walletManager.archivedAccounts)
        masterSeedAccounts.clear()
        masterSeedAccounts.putAll(walletManager.activeMasterseedAccounts)
    }

    override fun getAccounts(): ImmutableMap<UUID, WalletAccount> = ImmutableMap.copyOf<UUID, WalletAccount>(accounts)

    fun getBTCSingleAddressAccounts(): ImmutableMap<UUID, WalletAccount> = ImmutableMap.copyOf<UUID, WalletAccount>(accounts.filter {
        it.value.type == BTCSINGLEADDRESS && !Utils.checkIsLinked(it.value, accounts.values)
    })

    fun getBTCBip44Accounts() = getAccountsByType(BTCBIP44)

    fun getBCHSingleAddressAccounts() = getAccountsByType(BCHSINGLEADDRESS)

    fun getBCHBip44Accounts() = getAccountsByType(BCHBIP44)

    fun getCoinapultAccounts() = getAccountsByType(COINAPULT)

    fun getColuAccounts() = getAccountsByType(COLU)

    fun getDashAccounts() = getAccountsByType(DASH)

    fun getActiveAccounts(): ImmutableMap<UUID, WalletAccount> = ImmutableMap.copyOf<UUID, WalletAccount>(accounts.filter {
        it.value.isVisible
    })

    private fun getAccountsByType(coinType: Type): ImmutableMap<UUID, WalletAccount> = ImmutableMap.copyOf<UUID, WalletAccount>(accounts.filter {
        it.value.type == coinType && it.value.isVisible
    })

    fun getBTCMasterSeedAccounts(): ImmutableMap<UUID, WalletAccount> = ImmutableMap.copyOf<UUID, WalletAccount>(masterSeedAccounts.filter {
        it.value.type == BTCBIP44 && it.value.isVisible
    })

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
        fillAccounts()
    }

    @Subscribe
    fun selectedAccountChanged(event: SelectedAccountChanged) {
        fillAccounts()
    }

    @Subscribe
    fun extraAccountsChanged(event: ExtraAccountsChanged) {
        fillAccounts()
    }
}

