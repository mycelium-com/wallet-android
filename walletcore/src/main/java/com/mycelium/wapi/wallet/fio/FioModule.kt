package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.HexUtils
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.eth.toUUID
import com.mycelium.wapi.wallet.fio.coins.FIOMain
import com.mycelium.wapi.wallet.fio.coins.FIOTest
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import fiofoundation.io.fiosdk.FIOSDK
import fiofoundation.io.fiosdk.interfaces.ISerializationProvider
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import java.util.*

class FIOModule(
        private val serializationProvider : ISerializationProvider,
        private val secureStore: SecureKeyValueStore,
        private val walletDB: WalletDB,
        metaDataStorage: IMetaDataStorage,
        private val fioKeyManager: FioKeyManager,
        private val accountListener: AccountListener?
) : WalletModule(metaDataStorage) {

    companion object {
        const val ID: String = "FIO"
        const val URL: String = FIOTest.url
    }

    private val accounts = mutableMapOf<UUID, FioAccount>()

    private fun getFioSdk(accountIndex: Int, config: FIOConfig): FIOSDK {
        val fioPublicKey = fioKeyManager.getFioPublicKey(accountIndex)

        val publicKey = fioKeyManager.formatPubKey(fioPublicKey)
        val privateKey = fioKeyManager.getFioPrivateKey(accountIndex).getBase58EncodedPrivateKey(NetworkParameters.productionNetwork)
        val url = if (config.isTestnet) FIOTest.url else FIOMain.url
        return FIOSDK.getInstance(privateKey, publicKey, serializationProvider, url)
    }

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> {
        return hashMapOf()
    }

    override val id: String
        get() = "Fio"

    override fun createAccount(config: Config): WalletAccount<*> {
        val newIndex = getCurrentBip44Index() + 1
        val newAccount = FioAccount(fioKeyManager, getFioSdk(newIndex, config as FIOConfig), deriveKey(newIndex))
        val baseLabel = "FIO $newIndex"
        newAccount.label = createLabel(baseLabel)
        storeLabel(newAccount.id, newAccount.label)
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
        return config is FIOConfig
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