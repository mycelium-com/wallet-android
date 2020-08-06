package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mrd.bitlib.util.HexUtils
import com.mycelium.generated.wallet.database.WalletDB

import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.fio.coins.FIOMain
import com.mycelium.wapi.wallet.fio.coins.FIOTest
import com.mycelium.wapi.wallet.genericdb.FioAccountBacking
import com.mycelium.wapi.wallet.genericdb.Backing
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import java.util.*


class FIOModule(
        private val secureStore: SecureKeyValueStore,
        private val backing: Backing<FioAccountContext>,
        private val walletDB: WalletDB,
        networkParameters: NetworkParameters,
        metaDataStorage: IMetaDataStorage,
        private val accountListener: AccountListener?) : WalletModule(metaDataStorage) {
    
    val password = ""
    fun getBip44Path(accountIndex: Int): HdKeyPath = HdKeyPath.valueOf("m/44'/60'/$accountIndex'/0/0")
    private val coinType = if (networkParameters.isProdnet) FIOMain else FIOTest

    private val accounts = mutableMapOf<UUID, FioAccount>()
    override val id = ID

    init {
        assetsList.add(coinType)
    }

    override fun getAccountById(id: UUID): WalletAccount<*>? {
        return accounts[id]
    }

    override fun setCurrencySettings(currencySettings: CurrencySettings) {
        this.settings = currencySettings as FIOSettings
    }

    override fun getAccounts(): List<WalletAccount<*>> = accounts.values.toList()

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> =
            backing.loadAccountContexts()
                    .associateBy({ it.uuid }, { ethAccountFromUUID(it.uuid) })

    override fun canCreateAccount(config: Config) = config is FIOMasterseedConfig
            || config is FioAddressConfig

    override fun createAccount(config: Config): WalletAccount<*> {
        val result: WalletAccount<*>
        val baseLabel: String
        when (config) {
            is FIOMasterseedConfig -> {
                val credentials = deriveKey()

                val accountContext = createAccountContext(credentials.ecKeyPair.toUUID())
                backing.createAccountContext(accountContext)
                baseLabel = accountContext.accountName
                val ethAccountBacking = FioAccountBacking(walletDB, accountContext.uuid, coinType)
                result = FioAccount(accountContext, credentials, ethAccountBacking, accountListener, blockchainService)
            }
            is FioAddressConfig -> {
                val uuid = UUID.nameUUIDFromBytes(config.address.getBytes())
                secureStore.storePlaintextValue(uuid.toString().toByteArray(),
                        config.address.addressString.toByteArray())
                val accountContext = createAccountContext(uuid)
                backing.createAccountContext(accountContext)
                baseLabel = accountContext.accountName
                val ethAccountBacking = FioAccountBacking(walletDB, accountContext.uuid, coinType)
                result = FioAccount(accountContext, address = config.address, backing = ethAccountBacking,
                        accountListener = accountListener, blockchainService = blockchainService)
            }
            else -> {
                throw NotImplementedError("Unknown config")
            }
        }
        accounts[result.id] = result
        result.label = createLabel(baseLabel)
        storeLabel(result.id, result.label)
        return result
    }

    private fun ethAccountFromUUID(uuid: UUID): FioAccount {
        return if (secureStore.hasCiphertextValue(uuid.toString().toByteArray())) {
            val credentials = Credentials.create(Keys.deserialize(
                    secureStore.getDecryptedValue(uuid.toString().toByteArray(), AesKeyCipher.defaultKeyCipher())))
            val accountContext = createAccountContext(uuid)
            val ethAccountBacking = FioAccountBacking(walletDB, accountContext.uuid, coinType)
            val ethAccount = FioAccount(accountContext, credentials, ethAccountBacking, accountListener, blockchainService)
            accounts[ethAccount.id] = ethAccount
            ethAccount
        } else {
            val accountContext = createAccountContext(uuid)
            val ethAddress = FioAddress(coinType, secureStore.getPlaintextValue(uuid.toString().toByteArray()).toString())
            val ethAccountBacking = FioAccountBacking(walletDB, accountContext.uuid, coinType)
            val ethAccount = FioAccount(accountContext, address = ethAddress, backing = ethAccountBacking,
                    accountListener = accountListener, blockchainService = blockchainService)
            accounts[ethAccount.id] = ethAccount
            ethAccount
        }
    }

    private fun deriveKey(): Credentials {
        val seed = MasterSeedManager.getMasterSeed(secureStore, AesKeyCipher.defaultKeyCipher())
        val rootNode = HdKeyNode.fromSeed(seed.bip32Seed, null)
        val path = getBip44Path(getCurrentBip44Index() + 1)

        val privKey = HexUtils.toHex(rootNode.createChildNode(path).privateKey.privateKeyBytes)
        val credentials = Credentials.create(privKey)

        secureStore.encryptAndStoreValue(credentials.ecKeyPair.toUUID().toString().toByteArray(),
                Keys.serialize(credentials.ecKeyPair), AesKeyCipher.defaultKeyCipher())
        return credentials
    }

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean {
        return if (walletAccount is FioAccount) {
            if (secureStore.hasCiphertextValue(walletAccount.id.toString().toByteArray())) {
                secureStore.deleteEncryptedValue(walletAccount.id.toString().toByteArray(), AesKeyCipher.defaultKeyCipher())
            } else {
                secureStore.deletePlaintextValue(walletAccount.id.toString().toByteArray())
            }
            backing.deleteAccountContext(walletAccount.id)
            walletAccount.clearBacking()
            accounts.remove(walletAccount.id)
            true
        } else {
            false
        }
    }

    private fun createAccountContext(uuid: UUID): FioAccountContext {
        val accountContextInDB = backing.loadAccountContext(uuid)
        return if (accountContextInDB != null) {
            FioAccountContext(accountContextInDB.uuid,
                    accountContextInDB.currency,
                    accountContextInDB.accountName,
                    accountContextInDB.balance,
                    backing::updateAccountContext,
                    accountContextInDB.accountIndex,
                    accountContextInDB.enabledTokens,
                    accountContextInDB.archived,
                    accountContextInDB.blockHeight,
                    accountContextInDB.nonce)
        } else {
            FioAccountContext(
                    uuid,
                    coinType,
                    "FIO ${getCurrentBip44Index() + 2}",
                    Balance.getZeroBalance(coinType),
                    backing::updateAccountContext,
                    getCurrentBip44Index() + 1)
        }
    }

    private fun getCurrentBip44Index() = accounts.values
            .filter { it.isDerivedFromInternalMasterseed }
            .maxBy { it.accountIndex }
            ?.accountIndex
            ?: -1

    companion object {
        const val ID: String = "FIO"
    }
}

fun WalletManager.getFioAccounts() = getAccounts().filter { it is FioAccount && it.isVisible }
fun WalletManager.getActiveFioAccounts() = getAccounts().filter { it is FioAccount && it.isVisible && it.isActive }