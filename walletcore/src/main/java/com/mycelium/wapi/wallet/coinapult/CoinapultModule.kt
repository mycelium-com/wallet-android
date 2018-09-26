package com.mycelium.wapi.wallet.coinapult

import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import java.util.*


class CoinapultModule : WalletModule {
    override fun getId(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createAccount(config: Config): WalletAccount<*, *>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canCreateAccount(config: Config): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteAccount(walletAccount: WalletAccount<*, *>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}