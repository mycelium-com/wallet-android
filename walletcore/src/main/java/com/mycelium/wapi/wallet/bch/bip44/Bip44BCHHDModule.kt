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


class Bip44BCHHDModule(internal val backing: WalletManagerBacking<SingleAddressAccountContext, BtcTransaction>,
                       internal val secureStore: SecureKeyValueStore,
                       internal val networkParameters: NetworkParameters,
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
        @JvmField
        val ID: String = "Bip44HD"
    }
}

/**
 * Get Bitcoin Cash HD-accounts
 *
 * @return list of accounts
 */
fun WalletManager.getBCHBip44Accounts() = getAccounts().filter { it is Bip44BCHAccount && it.isVisible }