package com.mycelium.wapi.wallet.btcvault.hd

import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.api.WapiClientElectrumX
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BTCSettings
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext
import com.mycelium.wapi.wallet.btcvault.BTCVNetworkParameters
import com.mycelium.wapi.wallet.btcvault.BtcvAddress
import com.mycelium.wapi.wallet.btcvault.BtcvAddressFactory
import com.mycelium.wapi.wallet.btcvault.coins.BitcoinVaultMain
import com.mycelium.wapi.wallet.btcvault.coins.BitcoinVaultTest
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.genericdb.Backing
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.HDAccountKeyManager
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import java.util.*


class BitcoinVaultHDModule(internal val backing: Backing<BitcoinVaultHDAccountContext>,
                           private val secureStore: SecureKeyValueStore,
                           internal val networkParameters: BTCVNetworkParameters,
                           private val walletDB: WalletDB,
                           internal var _wapi: Wapi,
                           internal var settings: BTCSettings,
                           metadataStorage: IMetaDataStorage,
                           private val accountListener: AccountListener?) : WalletModule(metadataStorage) {

    private val accounts = mutableMapOf<UUID, BitcoinVaultHdAccount>()

    private val coinType = if (networkParameters.isProdnet) BitcoinVaultMain else BitcoinVaultTest

    init {
        assetsList.add(coinType)
    }

    override val id: String = ID

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> =
            backing.loadAccountContexts().associateBy({ it.uuid }, {
                BitcoinVaultHdAccount(it, loadKeyManagers(it), networkParameters, _wapi,
                        BitcoinVaultHDAccountBacking(walletDB, it.uuid), accountListener,
                        settings.changeAddressModeReference)
                        .apply { accounts[this.id] = this }
            }).also { setupClientIsActive() }

    override fun createAccount(config: Config): WalletAccount<*> {
        val result: WalletAccount<*>
        when (config) {
            is AdditionalMasterseedAccountConfig -> {
                val masterSeed = MasterSeedManager.getMasterSeed(secureStore, AesKeyCipher.defaultKeyCipher())
                val accountIndex = getCurrentBip44Index() + 1

                // Create the base keys for the account
                val keyManagerMap = BipDerivationType.values().associate { derivationType ->
                    val root = HdKeyNode.fromSeed(masterSeed.bip32Seed, derivationType)
                    derivationType to HDAccountKeyManager.createNew(root, coinType,
                            networkParameters, accountIndex, secureStore,
                            AesKeyCipher.defaultKeyCipher(), derivationType, BtcvAddressFactory(coinType, networkParameters)
                    )
                }

                // Generate the context for the account
                val accountContext = BitcoinVaultHDAccountContext(keyManagerMap[BipDerivationType.BIP44]!!.accountId,
                        coinType, accountIndex, false, "Bitcoin Vault ${accountIndex + 1}",
                        Balance.getZeroBalance(coinType), backing::updateAccountContext)

                backing.createAccountContext(accountContext)
                result = BitcoinVaultHdAccount(accountContext, keyManagerMap, networkParameters, _wapi,
                        BitcoinVaultHDAccountBacking(walletDB, accountContext.uuid),
                        accountListener, settings.changeAddressModeReference)
            }
            else -> throw IllegalStateException("Account can't be created")
        }
        accounts[result.id] = result
        setupClientIsActive()
        return result
    }

    fun setupClientIsActive() {
        val isActive = accounts.filterValues(BitcoinVaultHdAccount::isActive).isNotEmpty()
        (_wapi as? WapiClientElectrumX)?.setClientIsActive(isActive)
    }

    private fun loadKeyManagers(context: BitcoinVaultHDAccountContext): Map<BipDerivationType, HDAccountKeyManager<BtcvAddress>> =
            if (context.accountType == HDAccountContext.ACCOUNT_TYPE_FROM_MASTERSEED) {
                context.indexesMap.keys.associateWith { derivationType ->
                    HDAccountKeyManager(context.accountIndex, networkParameters, secureStore,
                            derivationType, BtcvAddressFactory(coinType, networkParameters))
                }
            } else {
                emptyMap()
            }

    private fun getCurrentBip44Index() = accounts.values
            .filter { it.isDerivedFromInternalMasterseed() }
            .maxBy { it.accountIndex }
            ?.accountIndex
            ?: -1

    override fun canCreateAccount(config: Config): Boolean =
            config is AdditionalMasterseedAccountConfig && canCreateAdditionalBip44Account()

    /**
     * To create an additional HD account from the master seed, the master seed must be present and
     * all existing master seed accounts must have had transactions (no gap accounts) or be archived
     * (we may archive only used accounts therefore if account is archived it has had activity by definition.
     * But at the same time we clear all the information regardless if the account was used when we archive it
     * so we cannot rely on the according method for archived accounts)
     */
    fun canCreateAdditionalBip44Account(): Boolean =
        accounts.values.filter { it.isDerivedFromInternalMasterseed() }
            .all { it.hasHadActivity() || it.isArchived }

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean {
        accounts.remove(walletAccount.id)
        return true
    }

    override fun getAccounts(): List<WalletAccount<*>> = accounts.values.toList()

    override fun getAccountById(id: UUID): WalletAccount<*>? = accounts[id]

    companion object {
        const val ID = "BitcoinVaultHD"
    }
}

fun WalletManager.getBtcvHdAccounts() = getAccounts().filter { it is BitcoinVaultHdAccount && it.isVisible() }
fun WalletManager.getActiveBtcvAccounts() = getAccounts().filter { it is BitcoinVaultHdAccount && it.isVisible() && it.isActive }
