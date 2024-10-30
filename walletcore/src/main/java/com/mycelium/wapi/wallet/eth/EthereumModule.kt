package com.mycelium.wapi.wallet.eth

import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mrd.bitlib.util.HexUtils
import com.mycelium.generated.wallet.database.WalletDB

import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.eth.coins.EthMain
import com.mycelium.wapi.wallet.eth.coins.EthTest

import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import com.mycelium.wapi.wallet.genericdb.Backing
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import org.web3j.tx.ChainId
import java.util.*

class EthereumModule(
        private val secureStore: SecureKeyValueStore,
        private val backing: Backing<EthAccountContext>,
        private val walletDB: WalletDB,
        private val blockchainService: EthBlockchainService,
        networkParameters: NetworkParameters,
        metaDataStorage: IMetaDataStorage,
        private val accountListener: AccountListener?) : WalletModule(metaDataStorage) {

    var settings: EthereumSettings = EthereumSettings()
    val password = ""
    fun getBip44Path(accountIndex: Int): HdKeyPath = HdKeyPath.valueOf("m/44'/60'/$accountIndex'/0/0")
    private val coinType = if (networkParameters.isProdnet) EthMain else EthTest
    private val chainId = if (networkParameters.isProdnet) ChainId.MAINNET else CHAIN_ID_GOERLI

    private val accounts = mutableMapOf<UUID, EthAccount>()
    override val id = ID

    init {
        assetsList.add(coinType)
    }

    override fun getAccountById(id: UUID): WalletAccount<*>? {
        return accounts[id]
    }

    override fun setCurrencySettings(currencySettings: CurrencySettings) {
        this.settings = currencySettings as EthereumSettings
    }

    override fun getAccounts(): List<WalletAccount<*>> = accounts.values.toList()

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> =
            backing.loadAccountContexts()
                    .associateBy({ it.uuid }, { ethAccountFromUUID(it.uuid) })

    override fun canCreateAccount(config: Config) = config is EthereumMasterseedConfig
            || config is EthAddressConfig

    override fun createAccount(config: Config): WalletAccount<*> {
        val result: WalletAccount<*>
        val baseLabel: String
        when (config) {
            is EthereumMasterseedConfig -> {
                val credentials = deriveKey()

                val accountContext = createAccountContext(credentials.ecKeyPair.toUUID())
                backing.createAccountContext(accountContext.uuid, accountContext)
                baseLabel = accountContext.accountName
                val ethAccountBacking = EthAccountBacking(walletDB, accountContext.uuid, coinType)
                result = EthAccount(chainId, accountContext, credentials, ethAccountBacking, accountListener, blockchainService)
            }
            is EthAddressConfig -> {
                val uuid = UUID.nameUUIDFromBytes(config.address.getBytes())
                secureStore.storePlaintextValue(uuid.toString().toByteArray(),
                        config.address.addressString.toByteArray())
                val accountContext = createAccountContext(uuid)
                backing.createAccountContext(accountContext.uuid, accountContext)
                baseLabel = accountContext.accountName
                val ethAccountBacking = EthAccountBacking(walletDB, accountContext.uuid, coinType)
                result = EthAccount(chainId, accountContext, address = config.address, backing = ethAccountBacking,
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

    private fun ethAccountFromUUID(uuid: UUID): EthAccount {
        return if (secureStore.hasCiphertextValue(uuid.toString().toByteArray())) {
            val credentials = Credentials.create(Keys.deserialize(
                    secureStore.getDecryptedValue(uuid.toString().toByteArray(), AesKeyCipher.defaultKeyCipher())))
            val accountContext = createAccountContext(uuid)
            val ethAccountBacking = EthAccountBacking(walletDB, accountContext.uuid, coinType)
            val ethAccount = EthAccount(chainId, accountContext, credentials, ethAccountBacking, accountListener, blockchainService)
            accounts[ethAccount.id] = ethAccount
            ethAccount
        } else {
            val accountContext = createAccountContext(uuid)
            val ethAddress = EthAddress(coinType, String(secureStore.getPlaintextValue(uuid.toString().toByteArray())))
            val ethAccountBacking = EthAccountBacking(walletDB, accountContext.uuid, coinType)
            val ethAccount = EthAccount(chainId, accountContext, address = ethAddress, backing = ethAccountBacking,
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
        return if (walletAccount is EthAccount) {
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

    private fun createAccountContext(uuid: UUID): EthAccountContext {
        val accountContextInDB = backing.loadAccountContext(uuid)
        return if (accountContextInDB != null) {
            EthAccountContext(accountContextInDB.uuid,
                    accountContextInDB.currency,
                    accountContextInDB.accountName,
                    accountContextInDB.balance,
                { backing.updateAccountContext(it.uuid, it) },
                    backing::loadAccountContext,
                    accountContextInDB.accountIndex,
                    accountContextInDB.enabledTokens,
                    accountContextInDB.archived,
                    accountContextInDB.blockHeight,
                    accountContextInDB.nonce)
        } else {
            EthAccountContext(
                    uuid,
                    coinType,
                    "Ethereum ${getCurrentBip44Index() + 2}",
                    Balance.getZeroBalance(coinType),
                { backing.updateAccountContext(it.uuid, it) },
                    backing::loadAccountContext,
                    getCurrentBip44Index() + 1)
        }
    }

    private fun getCurrentBip44Index() = accounts.values
            .filter { it.isDerivedFromInternalMasterseed() }
            .maxByOrNull { it.accountIndex }
            ?.accountIndex
            ?: -1

    companion object {
        private const val CHAIN_ID_GOERLI: Byte = 5
        const val ID: String = "Ethereum"
    }
}

fun WalletManager.getEthAccounts() = getAccounts().filter { it is EthAccount && it.isVisible() }
fun WalletManager.getActiveEthAccounts() = getAccounts().filter { it is EthAccount && it.isVisible() && it.isActive }
