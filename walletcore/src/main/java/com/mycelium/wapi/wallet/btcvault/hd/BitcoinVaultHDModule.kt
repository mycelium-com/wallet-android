package com.mycelium.wapi.wallet.btcvault.hd

import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.ByteWriter
import com.mrd.bitlib.util.HashUtils
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.bip44.HDAccountKeyManager
import com.mycelium.wapi.wallet.btcvault.coins.BitcoinVaultMain
import com.mycelium.wapi.wallet.btcvault.coins.BitcoinVaultTest
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.CryptoCurrency
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
                           metadataStorage: IMetaDataStorage,
                           private val accountListener: AccountListener?) : WalletModule(metadataStorage) {

    private val accounts = mutableMapOf<UUID, BitcoinVaultHDAccount>()

    private val coinType = if (networkParameters.isProdnet) BitcoinVaultMain else BitcoinVaultTest

    override val id: String = ID

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> =
            backing.loadAccountContexts().associateBy({ it.uuid }, {
                val keyManagerMap = HashMap<BipDerivationType, HDAccountKeyManager>()
                BitcoinVaultHDAccount(it, keyManagerMap, networkParameters,
                        BitcoinVaultHDAccountBacking(walletDB, it.uuid, coinType), accountListener)
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
                for (derivationType in BipDerivationType.values()) {
                    // Generate the root private key
                    val root = HdKeyNode.fromSeed(masterSeed.bip32Seed, derivationType)
                    keyManagerMap[derivationType] = HDAccountKeyManager.createNew(root, networkParameters, accountIndex,
                            secureStore, AesKeyCipher.defaultKeyCipher(), derivationType)
                }

                // Generate the context for the account
                val btcAccountId = keyManagerMap[BipDerivationType.BIP44]!!.accountId
                val accountContext = BitcoinVaultHDAccountContext(btcAccountId.generateChild(coinType),
                        coinType, accountIndex, false, "Bitcoin Vault ${getCurrentBip44Index()}",
                        Balance.getZeroBalance(coinType), backing::updateAccountContext)

                backing.createAccountContext(accountContext)
                result = BitcoinVaultHDAccount(accountContext, keyManagerMap, networkParameters,
                        BitcoinVaultHDAccountBacking(walletDB, accountContext.uuid, coinType),
                        accountListener)
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

        fun UUID.generateChild(type: CryptoCurrency): UUID {
            val byteWriter = ByteWriter(36)
            byteWriter.putLongBE(mostSignificantBits)
            byteWriter.putLongBE(leastSignificantBits)
            byteWriter.putRawStringUtf8(type.id)
            return UUID.nameUUIDFromBytes(HashUtils.sha256(byteWriter.toBytes()).bytes)
        }
    }
}

fun WalletManager.getBTCVHDAccounts() = getAccounts().filter { it is BitcoinVaultHDAccount && it.isVisible }
fun WalletManager.getActiveBTCVAccounts() = getAccounts().filter { it is BitcoinVaultHDAccount && it.isVisible && it.isActive }