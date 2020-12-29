package com.mycelium.wapi.wallet.btcvault.hd

import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.*
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
                           internal var _wapi: Wapi,
                           metadataStorage: IMetaDataStorage,
                           private val accountListener: AccountListener?) : WalletModule(metadataStorage) {

    private val accounts = mutableMapOf<UUID, BitcoinVaultHDAccount>()

    private val coinType = if (networkParameters.isProdnet) BitcoinVaultMain else BitcoinVaultTest

    fun getBip44Path(accountIndex: Int): HdKeyPath = HdKeyPath.valueOf("m/44'/60'/$accountIndex'/0/0")

    override val id: String = ID

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> =
            backing.loadAccountContexts().associateBy({ it.uuid }, {
                BitcoinVaultHDAccount(it, networkParameters, accountListener)
                        .apply { accounts[this.id] = this }
            })

    override fun createAccount(config: Config): WalletAccount<*> {
        val result: WalletAccount<*>
        when (config) {
            is AdditionalMasterseedAccountConfig -> {
                val masterSeed = MasterSeedManager.getMasterSeed(secureStore, AesKeyCipher.defaultKeyCipher())
                val accountIndex = getCurrentBip44Index() + 1


                // Create the base keys for the account
                val keyManagerMap = HashMap<BipDerivationType, HDAccountKeyManager>()
//                    for (derivationType in BipDerivationType.values()) {
//                        // Generate the root private key
//                        val root = HdKeyNode.fromSeed(masterSeed.bip32Seed, derivationType)
//                        keyManagerMap[derivationType] = HDAccountKeyManager.createNew(root, networkParameters, accountIndex,
//                                secureStore, AesKeyCipher.defaultKeyCipher(), derivationType)
//                    }
//
                // Generate the context for the account
                val accountContext = BitcoinVaultHDAccountContext(UUID.randomUUID(), coinType,
                        accountIndex, false, "Bitcoin Vault ${getCurrentBip44Index()}",
                        Balance.getZeroBalance(coinType), backing::updateAccountContext)

                backing.createAccountContext(accountContext)
                result = BitcoinVaultHDAccount(accountContext, networkParameters, accountListener)
            }
            else -> throw IllegalStateException("Account can't be created")
        }
        accounts[result.id] = result
        return result
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
        const val ID: String = "BitcoinVaultHD"
    }
}

fun WalletManager.getBTCVHDAccounts() = getAccounts().filter { it is BitcoinVaultHDAccount && it.isVisible }
fun WalletManager.getActiveBTCVAccounts() = getAccounts().filter { it is BitcoinVaultHDAccount && it.isVisible && it.isActive }