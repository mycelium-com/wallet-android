package com.mycelium.wapi.wallet.btc.bip44

import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.bip44.ChangeAddressMode
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.SecureKeyValueStore
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.BtcTransaction
import com.mycelium.wapi.wallet.btc.WalletManagerBacking
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext.Companion.ACCOUNT_TYPE_UNRELATED_X_PRIV
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext.Companion.ACCOUNT_TYPE_UNRELATED_X_PUB
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccountContext
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import java.util.*


class BitcoinHDModule(internal val backing: WalletManagerBacking<SingleAddressAccountContext, BtcTransaction>
                      , internal val secureStore: SecureKeyValueStore
                      , internal val networkParameters: NetworkParameters
                      , internal var _wapi: Wapi) : WalletModule {

    private val MASTER_SEED_ID = HexUtils.toBytes("D64CA2B680D8C8909A367F28EB47F990")

    private val accounts = mutableMapOf<UUID, HDAccount>()

    override fun getId(): String = "BitcoinHD"

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        return mapOf()
    }

    override fun createAccount(config: Config): WalletAccount<*, *>? {
        var result: WalletAccount<*, *>? = null
        if (config is HDConfig) {
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
            backing.beginTransaction()
            try {

                // Generate the context for the account
                val context: HDAccountContext
                if (cfg.hdKeyNodes.get(0).isPrivateHdKeyNode) {
                    context = HDAccountContext(id, accountIndex, false, ACCOUNT_TYPE_UNRELATED_X_PRIV,
                            secureStorage.subId, derivationTypes)
                } else {
                    context = HDAccountContext(id, accountIndex, false, ACCOUNT_TYPE_UNRELATED_X_PUB,
                            secureStorage.subId, derivationTypes)
                }
                if (isUpgrade) {
                    backing.upgradeBip44AccountContext(context)
                } else {
                    backing.createBip44AccountContext(context)
                }
                // Get the backing for the new account
                val accountBacking = backing.getBip44AccountBacking(context.id)

                // Create actual account
                result = if (cfg.hdKeyNodes.get(0).isPrivateHdKeyNode) {
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
        }

        accounts.put(result!!.id, result as HDAccount)
        return result
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
        if (getNextBip44Index() === 0) {
            // First account not created
            return true
        }
        // We can add an additional account if the last account had activity
        val last = accounts.values.last()
        return last.hasHadActivity()
    }
    override fun canCreateAccount(config: Config): Boolean = config is HDConfig


    override fun deleteAccount(walletAccount: WalletAccount<*, *>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}