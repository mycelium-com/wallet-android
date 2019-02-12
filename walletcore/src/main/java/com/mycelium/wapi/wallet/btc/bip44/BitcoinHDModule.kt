package com.mycelium.wapi.wallet.btc.bip44

import com.mrd.bitlib.crypto.Bip39
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.crypto.RandomSource
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.Currency
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
import com.mycelium.wapi.wallet.btc.BTCSettings
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.model.AddressType
import javax.annotation.Nonnull
import kotlin.collections.ArrayList
import sun.java2d.marlin.MarlinUtils.logInfo
import com.mycelium.wapi.wallet.WalletManager
import sun.java2d.marlin.MarlinUtils.logInfo








class BitcoinHDModule(internal val backing: WalletManagerBacking<SingleAddressAccountContext, BtcTransaction>,
                      internal val secureStore: SecureKeyValueStore,
                      internal val networkParameters: NetworkParameters,
                      internal var _wapi: Wapi,
                      internal val currenciesSettingsMap: MutableMap<Currency, CurrencySettings>,
                      internal val metadataStorage: IMetaDataStorage,
                      internal val signatureProviders: ExternalSignatureProviderProxy?) : GenericModule(metadataStorage), WalletModule {

    init {
        assetsList.add(if (networkParameters.isProdnet) BitcoinMain.get() else BitcoinTest.get())
    }

    private val MASTER_SEED_ID = HexUtils.toBytes("D64CA2B680D8C8909A367F28EB47F990")

    private val accounts = mutableMapOf<UUID, HDAccount>()
    private val _accountEventManager: AccountEventManager? = null
    private val hdAccounts: List<HDAccount> = ArrayList()
    override fun getId(): String = ID

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        val result = mutableMapOf<UUID, WalletAccount<*, *>>()
        val contexts = backing.loadBip44AccountContexts()
        for (context in contexts) {
            val keyManagerMap = HashMap<BipDerivationType, HDAccountKeyManager>()

            loadKeyManagers(context, keyManagerMap)

            val accountBacking = backing.getBip44AccountBacking(context.id)
            val account: WalletAccount<*, *>
            val btcSettings = currenciesSettingsMap[Currency.BTC] as BTCSettings
            if (context.accountType == ACCOUNT_TYPE_UNRELATED_X_PRIV) {
                account = HDAccount(context, keyManagerMap, networkParameters, accountBacking
                        , _wapi, btcSettings.changeAddressModeReference)
            } else if (context.accountType == ACCOUNT_TYPE_UNRELATED_X_PUB) {
                account = HDPubOnlyAccount(context, keyManagerMap, networkParameters, accountBacking, _wapi)
            } else if (context.accountType == ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR) {
                account = HDAccountExternalSignature(context, keyManagerMap, networkParameters, accountBacking, _wapi,
                        signatureProviders!!.get(ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR), btcSettings.changeAddressModeReference)
            } else if (context.accountType == ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER) {
                account = HDAccountExternalSignature(context, keyManagerMap, networkParameters, accountBacking, _wapi,
                        signatureProviders!!.get(ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER), btcSettings.changeAddressModeReference)
            } else if (context.accountType == ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY) {
                account = HDAccountExternalSignature(context, keyManagerMap, networkParameters, accountBacking, _wapi,
                        signatureProviders!!.get(ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY), btcSettings.changeAddressModeReference)
            } else {
                account = HDAccount(context, keyManagerMap, networkParameters, accountBacking, _wapi,
                        btcSettings.changeAddressModeReference)
            }
            result[account.id] = account
            accounts.put(account.id, account as HDAccount)
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

    override fun createAccount(config: Config): WalletAccount<*, *>? {
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
            var isUpgrade = false
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
                keyManagerMap.put(derivationType, HDAccountKeyManager.createNew(root, networkParameters, accountIndex,
                        secureStore, AesKeyCipher.defaultKeyCipher(), derivationType))
            }
            val btcSettings = currenciesSettingsMap[Currency.BTC] as BTCSettings
            val defaultAddressType = btcSettings.defaultAddressType

            // Generate the context for the account
            val context = HDAccountContext(keyManagerMap[BipDerivationType.BIP44]!!.accountId
                    , accountIndex, false, defaultAddressType)

            backing.beginTransaction()
            try {
                backing.createBip44AccountContext(context)

                // Get the accountBacking for the new account
                val accountBacking = backing.getBip44AccountBacking(context.id)

                // Create actual account
                result = HDAccount(context, keyManagerMap, networkParameters, accountBacking, _wapi,
                        btcSettings.changeAddressModeReference)

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
            val btcSettings = currenciesSettingsMap[Currency.BTC] as BTCSettings
            val defaultAddressType = btcSettings.defaultAddressType

            // Generate the context for the account
            val context = HDAccountContext(id, accountIndex, false, cfg.provider.biP44AccountType,
                    secureStorage.subId, derivationTypes, defaultAddressType)
            backing.beginTransaction()
            try {
                backing.createBip44AccountContext(context)

                // Get the accountBacking for the new account
                val accountBacking = backing.getBip44AccountBacking(context.id)

                // Create actual account
                result = HDAccountExternalSignature(context, keyManagerMap, networkParameters, accountBacking, _wapi,
                        cfg.provider, btcSettings.changeAddressModeReference)

                // Finally persist context and add account
                context.persist(accountBacking)
                backing.setTransactionSuccessful()
            } finally {
                backing.endTransaction()
            }
        }

        accounts.put(result!!.id, result as HDAccount)

        val baseLabel = "Account" + " " + (result.accountIndex + 1)
        result.label = createLabel(baseLabel, result.id)
        return result
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

    fun getGapsBug(): List<Int> {
        val mainAccounts = filterAndConvert(MASTER_SEED_ID) as List<*> as List<HDAccount>

        // sort it according to their index
        Collections.sort(mainAccounts, object : Comparator<HDAccount>() {
            override fun compare(o1: HDAccount, o2: HDAccount): Int {
                val x = o1.accountIndex
                val y = o2.accountIndex
                return if (x < y) -1 else if (x == y) 0 else 1
            }
        })
        val gaps: List<Int> = LinkedList()
        var lastIndex = 0
        for (acc in mainAccounts) {
            while (acc.accountIndex > lastIndex++) {
                gaps.plus(lastIndex - 1)
            }
        }
        return gaps
    }


    @Throws(InvalidKeyCipher::class)
    fun getGapAddresses(cipher: KeyCipher): List<Address> {
        val gaps: List<Int> = getGapsBug()
        // Get the master seed
        val masterSeed: Bip39.MasterSeed = getMasterSeed(cipher)
        val tempSecureBacking = InMemoryWalletManagerBacking()

        val tempSecureKeyValueStore = SecureKeyValueStore(tempSecureBacking, RandomSource {
            // randomness not needed for the temporary keystore
        })

        val addresses: LinkedList<Address> = LinkedList()
        for (gapIndex in gaps.indices) {
            for (derivationType: BipDerivationType in BipDerivationType.values()) {
                // Generate the root private key
                val root: HdKeyNode = HdKeyNode.fromSeed(masterSeed.bip32Seed, derivationType)
                val keyManager: HDAccountKeyManager = HDAccountKeyManager.createNew(root, networkParameters, gapIndex!!, tempSecureKeyValueStore, cipher, derivationType)
                addresses.add(keyManager.getAddress(false, 0)) // get first external address for the account in the gap
            }
        }

        return addresses
    }

    @Throws(InvalidKeyCipher::class)
    fun createArchivedGapFiller(cipher: KeyCipher, accountIndex: Int?, archived: Boolean): UUID {
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
                    keyManagerMap[derivationType] = HDAccountKeyManager.createNew(root, networkParameters, accountIndex!!,
                            secureStore, cipher, derivationType)
                }

                val btcSettings: BTCSettings = currenciesSettingsMap[Currency.BTC] as BTCSettings
                val defaultAddressType: AddressType = btcSettings.defaultAddressType
                // Generate the context for the account
                val context = HDAccountContext(
                        keyManagerMap[BipDerivationType.BIP44]!!.accountId, accountIndex!!, false, defaultAddressType)
                backing.createBip44AccountContext(context)

                // Get the backing for the new account
                val accountBacking: Bip44AccountBacking = getBip44AccountBacking(context.id)

                // Create actual account
                val account = HDAccount(context, keyManagerMap, networkParameters, accountBacking, _wapi, btcSettings.changeAddressModeReference)

                // Finally persist context and add account
                context.persist(accountBacking)
                backing.setTransactionSuccessful()
                if (archived) {
                    account.archiveAccount()
                }
                val uuidList: List<UUID> = getAccountVirtualIds(keyManagerMap, account)

                addAccount(account, uuidList)
                hdAccounts.plus(account)
                return account.id
            } finally {
                backing.endTransaction()
            }
        }
    }

    /**
     * This method is intended to get all possible ids for mixed HD account.
     */
    @Nonnull
    fun getAccountVirtualIds(keyManagerMap: Map<BipDerivationType, HDAccountKeyManager>, account: HDAccount): List<UUID> {
        val uuidList: MutableList<UUID> = mutableListOf()
        for (addressType in account.availableAddressTypes) {
            uuidList.add(keyManagerMap[BipDerivationType.getDerivationTypeByAddressType(addressType)].getAccountId())
        }
        return uuidList
    }

    /**
     * @param accountIds - because of mixed mode account might have multiple ids, some of which might be virtual.
     */
    private fun addAccount(account: AbstractAccount, accountIds: List<UUID>) {
        synchronized(accounts) {
            account.setEventHandler(_accountEventManager)
            for (id in accountIds) {
                accounts[id] = account
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
fun WalletManager.getActiveMasterseedAccounts(): List<WalletAccount<*, *>> = getAccounts().filter { it is HDAccount && it.isDerivedFromInternalMasterseed }


