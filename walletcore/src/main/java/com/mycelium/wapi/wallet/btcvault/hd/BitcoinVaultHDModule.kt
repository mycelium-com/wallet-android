package com.mycelium.wapi.wallet.btcvault.hd

import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BTCSettings
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext
import com.mycelium.wapi.wallet.btc.bip44.HDAccountKeyManager
import com.mycelium.wapi.wallet.btcvault.coins.BitcoinVaultMain
import com.mycelium.wapi.wallet.btcvault.coins.BitcoinVaultTest
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.genericdb.Backing
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import java.util.*


class BitcoinVaultHDModule(internal val backing: Backing<BitcoinVaultHDAccountContext>,
                           internal val secureStore: SecureKeyValueStore,
                           internal val networkParameters: NetworkParameters,
                           private val walletDB: WalletDB,
                           internal var _wapi: Wapi,
                           internal var settings: BTCSettings,
                           metadataStorage: IMetaDataStorage,
                           private val accountListener: AccountListener?) : WalletModule(metadataStorage) {

    private val accounts = mutableMapOf<UUID, BitcoinVaultHDAccount>()

    private val coinType = if (networkParameters.isProdnet) BitcoinVaultMain else BitcoinVaultTest

    override val id: String = ID

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> =
            backing.loadAccountContexts().associateBy({ it.uuid }, {
                BitcoinVaultHDAccount(it, loadKeyManagers(it), networkParameters, _wapi,
                        BitcoinVaultHDAccountBacking(walletDB, it.uuid, coinType), accountListener,
                        settings.changeAddressModeReference)
                        .apply { accounts[this.id] = this }
            })

    override fun createAccount(config: Config): WalletAccount<*> {
        val result: WalletAccount<*>
        when (config) {
            is AdditionalMasterseedAccountConfig -> {
                val masterSeed = MasterSeedManager.getMasterSeed(secureStore, AesKeyCipher.defaultKeyCipher())
                val accountIndex = getCurrentBip44Index() + 1


                // Create the base keys for the account
                val keyManagerMap = hashMapOf<BipDerivationType, HDAccountKeyManager>()
                for (derivationType in BipDerivationType.values()) {
                    // Generate the root private key
                    val root = HdKeyNode.fromSeed(masterSeed.bip32Seed, derivationType)

                    keyManagerMap[derivationType] = root.createHDAccountKeyManager(
                            networkParameters, accountIndex,
                            secureStore, AesKeyCipher.defaultKeyCipher(), derivationType,
                            PRODNET_COIN_TYPE //TODO add depends cointype from network
                    )
                }

                // Generate the context for the account
                val accountContext = BitcoinVaultHDAccountContext(keyManagerMap[BipDerivationType.BIP44]!!.accountId,
                        coinType, accountIndex, false, "Bitcoin Vault ${getCurrentBip44Index()}",
                        Balance.getZeroBalance(coinType), backing::updateAccountContext)

                backing.createAccountContext(accountContext)
                result = BitcoinVaultHDAccount(accountContext, keyManagerMap, networkParameters, _wapi,
                        BitcoinVaultHDAccountBacking(walletDB, accountContext.uuid, coinType),
                        accountListener, settings.changeAddressModeReference)
            }
            else -> throw IllegalStateException("Account can't be created")
        }
        accounts[result.id] = result
        return result
    }

    private fun loadKeyManagers(context: BitcoinVaultHDAccountContext): HashMap<BipDerivationType, HDAccountKeyManager> =
            hashMapOf<BipDerivationType, HDAccountKeyManager>().apply {
                for (entry in context.indexesMap) {
                    when (context.accountType) {
                        HDAccountContext.ACCOUNT_TYPE_FROM_MASTERSEED -> this[entry.key] = HDAccountKeyManager(context.accountIndex, networkParameters, secureStore, entry.key)
                    }
                }
            }

    private fun getCurrentBip44Index() = accounts.values
            .filter { it.isDerivedFromInternalMasterseed }
            .maxBy { it.accountIndex }
            ?.accountIndex
            ?: -1

    override fun canCreateAccount(config: Config): Boolean = config is AdditionalMasterseedAccountConfig

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean {
        accounts.remove(walletAccount.id)
        return true
    }

    override fun getAccounts(): List<WalletAccount<*>> = accounts.values.toList()

    override fun getAccountById(id: UUID): WalletAccount<*>? = accounts[id]

    companion object {
        const val ID = "BitcoinVaultHD"
        const val PRODNET_COIN_TYPE = 440
    }
}

@Throws(KeyCipher.InvalidKeyCipher::class)
fun HdKeyNode.createHDAccountKeyManager(network: NetworkParameters, accountIndex: Int,
                                        secureKeyValueStore: SecureKeyValueStore, cipher: KeyCipher,
                                        derivationType: BipDerivationType, coinType: Int): HDAccountKeyManager {
    val bip44Root = createChildNode(derivationType.getHardenedPurpose())
    val coinTypeRoot = bip44Root.createChildNode(coinType or (0x80000000).toInt())

    // Create the account root.
    val accountRoot = coinTypeRoot.createChildNode(accountIndex or (0x80000000).toInt())
    return HDAccountKeyManager.createFromAccountRoot(accountRoot, network, accountIndex, secureKeyValueStore, cipher, derivationType)
}

fun WalletManager.getBTCVHDAccounts() = getAccounts().filter { it is BitcoinVaultHDAccount && it.isVisible }
fun WalletManager.getActiveBTCVAccounts() = getAccounts().filter { it is BitcoinVaultHDAccount && it.isVisible && it.isActive }