package com.mycelium.wapi.wallet.bch.bip44

import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.SecureKeyValueStore
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.btc.BtcTransaction
import com.mycelium.wapi.wallet.btc.WalletManagerBacking
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.bip44.UnrelatedHDAccountConfig
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccountContext
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.GenericModule
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import java.util.*
import com.mycelium.wapi.wallet.btc.bip44.HDAccountKeyManager
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.RandomSource
import com.mycelium.wapi.wallet.btc.InMemoryWalletManagerBacking
import com.mrd.bitlib.crypto.Bip39
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher
import com.mrd.bitlib.model.Address
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext
import android.location.Geocoder.isPresent
import com.mrd.bitlib.util.HexUtils


class Bip44BCHHDModule(private val MASTER_SEED_ID: ByteArray  = HexUtils.toBytes("D64CA2B680D8C8909A367F28EB47F990"),
                       internal val backing: WalletManagerBacking<SingleAddressAccountContext, BtcTransaction>,
                       internal val secureStore: SecureKeyValueStore,
                       internal val networkParameters: NetworkParameters,
                       private val _secureKeyValueStore: SecureKeyValueStore,
                       private val _network: NetworkParameters,
                       private val _walletAccounts: Map<UUID, WalletAccount>,
                       internal var _wapi: Wapi,
                       metaDataStorage: IMetaDataStorage) : GenericModule(metaDataStorage), WalletModule {

    override fun getId(): String = ID

    private val accounts = mutableMapOf<UUID, Bip44BCHAccount>()

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        return mapOf()
    }

    override fun createAccount(config: Config): WalletAccount<*, *>? {
        var result: WalletAccount<*, *>? = null

        val baseName = "BCH HD Account"
        if (result != null) {
            result.label = createLabel(baseName, result.id)
        }
        return result
    }

    fun getAccountByIndex(index: Int): HDAccount? {
        return accounts.values.firstOrNull { it.accountIndex == index }
    }

    override fun canCreateAccount(config: Config): Boolean = config is UnrelatedHDAccountConfig

    override fun deleteAccount(walletAccount: WalletAccount<*, *>, keyCipher: KeyCipher): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        const val ID: String = "Bip44HD"
    }

    fun getGapsBug(): List<Int> {
        val mainAccounts = filterAndConvert(MAIN_SEED_HD_ACCOUNT) as List<*> as List<HDAccount>

        // sort it according to their index
        Collections.sort(mainAccounts, object : Comparator<HDAccount>() {
            fun compare(o1: HDAccount, o2: HDAccount): Int {
                val x = o1.accountIndex
                val y = o2.accountIndex
                return if (x < y) -1 else if (x == y) 0 else 1
            }
        })
        val gaps = LinkedList()
        var lastIndex = 0
        for (acc in mainAccounts) {
            while (acc.accountIndex > lastIndex++) {
                gaps.add(lastIndex - 1)
            }
        }
        return gaps
    }

    @Throws(InvalidKeyCipher::class)
    fun getMasterSeed(cipher: KeyCipher): Bip39.MasterSeed {
        val binaryMasterSeed: ByteArray = _secureKeyValueStore.getDecryptedValue(MASTER_SEED_ID, cipher)
        val masterSeed = Bip39.MasterSeed.fromBytes(binaryMasterSeed, false)
        if (!masterSeed.isPresent) {
            throw RuntimeException()
        }
        return masterSeed.get()
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
                val keyManager: HDAccountKeyManager = HDAccountKeyManager.createNew(root, _network, gapIndex!!, tempSecureKeyValueStore, cipher, derivationType)
                addresses.add(keyManager.getAddress(false, 0)) // get first external address for the account in the gap
            }
        }

        return addresses
    }

    @Throws(InvalidKeyCipher::class)
    fun createArchivedGapFiller(cipher: KeyCipher, accountIndex: Int?, archived: Boolean): UUID {
        // Get the master seed
        val masterSeed: Bip39.MasterSeed = getMasterSeed(cipher)

        synchronized(_walletAccounts) {
            _backing.beginTransaction()
            try {
                // Create the base keys for the account
                val keyManagerMap = HashMap()
                for (derivationType in BipDerivationType.values()) {
                    // Generate the root private key
                    val root = HdKeyNode.fromSeed(masterSeed.getBip32Seed(), derivationType)
                    keyManagerMap.put(derivationType, HDAccountKeyManager.createNew(root, _network, accountIndex!!,
                            _secureKeyValueStore, cipher, derivationType))
                }

                val (defaultAddressType, changeAddressModeReference) = currenciesSettingsMap.get(Currency.BTC)
// Generate the context for the account
                val context = HDAccountContext(
                        keyManagerMap.get(BipDerivationType.BIP44).getAccountId(), accountIndex!!, false, defaultAddressType)
                _backing.createBip44AccountContext(context)

                // Get the backing for the new account
                val accountBacking = getBip44AccountBacking(context.id)

                // Create actual account
                val account = HDAccount(context, keyManagerMap, _network, accountBacking, _wapi,
                        changeAddressModeReference)

                // Finally persist context and add account
                context.persist(accountBacking)
                _backing.setTransactionSuccessful()
                if (archived) {
                    account.archiveAccount()
                }
                val uuidList = getAccountVirtualIds(keyManagerMap, account)

                addAccount(account, uuidList)
                hdAccounts.add(account)
                return account.id
            } finally {
                _backing.endTransaction()
            }
        }
    }


}

/**
 * Get Bitcoin Cash HD-accounts
 *
 * @return list of accounts
 */
fun WalletManager.getBCHBip44Accounts() = getAccounts().filter { it is Bip44BCHAccount && it.isVisible }