package com.mycelium.wapi.wallet.bch.bip44

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcTransaction
import com.mycelium.wapi.wallet.btc.WalletManagerBacking
import com.mycelium.wapi.wallet.btc.bip44.*
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccountContext
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import java.util.*


class Bip44BCHHDModule(internal val backing: WalletManagerBacking<SingleAddressAccountContext, BtcTransaction>
                      , internal val secureStore: SecureKeyValueStore
                      , internal val networkParameters: NetworkParameters
                      , internal var _wapi: Wapi) : WalletModule {


    override fun getId(): String = "Bip44HD"

    private val accounts = mutableMapOf<UUID, Bip44BCHAccount>()

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        return mapOf()
    }

    override fun createAccount(config: Config): WalletAccount<*, *>? {
        var result: WalletAccount<*, *>? = null
        return result
    }

    fun getAccountByIndex(index: Int): HDAccount? {
        return accounts.values.firstOrNull { it.accountIndex == index }
    }

    override fun canCreateAccount(config: Config): Boolean = config is HDConfig

    override fun deleteAccount(walletAccount: WalletAccount<*, *>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}