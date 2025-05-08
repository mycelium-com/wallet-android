package com.mycelium.wapi.wallet.btc.bip44

import com.mrd.bitlib.crypto.Bip39
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.crypto.RandomSource
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.AccountIndexesContext
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.CurrencySettings
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher
import com.mycelium.wapi.wallet.LoadingProgressStatus
import com.mycelium.wapi.wallet.LoadingProgressTracker
import com.mycelium.wapi.wallet.LoadingProgressUpdater
import com.mycelium.wapi.wallet.SecureKeyValueStore
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.btc.BTCSettings
import com.mycelium.wapi.wallet.btc.Bip44BtcAccountBacking
import com.mycelium.wapi.wallet.btc.BtcWalletManagerBacking
import com.mycelium.wapi.wallet.btc.InMemoryBtcWalletManagerBacking
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext.Companion.ACCOUNT_TYPE_FROM_MASTERSEED
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext.Companion.ACCOUNT_TYPE_UNRELATED_X_PRIV
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext.Companion.ACCOUNT_TYPE_UNRELATED_X_PUB
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext.Companion.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext.Companion.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext.Companion.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import java.util.UUID


class BitcoinHDModule(internal val backing: BtcWalletManagerBacking<HDAccountContext>,
                      internal val secureStore: SecureKeyValueStore,
                      internal val networkParameters: NetworkParameters,
                      internal var _wapi: Wapi,
                      internal var settings: BTCSettings,
                      internal val metadataStorage: IMetaDataStorage,
                      internal val signatureProviders: ExternalSignatureProviderProxy?,
                      internal val loadingProgressUpdater: LoadingProgressUpdater?,
                      internal val eventHandler: AbstractBtcAccount.EventHandler?) :
        WalletModule(metadataStorage) {

    override fun getAccountById(id: UUID): WalletAccount<*>? {
        return accounts[id]
    }

    init {
        assetsList.add(if (networkParameters.isProdnet) BitcoinMain else BitcoinTest)
    }

    override fun setCurrencySettings(currencySettings: CurrencySettings) {
        this.settings = currencySettings as BTCSettings
    }

    private val accounts = mutableMapOf<UUID, HDAccount>()

    override val id = ID

    override fun getAccounts(): List<WalletAccount<*>> = accounts.values.toList()

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> {
        LoadingProgressTracker.subscribe(loadingProgressUpdater!!)
        val result = mutableMapOf<UUID, WalletAccount<*>>()
        val contexts = backing.loadBip44AccountContexts()
        var counter = 1
        for (context in contexts) {
            if (loadingProgressUpdater.status.state == LoadingProgressStatus.State.LOADING) {
                LoadingProgressTracker.setStatus(LoadingProgressStatus(LoadingProgressStatus.State.MIGRATING))
            }
            val account: WalletAccount<*> = loadAccount(context)
            result[account.id] = account
            account.label = readLabel(account.id)
            accounts[account.id] = account as HDAccount
            account.setEventHandler(eventHandler)
            LoadingProgressTracker.clearLastFullUpdateTime()

            val state = if (loadingProgressUpdater.status.state == LoadingProgressStatus.State.MIGRATING ||
                loadingProgressUpdater.status.state == LoadingProgressStatus.State.MIGRATING_N_OF_M_HD) {
                LoadingProgressStatus.State.MIGRATING_N_OF_M_HD
            } else {
                LoadingProgressStatus.State.LOADING_N_OF_M_HD
            }
            LoadingProgressTracker.setStatus(LoadingProgressStatus(state, counter++, contexts.size))
        }
        return result
    }

    private fun loadAccount(context: HDAccountContext): WalletAccount<*> {
        val keyManagerMap = HashMap<BipDerivationType, HDAccountKeyManager>()

        loadKeyManagers(context, keyManagerMap)

        val accountBacking = backing.getBip44AccountBacking(context.id)
        return when (context.accountType) {
            ACCOUNT_TYPE_UNRELATED_X_PUB ->
                HDPubOnlyAccount(context, keyManagerMap, networkParameters, accountBacking, _wapi)
            ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR ->
                HDAccountExternalSignature(context, keyManagerMap, networkParameters, accountBacking, _wapi,
                    signatureProviders!!.get(ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR), settings.changeAddressModeReference)
            ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER ->
                HDAccountExternalSignature(context, keyManagerMap, networkParameters, accountBacking, _wapi,
                    signatureProviders!!.get(ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER), settings.changeAddressModeReference)
            ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY ->
                HDAccountExternalSignature(context, keyManagerMap, networkParameters, accountBacking, _wapi,
                    signatureProviders!!.get(ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY), settings.changeAddressModeReference)
            else -> HDAccount(context, keyManagerMap, networkParameters, accountBacking, _wapi,
                settings.changeAddressModeReference)
        }
    }

    private fun loadKeyManagers(context: HDAccountContext, keyManagerMap: HashMap<BipDerivationType, HDAccountKeyManager>) {
        for (entry in context.indexesMap) {
            when (context.accountType) {
                ACCOUNT_TYPE_FROM_MASTERSEED -> keyManagerMap[entry.key] = HDAccountKeyManager(context.accountIndex, networkParameters, secureStore, entry.key)
                ACCOUNT_TYPE_UNRELATED_X_PRIV -> {
                    val subKeyStore = secureStore.getSubKeyStore(context.accountSubId)
                    keyManagerMap[entry.key] = HDAccountKeyManager(context.accountIndex, networkParameters, subKeyStore, entry.key)
                }
                ACCOUNT_TYPE_UNRELATED_X_PUB,
                ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR,
                ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER,
                ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY -> {
                    val subKeyStore = secureStore.getSubKeyStore(context.accountSubId)
                    keyManagerMap[entry.key] = HDPubOnlyAccountKeyManager(context.accountIndex, networkParameters, subKeyStore, entry.key)
                }
            }
        }
    }

    override fun createAccount(config: Config): WalletAccount<*> {
        val result: WalletAccount<*>
        when (config) {
            is UnrelatedHDAccountConfig -> {
                val accountIndex = 0  // use any index for this account, as we don't know and we don't care
                val keyManagerMap = HashMap<BipDerivationType, HDAccountKeyManager>()
                val derivationTypes = ArrayList<BipDerivationType>()

                // get a subKeyStorage, to ensure that the data for this key does not get mixed up
                // with other derived or imported keys.
                val secureStorage = secureStore.createNewSubKeyStore()

                for (hdKeyNode in config.hdKeyNodes) {
                    val derivationType = hdKeyNode.derivationType
                    derivationTypes.add(derivationType)
                    if (hdKeyNode.isPrivateHdKeyNode) {
                        try {
                            keyManagerMap[derivationType] = HDAccountKeyManager.createFromAccountRoot(hdKeyNode, networkParameters,
                                    accountIndex, secureStorage, AesKeyCipher.defaultKeyCipher(), derivationType)
                        } catch (invalidKeyCipher: InvalidKeyCipher) {
                            throw RuntimeException(invalidKeyCipher)
                        }

                    } else {
                        keyManagerMap[derivationType] = HDPubOnlyAccountKeyManager.createFromPublicAccountRoot(hdKeyNode,
                                networkParameters, accountIndex, secureStorage, derivationType)
                    }
                }
                val id = keyManagerMap[derivationTypes[0]]!!.accountId

                // Generate the context for the account
                val accountType = if (config.hdKeyNodes[0].isPrivateHdKeyNode) {
                    ACCOUNT_TYPE_UNRELATED_X_PRIV
                } else {
                    ACCOUNT_TYPE_UNRELATED_X_PUB
                }
                val context = HDAccountContext(id, accountIndex, false, accountType,
                        secureStorage.subId, derivationTypes)
                backing.beginTransaction()
                try {
                    backing.createBip44AccountContext(context)
                    // Get the accountBacking for the new account
                    val accountBacking = backing.getBip44AccountBacking(context.id)

                    // Create actual account
                    result = if (config.hdKeyNodes[0].isPrivateHdKeyNode) {
                        HDAccount(context, keyManagerMap, networkParameters, accountBacking, _wapi, settings.changeAddressModeReference)
                    } else {
                        HDPubOnlyAccount(context, keyManagerMap, networkParameters, accountBacking, _wapi)
                    }

                    // Finally persist context and add account
                    context.persist(accountBacking)
                    backing.setTransactionSuccessful()
                } finally {
                    backing.endTransaction()
                }
            }
            is AdditionalHDAccountConfig -> {
                // Get the master seed
                val masterSeed = MasterSeedManager.getMasterSeed(secureStore, AesKeyCipher.defaultKeyCipher())

                val accountIndex = getCurrentBip44Index() + 1

                // Create the base keys for the account
                val keyManagerMap = HashMap<BipDerivationType, HDAccountKeyManager>()
                for (derivationType in BipDerivationType.entries) {
                    // Generate the root private key
                    val root = HdKeyNode.fromSeed(masterSeed.bip32Seed, derivationType)
                    keyManagerMap[derivationType] = HDAccountKeyManager.createNew(root, networkParameters, accountIndex,
                            secureStore, AesKeyCipher.defaultKeyCipher(), derivationType)
                }

                // Generate the context for the account
                val context = HDAccountContext(keyManagerMap[BipDerivationType.BIP44]!!.accountId,
                        accountIndex, false, settings.defaultAddressType)

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
            }
            is TaprootMigrationHDAccountConfig -> {
                val context = backing.getBip44AccountContext(config.account.id)

                if (context.indexesMap.keys.contains(BipDerivationType.BIP86)) return config.account

                val masterSeed = MasterSeedManager.getMasterSeed(secureStore, AesKeyCipher.defaultKeyCipher())
                val accountIndex = config.account.accountIndex
                val root = HdKeyNode.fromSeed(masterSeed.bip32Seed, BipDerivationType.BIP86)
                HDAccountKeyManager.createNew(
                    root, networkParameters, accountIndex,
                    secureStore, AesKeyCipher.defaultKeyCipher(), BipDerivationType.BIP86
                )


                result = if (!context.indexesMap.keys.contains(BipDerivationType.BIP86)) {
                    context.indexesMap[BipDerivationType.BIP86] = AccountIndexesContext(-1, -1, 0)
                    try {
                        context.persist(backing.getBip44AccountBacking(context.id))
                    } catch (e: Exception) {
                    }
                    loadAccount(context)
                } else {
                    config.account
                }
            }
            is ExternalSignaturesAccountConfig -> {
                val accountIndex = config.accountIndex
                val keyManagerMap = HashMap<BipDerivationType, HDAccountKeyManager>()
                val derivationTypes = ArrayList<BipDerivationType>()

                // get a subKeyStorage, to ensure that the data for this key does not get mixed up
                // with other derived or imported keys.
                val secureStorage = secureStore.createNewSubKeyStore()

                for (hdKeyNode in config.hdKeyNodes) {
                    val derivationType = hdKeyNode.derivationType
                    derivationTypes.add(derivationType)
                    keyManagerMap[derivationType] = HDPubOnlyAccountKeyManager.createFromPublicAccountRoot(hdKeyNode,
                            networkParameters, accountIndex, secureStorage, derivationType)
                }
                val id = keyManagerMap[derivationTypes[0]]!!.accountId

                // Generate the context for the account
                val context = HDAccountContext(id, accountIndex, false, config.provider.biP44AccountType,
                        secureStorage.subId, derivationTypes, settings.defaultAddressType)
                backing.beginTransaction()
                try {
                    backing.createBip44AccountContext(context)

                    // Get the accountBacking for the new account
                    val accountBacking = backing.getBip44AccountBacking(context.id)

                    // Create actual account
                    result = HDAccountExternalSignature(context, keyManagerMap, networkParameters, accountBacking, _wapi,
                            config.provider, settings.changeAddressModeReference)

                    // Finally persist context and add account
                    context.persist(accountBacking)
                    backing.setTransactionSuccessful()
                } finally {
                    backing.endTransaction()
                }
            }
            else -> throw IllegalStateException("Account can't be created")
        }
        accounts[result.id] = result as HDAccount
        result.setEventHandler(eventHandler)

        result.label = createLabel(config, result.id)
        return result
    }

    private fun createLabel(config: Config, id: UUID): String {
        // can't fetch hardware wallet's account labels for temp accounts
        // as we don't pass a signatureProviders and no need anyway
        if (config is ExternalSignaturesAccountConfig && signatureProviders == null)
            return "Unknown"

        val label = createLabel(getBaseLabel(config))
        storeLabel(id, label)
        return label
    }

    private fun getBaseLabel(cfg: Config): String {
        return when (cfg) {
            is AdditionalHDAccountConfig -> "Account " + (getCurrentBip44Index() + 1)
            is ExternalSignaturesAccountConfig ->
                signatureProviders!!.get(cfg.provider.biP44AccountType).labelOrDefault + " #" + (cfg.hdKeyNodes[0].index + 1)
            is UnrelatedHDAccountConfig -> if (cfg.hdKeyNodes[0].isPrivateHdKeyNode) "Account 1" else "Imported"
            is TaprootMigrationHDAccountConfig -> cfg.account.label
            else -> throw IllegalArgumentException("Unsupported config")
        }
    }

    fun getAccountByIndex(index: Int): HDAccount? {
        return accounts.values.firstOrNull { it.accountIndex == index }
    }

    fun getCurrentBip44Index() = accounts.values
        .filter { it.isDerivedFromInternalMasterseed() }
        .maxByOrNull { it.accountIndex }
        ?.accountIndex
        ?: -1

    fun hasBip32MasterSeed(): Boolean = secureStore.hasCiphertextValue(MasterSeedManager.MASTER_SEED_ID)

    /**
     * To create an additional HD account from the master seed, the master seed must be present and
     * all existing master seed accounts must have had transactions (no gap accounts) or be archived
     * (we may archive only used accounts therefore if account is archived it has had activity by definition.
     * But at the same time we clear all the information regardless if the account was used when we archive it
     * so we cannot rely on the according method for archived accounts)
     */
    fun canCreateAdditionalBip44Account(): Boolean =
            hasBip32MasterSeed() && accounts.values.filter { it.isDerivedFromInternalMasterseed() }
                    .all { it.hasHadActivity() || it.isArchived }

    override fun canCreateAccount(config: Config): Boolean {
        return config is UnrelatedHDAccountConfig ||
                (config is AdditionalHDAccountConfig && canCreateAdditionalBip44Account()) ||
                config is ExternalSignaturesAccountConfig ||
                config is TaprootMigrationHDAccountConfig
    }


    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean =
            if (walletAccount is HDAccount || walletAccount is HDPubOnlyAccount) {
                accounts.remove(walletAccount.id)
                backing.deleteBip44AccountContext(walletAccount.id)
                true
            } else {
                false
            }

    fun upgradeExtSigAccount(accountRoots: List<HdKeyNode>, account: HDAccountExternalSignature): Boolean =
            account.upgradeAccount(accountRoots, secureStore)

    companion object {
        const val ID: String = "BitcoinHD"
    }

    fun getGapsBug(): Set<Int> {
        val accountIndices = accounts.values
                .filter { it.isDerivedFromInternalMasterseed() }
                .map { it.accountIndex }
        val allIndices = 0..(accountIndices.maxOrNull() ?: 0)
        return allIndices.subtract(accountIndices)
    }

    fun getGapAddresses(cipher: KeyCipher): List<BitcoinAddress> {
        val gaps: Set<Int> = getGapsBug()
        // Get the master seed
        val masterSeed: Bip39.MasterSeed = MasterSeedManager.getMasterSeed(secureStore, cipher)
        val tempSecureBacking = InMemoryBtcWalletManagerBacking()

        val tempSecureKeyValueStore = SecureKeyValueStore(tempSecureBacking, RandomSource {
            // randomness not needed for the temporary keystore
        })

        val addresses: MutableList<BitcoinAddress> = mutableListOf()
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
        val masterSeed: Bip39.MasterSeed = MasterSeedManager.getMasterSeed(secureStore, cipher)

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
                val accountBacking: Bip44BtcAccountBacking = backing.getBip44AccountBacking(context.id)

                // Create actual account
                val account = HDAccount(context, keyManagerMap, networkParameters, accountBacking, _wapi, settings.changeAddressModeReference)

                // Finally persist context and add account
                context.persist(accountBacking)
                account.archiveAccount()
                accounts[account.id] = account
                backing.setTransactionSuccessful()
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
fun WalletManager.getBTCBip44Accounts() = getAccounts().filter { it is HDAccount && it.isVisible() }

/**
 * Get the active BTC HD-accounts managed by the wallet manager
 * , excluding on-the-fly-accounts and single-key accounts
 *
 * @return the list of accounts
 */
fun WalletManager.getActiveHDAccounts(): List<WalletAccount<*>> = getAccounts().filter { it is HDAccount && it.isActive }

/**
 * Get the active HD-accounts managed by the wallet manager, excluding on-the-fly-accounts and single-key accounts
 *
 * @return the list of accounts
 */
fun WalletManager.getActiveMasterseedHDAccounts(): List<WalletAccount<*>> = getAccounts().filter { it is HDAccount && it.isDerivedFromInternalMasterseed() }

/**
 * Get the active accounts managed by the wallet manager
 *
 * @return the list of accounts
 */
fun WalletManager.getActiveMasterseedAccounts(): List<WalletAccount<*>> = getAccounts().filter { it.isActive && it.isDerivedFromInternalMasterseed() }

