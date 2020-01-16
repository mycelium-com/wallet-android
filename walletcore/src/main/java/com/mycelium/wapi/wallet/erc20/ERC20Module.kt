package com.mycelium.wapi.wallet.erc20

import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.GenericModule
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import org.web3j.crypto.Credentials
import org.web3j.protocol.http.HttpService
import java.util.*

class ERC20Module(
        private val secureStore: SecureKeyValueStore,
        private val web3jServices: List<HttpService>,
        metaDataStorage: IMetaDataStorage) : GenericModule(metaDataStorage), WalletModule {
    private val accounts = mutableMapOf<UUID, ERC20Account>()
    override val id = ID

    override fun createAccount(config: Config): WalletAccount<*> {
        val result: WalletAccount<*>
        val baseLabel: String
        when (config) {
            is ERC20Config -> {
                val credentials = deriveKey()
                val token = config.token as ERC20Token
                baseLabel = token.name
                result = ERC20Account(token, credentials, web3jServices)
            }
            else -> {
                throw NotImplementedError("Unknown config")
            }
        }
        accounts[result.id] = result
        storeLabel(result.id, baseLabel)
        return result
    }

    override fun canCreateAccount(config: Config) = config is ERC20Config

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean {
        return if (walletAccount is ERC20Account) {
            accounts.remove(walletAccount.id)
            true
        } else {
            false
        }
    }

    override fun getAccounts() = accounts.values.toList()

    override fun getAccountById(id: UUID) = accounts[id]

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> = emptyMap()

    private fun deriveKey(): Credentials {
        val seed = MasterSeedManager.getMasterSeed(secureStore, AesKeyCipher.defaultKeyCipher())
        val rootNode = HdKeyNode.fromSeed(seed.bip32Seed, null)
        val path = "m/44'/60'/0'/0/0"

        val privKey = HexUtils.toHex(rootNode.createChildNode(HdKeyPath.valueOf(path)).privateKey.privateKeyBytes)
        return Credentials.create(privKey)
    }

    companion object {
        const val ID: String = "ERC20"
    }
}

fun WalletManager.getERC20Accounts() = getAccounts().filter { it is ERC20Account && it.isVisible }