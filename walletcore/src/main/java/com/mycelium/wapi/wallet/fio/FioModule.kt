package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.util.HexUtils
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import fiofoundation.io.fiosdk.FIOSDK
import fiofoundation.io.fiosdk.implementations.SoftKeySignatureProvider
import java.util.*


class FIOModule(
        private val secureStore: SecureKeyValueStore,
        private val walletDB: WalletDB,
        metaDataStorage: IMetaDataStorage,
        private val fioKeyManager: FioKeyManager,
        private val accountListener: AccountListener?
) : WalletModule(metaDataStorage) {

    companion object {

        const val ID: String = "FIO"
        const val URL: String = "HERE_URL"
    }

    private val fiosdk: FIOSDK by lazy {
        val publicKey = HexUtils.toHex(fioKeyManager.getFioPublicKey(1).publicKeyBytes)
        val privateKey = HexUtils.toHex(fioKeyManager.getFioPrivateKey(1).privateKeyBytes)
        return@lazy FIOSDK(privateKey, publicKey, "", null, SoftKeySignatureProvider(), URL)
    }

    override val id: String
        get() = "Fio"

    override fun createAccount(config: Config): WalletAccount<*> {
        return FioAccount(fioKeyManager, fiosdk)
    }

    override fun canCreateAccount(config: Config): Boolean {
        return true
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