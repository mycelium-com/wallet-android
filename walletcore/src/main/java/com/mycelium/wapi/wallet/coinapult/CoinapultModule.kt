package com.mycelium.wapi.wallet.coinapult

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import java.util.*


class CoinapultModule(val accountKey: InMemoryPrivateKey
                      , val networkParameters: NetworkParameters) : WalletModule {
    override fun getId(): String = "coinapult module"

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        return mapOf()
    }

    override fun createAccount(config: Config): WalletAccount<*, *>? {
        var result: WalletAccount<*, *>? = null
        if (config is CoinapultConfig) {
            result = CoinapultAccount(CoinapultAccountContext(false), accountKey
                    , networkParameters, config.currency)
        }
        result?.synchronize(SyncMode.NORMAL)
        return result
    }

    override fun canCreateAccount(config: Config): Boolean {
        return config is CoinapultConfig
    }

    override fun deleteAccount(walletAccount: WalletAccount<*, *>): Boolean {
        return true
    }

}