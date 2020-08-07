package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.util.HexUtils
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.eth.toUUID
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import fiofoundation.io.fiosdk.FIOSDK
import fiofoundation.io.fiosdk.implementations.SoftKeySignatureProvider
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import java.util.*

class FIOModule(
        private val secureStore: SecureKeyValueStore,
        val credentials: Credentials? = null,
        private val walletDB: WalletDB,
        metaDataStorage: IMetaDataStorage,
        private val fioKeyManager: FioKeyManager,
        private val accountListener: AccountListener?
) : WalletModule(metaDataStorage) {

    companion object {

        const val ID: String = "FIO"
        const val URL: String = "HERE_URL"
    }

    private val accounts = mutableMapOf<UUID, FioAccount>()

    private fun getFioSdk(accountIndex: Int): FIOSDK {
        val publicKey = HexUtils.toHex(fioKeyManager.getFioPublicKey(accountIndex).publicKeyBytes)
        val privateKey = HexUtils.toHex(fioKeyManager.getFioPrivateKey(accountIndex).privateKeyBytes)
        return FIOSDK(privateKey, publicKey, "", null, SoftKeySignatureProvider(), URL)
    }

    override val id: String
        get() = "Fio"

    override fun createAccount(config: Config): WalletAccount<*> {
        val newIndex = getCurrentBip44Index() + 1
        val newAccount = FioAccount(fioKeyManager, getFioSdk(newIndex), deriveKey(newIndex))
        accounts[newAccount.id] = newAccount
        return newAccount
    }

    private fun deriveKey(newIndex: Int): Credentials {
        val fioPrivateKey = fioKeyManager.getFioPrivateKey(newIndex)
        val privKey = HexUtils.toHex(fioPrivateKey.privateKeyBytes)
        val credentials = Credentials.create(privKey)

        secureStore.encryptAndStoreValue(credentials.ecKeyPair.toUUID().toString().toByteArray(),
                Keys.serialize(credentials.ecKeyPair), AesKeyCipher.defaultKeyCipher())
        return credentials
    }

    private fun getCurrentBip44Index() = accounts.values
            .filter { it.isDerivedFromInternalMasterseed }.size

    override fun canCreateAccount(config: Config): Boolean {
        return true
    }

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean {
        accounts.remove(walletAccount.id)
        return true
    }

    override fun getAccounts(): List<WalletAccount<*>> {
        return accounts.values.toList()
    }

    override fun getAccountById(id: UUID): WalletAccount<*>? {
        return accounts[id]
    }
}

fun WalletManager.getFioAccounts() = getAccounts().filter { it is FioAccount && it.isVisible }
fun WalletManager.getActiveFioAccounts() = getAccounts().filter { it is FioAccount && it.isVisible && it.isActive }