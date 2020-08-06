package com.mycelium.wapi.wallet.fio

import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import java.util.*


class FIOModule(
        private val secureStore: SecureKeyValueStore,
        private val walletDB: WalletDB,
        metaDataStorage: IMetaDataStorage,
        private val accountListener: AccountListener?) : WalletModule(metaDataStorage) {

    companion object {
        const val ID: String = "FIO"
    }

    override val id: String
        get() = "Fio"

    override fun createAccount(config: Config): WalletAccount<*> {
        TODO("Not yet implemented")
    }

    override fun canCreateAccount(config: Config): Boolean {
        TODO("Not yet implemented")
    }

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean {
        TODO("Not yet implemented")
    }

    override fun getAccounts(): List<WalletAccount<*>> {
        TODO("Not yet implemented")
    }

    override fun getAccountById(id: UUID): WalletAccount<*>? {
        TODO("Not yet implemented")
    }
}

fun WalletManager.getFioAccounts() = getAccounts().filter { it is FioAccount && it.isVisible }
fun WalletManager.getActiveFioAccounts() = getAccounts().filter { it is FioAccount && it.isVisible && it.isActive }