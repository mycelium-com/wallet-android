package com.mycelium.wapi.wallet.tron

import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mrd.bitlib.util.HexUtils
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.AccountListener
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.CurrencySettings
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.SecureKeyValueStore
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.eth.toUUID
import com.mycelium.wapi.wallet.genericdb.Backing
import com.mycelium.wapi.wallet.genericdb.TronAccountBacking
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import com.mycelium.wapi.wallet.tron.coins.TronMain
import com.mycelium.wapi.wallet.tron.coins.TronTest
import com.mycelium.wapi.wallet.tron.remote.TronBlockchainService
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import java.util.UUID

/**
 * TRON accounts derived from the wallet masterseed at m/44'/195'/n'/0/0 (one address per
 * account, like Ethereum) plus watch-only address accounts. Keys are stored the same way as
 * Ethereum's: the serialized secp256k1 key pair encrypted in [secureStore] under the account id.
 */
class TronModule(
    private val secureStore: SecureKeyValueStore,
    private val backing: Backing<TronAccountContext>,
    private val walletDB: WalletDB,
    val blockchainService: TronBlockchainService,
    networkParameters: NetworkParameters,
    metaDataStorage: IMetaDataStorage,
    private val accountListener: AccountListener?,
) : WalletModule(metaDataStorage) {

    var settings: TronSettings = TronSettings()
    fun getBip44Path(accountIndex: Int): HdKeyPath = HdKeyPath.valueOf("m/44'/195'/$accountIndex'/0/0")
    val coinType = if (networkParameters.isProdnet) TronMain else TronTest

    private val accounts = mutableMapOf<UUID, TronAccount>()
    override val id = ID

    init {
        assetsList.add(coinType)
    }

    override fun getAccountById(id: UUID): WalletAccount<*>? = accounts[id]

    override fun setCurrencySettings(currencySettings: CurrencySettings) {
        this.settings = currencySettings as TronSettings
    }

    override fun getAccounts(): List<WalletAccount<*>> = accounts.values.toList()

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> =
        backing.loadAccountContexts().associateBy({ it.uuid }, { tronAccountFromUUID(it.uuid) })

    override fun canCreateAccount(config: Config) = config is TronMasterseedConfig || config is TronAddressConfig

    override fun createAccount(config: Config): WalletAccount<*> {
        val result: TronAccount
        val baseLabel: String
        when (config) {
            is TronMasterseedConfig -> {
                val credentials = deriveKey()
                val uuid = credentials.ecKeyPair.toUUID()
                accounts[uuid]?.let { return it }
                val accountContext = createAccountContext(uuid)
                backing.createAccountContext(accountContext.uuid, accountContext)
                baseLabel = accountContext.accountName
                result = TronAccount(accountContext, credentials, createBacking(accountContext), accountListener, blockchainService)
            }
            is TronAddressConfig -> {
                val uuid = UUID.nameUUIDFromBytes(config.address.addressString.toByteArray())
                accounts[uuid]?.let { return it }
                secureStore.storePlaintextValue(uuid.toString().toByteArray(), config.address.addressString.toByteArray())
                val accountContext = createAccountContext(uuid, config.address.addressString)
                backing.createAccountContext(accountContext.uuid, accountContext)
                baseLabel = accountContext.accountName
                result = TronAccount(accountContext, address = config.address, backing = createBacking(accountContext),
                    accountListener = accountListener, blockchainService = blockchainService)
            }
            else -> throw NotImplementedError("Unknown config")
        }
        accounts[result.id] = result
        result.label = createLabel(baseLabel)
        storeLabel(result.id, result.label)
        return result
    }

    private fun createBacking(context: TronAccountContext) =
        TronAccountBacking(walletDB, context.uuid, coinType, coinType) { context.blockHeight }

    private fun tronAccountFromUUID(uuid: UUID): TronAccount {
        val account = if (secureStore.hasCiphertextValue(uuid.toString().toByteArray())) {
            val credentials = Credentials.create(Keys.deserialize(
                secureStore.getDecryptedValue(uuid.toString().toByteArray(), AesKeyCipher.defaultKeyCipher())))
            val accountContext = createAccountContext(uuid)
            TronAccount(accountContext, credentials, createBacking(accountContext), accountListener, blockchainService)
        } else {
            val accountContext = createAccountContext(uuid)
            val addressString = accountContext.address
                ?: String(secureStore.getPlaintextValue(uuid.toString().toByteArray())!!)
            TronAccount(accountContext, address = TronAddress(coinType, addressString), backing = createBacking(accountContext),
                accountListener = accountListener, blockchainService = blockchainService)
        }
        accounts[account.id] = account
        return account
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

    /** Key pair of [account] for dependent (TRC20) accounts; null for watch-only. */
    fun credentialsOf(account: TronAccount): Credentials? {
        val key = account.id.toString().toByteArray()
        if (!secureStore.hasCiphertextValue(key)) return null
        return Credentials.create(Keys.deserialize(secureStore.getDecryptedValue(key, AesKeyCipher.defaultKeyCipher())))
    }

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean {
        return if (walletAccount is TronAccount) {
            val key = walletAccount.id.toString().toByteArray()
            if (secureStore.hasCiphertextValue(key)) {
                secureStore.deleteEncryptedValue(key, AesKeyCipher.defaultKeyCipher())
            } else {
                secureStore.deletePlaintextValue(key)
            }
            backing.deleteAccountContext(walletAccount.id)
            walletAccount.clearBacking()
            accounts.remove(walletAccount.id)
            true
        } else {
            false
        }
    }

    private fun createAccountContext(uuid: UUID, watchAddress: String? = null): TronAccountContext {
        val accountContextInDB = backing.loadAccountContext(uuid)
        return if (accountContextInDB != null) {
            TronAccountContext(accountContextInDB.uuid,
                accountContextInDB.currency,
                accountContextInDB.accountName,
                accountContextInDB.balance,
                { backing.updateAccountContext(it.uuid, it) },
                backing::loadAccountContext,
                accountContextInDB.accountIndex,
                accountContextInDB.enabledTokens,
                accountContextInDB.archived,
                accountContextInDB.blockHeight,
                accountContextInDB.address)
        } else if (watchAddress != null) {
            TronAccountContext(uuid, coinType, "${coinType.name} Watch", Balance.getZeroBalance(coinType),
                { backing.updateAccountContext(it.uuid, it) },
                backing::loadAccountContext,
                accountIndex = -1,
                address = watchAddress)
        } else {
            val accountIndex = getCurrentBip44Index() + 1
            TronAccountContext(uuid, coinType, "${coinType.name} ${accountIndex + 1}", Balance.getZeroBalance(coinType),
                { backing.updateAccountContext(it.uuid, it) },
                backing::loadAccountContext,
                accountIndex)
        }
    }

    private fun getCurrentBip44Index() = accounts.values
        .filter { it.isDerivedFromInternalMasterseed() }
        .maxByOrNull { it.accountIndex }
        ?.accountIndex
        ?: -1

    companion object {
        const val ID: String = "Tron"
    }
}

fun WalletManager.getTronAccounts() = getAccounts().filter { it is TronAccount && it.isVisible() }
fun WalletManager.getActiveTronAccounts() = getAccounts().filter { it is TronAccount && it.isVisible() && it.isActive }
