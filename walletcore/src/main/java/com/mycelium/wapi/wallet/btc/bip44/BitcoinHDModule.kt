package com.mycelium.wapi.wallet.btc.bip44

import com.mrd.bitlib.crypto.Bip39
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.crypto.RandomSource
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher
import com.mycelium.wapi.wallet.btc.*
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext.Companion.ACCOUNT_TYPE_FROM_MASTERSEED
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext.Companion.ACCOUNT_TYPE_UNRELATED_X_PRIV
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext.Companion.ACCOUNT_TYPE_UNRELATED_X_PUB
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext.Companion.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext.Companion.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext.Companion.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccountContext
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.GenericModule
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import java.util.*
import kotlin.collections.ArrayList


class BitcoinHDModule(internal val backing: WalletManagerBacking<HDAccountContext, BtcTransaction>,
                      internal val secureStore: SecureKeyValueStore,
                      internal val networkParameters: NetworkParameters,
                      internal var _wapi: Wapi,
                      internal var settings: BTCSettings,
                      internal val metadataStorage: IMetaDataStorage,
                      internal val signatureProviders: ExternalSignatureProviderProxy?) : GenericModule(metadataStorage), WalletModule {

    init {
        assetsList.add(if (networkParameters.isProdnet) BitcoinMain.get() else BitcoinTest.get())
    }

    private val MASTER_SEED_ID = HexUtils.toBytes("D64CA2B680D8C8909A367F28EB47F990")

    override fun setCurrencySettings(currencySettings: CurrencySettings) {
        this.settings = currencySettings as BTCSettings
    }

    private val accounts = mutableMapOf<UUID, HDAccount>()
    override fun getId(): String = ID

    override fun getAccounts(): List<WalletAccount<*, *>> = accounts.values.toList()

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        val result = mutableMapOf<UUID, WalletAccount<*, *>>()
        val contexts = backing.loadBip44AccountContexts()
        for (context in contexts) {
            val keyManagerMap = HashMap<BipDerivationType, HDAccountKeyManager>()

            loadKeyManagers(context, keyManagerMap)

            val accountBacking = backing.getBip44AccountBacking(context.id)
            val account: WalletAccount<*, *>
            if (context.accountType == ACCOUNT_TYPE_UNRELATED_X_PUB) {
                account = HDPubOnlyAccount(context, keyManagerMap, networkParameters, accountBacking, _wapi)
            } else if (context.accountType == ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR) {
                account = HDAccountExternalSignature(context, keyManagerMap, networkParameters, accountBacking, _wapi,
                        signatureProviders!!.get(ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR), settings.changeAddressModeReference)
            } else if (context.accountType == ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER) {
                account = HDAccountExternalSignature(context, keyManagerMap, networkParameters, accountBacking, _wapi,
                        signatureProviders!!.get(ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER), settings.changeAddressModeReference)
            } else if (context.accountType == ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY) {
                account = HDAccountExternalSignature(context, keyManagerMap, networkParameters, accountBacking, _wapi,
                        signatureProviders!!.get(ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY), settings.changeAddressModeReference)
            } else {
                account = HDAccount(context, keyManagerMap, networkParameters, accountBacking, _wapi,
                        settings.changeAddressModeReference)
            }
            result[account.id] = account
            accounts[account.id] = account as HDAccount
        }
        return result
    }

    private fun loadKeyManagers(context: HDAccountContext, keyManagerMap: HashMap<BipDerivationType, HDAccountKeyManager>) {
        for (entry in context.indexesMap) {
            when (context.accountType) {
                ACCOUNT_TYPE_FROM_MASTERSEED -> keyManagerMap[entry.key] = HDAccountKeyManager(context.accountIndex, networkParameters, secureStore, entry.key)
                ACCOUNT_TYPE_UNRELATED_X_PRIV -> {
                    val subKeyStore = secureStore.getSubKeyStore(context.accountSubId)
                    keyManagerMap[entry.key] = HDAccountKeyManager(context.accountIndex, networkParameters, subKeyStore, entry.key)
                }
                ACCOUNT_TYPE_UNRELATED_X_PUB -> {
                    val subKeyStore = secureStore.getSubKeyStore(context.accountSubId)
                    keyManagerMap[entry.key] = HDPubOnlyAccountKeyManager(context.accountIndex, networkParameters, subKeyStore, entry.key)
                }
                ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR -> {
                    val subKeyStore = secureStore.getSubKeyStore(context.accountSubId)
                    keyManagerMap[entry.key] = HDPubOnlyAccountKeyManager(context.accountIndex, networkParameters, subKeyStore, entry.key)
                }
                ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER -> {
                    val subKeyStore = secureStore.getSubKeyStore(context.accountSubId)
                    keyManagerMap[entry.key] = HDPubOnlyAccountKeyManager(context.accountIndex, networkParameters, subKeyStore, entry.key)
                }
                ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY -> {
                    val subKeyStore = secureStore.getSubKeyStore(context.accountSubId)
                    keyManagerMap[entry.key] = HDPubOnlyAccountKeyManager(context.accountIndex, networkParameters, subKeyStore, entry.key)
                }
            }
        }
    }

    override fun createAccount(config: Config): WalletAccount<*, *> {
        var result: WalletAccount<*, *>? = null
        if (config is UnrelatedHDAccountConfig) {
            var cfg = config
            val accountIndex = 0  // use any index for this account, as we don't know and we don't care
            val keyManagerMap = HashMap<BipDerivationType, HDAccountKeyManager>()
            val derivationTypes = ArrayList<BipDerivationType>()

            // get a subKeyStorage, to ensure that the data for this key does not get mixed up
            // with other derived or imported keys.
            val secureStorage = secureStore.createNewSubKeyStore()

            for (hdKeyNode in cfg.hdKeyNodes) {
                val derivationType = hdKeyNode.derivationType
                derivationTypes.add(derivationType)
                if (hdKeyNode.isPrivateHdKeyNode) {
                    try {
                        keyManagerMap[derivationType] = HDAccountKeyManager.createFromAccountRoot(hdKeyNode, networkParameters,
                                accountIndex, secureStorage, AesKeyCipher.defaultKeyCipher(), derivationType)
                    } catch (invalidKeyCipher: KeyCipher.InvalidKeyCipher) {
                        throw RuntimeException(invalidKeyCipher)
                    }

                } else {
                    keyManagerMap[derivationType] = HDPubOnlyAccountKeyManager.createFromPublicAccountRoot(hdKeyNode,
                            networkParameters, accountIndex, secureStorage, derivationType)
                }
            }
            val id = keyManagerMap[derivationTypes[0]]!!.accountId


            // check if it already exists
            val isUpgrade = false
//            if (_walletAccounts.containsKey(id)) {
//                isUpgrade = !_walletAccounts.get(id).canSpend() && cfg.hdKeyNodes[0].isPrivateHdKeyNode
//                if (!isUpgrade) {
//                    return id
//                }
//            }
            // Generate the context for the account
            val context: HDAccountContext
            if (cfg.hdKeyNodes.get(0).isPrivateHdKeyNode) {
                context = HDAccountContext(id, accountIndex, false, ACCOUNT_TYPE_UNRELATED_X_PRIV,
                        secureStorage.subId, derivationTypes)
            } else {
                context = HDAccountContext(id, accountIndex, false, ACCOUNT_TYPE_UNRELATED_X_PUB,
                        secureStorage.subId, derivationTypes)
            }
            backing.beginTransaction()
            try {

                if (isUpgrade) {
                    backing.upgradeBip44AccountContext(context)
                } else {
                    backing.createBip44AccountContext(context)
                }
                // Get the accountBacking for the new account
                val accountBacking = backing.getBip44AccountBacking(context.id)

                // Create actual account
                result = if (cfg.hdKeyNodes[0].isPrivateHdKeyNode) {
                    HDAccount(context, keyManagerMap, networkParameters, accountBacking, _wapi, Reference(ChangeAddressMode.P2WPKH))
                } else {
                    HDPubOnlyAccount(context, keyManagerMap, networkParameters, accountBacking, _wapi)
                }

                // Finally persist context and add account
                context.persist(accountBacking)
                backing.setTransactionSuccessful()
            } finally {
                backing.endTransaction()
            }
        } else if (config is AdditionalHDAccountConfig) {
            // Get the master seed
            val masterSeed = getMasterSeed(AesKeyCipher.defaultKeyCipher())

            val accountIndex = getNextBip44Index()

            // Create the base keys for the account
            val keyManagerMap = HashMap<BipDerivationType, HDAccountKeyManager>()
            for (derivationType in BipDerivationType.values()) {
                // Generate the root private key
                val root = HdKeyNode.fromSeed(masterSeed.bip32Seed, derivationType)
                keyManagerMap[derivationType] = HDAccountKeyManager.createNew(root, networkParameters, accountIndex,
                        secureStore, AesKeyCipher.defaultKeyCipher(), derivationType)
            }

            // Generate the context for the account
            val context = HDAccountContext(keyManagerMap[BipDerivationType.BIP44]!!.accountId
                    , accountIndex, false, settings.defaultAddressType)

            backing.beginTransaction()
            try {
                backing.createBip44AccountContext(context)

                // Get the accountBacking for the new account
                val accountBacking = backing.getBip44AccountBacking(context.id)

                // Create actual account
                result = HDAccount(context, keyManagerMap, networkParameters, accountBacking, _wapi,
                        settings.changeAddressModeReference)

                // Finally persist context and add account
                context.persist(accountBacking)
                backing.setTransactionSuccessful()
            } finally {
                backing.endTransaction()
            }

        } else if (config is ExternalSignaturesAccountConfig) {
            val cfg = config
            val accountIndex = cfg.accountIndex
            val keyManagerMap = HashMap<BipDerivationType, HDAccountKeyManager>()
            val derivationTypes = ArrayList<BipDerivationType>()

            // get a subKeyStorage, to ensure that the data for this key does not get mixed up
            // with other derived or imported keys.
            val secureStorage = secureStore.createNewSubKeyStore()

            for (hdKeyNode in cfg.hdKeyNodes) {
                val derivationType = hdKeyNode.derivationType
                derivationTypes.add(derivationType)
                keyManagerMap[derivationType] = HDPubOnlyAccountKeyManager.createFromPublicAccountRoot(hdKeyNode,
                        networkParameters, accountIndex, secureStorage, derivationType)
            }
            val id = keyManagerMap[derivationTypes[0]]!!.accountId

            // Generate the context for the account
            val context = HDAccountContext(id, accountIndex, false, cfg.provider.biP44AccountType,
                    secureStorage.subId, derivationTypes, settings.defaultAddressType)
            backing.beginTransaction()
            try {
                backing.createBip44AccountContext(context)

                // Get the accountBacking for the new account
                val accountBacking = backing.getBip44AccountBacking(context.id)

                // Create actual account
                result = HDAccountExternalSignature(context, keyManagerMap, networkParameters, accountBacking, _wapi,
                        cfg.provider, settings.changeAddressModeReference)

                // Finally persist context and add account
                context.persist(accountBacking)
                backing.setTransactionSuccessful()
            } finally {
                backing.endTransaction()
            }
        }

        if (result == null) {
            throw IllegalStateException("Account can't be created")
        }
        accounts[result.id] = result as HDAccount

        result.label = createLabel(config, result.id)
        return result
    }

    private fun createLabel(config: Config, id: UUID): String? {
        // can't fetch hardware wallet's account labels for temp accounts
        // as we don't pass a signatureProviders and no need anyway
        if (config is ExternalSignaturesAccountConfig && signatureProviders == null)
            return null

        return createLabel(getBaseLabel(config), id)
    }

    private fun getBaseLabel(cfg: Config): String {
        return when (cfg) {
            is AdditionalHDAccountConfig -> "Account " + getNextBip44Index()
            is ExternalSignaturesAccountConfig ->
                signatureProviders!!.get(cfg.provider.biP44AccountType).labelOrDefault + " #" + (cfg.hdKeyNodes.get(0).index + 1)
            is UnrelatedHDAccountConfig -> if (cfg.hdKeyNodes.get(0).isPrivateHdKeyNode) "Account 1" else "Imported"
            else -> throw IllegalArgumentException("Unsupported config")
        }
    }

    /**
     * Get the master seed in plain text
     *
     * @param cipher the cipher used to decrypt the master seed
     * @return the master seed in plain text
     * @throws InvalidKeyCipher if the cipher is invalid
     */
    @Throws(InvalidKeyCipher::class)
    fun getMasterSeed(cipher: KeyCipher): Bip39.MasterSeed {
        val binaryMasterSeed = secureStore.getDecryptedValue(MASTER_SEED_ID, cipher)
        val masterSeed = Bip39.MasterSeed.fromBytes(binaryMasterSeed, false)
        if (!masterSeed.isPresent) {
            throw RuntimeException()
        }
        return masterSeed.get()
    }

    fun getAccountByIndex(index: Int): HDAccount? {
        return accounts.values.firstOrNull { it.accountIndex == index }
    }

    fun getCurrentBip44Index(): Int {
        var maxIndex = -1
        for (walletAccount in accounts.values) {
            maxIndex = Math.max(walletAccount.accountIndex, maxIndex)
        }
        return maxIndex
    }

    private fun getNextBip44Index(): Int {
        var maxIndex = -1
        for (walletAccount in accounts.values) {
            maxIndex = Math.max(walletAccount.accountIndex, maxIndex)
        }
        return maxIndex + 1
    }

    fun hasBip32MasterSeed(): Boolean {
        return secureStore.hasCiphertextValue(MASTER_SEED_ID)
    }

    fun canCreateAdditionalBip44Account(): Boolean {
        if (!hasBip32MasterSeed()) {
            // No master seed
            return false
        }

        for (account in accounts.values) {
            if (!account.hasHadActivity()) {
                return false
            }
        }
        return true
    }

    override fun canCreateAccount(config: Config): Boolean {
        return config is UnrelatedHDAccountConfig ||
                (config is AdditionalHDAccountConfig && canCreateAdditionalBip44Account()) ||
                config is ExternalSignaturesAccountConfig
    }


    override fun deleteAccount(walletAccount: WalletAccount<*, *>, keyCipher: KeyCipher): Boolean {
        if(walletAccount is HDAccount || walletAccount is HDPubOnlyAccount) {
            accounts.remove(walletAccount.id)
            backing.deleteBip44AccountContext(walletAccount.id)
            return true
        }
        return false
    }

    fun upgradeExtSigAccount(accountRoots: List<HdKeyNode>, account: HDAccountExternalSignature): Boolean {
        return account.upgradeAccount(accountRoots, secureStore)
    }

    companion object {
        @JvmField
        val ID: String = "BitcoinHD"
    }

    fun getGapsBug(): Set<Int> {
        val accountIndices = accounts.values
                .filter { it.isDerivedFromInternalMasterseed }
                .map { it.accountIndex }
        val allIndices = 0..(accountIndices.max() ?: 0)
        return allIndices.subtract(accountIndices)
    }

    fun getGapAddresses(cipher: KeyCipher): List<Address> {
        val gaps: Set<Int> = getGapsBug()
        // Get the master seed
        val masterSeed: Bip39.MasterSeed = getMasterSeed(cipher)
        val tempSecureBacking = InMemoryWalletManagerBacking()

        val tempSecureKeyValueStore = SecureKeyValueStore(tempSecureBacking, RandomSource {
            // randomness not needed for the temporary keystore
        })

        val addresses: MutableList<Address> = mutableListOf()
        for (gapIndex in gaps.indices) {
            for (derivationType: BipDerivationType in BipDerivationType.values()) {
                // Generate the root private key
                val root: HdKeyNode = HdKeyNode.fromSeed(masterSeed.bip32Seed, derivationType)
                val keyManager: HDAccountKeyManager = HDAccountKeyManager.createNew(root, networkParameters, gapIndex, tempSecureKeyValueStore, cipher, derivationType)
                addresses.add(keyManager.getAddress(false, 0)) // get first external address for the account in the gap
            }
        }
        return addresses
    }

    fun createArchivedGapFiller(cipher: KeyCipher, accountIndex: Int): UUID {
        // Get the master seed
        val masterSeed: Bip39.MasterSeed = getMasterSeed(cipher)

        synchronized(accounts) {
            backing.beginTransaction()
            try {
                // Create the base keys for the account
                val keyManagerMap = mutableMapOf<BipDerivationType, HDAccountKeyManager>()
                for (derivationType in BipDerivationType.values()) {
                    // Generate the root private key
                    val root = HdKeyNode.fromSeed(masterSeed.bip32Seed, derivationType)
                    keyManagerMap[derivationType] = HDAccountKeyManager.createNew(root, networkParameters, accountIndex,
                            secureStore, cipher, derivationType)
                }


                // Generate the context for the account
                val context = HDAccountContext(
                        keyManagerMap[BipDerivationType.BIP44]!!.accountId, accountIndex, false, settings.defaultAddressType)
                backing.createBip44AccountContext(context)

                // Get the backing for the new account
                val accountBacking: Bip44AccountBacking = backing.getBip44AccountBacking(context.id)

                // Create actual account
                val account = HDAccount(context, keyManagerMap, networkParameters, accountBacking, _wapi, settings.changeAddressModeReference)

                // Finally persist context and add account
                context.persist(accountBacking)
                backing.setTransactionSuccessful()
                account.archiveAccount()

                accounts[account.id] = account
                return account.id
            } finally {
                backing.endTransaction()
            }
        }
    }
}

/**
 * Get the active BTC HD-accounts managed by the wallet manager
 * , excluding on-the-fly-accounts and single-key accounts
 *
 * @return the list of accounts
 */
fun WalletManager.getBTCBip44Accounts() = getAccounts().filter { it is HDAccount && it.isVisible }

/**
 * Get the active BTC HD-accounts managed by the wallet manager
 * , excluding on-the-fly-accounts and single-key accounts
 *
 * @return the list of accounts
 */
fun WalletManager.getActiveHDAccounts(): List<WalletAccount<*, *>> = getAccounts().filter { it is HDAccount && it.isActive }

/**
 * Get the active HD-accounts managed by the wallet manager, excluding on-the-fly-accounts and single-key accounts
 *
 * @return the list of accounts
 */
fun WalletManager.getActiveMasterseedHDAccounts(): List<WalletAccount<*, *>> = getAccounts().filter { it is HDAccount && it.isDerivedFromInternalMasterseed }

/**
 * Get the active accounts managed by the wallet manager
 *
 * @return the list of accounts
 */
fun WalletManager.getActiveMasterseedAccounts(): List<WalletAccount<*, *>> = getAccounts().filter { it.isActive && it.isDerivedFromInternalMasterseed }

