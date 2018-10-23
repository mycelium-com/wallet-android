package com.mycelium.wapi.wallet.coinapult

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.AccountListener
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletBacking
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import java.util.*


class CoinapultModule(val accountKey: InMemoryPrivateKey
                      , val networkParameters: NetworkParameters
                      , val coinapultApi: CoinapultApi
                      , val backing: WalletBacking<CoinapultAccountContext, CoinapultTransaction>
                      , val listener: AccountListener) : WalletModule {
    override fun getId(): String = "coinapult module"

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        val result = mutableMapOf<UUID, WalletAccount<*, *>>()
        backing.loadAccountContexts().forEach { context ->
            val id = CoinapultUtils.getGuidForAsset(context.currency, accountKey.publicKey.publicKeyBytes)
            val account = CoinapultAccount(context, accountKey, coinapultApi, backing.getAccountBacking(id)
                    , networkParameters, context.currency, listener)
            result[account.id] = account
        }
        return result
    }

    override fun createAccount(config: Config): WalletAccount<*, *>? {
        var result: WalletAccount<*, *>? = null
        if (config is CoinapultConfig) {
            val id = CoinapultUtils.getGuidForAsset(config.currency, accountKey.publicKey.publicKeyBytes)
            val address = coinapultApi.getAddress(config.currency, null)
            val context = CoinapultAccountContext(id
                    , address!!
                    , false, config.currency)
            backing.createAccountContext(context)
            result = CoinapultAccount(context, accountKey
                    , coinapultApi, backing.getAccountBacking(id), networkParameters, config.currency, listener)


        }
        result?.synchronize(SyncMode.NORMAL)
        return result
    }

    override fun canCreateAccount(config: Config): Boolean {
        return config is CoinapultConfig
    }

    override fun deleteAccount(walletAccount: WalletAccount<*, *>): Boolean {
        backing.deleteAccountContext(walletAccount.id)
        return true
    }

}